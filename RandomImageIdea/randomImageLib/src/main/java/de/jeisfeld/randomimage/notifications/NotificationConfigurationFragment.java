package de.jeisfeld.randomimage.notifications;

import java.util.ArrayList;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.ConfigureImageListActivity;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.widgets.WidgetSettingsActivity;
import de.jeisfeld.randomimagelib.R;

/**
 * Fragment for displaying the settings of the image widget.
 */
public class NotificationConfigurationFragment extends PreferenceFragment {
	/**
	 * The resource key for the notification id.
	 */
	protected static final String STRING_NOTIFICATION_ID = "de.eisfeldj.randomimage.NOTIFICATION_ID";

	/**
	 * The notification id.
	 */
	private int mNotificationId;

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private OnWidgetPreferenceChangeListener mOnPreferenceChangeListener = new OnWidgetPreferenceChangeListener();

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mNotificationId = getArguments().getInt(STRING_NOTIFICATION_ID);

		setDefaultValues();
		setNonIndexedValues();

		addPreferencesFromResource(R.xml.pref_notification);

		configureListNameProperty();
		bindPreferenceSummaryToValue(R.string.key_notification_frequency);
		addEditListListener();
		addCancelNotificationListener();
	}

	/**
	 * Add the listener for the button to edit the image list.
	 */
	private void addEditListListener() {
		Preference editListPreference = findPreference(getString(R.string.key_pref_edit_list));

		editListPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				ConfigureImageListActivity.startActivity(getActivity(),
						PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mNotificationId));
				return true;
			}
		});
	}

	/**
	 * Add the listener for the button to cancel the notification.
	 */
	private void addCancelNotificationListener() {
		Preference cancelNotificationPreference = findPreference(getString(R.string.key_dummy_cancel_notification));

		cancelNotificationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				PreferenceUtil.removeIndexedSharedPreference(R.string.key_notification_list_name, mNotificationId);
				PreferenceUtil.removeIndexedSharedPreference(R.string.key_notification_frequency, mNotificationId);
				NotificationSettingsActivity.removeNotificationId(mNotificationId);

				NotificationAlarmReceiver.cancelAlarm(getActivity(), mNotificationId);

				updateHeader();

				getActivity().finish();
				return true;
			}
		});
	}

	/**
	 * Set the default values of preferences if not yet given.
	 *
	 * @return true if some value needed to be set.
	 */
	protected final boolean setDefaultValues() {
		boolean isUpdated = false;
		String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId);
		if (listName == null) {
			String currentListName = ImageRegistry.getCurrentListName();
			if (currentListName != null) {
				isUpdated = true;
				PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId, currentListName);
			}
		}
		long alarmInterval = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_frequency, mNotificationId, -1);
		if (alarmInterval == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_frequency, mNotificationId,
					Long.parseLong(getActivity().getString(R.string.pref_default_notification_frequency)));
		}

		if (!NotificationSettingsActivity.getNotificationIds().contains(mNotificationId)) {
			NotificationSettingsActivity.addNotificationId(mNotificationId);
			int maxNotificationId = PreferenceUtil.getSharedPreferenceInt(R.string.key_notification_max_id, -1);
			if (mNotificationId > maxNotificationId) {
				PreferenceUtil.setSharedPreferenceInt(R.string.key_notification_max_id, mNotificationId);
			}

			NotificationAlarmReceiver.setAlarm(getActivity(), mNotificationId,
					PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_frequency, mNotificationId, -1));

			updateHeader();

			isUpdated = true;
		}
		return isUpdated;
	}

	/**
	 * Set the non-indexed preferences to the same values as the indexed ones, so that the views behave well.
	 */
	private void setNonIndexedValues() {
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_list_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId));
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_frequency,
				Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_frequency, mNotificationId, -1)));
	}

	/**
	 * Configure the property for the list name.
	 */
	private void configureListNameProperty() {
		ListPreference preference = (ListPreference) findPreference(getString(R.string.key_notification_list_name));

		ArrayList<String> listNameList = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
		String[] listNames = listNameList.toArray(new String[listNameList.size()]);

		preference.setEntries(listNames);
		preference.setEntryValues(listNames);

		bindPreferenceSummaryToValue(R.string.key_notification_list_name);
	}

	/**
	 * Binds a preference's summary to its value. More specifically, when the preference's value is changed, its summary
	 * (line of text below the preference title) is updated to reflect the value. The summary is also immediately
	 * updated upon calling this method. The exact display format is dependent on the type of preference.
	 *
	 * @param preferenceKey The key of the preference to be bound.
	 */
	private void bindPreferenceSummaryToValue(final int preferenceKey) {
		Preference preference = findPreference(getString(preferenceKey));

		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		String value;
		if (preferenceKey == R.string.key_notification_frequency) {
			value = Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(preferenceKey, mNotificationId, -1));
		}
		else {
			value = PreferenceUtil.getIndexedSharedPreferenceString(preferenceKey, mNotificationId);
		}

		// Trigger the listener immediately with the preference's current value.
		mOnPreferenceChangeListener.setSummary(preference, value);
	}

	/**
	 * Update the preference header after changing the settings.
	 */
	private void updateHeader() {
		NotificationSettingsActivity.updateHeader(getArguments().getInt(WidgetSettingsActivity.STRING_HASH_CODE, 0), mNotificationId);
	}

	/**
	 * Get the frequency of a notification as String.
	 *
	 * @param notificationId The notification id
	 * @return The frequency as String.
	 */
	public static String getNotificationFrequencyString(final int notificationId) {
		long frequency = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_frequency, notificationId, -1);
		if (frequency <= 0) {
			return null;
		}
		String[] frequencyValues = Application.getAppContext().getResources().getStringArray(R.array.notification_frequency_values);
		String[] frequencyNames = Application.getAppContext().getResources().getStringArray(R.array.notification_frequency_names);
		for (int i = 0; i < frequencyValues.length; i++) {
			if (Long.parseLong(frequencyValues[i]) == frequency) {
				return frequencyNames[i];
			}
		}
		return null;
	}

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private class OnWidgetPreferenceChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(final Preference preference, final Object value) {
			String stringValue = value.toString();
			setSummary(preference, stringValue);

			if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_list_name))) {
				PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId, stringValue);
				updateHeader();
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_frequency))) {
				PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_frequency, mNotificationId, Long.parseLong(stringValue));
				updateHeader();
				NotificationAlarmReceiver.setAlarm(getActivity(), mNotificationId, Long.parseLong(stringValue));
			}

			return true;
		}

		/**
		 * Set the summary of the preference.
		 *
		 * @param preference The preference.
		 * @param value      The value of the preference.
		 */
		private void setSummary(final Preference preference, final String value) {
			// set summary
			if (preference.getClass().equals(ListPreference.class)) {
				// For list preferences (except customized ones), look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(value);

				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
			}
			else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(value);
			}
		}
	}

}
