package de.jeisfeld.randomimage.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.jeisfeld.randomimage.StartActivity;

/**
 * Activity for the configuration of a notification.
 */
public class NotificationConfigurationActivity extends StartActivity {
	/**
	 * The fragment tag.
	 */
	private static final String FRAGMENT_TAG = "FRAGMENT_TAG";

	/**
	 * The Intent used as result.
	 */
	private Intent mResultValue;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		// Retrieve the notification id.
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			finish();
			return;
		}
		final int notificationId = extras.getInt(NotificationConfigurationFragment.STRING_NOTIFICATION_ID, -1);
		// If they gave us an intent without the notification id, just bail.
		if (notificationId == -1) {
			finish();
			return;
		}
		mResultValue = new Intent();
		mResultValue.putExtra(NotificationConfigurationFragment.STRING_NOTIFICATION_ID, notificationId);
		setResult(false);

		PreferenceFragment fragment = (PreferenceFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
		if (fragment == null) {
			fragment = new NotificationConfigurationFragment();
			Bundle bundle = new Bundle();
			bundle.putInt(NotificationConfigurationFragment.STRING_NOTIFICATION_ID, notificationId);
			fragment.setArguments(bundle);

			getFragmentManager().beginTransaction().replace(android.R.id.content, fragment, FRAGMENT_TAG).commit();
			getFragmentManager().executePendingTransactions();
		}
	}

	/**
	 * Set the result of the activity.
	 *
	 * @param success true if widget successfully created.
	 */
	protected final void setResult(final boolean success) {
		setResult(success ? RESULT_OK : RESULT_CANCELED, mResultValue);
	}
}
