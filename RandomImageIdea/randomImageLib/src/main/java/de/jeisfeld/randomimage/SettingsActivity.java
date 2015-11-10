package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import de.jeisfeld.randomimage.util.AuthorizationHelper;
import de.jeisfeld.randomimage.util.GoogleBillingHelper;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display the settings page.
 */
public class SettingsActivity extends Activity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 4;
	/**
	 * The fragment tag.
	 */
	private static final String FRAGMENT_TAG = "FRAGMENT_TAG";
	/**
	 * The resource key for the flag if a premium pack has been bought.
	 */
	private static final String STRING_RESULT_BOUGHT_PREMIUM = "de.jeisfeld.randomimage.BOUGHT_PREMIUM";

	/**
	 * Utility method to start the activity.
	 *
	 * @param activity The activity from which the activity is started.
	 */
	public static final void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, SettingsActivity.class);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the SettingsFragment as the main content.

		SettingsFragment fragment = (SettingsFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
		if (fragment == null) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment(), FRAGMENT_TAG)
					.commit();
			getFragmentManager().executePendingTransactions();

			if (savedInstanceState == null) {
				PreferenceUtil.incrementCounter(R.string.key_statistics_countsettings);
			}
		}

	}

	/**
	 * Set the default shared preferences (after first installation).
	 *
	 * @param context The Context in which the preferences should be set.
	 */
	public static final void setDefaultSharedPreferences(final Context context) {
		PreferenceManager.setDefaultValues(Application.getAppContext(), R.xml.pref_general, false);
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (AuthorizationHelper.requiresGoogleBilling()) {
			GoogleBillingHelper.handleActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public final void onDestroy() {
		super.onDestroy();
		if (AuthorizationHelper.requiresGoogleBilling()) {
			GoogleBillingHelper.dispose();
		}
	}

	/**
	 * Static helper method to extract the result flag.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the flag if a premium pack has been bought.
	 */
	public static final boolean getResultBoughtPremium(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_BOUGHT_PREMIUM);
		}
		else {
			return false;
		}
	}

	/**
	 * Helper method: Return the flag if a premium pack has been bought.
	 *
	 * @param boughtPremium The flag if a premium pack has been bought.
	 */
	protected final void returnResult(final boolean boughtPremium) {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_BOUGHT_PREMIUM, boughtPremium);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
	}
}
