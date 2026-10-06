package com.itismob.s03.group7.leef.auth;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.data.FirebaseAccountRepository;

/** State of the Verify Email screen: checking, resending, and the wait between resends. */
public class VerifyEmailViewModel extends ViewModel {

    /** One-time result for the screen to react to. */
    enum Outcome {
        VERIFIED,
        NOT_VERIFIED_YET,
        EMAIL_SENT,
        SEND_FAILED,
        NETWORK,
        TOO_MANY_REQUESTS,
        SIGNED_OUT,
        UNKNOWN
    }

    static final long RESEND_COOLDOWN_MS = 60_000L;

    private final AccountRepository repository;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Outcome> outcome = new MutableLiveData<>();
    private boolean started;
    private long cooldownEndsAt;

    public VerifyEmailViewModel() {
        this(new FirebaseAccountRepository());
    }

    VerifyEmailViewModel(@NonNull AccountRepository repository) {
        this.repository = repository;
    }

    LiveData<Boolean> getBusy() {
        return busy;
    }

    /** Emits once per result; call {@link #outcomeHandled()} after reacting to it. */
    LiveData<Outcome> getOutcome() {
        return outcome;
    }

    void outcomeHandled() {
        outcome.setValue(null);
    }

    @Nullable
    String getEmail() {
        return repository.getSignedInEmail();
    }

    /** Call once when the screen first opens. */
    void start(boolean verificationEmailAlreadySent) {
        if (started) {
            return;
        }
        started = true;
        if (verificationEmailAlreadySent) {
            startCooldown();
        }
    }

    /** Milliseconds until another email may be requested (0 when allowed now). */
    long getCooldownRemainingMs() {
        return Math.max(0L, cooldownEndsAt - SystemClock.elapsedRealtime());
    }

    private void startCooldown() {
        cooldownEndsAt = SystemClock.elapsedRealtime() + RESEND_COOLDOWN_MS;
    }

    /**
     * Asks the server whether the link has been tapped. A silent check (when the screen returns to
     * the foreground) only reacts if the email is verified and stays quiet otherwise.
     */
    void checkVerified(boolean silent) {
        if (Boolean.TRUE.equals(busy.getValue())) {
            return;
        }
        if (!silent) {
            busy.setValue(true);
        }
        repository.checkEmailVerified(new AccountRepository.CheckVerificationCallback() {
            @Override
            public void onChecked(boolean verified) {
                busy.setValue(false);
                if (verified) {
                    outcome.setValue(Outcome.VERIFIED);
                } else if (!silent) {
                    outcome.setValue(Outcome.NOT_VERIFIED_YET);
                }
            }

            @Override
            public void onFailure(@NonNull AccountRepository.VerificationFailure failure) {
                busy.setValue(false);
                if (!silent || failure == AccountRepository.VerificationFailure.SIGNED_OUT) {
                    outcome.setValue(toOutcome(failure));
                }
            }
        });
    }

    void resend() {
        if (Boolean.TRUE.equals(busy.getValue()) || getCooldownRemainingMs() > 0) {
            return;
        }
        busy.setValue(true);
        repository.sendVerificationEmail(new AccountRepository.SendVerificationCallback() {
            @Override
            public void onSent() {
                startCooldown();
                busy.setValue(false);
                outcome.setValue(Outcome.EMAIL_SENT);
            }

            @Override
            public void onFailure(@NonNull AccountRepository.VerificationFailure failure) {
                busy.setValue(false);
                outcome.setValue(failure == AccountRepository.VerificationFailure.UNKNOWN
                        ? Outcome.SEND_FAILED : toOutcome(failure));
            }
        });
    }

    void signOut() {
        repository.signOut();
    }

    @NonNull
    private static Outcome toOutcome(@NonNull AccountRepository.VerificationFailure failure) {
        switch (failure) {
            case NETWORK:
                return Outcome.NETWORK;
            case TOO_MANY_REQUESTS:
                return Outcome.TOO_MANY_REQUESTS;
            case SIGNED_OUT:
                return Outcome.SIGNED_OUT;
            case UNKNOWN:
            default:
                return Outcome.UNKNOWN;
        }
    }
}
