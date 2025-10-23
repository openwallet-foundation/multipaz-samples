package org.multipaz.photoididentityreader

import io.ktor.client.engine.HttpClientEngineFactory

actual suspend fun signInWithGoogle(
    explicitSignIn: Boolean,
    serverClientId: String,
    nonce: String,
    httpClientEngineFactory: HttpClientEngineFactory<*>,
): Pair<String, SignInWithGoogleUserData> {
    TODO()
}

actual suspend fun signInWithGoogleSignedOut() {
    TODO()
}
