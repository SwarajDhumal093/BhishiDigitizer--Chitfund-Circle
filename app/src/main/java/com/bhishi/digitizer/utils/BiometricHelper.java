package com.bhishi.digitizer.utils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.bhishi.digitizer.R;

/**
 * Wraps androidx.biometric so LoginActivity (and any future re-auth screens)
 * can trigger real fingerprint / face unlock with three lines of code.
 *
 * How it fits the login flow:
 *  1. First-ever login always happens with phone number + password (or OTP).
 *  2. On success, LoginActivity asks "enable fingerprint login next time?"
 *     and stores the choice via PrefsManager.setBiometricEnabled(true).
 *  3. On the NEXT app open, if biometric is enabled and the device has an
 *     enrolled fingerprint, the Login screen shows a fingerprint button.
 *     Tapping it calls BiometricHelper.authenticate(); on success we reuse
 *     the already-persisted FirebaseAuth session instead of asking for the
 *     password again.
 */
public class BiometricHelper {

    public interface Callback {
        void onSuccess();
        void onFailed(String reason);
    }

    /** Returns true only if the device has fingerprint/face hardware AND at least one is enrolled. */
    public static boolean isBiometricAvailable(AppCompatActivity activity) {
        BiometricManager manager = BiometricManager.from(activity);
        int result = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void authenticate(AppCompatActivity activity, Callback callback) {
        java.util.concurrent.Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt prompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        callback.onFailed(errString.toString());
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        // Fingerprint read but did not match -- let the user retry,
                        // BiometricPrompt keeps its own UI open automatically.
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_prompt_title))
                .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText("Use password instead")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        prompt.authenticate(promptInfo);
    }
}
