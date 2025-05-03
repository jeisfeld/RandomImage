package de.jeisfeld.randomimage.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.view.TimeSelectorPreference;
import de.jeisfeld.randomimage.widgets.GenericImageWidget.ButtonStyle;
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

	// OVERRIDABLE
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mOnPreferenceChangeListener = createOnPreferenceChangeListener();
		mAppWidgetId = getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
		mReconfigureWidget = getArguments().getBoolean(GenericWidgetConfigurationActivity.EXTRA_RECONFIGURE_WIDGET, false);

		boolean isUpdated = setDefaultValues();
		if (isUpdated) {
			mOnPreferenceChangeListener.updateWidget(null);
		}
		setNonIndexedValues();

		addPreferencesFromResource(R.xml.pref_widget_image);

		configureListNameProperty();
		bindPreferenceSummaryToValue(R.string.key_widget_timer_duration);
		bindPreferenceSummaryToValue(R.string.key_widget_view_as_list);
		bindPreferenceSummaryToValue(R.string.key_widget_show_cyclically);
		bindPreferenceSummaryToValue(R.string.key_widget_background_style);
		bindPreferenceSummaryToValue(R.string.key_widget_button_style);
		bindPreferenceSummaryToValue(R.string.key_widget_button_color);
		bindPreferenceSummaryToValue(R.string.key_widget_display_name);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_use_default);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_scale_type);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_background);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_flip_behavior);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_change_timeout);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_change_with_tap);
		bindPreferenceSummaryToValue(R.string.key_widget_detail_prevent_screen_timeout);
		addEditListListener();
		updatePropertyEnablement();
	}

	/**
	 * Enable or disable properties in dependence of other properties.
	 */
	protected final void updatePropertyEnablement() {
		ButtonStyle buttonStyle = ButtonStyle.fromWidgetId(mAppWidgetId);
		Preference buttonColorPreference = findPreference(getString(R.string.key_widget_button_color));
		buttonColorPreference.setEnabled(buttonStyle != ButtonStyle.GONE);

		boolean useDefaultSettings =
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_detail_use_default, mAppWidgetId, false);
		findPreference(getString(R.string.key_widget_detail_scale_type)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_background)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_flip_behavior)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_change_timeout)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_change_with_tap)).setEnabled(!useDefaultSettings);
		findPreference(getString(R.string.key_widget_detail_prevent_screen_timeout)).setEnabled(!useDefaultSettings);
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		LinearLayout preferenceLayout = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

		View buttonLayout = LayoutInflater.from(getActivity()).inflate(R.layout.layout_configure_widget_buttons, null);
		if (preferenceLayout != null) {
			preferenceLayout.setOnApplyWindowInsetsListener((v, insets) -> {
				v.setPadding(insets.getSystemWindowInsetLeft(),
						insets.getSystemWindowInsetTop(),
						insets.getSystemWindowInsetRight(),
						insets.getSystemWindowInsetBottom());
				return insets.consumeSystemWindowInsets();
			});
			preferenceLayout.setFitsSystemWindows(true);

			preferenceLayout.addView(buttonLayout);

			buttonLayout.findViewById(R.id.buttonFinishWidgetConfiguration).setOnClickListener(v -> getActivity().finish());

			buttonLayout.findViewById(R.id.buttonRunWidget).setOnClickListener(v -> {
				String currentFileName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_current_file_name, mAppWidgetId);
				Intent intent = DisplayRandomImageActivity.createIntent(getActivity(),
						PreferenceUtil.getSharedPreferenceString(R.string.key_widget_list_name), currentFileName, true, mAppWidgetId, null);
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
	 * @return true if some value needed to be set.
	 */
	protected final boolean setDefaultValues() {
		boolean isUpdated = false;
		if (PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceLongString(R.string.key_widget_timer_duration, R.string.pref_default_widget_timer_duration));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_background_style, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_widget_background_style, R.string.pref_default_widget_background_style));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_button_style, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_widget_button_style, R.string.pref_default_widget_button_style));
		}
		if (PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_color, mAppWidgetId, -1) == -1) {
			isUpdated = true;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_button_color, mAppWidgetId,
					PreferenceUtil.getSharedPreferenceIntString(R.string.key_widget_button_color, R.string.pref_default_widget_button_color));
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
		return isUpdated;
	}

	/**
	 * Set the non-indexed preferences to the same values as the indexed ones, so that the views behave well.
	 */
	private void setNonIndexedValues() {
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_list_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId));
		PreferenceUtil.setSharedPreferenceLongString(R.string.key_widget_timer_duration,
				PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, mAppWidgetId, -1));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_widget_view_as_list,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_view_as_list, mAppWidgetId, false));
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_widget_show_cyclically,
				PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_show_cyclically, mAppWidgetId, false));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_widget_background_style,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, mAppWidgetId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_widget_button_style,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, mAppWidgetId, -1));
		PreferenceUtil.setSharedPreferenceIntString(R.string.key_widget_button_color,
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_color, mAppWidgetId, -1));
		PreferenceUtil.setSharedPreferenceString(R.string.key_widget_display_name,
				PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_display_name, mAppWidgetId));
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
		if (preferenceKey == R.string.key_widget_timer_duration) {
			value = Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(preferenceKey, mAppWidgetId, -1));
		}
		else if (preferenceKey == R.string.key_widget_background_style // BOOLEAN_EXPRESSION_COMPLEXITY
				|| preferenceKey == R.string.key_widget_button_style
				|| preferenceKey == R.string.key_widget_button_color
				|| preferenceKey == R.string.key_widget_detail_scale_type
				|| preferenceKey == R.string.key_widget_detail_background
				|| preferenceKey == R.string.key_widget_detail_flip_behavior) {
			value = Integer.toString(PreferenceUtil.getIndexedSharedPreferenceInt(preferenceKey, mAppWidgetId, -1));
		}
		else if (preferenceKey == R.string.key_widget_detail_change_timeout) {
			value = Long.toString(PreferenceUtil.getIndexedSharedPreferenceLong(preferenceKey, mAppWidgetId, -1));
		}
		else if (preferenceKey == R.string.key_widget_show_cyclically // BOOLEAN_EXPRESSION_COMPLEXITY
				|| preferenceKey == R.string.key_widget_view_as_list
				|| preferenceKey == R.string.key_widget_detail_use_default
				|| preferenceKey == R.string.key_widget_detail_change_with_tap
				|| preferenceKey == R.string.key_widget_detail_prevent_screen_timeout) {
			value = "";
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
	 * @param context     the context.
	 * @param appWidgetId The widget id.
	 * @return The alarm interval as String.
	 */
	public static String getAlarmIntervalHeaderString(final Context context, final int appWidgetId) {
		long alarmInterval = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, appWidgetId, -1);
		return TimeSelectorPreference.getDefaultSummaryFromValue(context, Long.toString(alarmInterval));
	}

	/**
	 * Create the onPreferenceChangeListener.
	 *
	 * @return the onPreferenceChangeListener.
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
		void setSummary(Preference preference, String value);

		/**
		 * Update the widget.
		 *
		 * @param updateType The update type.
		 */
		void updateWidget(UpdateType updateType);
	}

}
