package com.example.shared.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Credential Manager + Firebase Auth Google Sign-In helper.
 * Uses strictly 1 option per GetCredentialRequest and avoids anonymous auth.
 */
object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    suspend fun attemptSilentSignIn(
        context: Context,
        webClientId: String,
        auth: FirebaseAuth = FirebaseAuth.getInstance()
    ): FirebaseUser? {
        auth.currentUser?.let { return it }
        if (webClientId.isBlank()) return null

        return try {
            val credentialManager = CredentialManager.create(context)
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = credentialManager.getCredential(context, request)
            val cred = response.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleId = GoogleIdTokenCredential.createFrom(cred.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleId.idToken, null)
                auth.signInWithCredential(firebaseCred).await().user
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google Sign-In skipped: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogle(
        activityContext: Context,
        webClientId: String,
        auth: FirebaseAuth = FirebaseAuth.getInstance()
    ): Result<FirebaseUser> {
        if (webClientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Sign-In client ID is missing from configuration."))
        }
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val cred = result.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(cred.data)
                val authCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val signedInUser = auth.signInWithCredential(authCred).await().user
                    ?: return Result.failure(IllegalStateException("Firebase Google authentication returned no user."))
                Result.success(signedInUser)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type received."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google Sign-In failed: ${e.message}", e)
            Result.failure(IllegalStateException("Google Sign-In failed: ${e.localizedMessage ?: "Try again"}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Google Sign-In error", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(
        context: Context,
        auth: FirebaseAuth = FirebaseAuth.getInstance()
    ) {
        auth.signOut()
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
