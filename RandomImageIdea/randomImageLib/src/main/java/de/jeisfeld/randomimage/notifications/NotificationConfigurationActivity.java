package de.jeisfeld.randomimage.notifications;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.jeisfeld.randomimage.StartActivity;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.NotificationPermissionUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity for the configuration of a notification.
 */
public class NotificationConfigurationActivity extends StartActivity {
	/**
	 * The fragment tag.
	 */
	private static final String FRAGMENT_TAG = "FRAGMENT_TAG";

	/**
	 * The request code used to query for notification permission.
	 */
	private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 4;

	/**
	 * The Intent used as result.
	 */
	private Intent mResultValue;

	@Override
	protected final boolean shouldRequestNotificationPermissionOnStartup() {
		return false;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!NotificationPermissionUtil.hasNotificationPermission(this)) {
			requestNotificationPermission();
			return;
		}
		getWindow().getDecorView().setFitsSystemWindows(true);

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
	 * Request the notification permission after explaining why it is required.
	 */
	private void requestNotificationPermission() {
		DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
			@Override
			public void onDialogPositiveClick(final DialogFragment dialog) {
				NotificationPermissionUtil.requestNotificationPermission(NotificationConfigurationActivity.this, REQUEST_CODE_NOTIFICATION_PERMISSION);
			}

			@Override
			public void onDialogNegativeClick(final DialogFragment dialog) {
				finish();
			}
		}, R.string.title_dialog_request_permission, R.string.button_continue, R.string.dialog_confirmation_need_notification_permission);
	}

	@Override
	public final void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
		if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
			if (NotificationPermissionUtil.hasNotificationPermission(this)) {
				recreate();
			}
			else {
				DialogUtil.displayInfo(this, () -> finish(), 0, R.string.dialog_confirmation_need_notification_permission);
			}
		}
		else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
