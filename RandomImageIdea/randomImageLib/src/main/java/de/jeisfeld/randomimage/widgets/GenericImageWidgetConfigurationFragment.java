package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.ConfigureImageListActivity;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.widgets.GenericWidget.UpdateType;
import de.jeisfeld.randomimagelib.R;

/**
 * Fragment for displaying the settings of the image widget.
 */
public abstract class GenericImageWidgetConfigurationFragment extends PreferenceFragment {
	/**
	 * The app widget id.
	 */
	private int mAppWidgetId;

	protected final int getAppWidgetId() {
		return mAppWidgetId;
	}

	/**
	 * A flag indicating if the widget is already configured.
	 */
	private boolean mReconfigureWidget;

	protected final boolean isReconfigureWidget() {
		return mReconfigureWidget;
	}

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private OnWidgetPreferenceChangeListener mOnPreferenceChangeListener;

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mOnPreferenceChangeListener = createOnPreferenceChangeListener();
		mAppWidgetId = getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
		mReconfigureWidget = getArguments().getBoolean(GenericWidgetConfigurationActivity.EXTRA_RECONFIGURE_WIDGET, false);

		boolean isUpdated = setDefaultValues(getActivity(), mAppWidgetId);
		if (isUpdated) {
			mOnPreferenceChangeListener.updateWidget(null);
		}
		setNonIndexedValues();

		addPreferencesFromResource(R.xml.pref_widget_image);

		configureListNameProperty();
		bindPreferenceSummaryToValue(R.string.key_widget_alarm_interval);
		bindPreferenceSummaryToValue(R.string.key_widget_background_style);
		bindPreferenceSummaryToValue(R.string.key_widget_button_style);
		bindPreferenceSummaryToValue(R.string.key_widget_button_color);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_scale_type);
		addEditListListener();
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		LinearLayout preferenceLayout = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

		// Add finish button
		if (preferenceLayout != null && !(getActivity() instanceof WidgetSettingsActivity)) {
			Button btn = new Button(getActivity());
			btn.setText(R.string.button_widget_configuration);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					getActivity().finish();
				}
			});
			preferenceLayout.addView(btn);
		}

		return preferenceLayout;
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
						PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId));
				if (isReconfigureWidget()) {
					getActivity().finish();
				}
				return true;
			}
		});
	}

	/**
	 * Set the default values of preferences if not yet given.
	 *
	 * @param context     The context in which this method is called.
	 * @param appWidgetId The app widget id.
	 * @return true if some value needed to be set.
	 */
	protected static boolean setDefaultValues(final Context context, final int appWidgetId) {
		boolean isUpdated = false;
		long alarmInterval = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId, -1);
		if (alarmInterval == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId,
					Long.parseLong(context.getString(R.string.pref_default_widget_alarm_interval)));
		}
		int backgroundStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, appWidgetId, -1);
		if (backgroundStyle == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_background_style, appWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_widget_background_style, R.string.pref_default_widget_background_style));
		}
		int buttonStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, appWidgetId, -1);
		if (buttonStyle == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_button_style, appWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_widget_button_style, R.string.pref_default_widget_button_style));
		}
		int buttonColor = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_color, appWidgetId, -1);
		if (buttonColor == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_button_color, appWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_widget_button_color, R.string.pref_default_widget_button_color));
		}
		int detailScaleType = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, appWidgetId, -1);
		if (detailScaleType == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, appWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_widget_detail_scale_type,
							R.string.pref_default_widget_detail_scale_type));
		}
		return isUpdated;
	}

	/**
	 * Set the non-indexed preferences to the same values as the indexed ones, so that the views behave well.
	 */
	private void setNonIndexedValues() {
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_list_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_alarm_interval,
				Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, mAppWidgetId, -1)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_background_style,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, mAppWidgetId, -1)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_button_style,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, mAppWidgetId, -1)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_button_color,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_color, mAppWidgetId, -1)));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_detail_scale_type,
				Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, mAppWidgetId, -1)));
	}

	/**
	 * Configure the property for the list name.
	 */
	private void configureListNameProperty() {
		ListPreference preference = (ListPreference) findPreference(getString(R.string.key_widget_list_name));

		ArrayList<String> listNameList = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
		String[] listNames = listNameList.toArray(new String[listNameList.size()]);

		preference.setEntries(listNames);
		preference.setEntryValues(listNames);

		bindPreferenceSummaryToValue(R.string.key_widget_list_name);
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
		if (preferenceKey == R.string.key_widget_alarm_interval) {
			value = Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(preferenceKey, mAppWidgetId, -1));
		}
		else if (preferenceKey == R.string.key_widget_background_style
				|| preferenceKey == R.string.key_widget_button_style
				|| preferenceKey == R.string.key_widget_button_color
				|| preferenceKey == R.string.key_widget_detail_scale_type) {
			value = Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(preferenceKey, mAppWidgetId, -1));
		}
		else {
			value = PreferenceUtil.getIndexedSharedPreferenceString(preferenceKey, mAppWidgetId);
		}

		// Trigger the listener immediately with the preference's current value.
		mOnPreferenceChangeListener.setSummary(preference, value);
	}

	/**
	 * Get the alarm interval of a Widget as String.
	 *
	 * @param appWidgetId The widget id.
	 * @return The alarm interval as String.
	 */
	public static String getAlarmIntervalString(final int appWidgetId) {
		long alarmInterval = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId, -1);
		if (alarmInterval <= 0) {
			return null;
		}
		String[] timerDurations = Application.getAppContext().getResources().getStringArray(R.array.timer_durations);
		String[] timerDurationNames = Application.getAppContext().getResources().getStringArray(R.array.timer_duration_names);
		for (int i = 0; i < timerDurations.length; i++) {
			if (Long.parseLong(timerDurations[i]) == alarmInterval) {
				return timerDurationNames[i];
			}
		}
		return null;
	}


	/**
	 * Set the onPreferenceChangeListener.
	 */
	protected abstract OnWidgetPreferenceChangeListener createOnPreferenceChangeListener();

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	protected interface OnWidgetPreferenceChangeListener extends OnPreferenceChangeListener {
		/**
		 * Set the summary of the preference.
		 *
		 * @param preference The preference.
		 * @param value      The value of the preference.
		 */
		void setSummary(final Preference preference, final String value);

		/**
		 * Update the widget.
		 *
		 * @param updateType The update type.
		 */
		void updateWidget(final UpdateType updateType);
	}

}
