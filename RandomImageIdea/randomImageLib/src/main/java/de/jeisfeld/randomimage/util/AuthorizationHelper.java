package de.jeisfeld.randomimage.util;

import android.util.Log;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimagelib.R;

/**
 * Class for identification of premium status.
 */
public abstract class AuthorizationHelper {
	/**
	 * Get an instance of the AuthorizationHelper.
	 *
	 * @return An instance of the AuthorizationHelper.
	 */
	public static AuthorizationHelper getInstance() {
		try {
			return (AuthorizationHelper) Class.forName(Application.getResourceString(R.string.authorization_class))
					.getDeclaredMethod("getInstance", new Class<?>[0])
					.invoke(null, new Object[0]);
		}
		catch (Exception e) {
			Log.e(Application.TAG, "Error in getting PrivateConstants and ApplicationSettings", e);
			return null;
		}
	}

	/**
	 * Method to find out if the user has premium access.
	 *
	 * @return true if he has premium access.
	 */
	public static boolean hasPremium() {
		return getInstance().hasPremiumImpl();
	}

	/**
	 * Method to identify if Google Billing is required.
	 *
	 * @return true if Google Billing is required.
	 */
	public static boolean requiresGoogleBilling() {
		return getInstance().requiresGoogleBillingImpl();
	}

	/**
	 * Method to find out if the user has premium access.
	 *
	 * @return true if he has premium access.
	 */
	public abstract boolean hasPremiumImpl();

	/**
	 * Method to identify if Google Billing is required.
	 *
	 * @return true if Google Billing is required.
	 */
	public abstract boolean requiresGoogleBillingImpl();
}
