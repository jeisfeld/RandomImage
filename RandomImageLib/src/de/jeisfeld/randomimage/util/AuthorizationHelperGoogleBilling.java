package de.jeisfeld.randomimage.util;

import de.jeisfeld.randomimagelib.R;

/**
 * Class for identification of premium status in case of standard app with Google Billing.
 */
public final class AuthorizationHelperGoogleBilling extends AuthorizationHelper {
	/**
	 * An instance of this class.
	 */
	private static volatile AuthorizationHelperGoogleBilling mInstance;

	/**
	 * Keep constructor private.
	 */
	private AuthorizationHelperGoogleBilling() {
	}

	/**
	 * Get an instance of this class.
	 *
	 * @return An instance of this class.
	 */
	public static AuthorizationHelper getInstance() {
		if (mInstance == null) {
			mInstance = new AuthorizationHelperGoogleBilling();
		}
		return mInstance;
	}

	@Override
	public boolean hasPremiumImpl() {
		return PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_has_premium);
	}

	@Override
	public boolean requiresGoogleBillingImpl() {
		return true;
	}
}
