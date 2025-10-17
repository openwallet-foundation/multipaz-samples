package org.multipaz.photoididentityreader

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Signs a user in, using their Google account.
 *
 * The app should first obtain a nonce from its backend before calling this function.
 *
 * Calling this function will prompt the user to sign in with a Google account.
 *
 * When this completes the app should the returned googleIdToken to the backend for verification.
 *
 * When the user signs out, [signInWithGoogleSignedOut] should be called.
 *
 * This is a wrapper around the platform native Credential Manager, see
 * [this article](https://developer.android.com/identity/sign-in/credential-manager-siwg)
 * for the Android implementation.
 *
 * @param explicitSignIn should be set to `true` if the user explicitly clicked a button to sign into Google. Set
 *   to false if opportunistically signing in an user e.g. the first time the application runs.
 * @param serverClientId the clientId of the server side.
 * @param nonce the nonce to include in the returned googleIdToken.
 * @param httpClientEngineFactory a [HttpClientEngineFactory] used for retrieving the profile photo.
 * @return a pair where the first member is the googleIdToken for the nonce, to be sent to a server for
 *   verification and the second being the sign-in data about the user which can be used in the user interface
 * @throws SignInWithGoogleDismissedException if the user dismissed the sign-in dialog.
 * @throws Throwable if an error occurs.
 */
expect suspend fun signInWithGoogle(
    explicitSignIn: Boolean,
    serverClientId: String,
    nonce: String,
    httpClientEngineFactory: HttpClientEngineFactory<*>,
): Pair<String, SignInWithGoogleUserData>

/**
 * Should be called when a user previously signed in via [signInToGoogle] signs out.
 */
expect suspend fun signInWithGoogleSignedOut()
