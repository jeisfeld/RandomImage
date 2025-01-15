package de.jeisfeld.randomimage.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

import de.jeisfeld.randomimage.ConfigureImageListActivity;
import de.jeisfeld.randomimage.DisplayListInfoActivity;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.view.TimeSelectorPreference;
import de.jeisfeld.randomimage.widgets.GenericWidget.UpdateType;
import de.jeisfeld.randomimagelib.R;

/**
 * Fragment for displaying the settings of the image widget.
 */
public class MiniWidgetConfigurationFragment extends PreferenceFragment {
	/**
	 * The app widget id.
	 */
	private int mAppWidgetId;

	/**
	 * Flag indicating if this is shortcut instead of mini widget.
	 */
	private boolean mIsShortcut;

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private OnWidgetPreferenceChangeListener mOnPreferenceChangeListener = new OnWidgetPreferenceChangeListener();

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAppWidgetId = getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
		mIsShortcut = !MiniWidget.hasWidgetOfId(mAppWidgetId);

		setDefaultValues();
		setNonIndexedValues();

		addPreferencesFromResource(mIsShortcut ? R.xml.pref_shortcut : R.xml.pref_widget_mini);

		configureListNameProperty();
		bindPreferenceSummaryToValue(R.string.key_widget_display_name);
		bindPreferenceSummaryToValue(R.string.key_widget_icon_image);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_use_default);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_scale_type);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_background);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_flip_behavior);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_change_timeout);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_change_with_tap);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_prevent_screen_timeout);
		bindPreferenceSummaryToValue(R.string.key_widget_timeout);
		bindPreferenceSummaryToValue(R.string.key_widget_allowed_call_frequency);
		addEditListListener();
		updatePropertyEnablement();
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		LinearLayout preferenceLayout = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

		// Add run and cancel buttons
		View buttonLayout = LayoutInflater.from(getActivity()).inflate(
				mIsShortcut ? R.layout.layout_configure_shortcut_buttons : R.layout.layout_configure_widget_buttons, null);
		if (preferenceLayout != null) {
			preferenceLayout.addView(buttonLayout);

			buttonLayout.findViewById(R.id.buttonFinishWidgetConfiguration).setOnClickListener(v -> getActivity().finish());

			buttonLayout.findViewById(R.id.buttonRunWidget).setOnClickListener(v -> {
				Intent intent = DisplayRandomImageActivity.createIntent(getActivity(),
						PreferenceUtil.getSharedPreferenceString(R.string.key_widget_list_name), null, true, mAppWidgetId, null);
				getActivity().startActivity(intent);
			});
		}

		return preferenceLayout;
	}

	@Override
	public final void onResume() {
		super.onResume();
	}

	/**
	 * Add the listener for the button to edit the image list.
	 */
	private void addEditListListener() {
		Preference editListPreference = findPreference(getString(R.string.key_pref_edit_list));

		editListPreference.setOnPreferenceClickListener(preference -> {
			ConfigureImageListActivity.startActivity(getActivity(),
					PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId));
			return true;
		});
	}

	/**
	 * Set the default values of preferences if not yet given.
	 *
	 * @return true if some value needed to be set.
	 */
	protected final boolean setDefaultValues() {
		boolean isUpdated = false;
		if (PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_icon_image, mAppWidgetId) == null) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_icon_image, mAppWidgetId,
					getString(R.string.pref_default_widget_icon_image));
		}
		if (!PreferenceUtil.hasIndexedSharedPreference(R.string.key_widget_detail_use_default, mAppWidgetId)) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_use_default, mAppWidgetId, true);
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_scale_type,
							R.string.pref_default_detail_scale_type));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_background, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_background, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_background,
							R.string.pref_default_detail_background));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_flip_behavior, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_flip_behavior, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_flip_behavior,
							R.string.pref_default_detail_flip_behavior));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_detail_change_timeout, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_detail_change_timeout, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_detail_change_timeout,
							R.string.pref_default_notification_duration));
		}
		if (!PreferenceUtil.hasIndexedSharedPreference(R.string.key_widget_detail_change_with_tap, mAppWidgetId)) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_change_with_tap, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_detail_change_with_tap));
		}
		if (!PreferenceUtil.hasIndexedSharedPreference(R.string.key_widget_detail_prevent_screen_timeout, mAppWidgetId)) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_prevent_screen_timeout, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_detail_prevent_screen_timeout));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_timeout, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_timeout, mAppWidgetId, 0);
		}
		if (PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_allowed_call_frequency, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_allowed_call_frequency, mAppWidgetId, 0);
		}
		return isUpdated;
	}

	/**
	 * Set the non-indexed preferences to the same values as the indexed ones, so that the views behave well.
	 */
	private void setNonIndexedValues() {
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_list_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_display_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_display_name, mAppWidgetId));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_icon_image,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_icon_image, mAppWidgetId));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_widget_detail_use_default,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_detail_use_default, mAppWidgetId, false));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_widget_detail_scale_type,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, mAppWidgetId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_widget_detail_background,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_background, mAppWidgetId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_widget_detail_flip_behavior,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_flip_behavior, mAppWidgetId, -1));
		PreferenceUtil.setSharedPreferenceLongString(R.string.key_widget_detail_change_timeout,
				PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_detail_change_timeout, mAppWidgetId, -1));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_widget_detail_change_with_tap,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_detail_change_with_tap, mAppWidgetId, false));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_widget_detail_prevent_screen_timeout,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_detail_prevent_screen_timeout, mAppWidgetId, false));
		PreferenceUtil.setSharedPreferenceLongString(R.string.key_widget_timeout,
				PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_timeout, mAppWidgetId, 0));
		PreferenceUtil.setSharedPreferenceLongString(R.string.key_widget_allowed_call_frequency,
				PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_allowed_call_frequency, mAppWidgetId, 0));
	}

	/**
	 * Enable or disable properties in dependence of other properties.
	 */
	protected final void updatePropertyEnablement() {
		boolean useDefaultSettings =
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_detail_use_default, mAppWidgetId, false);
		findPreference(getString(R.string.key_widget_detail_scale_type)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_background)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_flip_behavior)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_change_timeout)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_change_with_tap)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_prevent_screen_timeout)).setEnabled(!useDefaultSettings);
	}

	/**
	 * Configure the property for the list name.
	 */
	private void configureListNameProperty() {
		ListPreference preference = (ListPreference) findPreference(getString(R.string.key_widget_list_name));

		ArrayList<String> listNameList = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
		String[] listNames = listNameList.toArray(new String[0]);

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
		if (preferenceKey == R.string.key_widget_detail_scale_type
				|| preferenceKey == R.string.key_widget_detail_background
				|| preferenceKey == R.string.key_widget_detail_flip_behavior) {
			value = Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(preferenceKey, mAppWidgetId, -1));
		}
		else if (preferenceKey == R.string.key_widget_timeout
				|| preferenceKey == R.string.key_widget_allowed_call_frequency
				|| preferenceKey == R.string.key_widget_detail_change_timeout) {
			value = Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(preferenceKey, mAppWidgetId, -1));
		}
		else if (preferenceKey == R.string.key_widget_detail_use_default
				|| preferenceKey == R.string.key_widget_detail_change_with_tap
				|| preferenceKey == R.string.key_widget_detail_prevent_screen_timeout) {
			value = "";
		}
		else if (preferenceKey == R.string.key_widget_icon_image) {
			value = PreferenceUtil.getIndexedSharedPreferenceString(preferenceKey, mAppWidgetId);
			if (getActivity().getResources().getStringArray(R.array.icon_image_values)[0].equals(value)) {
				value = getActivity().getResources().getStringArray(R.array.icon_image_names)[0];
			}
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
				WidgetSettingsActivity.updateHeader(getArguments().getInt(WidgetSettingsActivity.STRING_HASH_CODE, 0), mAppWidgetId);
				PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_current_file_name, mAppWidgetId);
				if (!mIsShortcut) {
					MiniWidget.configure(mAppWidgetId, stringValue);
				}
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_display_name))) {
				PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_display_name, mAppWidgetId, stringValue);
				WidgetSettingsActivity.updateHeader(getArguments().getInt(WidgetSettingsActivity.STRING_HASH_CODE, 0), mAppWidgetId);
				if (mIsShortcut) {
					DisplayListInfoActivity.updateShortcut(getActivity(), mAppWidgetId);
				}
				else {
					MiniWidget.updateInstances(UpdateType.BUTTONS_BACKGROUND, mAppWidgetId);
				}
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_icon_image))) {
				PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_icon_image, mAppWidgetId, stringValue);
				if (mIsShortcut) {
					DisplayListInfoActivity.updateShortcut(getActivity(), mAppWidgetId);
				}
				else {
					MiniWidget.updateInstances(UpdateType.BUTTONS_BACKGROUND, mAppWidgetId);
				}
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_use_default))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_use_default, mAppWidgetId,
						Boolean.parseBoolean(stringValue));
				updatePropertyEnablement();
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_scale_type))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, mAppWidgetId,
						Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_background))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_background, mAppWidgetId,
						Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_flip_behavior))) {
				PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_flip_behavior, mAppWidgetId,
						Integer.parseInt(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_change_timeout))) {
				PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_detail_change_timeout, mAppWidgetId,
						Long.parseLong(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_change_with_tap))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_change_with_tap, mAppWidgetId,
						Boolean.parseBoolean(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_detail_prevent_screen_timeout))) {
				PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_prevent_screen_timeout, mAppWidgetId,
						Boolean.parseBoolean(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_timeout))) {
				PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_timeout, mAppWidgetId, Long.parseLong(stringValue));
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_widget_allowed_call_frequency))) {
				PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_allowed_call_frequency, mAppWidgetId, Long.parseLong(stringValue));
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
			else if (preference instanceof TimeSelectorPreference) {
				preference.setSummary(((TimeSelectorPreference) preference).getSummaryFromValue(value));
			}
			else if (!(preference instanceof CheckBoxPreference)) {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(value);
			}
		}
	}

}
