package de.jeisfeld.randomimage.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display the settings page.
 */
public class NotificationSettingsActivity extends PreferenceActivity {
	/**
	 * Resource String for the hash code parameter used to identify the instance of the activity.
	 */
	public static final String STRING_HASH_CODE = "de.jeisfeld.randomimage.HASH_CODE";

	/**
	 * The headers that are displayed.
	 */
	private List<Header> mHeaders = null;

	/**
	 * A temporary storage of the last updated notification id.
	 */
	private Integer mUpdatedNotificationId = null;

	/**
	 * A map allowing to get the activity from its hashCode.
	 */
	private static Map<Integer, NotificationSettingsActivity> mActivityMap = new HashMap<>();

	/**
	 * Utility method to start the activity.
	 *
	 * @param activity The activity from which the activity is started.
	 */
	public static final void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, NotificationSettingsActivity.class);
		activity.startActivity(intent);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mActivityMap.put(hashCode(), this);
	}

	@Override
	protected final void onDestroy() {
		mActivityMap.remove(hashCode());
		super.onDestroy();
	}

	@Override
	public final void onBuildHeaders(final List<Header> target) {
		List<Integer> notificationIds = getNotificationIds();
		for (int i = 0; i < notificationIds.size(); i++) {
			target.add(createHeaderForNotification(notificationIds.get(i), i));
		}

		// Add header for creating a new notification.
		target.add(createHeaderForNotification(PreferenceUtil.getSharedPreferenceInt(R.string.key_notification_max_id, 0) + 1, -1));

		mHeaders = target;

		if (mUpdatedNotificationId != null) {
			if (isMultiPane()) {
				switchToHeader(mUpdatedNotificationId);
			}
			mUpdatedNotificationId = null;
		}
	}

	/**
	 * Get the ids of existing notifications.
	 *
	 * @return The notification ids.
	 */
	protected static final List<Integer> getNotificationIds() {
		List<String> notificationIdStrings = PreferenceUtil.getSharedPreferenceStringList(R.string.key_notification_ids);
		List<Integer> notificationIds = new ArrayList<>();
		for (String idString : notificationIdStrings) {
			notificationIds.add(Integer.valueOf(idString));
		}
		return notificationIds;
	}

	/**
	 * Set the ids of existing notifications.
	 *
	 * @param notificationIds the notification ids.
	 */
	private static void setNotificationIds(final List<Integer> notificationIds) {
		List<String> notificationIdStrings = new ArrayList<>();
		for (int id : notificationIds) {
			notificationIdStrings.add(Integer.toString(id));
		}
		PreferenceUtil.setSharedPreferenceStringList(R.string.key_notification_ids, notificationIdStrings);
	}

	/**
	 * Add an id to the list of notificationIds.
	 *
	 * @param notificationId The notificationId to be added.
	 */
	protected static final void addNotificationId(final int notificationId) {
		List<Integer> notificationIds = getNotificationIds();
		if (!notificationIds.contains(notificationId)) {
			notificationIds.add(notificationId);
		}
		setNotificationIds(notificationIds);
	}

	/**
	 * Add an id from the list of notificationIds.
	 *
	 * @param notificationId The notificationId to be removed.
	 */
	protected static final void removeNotificationId(final int notificationId) {
		List<Integer> notificationIds = getNotificationIds();
		// Type cast required in order to access object instead of index.
		notificationIds.remove((Integer) notificationId);
		setNotificationIds(notificationIds);
	}

	@Override
	protected final boolean isValidFragment(final String fragmentName) {
		return fragmentName.startsWith("de.jeisfeld.randomimage");
	}

	/**
	 * Create a preference header for a notification.
	 *
	 * @param notificationId The notification id.
	 * @param index          The index of the notification, or -1 for the creation of a new notification.
	 * @return the preference header.
	 */
	private Header createHeaderForNotification(final int notificationId, final int index) {
		Header header = new Header();
		header.id = notificationId;
		header.fragment = "de.jeisfeld.randomimage.notifications.NotificationConfigurationFragment";

		Bundle arguments = new Bundle();
		arguments.putInt(NotificationConfigurationFragment.STRING_NOTIFICATION_ID, notificationId);
		arguments.putInt(STRING_HASH_CODE, hashCode());
		header.fragmentArguments = arguments;

		if (index < 0) {
			header.title = getString(R.string.pref_heading_create_new_notifcation);
		}
		else {
			header.title = getString(R.string.pref_heading_notifcation) + " " + (index + 1);
			header.summary = getHeaderSummary(notificationId);
		}
		return header;
	}

	/**
	 * Update the preference header for one entry.
	 *
	 * @param hashCode       The code identifying the owning activity.
	 * @param notificationId The notification id of the entry.
	 */
	protected static void updateHeader(final int hashCode, final int notificationId) {
		NotificationSettingsActivity activity = mActivityMap.get(hashCode);
		if (activity != null) {
			//			for (Header header : activity.mHeaders) {
			//				if (header.id == notificationId) {
			//					header.summary = getHeaderSummary(notificationId);
			//				}
			//			}
			activity.mUpdatedNotificationId = notificationId;
			activity.invalidateHeaders();
		}
	}

	/**
	 * Switch to the header with the given notificationId.
	 *
	 * @param notificationId The notificationId.
	 */
	final void switchToHeader(final int notificationId) {
		for (Header header : mHeaders) {
			if (header.id == notificationId) {
				switchToHeader(header);
				return;
			}
		}
	}

	/**
	 * Get the summary of the header entry for a notification.
	 *
	 * @param notificationId The notification id.
	 * @return The header summary
	 */
	private static String getHeaderSummary(final int notificationId) {
		String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_list_name, notificationId);
		String frequencyString = NotificationConfigurationFragment.getNotificationFrequencyString(notificationId);
		if (frequencyString == null) {
			return listName;
		}
		else {
			return listName + " (" + frequencyString + ")";
		}
	}
}