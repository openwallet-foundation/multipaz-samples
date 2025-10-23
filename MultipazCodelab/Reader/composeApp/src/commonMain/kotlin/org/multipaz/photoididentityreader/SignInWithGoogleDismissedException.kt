package org.multipaz.photoididentityreader

/**
 * Thrown by [signInWithGoogle] if the user dismisses the UI prompt.
 */
class SignInWithGoogleDismissedException(message: String, cause: Throwable?): Exception(message, cause)
