package de.jeisfeld.randomimage.util;

/**
 * Class for identification of premium status in case of RandomimagePro app.
 */
public final class AuthorizationHelperPro extends AuthorizationHelper {
	/**
	 * An instance of this class.
	 */
	private static volatile AuthorizationHelperPro mInstance;

	/**
	 * Keep constructor private.
	 */
	private AuthorizationHelperPro() {
	}

	/**
	 * Get an instance of this class.
	 *
	 * @return An instance of this class.
	 */
	public static AuthorizationHelper getInstance() {
		if (mInstance == null) {
			mInstance = new AuthorizationHelperPro();
		}
		return mInstance;
	}

	@Override
	public boolean hasPremiumImpl() {
		return true;
	}

	@Override
	public boolean requiresGoogleBillingImpl() {
		return false;
	}
}
