package de.jeisfeld.randomimage.widgets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display the settings page.
 */
public class WidgetSettingsActivity extends PreferenceActivity {
	/**
	 * Resource String for the hash code parameter used to identify the instance of the activity.
	 */
	public static final String STRING_HASH_CODE = "de.jeisfeld.randomimage.HASH_CODE";

	/**
	 * The headers that are displayed.
	 */
	private List<Header> mHeaders = null;

	/**
	 * A map allowing to get the activity from its hashCode.
	 */
	private static Map<Integer, WidgetSettingsActivity> mActivityMap = new HashMap<>();

	/**
	 * Utility method to start the activity.
	 *
	 * @param activity The activity from which the activity is started.
	 */
	public static final void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, WidgetSettingsActivity.class);
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
		for (Class<? extends GenericWidget> widgetClass : GenericWidget.WIDGET_TYPES) {
			int[] appWidgetIds = GenericWidget.getAllWidgetIds(widgetClass);
			for (int appWidgetId : appWidgetIds) {
				target.add(createHeaderForWidget(widgetClass, appWidgetId));
			}
		}
		mHeaders = target;

		if (target.size() == 0) {
			DialogUtil.displayInfo(this, new MessageDialogListener() {
				@Override
				public void onDialogFinished() {
					finish();
				}
			}, 0, R.string.dialog_info_no_widget);
		}
	}

	/**
	 * Create a preference header for a widget.
	 *
	 * @param widgetClass The widget class.
	 * @param appWidgetId The widget id.
	 * @return the preference header.
	 */
	private Header createHeaderForWidget(final Class<? extends GenericWidget> widgetClass, final int appWidgetId) {
		int widgetNameResourceId = 0;
		String fragmentString = null;
		if (widgetClass.equals(MiniWidget.class)) {
			widgetNameResourceId = R.string.mini_widget_name;
			fragmentString = "de.jeisfeld.randomimage.widgets.MiniWidgetConfigurationFragment";
		}
		else if (widgetClass.equals(ImageWidget.class)) {
			widgetNameResourceId = R.string.image_widget_name;
			fragmentString = "de.jeisfeld.randomimage.widgets.ImageWidgetConfigurationFragment";
		}
		else if (widgetClass.equals(StackedImageWidget.class)) {
			widgetNameResourceId = R.string.stacked_image_widget_name;
			fragmentString = "de.jeisfeld.randomimage.widgets.StackedImageWidgetConfigurationFragment";
		}
		String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);

		Header header = new Header();
		header.title = getString(widgetNameResourceId);
		header.summary = listName;
		header.fragment = fragmentString;
		header.id = appWidgetId;

		Bundle arguments = new Bundle();
		arguments.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		arguments.putBoolean(GenericWidgetConfigurationActivity.EXTRA_RECONFIGURE_WIDGET, true);
		arguments.putInt(STRING_HASH_CODE, hashCode());
		header.fragmentArguments = arguments;

		return header;
	}

	@Override
	protected final boolean isValidFragment(final String fragmentName) {
		return fragmentName.startsWith("de.jeisfeld.randomimage");
	}

	/**
	 * Update the preference header for one entry.
	 *
	 * @param hashCode    The code identifying the owning activity.
	 * @param appWidgetId The app widget id of the entry.
	 */
	protected static void updateHeader(final int hashCode, final int appWidgetId) {
		String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);
		WidgetSettingsActivity activity = mActivityMap.get(hashCode);
		if (activity != null) {
			for (Header header : activity.mHeaders) {
				if (header.id == appWidgetId) {
					header.summary = listName;
				}
			}
			activity.invalidateHeaders();
		}
	}
}
