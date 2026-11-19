/*
 * Open redirects, and why this file exists
 * --------------------------------------------------------------------------------
 * After a user signs in, the app sends them back to the page they were trying to
 * reach. That page is remembered as a path (for example "/portfolio") and handed
 * to the login page in the router's navigation state, under the name `from`.
 *
 * An "open redirect" is a weakness where an app trusts a value like that blindly.
 * If it can be set to "https://malicious.example" or "//malicious.example" (which
 * browsers can interpret as a different website), the user ends up on a lookalike
 * site straight after typing their password, and may be fooled into entering it 
 * again.
 *
 * As the value of `from` comes from user input, it's a good idea to validate it,
 * because redirecting to a value taken from stored input is exactly the situation
 * this weakness comes from. Someone could change something down the line turning
 * the value of `from` into a massive unchecked vulnerability.
 *
 * How it is used: SignIn.jsx calls safeRedirectPath(location.state?.from) and
 * passes the result to <Navigate>.
 * 
 * The rules: the value must be text, must start with a single "/", must not
 * contain a backslash, and must not be the login or register page (otherwise the
 * user would be sent back to the form they just finished). Anything else falls
 * back to "/", the home page.
 */
export function safeRedirectPath(from) {
  if (typeof from !== 'string') return '/';
  if (!from.startsWith('/') || from.startsWith('//') || from.includes('\\')) return '/';
  // Going back to the login or register page would loop.
  if (from.startsWith('/login') || from.startsWith('/register')) return '/';
  return from;
}