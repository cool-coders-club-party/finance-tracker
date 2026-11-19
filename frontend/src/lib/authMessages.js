/** 
 * Turns a failed register request into a message for the user.
 * `error` is normally an AuthApiError with a `status` (0 = network failure).
 */
export function registrationErrorMessage(error) {
    /**
     * ? is the optional chaining operator. If error is null or undefined,
     * the expression gives undefined instead of crashing so the default 
     * case can be reached
     */ 
    switch (error?.status) {
        case 409:
            return 'An account with this email already exists. Try signing in instead.';
        case 400:
            return 'The server rejected those details. Check your email and use a password of 8 to 72 characters.';
        case 0:
            return 'Cannot reach the server. Please try again.';
        default:
            return 'Registration failed. Please try again.';
  }
}