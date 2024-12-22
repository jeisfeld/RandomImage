package de.jeisfeld.randomimage.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;

import java.util.Arrays;
import java.util.List;

import de.jeisfeld.randomimage.BasePreferenceActivity;
import de.jeisfeld.randomimage.SelectDirectoryActivity;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.view.ImageSelectionPreference.ChosenImageListener;
import de.jeisfeld.randomimage.view.ImageSelectionPreference.ChosenImageListenerActivity;
import de.jeisfeld.randomimage.widgets.GenericWidget.UpdateType;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display the settings page.
 */
public class WidgetSettingsActivity extends BasePreferenceActivity implements ChosenImageListenerActivity {
	/**
	 * Resource String for the hash code parameter used to identify the instance of the activity.
	 */
	public static final String STRING_HASH_CODE = "de.jeisfeld.randomimage.HASH_CODE";

	/**
	 * A map allowing to get the activity from its hashCode.
	 */
	private static final SparseArray<WidgetSettingsActivity> mActivityMap = new SparseArray<>();

	/**
	 * A listener called when an image has been selected via ImageSelectionPreference.
	 */
	private ChosenImageListener mChosenImageListener = null;

	/**
	 * Utility method to start the activity.
	 *
	 * @param activity The activity from which the activity is started.
	 */
	public static void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, WidgetSettingsActivity.class);
		activity.startActivity(intent);
	}

	/**
	 * Update the list name in all widgets.
	 *
	 * @param oldName The old name.
	 * @param newName The new name.
	 */
	public static void updateListName(final String oldName, final String newName) {
		for (Class<? extends GenericWidget> widgetClass : GenericWidget.WIDGET_TYPES) {
			int[] appWidgetIds = GenericWidget.getAllWidgetIds(widgetClass);
			for (int appWidgetId : appWidgetIds) {
				if (oldName.equals(GenericWidget.getListName(appWidgetId))) {
					PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, newName);
					GenericWidget.updateInstances(widgetClass, UpdateType.NEW_LIST, appWidgetId);
				}
			}
		}
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
	protected final void onResume() {
		super.onResume();
		TrackingUtil.sendScreen(this);
	}

	@Override
	public final void onBuildHeaders(final List<Header> target) {
		for (Class<? extends GenericWidget> widgetClass : GenericWidget.WIDGET_TYPES) {
			int[] appWidgetIds = GenericWidget.getAllWidgetIds(widgetClass);
			Arrays.sort(appWidgetIds);
			for (int i = 0; i < appWidgetIds.length; i++) {
				target.add(createHeaderForWidget(widgetClass, appWidgetIds[i], i));
			}
		}

		if (target.isEmpty()) {
			DialogUtil.displayInfo(this, this::finish, 0, R.string.dialog_info_no_widget);
		}
	}

	/**
	 * Create a preference header for a widget.
	 *
	 * @param widgetClass The widget class.
	 * @param appWidgetId The widget id.
	 * @param index       The index of the widget of this type.
	 * @return the preference header.
	 */
	private Header createHeaderForWidget(final Class<? extends GenericWidget> widgetClass, final int appWidgetId, final int index) {
		int widgetNameResourceId = 0;
		String fragmentString = null;
		if (widgetClass.equals(MiniWidget.class)) {
			widgetNameResourceId = R.string.mini_widget_display_name;
			fragmentString = "de.jeisfeld.randomimage.widgets.MiniWidgetConfigurationFragment";
		}
		else if (widgetClass.equals(ImageWidget.class)) {
			widgetNameResourceId = R.string.image_widget_display_name;
			fragmentString = "de.jeisfeld.randomimage.widgets.ImageWidgetConfigurationFragment";
		}
		else if (widgetClass.equals(StackedImageWidget.class)) {
			widgetNameResourceId = R.string.stacked_image_widget_display_name;
			fragmentString = "de.jeisfeld.randomimage.widgets.StackedImageWidgetConfigurationFragment";
		}
		else if (widgetClass.equals(ShortcutDummyWidget.class)) {
			widgetNameResourceId = R.string.shortcut_display_name;
			fragmentString = "de.jeisfeld.randomimage.widgets.MiniWidgetConfigurationFragment";
		}

		String title = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_display_name, appWidgetId);
		if (title == null || title.isEmpty()) {
			title = getString(widgetNameResourceId) + " " + (index + 1);
		}

		Header header = new Header();
		header.title = title;
		header.summary = getHeaderSummary(this, widgetClass, appWidgetId);
		header.fragment = fragmentString;
		header.id = appWidgetId;

		Bundle arguments = new Bundle();
		arguments.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		arguments.putBoolean(GenericWidgetConfigurationActivity.EXTRA_RECONFIGURE_WIDGET, true);
		arguments.putInt(STRING_HASH_CODE, hashCode());
		header.fragmentArguments = arguments;

		return header;
	}

	/**
	 * Get a widgetId from its name.
	 *
	 * @param name The widget name.
	 * @return The widget id.
	 */
	public static Integer getWidgetIdByName(final String name) {
		if (name == null) {
			return null;
		}
		for (Integer appWidgetId : GenericWidget.getAllWidgetIds()) {
			String widgetName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_display_name, appWidgetId);
			if (name.equals(widgetName)) {
				return appWidgetId;
			}
		}
		return null;
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
		WidgetSettingsActivity activity = mActivityMap.get(hashCode);
		if (activity != null) {
			activity.invalidateHeaders();
		}
	}

	/**
	 * Get the summary of the header entry for a widget.
	 *
	 * @param context     the context.
	 * @param widgetClass The widget class.
	 * @param appWidgetId The widget id.
	 * @return The header summary
	 */
	private static String getHeaderSummary(final Context context, final Class<? extends GenericWidget> widgetClass, final int appWidgetId) {
		String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);
		String timerString = GenericImageWidgetConfigurationFragment.getAlarmIntervalHeaderString(context, appWidgetId);
		if (widgetClass.equals(MiniWidget.class) || timerString == null) {
			return listName;
		}
		else {
			return listName + " (" + timerString + ")";
		}
	}

	@Override
	public final void setChosenImageListener(final ChosenImageListener listener) {
		mChosenImageListener = listener;
	}

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case SelectDirectoryActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				String selectedImage = SelectDirectoryActivity.getSelectedImage(resultCode, data);
				if (mChosenImageListener != null) {
					mChosenImageListener.onChosenImage(selectedImage);
				}
			}
			break;
		default:
			break;
		}
	}
}
