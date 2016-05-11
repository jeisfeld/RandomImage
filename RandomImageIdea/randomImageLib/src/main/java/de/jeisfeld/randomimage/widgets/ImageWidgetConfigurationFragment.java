package de.jeisfeld.randomimage.widgets;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;

import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.widgets.GenericWidget.UpdateType;
import de.jeisfeld.randomimagelib.R;

/**
 * Fragment for displaying the settings of the image widget.
 */
public class ImageWidgetConfigurationFragment extends GenericImageWidgetConfigurationFragment {
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Do not offer cyclic change and list view in ImageWidget.
		getPreferenceScreen().removePreference(findPreference(getString(R.string.key_widget_show_cyclically)));
		getPreferenceScreen().removePreference(findPreference(getString(R.string.key_widget_view_as_list)));
	}

	@Override
	protected final GenericImageWidgetConfigurationFragment.OnWidgetPreferenceChangeListener createOnPreferenceChangeListener() {
		return new GenericImageWidgetConfigurationFragment.OnWidgetPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference preference, final Object value) {
				String stringValue = value.toString();
				setSummary(preference, stringValue);

				if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_list_name))) {
					PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, getAppWidgetId(), stringValue);
					WidgetSettingsActivity.updateHeader(getArguments().getInt(WidgetSettingsActivity.STRING_HASH_CODE, 0), getAppWidgetId());
					ImageWidget.configure(getAppWidgetId(), stringValue, UpdateType.NEW_LIST);
				}
				else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_timer_duration))) {
					PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, getAppWidgetId(), Long.parseLong(stringValue));
					WidgetSettingsActivity.updateHeader(getArguments().getInt(WidgetSettingsActivity.STRING_HASH_CODE, 0), getAppWidgetId());
					ImageWidget.updateTimers(getAppWidgetId());
				}
				else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_background_style))) {
					PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_background_style, getAppWidgetId(),
							Integer.parseInt(stringValue));
					ImageWidget.updateInstances(UpdateType.BUTTONS_BACKGROUND, getAppWidgetId());
				}
				else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_button_style))) {
					PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_button_style, getAppWidgetId(), Integer.parseInt(stringValue));
					updatePropertyEnablement();
					ImageWidget.updateInstances(UpdateType.BUTTONS_BACKGROUND, getAppWidgetId());
				}
				else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_button_color))) {
					PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_button_color, getAppWidgetId(), Integer.parseInt(stringValue));
					ImageWidget.updateInstances(UpdateType.BUTTONS_BACKGROUND, getAppWidgetId());
				}
				else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_display_name))) {
					PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_display_name, getAppWidgetId(), stringValue);
					WidgetSettingsActivity.updateHeader(getArguments().getInt(WidgetSettingsActivity.STRING_HASH_CODE, 0), getAppWidgetId());
				}
				else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_scale_type))) {
					PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, getAppWidgetId(),
							Integer.parseInt(stringValue));
				}
				else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_background))) {
					PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_background, getAppWidgetId(),
							Integer.parseInt(stringValue));
				}

				return true;
			}

			@Override
			public void setSummary(final Preference preference, final String value) {
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

			@Override
			public void updateWidget(final UpdateType updateType) {
				ImageWidget.updateInstances(UpdateType.BUTTONS_BACKGROUND, getAppWidgetId());
			}
		};
	}
}
