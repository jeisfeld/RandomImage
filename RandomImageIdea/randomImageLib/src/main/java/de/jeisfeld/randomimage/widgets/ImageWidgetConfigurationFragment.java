package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
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

import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Fragment for displaying the settings.
 */
public class ImageWidgetConfigurationFragment extends PreferenceFragment {
	/**
	 * The app widget id.
	 */
	private int mAppWidgetId;

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private OnWidgetPreferenceChangeListener mOnPreferenceChangeListener = new OnWidgetPreferenceChangeListener();

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppWidgetId = getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);

		setDefaultValues();
		setNonIndexedValues();

		addPreferencesFromResource(R.xml.pref_widget_image);

		configureListNameProperty();
		bindPreferenceSummaryToValue(R.string.key_widget_alarm_interval);
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		LinearLayout preferenceLayout = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

		Button btn = new Button(getActivity());
		btn.setText(R.string.button_widget_configuration);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				getActivity().finish();
			}
		});
		preferenceLayout.addView(btn);

		return preferenceLayout;
	}

	/**
	 * Set the default values of preferences if not yet given.
	 */
	private void setDefaultValues() {
		long alarmInterval = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, mAppWidgetId, -1);
		if (alarmInterval == -1) {
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, mAppWidgetId,
					Long.parseLong(getString(R.string.pref_default_widget_alarm_interval)));
		}
	}

	/**
	 * Set the non-indexed preferences to the same values as the indexed ones, so that the views behave well.
	 */
	private void setNonIndexedValues() {
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_list_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_alarm_interval,
				Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, mAppWidgetId, -1)));
	}

	/**
	 * Configure the property for the list name.
	 */
	private void configureListNameProperty() {
		ListPreference preference = (ListPreference) findPreference(getString(R.string.key_widget_list_name));

		ArrayList<String> listNameList = ImageRegistry.getImageListNames();
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
			value = Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(preferenceKey, mAppWidgetId,
					Long.parseLong(getString(R.string.pref_default_widget_alarm_interval))));
		}
		else {
			value = PreferenceUtil.getIndexedSharedPreferenceString(preferenceKey, mAppWidgetId);
		}

		// Trigger the listener immediately with the preference's current value.
		mOnPreferenceChangeListener.setSummary(preference, value);
	}

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private class OnWidgetPreferenceChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(final Preference preference, final Object value) {
			String stringValue = value.toString();
			setSummary(preference, stringValue);

			if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_list_name))) {
				PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId, stringValue);
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_alarm_interval))) {
				PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, mAppWidgetId, Long.parseLong(stringValue));
			}

			ImageWidget.configure(mAppWidgetId,
					PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId),
					PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, mAppWidgetId, -1));

			return true;
		}

		/**
		 * Set the summary of the preference.
		 *
		 * @param preference The preference.
		 * @param value The value of the preference.
		 */
		public void setSummary(final Preference preference, final String value) {
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
