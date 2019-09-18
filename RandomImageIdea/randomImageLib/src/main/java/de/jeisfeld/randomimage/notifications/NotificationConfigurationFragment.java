package de.jeisfeld.randomimage.notifications;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.jeisfeld.randomimage.ConfigureImageListActivity;
import de.jeisfeld.randomimage.util.DateUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.view.TimeSelectorPreference;
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
		handleNotificationCreation();

		configureListNameProperty();
		bindPreferenceSummaryToValue(R.string.key_notification_timer_duration);
		bindPreferenceSummaryToValue(R.string.key_notification_timer_variance);
		bindPreferenceSummaryToValue(R.string.key_notification_daily_start_time);
		bindPreferenceSummaryToValue(R.string.key_notification_daily_end_time);
		bindPreferenceSummaryToValue(R.string.key_notification_duration);
		bindPreferenceSummaryToValue(R.string.key_notification_duration_variance);
		bindPreferenceSummaryToValue(R.string.key_notification_style);
		bindPreferenceSummaryToValue(R.string.key_notification_led_color);
		bindPreferenceSummaryToValue(R.string.key_notification_colored_icon);
		bindPreferenceSummaryToValue(R.string.key_notification_display_name);
		bindPreferenceSummaryToValue(R.string.key_notification_detail_use_default);
		bindPreferenceSummaryToValue(R.string.key_notification_detail_scale_type);
		bindPreferenceSummaryToValue(R.string.key_notification_detail_background);
		bindPreferenceSummaryToValue(R.string.key_notification_detail_flip_behavior);
		bindPreferenceSummaryToValue(R.string.key_notification_detail_change_with_tap);
		bindPreferenceSummaryToValue(R.string.key_notification_detail_prevent_screenlock);
		addEditListListener();
		addCancelNotificationListener();
		updatePropertyEnablement();

		if (((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
			bindPreferenceSummaryToValue(R.string.key_notification_vibration);
		}
		else {
			((PreferenceGroup) findPreference(getString(R.string.key_dummy_pref_group_appearance)))
					.removePreference(findPreference(getString(R.string.key_notification_vibration)));
		}
	}

	@Override
	public final void onResume() {
		super.onResume();
		TrackingUtil.sendScreen(getActivity());
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
						PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId),
						"from Notification Config");
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

		// allow to display next timer occurrence
		// TODO: remove after debug phase
		boolean showTimer = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_show_list_notification);
		if (showTimer) {
			long nextTime = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_current_alarm_timestamp, mNotificationId, -1);
			if (nextTime >= 0) {
				Date date = new Date(nextTime);
				cancelNotificationPreference.setSummary(DateUtil.format(date, "yyyy-MM-dd HH:mm:ss"));
			}
		}
	}

	/**
	 * Set the default values of preferences if not yet given.
	 *
	 * @return true if some value needed to be set.
	 */
	protected final boolean setDefaultValues() {
		boolean isUpdated = false;
		if (PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId) == null) {
			String currentListName = ImageRegistry.getCurrentListName();
			if (currentListName != null) {
				isUpdated = true;
				PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId, currentListName);
			}
		}
		if (PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_timer_duration, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_timer_duration, mNotificationId,
					PreferenceUtil.getSharedPreferenceLongString(R.string.key_notification_timer_duration,
							R.string.pref_default_notification_timer_duration));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_timer_variance,
							R.string.pref_default_notification_timer_variance));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_daily_start_time,
							R.string.pref_default_notification_daily_start_time));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_daily_end_time,
							R.string.pref_default_notification_daily_end_time));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_duration, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_duration, mNotificationId,
					PreferenceUtil.getSharedPreferenceLongString(R.string.key_notification_duration, R.string.pref_default_notification_duration));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_duration_variance, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_duration_variance, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_duration_variance,
							R.string.pref_default_notification_duration_variance));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_style, R.string.pref_default_notification_style));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_led_color, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_led_color, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_notification_led_color, R.string.pref_default_notification_led_color));
		}
		if (!PreferenceUtil.hasIndexedSharedPreference(R.string.key_notification_detail_use_default, mNotificationId)) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_detail_use_default, mNotificationId, true);
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_scale_type,
							R.string.pref_default_detail_scale_type));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_background, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_detail_background, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_background,
							R.string.pref_default_detail_background));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_flip_behavior, mNotificationId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_detail_flip_behavior, mNotificationId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_flip_behavior,
							R.string.pref_default_detail_flip_behavior));
		}
		if (!PreferenceUtil.hasIndexedSharedPreference(R.string.key_notification_detail_change_with_tap, mNotificationId)) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_detail_change_with_tap, mNotificationId,
					PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_detail_change_with_tap));
		}
		if (!PreferenceUtil.hasIndexedSharedPreference(R.string.key_notification_detail_prevent_screenlock, mNotificationId)) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_detail_prevent_screenlock, mNotificationId,
					PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_detail_prevent_screenlock));
		}

		return isUpdated;
	}

	/**
	 * Handle the initial triggering of a new notification, including the query for a list name.
	 */
	private void handleNotificationCreation() {
		if (!NotificationSettingsActivity.getNotificationIds().contains(mNotificationId)) {
			ArrayList<String> listNames = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);

			if (listNames.size() == 0) {
				// On first startup need to create default list.
				ImageRegistry.getCurrentImageListRefreshed(true);
				String listName = ImageRegistry.getCurrentListName();

				ConfigureImageListActivity.startActivity(getActivity(), listName, "empty/Notification");
				getActivity().finish();
			}
			else if (listNames.size() == 1) {
				doAddNewNotification(listNames.get(0));
			}
			else {
				DialogUtil.displayListSelectionDialog(getActivity(), new SelectFromListDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
						PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId, text);
						findPreference(getString(R.string.key_notification_list_name)).setSummary(text);
						doAddNewNotification(text);
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						NotificationSettingsActivity.cancelNotification(mNotificationId);
						updateHeader();
						getActivity().finish();
					}
				}, R.string.title_dialog_select_list_name, listNames, R.string.dialog_select_list_for_notification);
			}
		}
	}

	/**
	 * Add the new notification.
	 *
	 * @param listName The list name to be used for the notification.
	 */
	private void doAddNewNotification(final String listName) {
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId, listName);
		findPreference(getString(R.string.key_notification_list_name)).setSummary(listName);

		NotificationSettingsActivity.addNotificationId(mNotificationId);
		int maxNotificationId = PreferenceUtil.getSharedPreferenceInt(R.string.key_notification_max_id, -1);
		if (mNotificationId > maxNotificationId) {
			PreferenceUtil.setSharedPreferenceInt(R.string.key_notification_max_id, mNotificationId);
		}
		NotificationAlarmReceiver.setAlarm(getActivity(), mNotificationId, false);
		updateHeader();
		NotificationUtil.createImageNotificationChannels();
	}

	/**
	 * Set the non-indexed preferences to the same values as the indexed ones, so that the views behave well.
	 */
	private void setNonIndexedValues() {
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_list_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_list_name, mNotificationId));
		PreferenceUtil.setSharedPreferenceLongString(R.string.key_notification_timer_duration,
				PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_timer_duration, mNotificationId, 0));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_timer_variance,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_daily_start_time,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_daily_end_time,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceLongString(R.string.key_notification_duration,
				PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_duration, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_duration_variance,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_duration_variance, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_style,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_led_color,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_led_color, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_notification_vibration,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_vibration, mNotificationId, false));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_notification_colored_icon,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_colored_icon, mNotificationId, false));
		PreferenceUtil.setSharedPreferenceString(R.string.key_notification_display_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_display_name, mNotificationId));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_notification_detail_use_default,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_detail_use_default, mNotificationId, false));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_detail_scale_type,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_detail_background,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_background, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_notification_detail_flip_behavior,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_flip_behavior, mNotificationId, -1));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_notification_detail_change_with_tap,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_detail_change_with_tap, mNotificationId, false));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_notification_detail_prevent_screenlock,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_detail_prevent_screenlock, mNotificationId, false));
	}

	/**
	 * Enable or disable properties in dependence of other properties.
	 */
	private void updatePropertyEnablement() {
		int notificationStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, -1);
		Preference ledPreference = findPreference(getString(R.string.key_notification_led_color));
		ledPreference.setEnabled(!NotificationUtil.isActivityNotificationStyle(notificationStyle));
		Preference coloredIconPreference = findPreference(getString(R.string.key_notification_colored_icon));
		coloredIconPreference.setEnabled(!NotificationUtil.isActivityNotificationStyle(notificationStyle));

		boolean useDefaultSettings =
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_detail_use_default, mNotificationId, false);
		findPreference(getString(R.string.key_notification_detail_scale_type)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_notification_detail_background)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_notification_detail_flip_behavior)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_notification_detail_change_with_tap)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_notification_detail_prevent_screenlock)).setEnabled(!useDefaultSettings);
	}

	/**
	 * Configure the property for the list name.
	 */
	private void configureListNameProperty() {
		ListPreference preference = (ListPreference) findPreference(getString(R.string.key_notification_list_name));

		ArrayList<String> listNameList = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
		String[] listNames = listNameList.toArray(new String[0]);

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
		if (preferenceKey == R.string.key_notification_timer_variance // BOOLEAN_EXPRESSION_COMPLEXITY
				|| preferenceKey == R.string.key_notification_daily_start_time
				|| preferenceKey == R.string.key_notification_daily_end_time
				|| preferenceKey == R.string.key_notification_duration_variance
				|| preferenceKey == R.string.key_notification_style
				|| preferenceKey == R.string.key_notification_led_color
				|| preferenceKey == R.string.key_notification_detail_scale_type
				|| preferenceKey == R.string.key_notification_detail_background
				|| preferenceKey == R.string.key_notification_detail_flip_behavior) {
			value = Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(preferenceKey, mNotificationId, -1));
		}
		else if (preferenceKey == R.string.key_notification_timer_duration
				|| preferenceKey == R.string.key_notification_duration) {
			value = Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(preferenceKey, mNotificationId, -1));
		}
		else if (preferenceKey == R.string.key_notification_vibration // BOOLEAN_EXPRESSION_COMPLEXITY
				|| preferenceKey == R.string.key_notification_colored_icon
				|| preferenceKey == R.string.key_notification_detail_use_default
				|| preferenceKey == R.string.key_notification_detail_change_with_tap
				|| preferenceKey == R.string.key_notification_detail_prevent_screenlock) {
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
	 * @param context the context.
	 * @param notificationId The notification id
	 * @return The frequency as String.
	 */
	public static String getNotificationFrequencyHeaderString(final Context context, final int notificationId) {
		long frequency = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_timer_duration, notificationId, 0);
		return TimeSelectorPreference.getDefaultSummaryFromValue(context, Long.toString(frequency));
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
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_timer_duration))) {
				PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_timer_duration, mNotificationId, Long.parseLong(stringValue));
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
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_duration))) {
				PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_duration, mNotificationId, Long.parseLong(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_duration_variance))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_duration_variance, mNotificationId,
						Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_style))) {
				NotificationUtil.deleteImageNotificationChannels();
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, Integer.parseInt(stringValue));
				NotificationUtil.createImageNotificationChannels();
				updatePropertyEnablement();
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_led_color))) {
				NotificationUtil.deleteImageNotificationChannels();
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_led_color, mNotificationId, Integer.parseInt(stringValue));
				NotificationUtil.createImageNotificationChannels();
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_vibration))) {
				NotificationUtil.deleteImageNotificationChannels();
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_vibration,
						mNotificationId, Boolean.parseBoolean(stringValue));
				NotificationUtil.createImageNotificationChannels();
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_colored_icon))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_colored_icon,
						mNotificationId, Boolean.parseBoolean(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_display_name))) {
				PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_notification_display_name, mNotificationId, stringValue);
				updateHeader();
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_detail_use_default))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_detail_use_default,
						mNotificationId, Boolean.parseBoolean(stringValue));
				updatePropertyEnablement();
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_detail_scale_type))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId,
						Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_detail_background))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_detail_background, mNotificationId,
						Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_detail_flip_behavior))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_detail_flip_behavior, mNotificationId,
						Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_detail_change_with_tap))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_detail_change_with_tap,
						mNotificationId, Boolean.parseBoolean(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_notification_detail_prevent_screenlock))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_notification_detail_prevent_screenlock,
						mNotificationId, Boolean.parseBoolean(stringValue));
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
				// For list preferences (except customized ones), look up the correct display value in the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(value);

				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
			}
			else if (preference instanceof TimeSelectorPreference) {
				preference.setSummary(((TimeSelectorPreference) preference).getSummaryFromValue(value));
			}
			else if (!(preference instanceof CheckBoxPreference)) {
				// For all other preferences, set the summary to the value's simple string representation.
				preference.setSummary(value);
			}
		}
	}

}
