package com.itismob.s03.group7.leef.auth;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/**
 * Shows Google's account sheet (Android Credential Manager) and hands the resulting ID token to the
 * {@link GoogleSignInViewModel}, which exchanges it for a Firebase sign-in. Only the ViewModel is
 * called back, so the result survives the screen being rotated while the sheet is open.
 */
final class GoogleSignInHelper {

    /** Why no ID token was obtained. */
    enum Failure {
        /** The person closed the sheet. Not an error worth a message. */
        CANCELLED,
        /** There is no Google account on the phone. */
        NO_ACCOUNT,
        /** google-services.json doesn't contain the web client id yet (Google sign-in not enabled in Firebase). */
        NOT_CONFIGURED,
        UNKNOWN
    }

    private static final String TAG = "LeefGoogle";

    private GoogleSignInHelper() {
    }

    static void request(@NonNull Activity activity, @NonNull GoogleSignInViewModel target) {
        // The plugin generates this string once Google is enabled in Firebase and google-services.json
        // has been re-downloaded. Looked up by name so the app still builds before that is done.
        int clientIdRes = activity.getResources()
                .getIdentifier("default_web_client_id", "string", activity.getPackageName());
        if (clientIdRes == 0) {
            target.onTokenFailure(Failure.NOT_CONFIGURED);
            return;
        }

        GetSignInWithGoogleOption option =
                new GetSignInWithGoogleOption.Builder(activity.getString(clientIdRes)).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        CredentialManager.create(activity).getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                .equals(credential.getType())) {
                            try {
                                target.onToken(GoogleIdTokenCredential
                                        .createFrom(credential.getData()).getIdToken());
                            } catch (Exception e) {
                                // Kotlin library: a malformed token is not declared as a checked exception.
                                target.onTokenFailure(Failure.UNKNOWN);
                            }
                        } else {
                            target.onTokenFailure(Failure.UNKNOWN);
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.w(TAG, "Credential Manager error " + e.getType() + ": " + e.getMessage());
                        if (e instanceof GetCredentialCancellationException) {
                            target.onTokenFailure(Failure.CANCELLED);
                        } else if (e instanceof NoCredentialException) {
                            target.onTokenFailure(Failure.NO_ACCOUNT);
                        } else {
                            target.onTokenFailure(Failure.UNKNOWN);
                        }
                    }
                });
    }

    /** Makes Google forget which account was picked, so the next sign-in shows the chooser again. */
    static void forgetChosenAccount(@NonNull Context context) {
        CredentialManager.create(context).clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                ContextCompat.getMainExecutor(context),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                    }

                    @Override
                    public void onError(ClearCredentialException e) {
                    }
                });
    }
}
