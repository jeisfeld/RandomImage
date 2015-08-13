package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import de.jeisfeld.randomimage.util.GoogleBillingHelper;
import de.jeisfeld.randomimage.util.PreferenceUtil;

/**
 * Activity to display the settings page.
 */
public class SettingsActivity extends Activity {
	/**
	 * The fragment tag.
	 */
	private static final String FRAGMENT_TAG = "FRAGMENT_TAG";

	/**
	 * Utility method to start the activity.
	 *
	 * @param context
	 *            The context in which the activity is started.
	 */
	public static final void startActivity(final Context context) {
		Intent intent = new Intent(context, SettingsActivity.class);
		context.startActivity(intent);
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
	 * @param context
	 *            The Context in which the preferences should be set.
	 */
	public static final void setDefaultSharedPreferences(final Context context) {
		PreferenceManager.setDefaultValues(Application.getAppContext(), R.xml.pref_general, false);
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		GoogleBillingHelper.handleActivityResult(requestCode, resultCode, data);
	}

	@Override
	public final void onDestroy() {
		super.onDestroy();
		GoogleBillingHelper.dispose();
	}
}
