package com.itismob.s03.group7.leef.auth;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.MainActivity;
import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.data.FirebaseAccountRepository;

/** Decides where the app should open for someone who is already signed in. */
public final class SessionRouter {

    private SessionRouter() {
    }

    /**
     * Where a signed-in person belongs: the app once their email is verified, otherwise the verify
     * screen. Returns null when nobody is signed in (show onboarding).
     */
    @Nullable
    public static Intent resumeIntent(@NonNull Context context) {
        AccountRepository accounts = new FirebaseAccountRepository();
        if (!accounts.isSignedIn()) {
            return null;
        }
        return accounts.isSignedInAndVerified()
                ? homeIntent(context)
                : VerifyEmailActivity.createIntent(context, false);
    }

    /** The main app, as a fresh task so Back leaves the app instead of returning to sign-up screens. */
    @NonNull
    public static Intent homeIntent(@NonNull Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}
