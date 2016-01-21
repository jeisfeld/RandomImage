package de.jeisfeld.randomimage.notifications;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
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
	 * The number of hours per day.
	 */
	private static final int HOURS_PER_DAY = (int) TimeUnit.DAYS.toHours(1);

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
		bindPreferenceSummaryToValue(R.string.key_notification_timer_variance);
		bindPreferenceSummaryToValue(R.string.key_notification_daily_start_time);
		bindPreferenceSummaryToValue(R.string.key_notification_daily_end_time);
		bindPreferenceSummaryToValue(R.string.key_notification_style);
		bindPreferenceSummaryToValue(R.string.key_notification_led_color);
		bindPreferenceSummaryToValue(R.string.key_notification_vibration);
		bindPreferenceSummaryToValue(R.string.key_notification_colored_icon);
		bindPreferenceSummaryToValue(R.string.key_notification_detail_scale_type);
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
				NotificationSettingsActivity.cancelNotification(mNotificationId);
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
		int frequency = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_frequency, mNotificationId, 0);
		if (frequency == 0) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_frequency, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_frequency, R.string.pref_default_notification_frequency));
		}
		int timerVariance = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, mNotificationId, -1);
		if (timerVariance == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_timer_variance,
							R.string.pref_default_notification_timer_variance));
		}
		int dailyStartTime = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, mNotificationId, -1);
		if (dailyStartTime == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_daily_start_time,
							R.string.pref_default_notification_daily_start_time));
		}
		int dailyEndTime = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId, -1);
		if (dailyEndTime == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_daily_end_time,
							R.string.pref_default_notification_daily_end_time));
		}
		int style = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, -1);
		if (style == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_style, R.string.pref_default_notification_style));
		}
		int ledColor = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_led_color, mNotificationId, -1);
		if (ledColor == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_led_color, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_led_color, R.string.pref_default_notification_led_color));
		}
		int detailScaleType = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId, -1);
		if (detailScaleType == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_detail_scale_type,
							R.string.pref_default_notification_detail_scale_type));
		}

		// Handle initial triggering of the notification
		if (!NotificationSettingsActivity.getNotificationIds().contains(mNotificationId)) {
			NotificationSettingsActivity.addNotificationId(mNotificationId);
			int maxNotificationId = PreferenceUtil.getSharedPreferenceInt(R.string.key_notification_max_id, -1);
			if (mNotificationId > maxNotificationId) {
				PreferenceUtil.setSharedPreferenceInt(R.string.key_notification_max_id, mNotificationId);
			}

			NotificationAlarmReceiver.setAlarm(getActivity(), mNotificationId, false);

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
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_frequency, mNotificationId, 0)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_timer_variance,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, mNotificationId, -1)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_daily_start_time,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, mNotificationId, -1)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_daily_end_time,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId, -1)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_style,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, -1)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_led_color,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_led_color, mNotificationId, -1)));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_notification_vibration,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_vibration, mNotificationId, false));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_notification_colored_icon,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_colored_icon, mNotificationId, false));
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_detail_scale_type,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId, -1)));
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
		if (preferenceKey == R.string.key_notification_frequency // BOOLEAN_EXPRESSION_COMPLEXITY
				|| preferenceKey == R.string.key_notification_timer_variance
				|| preferenceKey == R.string.key_notification_daily_start_time
				|| preferenceKey == R.string.key_notification_daily_end_time
				|| preferenceKey == R.string.key_notification_style
				|| preferenceKey == R.string.key_notification_led_color
				|| preferenceKey == R.string.key_notification_detail_scale_type) {
			value = Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(preferenceKey, mNotificationId, -1));
		}
		else if (preferenceKey == R.string.key_notification_vibration || preferenceKey == R.string.key_notification_colored_icon) {
			value = "";
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
		int frequency = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_frequency, notificationId, 0);
		if (frequency == 0) {
			return null;
		}
		String[] frequencyValues = Application.getAppContext().getResources().getStringArray(R.array.notification_frequency_values);
		String[] frequencyNames = Application.getAppContext().getResources().getStringArray(R.array.notification_frequency_names);
		for (int i = 0; i < frequencyValues.length; i++) {
			if (Integer.parseInt(frequencyValues[i]) == frequency) {
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
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_frequency, mNotificationId, Integer.parseInt(stringValue));
				updateHeader();
				NotificationAlarmReceiver.setAlarm(getActivity(), mNotificationId, false);
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_timer_variance))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, mNotificationId,
						Integer.parseInt(stringValue));
				NotificationAlarmReceiver.setAlarm(getActivity(), mNotificationId, false);
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_daily_start_time))) {
				int dailyEndTime = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId, -1);
				int dailyStartTime = Integer.parseInt(stringValue);
				if (dailyEndTime - dailyStartTime > HOURS_PER_DAY) {
					dailyEndTime = HOURS_PER_DAY;
					String endTimeValue = Integer.toString(dailyEndTime);
					ListPreference endTimePreference = (ListPreference) findPreference(getString(R.string.key_notification_daily_end_time));
					PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId, dailyEndTime);
					endTimePreference.setValue(endTimeValue);
					setSummary(endTimePreference, endTimeValue);
				}

				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, mNotificationId, dailyStartTime);
				NotificationAlarmReceiver.setAlarm(getActivity(), mNotificationId, false);
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_daily_end_time))) {
				int dailyStartTime = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, mNotificationId, -1);
				int dailyEndTime = Integer.parseInt(stringValue);
				if (dailyEndTime - dailyStartTime > HOURS_PER_DAY) {
					dailyEndTime = dailyStartTime + HOURS_PER_DAY;
					PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId, dailyEndTime);
					String newValue = Integer.toString(dailyEndTime);
					((ListPreference) preference).setValue(newValue);
					setSummary(preference, newValue);
					// do not update, as it is anyway overridden.
					return false;
				}
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId, dailyEndTime);
				NotificationAlarmReceiver.setAlarm(getActivity(), mNotificationId, false);
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_style))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_led_color))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_led_color, mNotificationId, Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_vibration))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_vibration, mNotificationId,
						Boolean.parseBoolean(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_colored_icon))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_colored_icon, mNotificationId,
						Boolean.parseBoolean(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_detail_scale_type))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId,
						Integer.parseInt(stringValue));
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
			else if (!(preference instanceof CheckBoxPreference)) {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(value);
			}
		}
	}

}
