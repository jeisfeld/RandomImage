package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * The stacked widget, showing a stack of images.
 */
public class StackedImageWidget extends GenericImageWidget {
	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_ITEM = "de.jeisfeld.randomimage.ITEM";

	/**
	 * The resource key for the list name.
	 */
	public static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";

	@Override
	public final void onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager,
									 final int appWidgetId, final UpdateType updateType) {
		Log.i(Application.TAG, "Updating StackedImageWidget " + appWidgetId + " with type " + updateType);

		Intent intent = new Intent(context, StackedImageWidgetService.class);
		// Add the app widget ID to the intent extras.
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

		determineWidgetDimensions(appWidgetManager, appWidgetId);

		determineWidgetDimensions(appWidgetManager, appWidgetId);

		intent.putExtra(STRING_EXTRA_LISTNAME, getListName(appWidgetId));
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId));

		// Set up the RemoteViews object to use a RemoteViews adapter.
		// This adapter connects to a RemoteViewsService through the specified intent.
		// This is how you populate the data.
		remoteViews.setRemoteAdapter(R.id.stackViewWidget, intent);

		// The empty view is displayed when the collection has no items.
		// It should be in the same layout used to instantiate the RemoteViews object above.
		remoteViews.setEmptyView(R.id.stackViewWidget, R.id.textViewWidgetEmpty);

		Intent nestedIntent = DisplayRandomImageActivity.createIntent(context, getListName(appWidgetId), null, false, appWidgetId, null);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, nestedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setPendingIntentTemplate(R.id.stackViewWidget, pendingIntent);

		boolean viewAsList = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_view_as_list, appWidgetId, false);
		if (viewAsList && (updateType == UpdateType.NEW_LIST
				|| updateType == UpdateType.NEW_IMAGE_BY_USER || updateType == UpdateType.NEW_IMAGE_AUTOMATIC)) {
			remoteViews.setInt(R.id.stackViewWidget, "smoothScrollToPosition", 0);
		}

		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

		configureButtons(context, appWidgetManager, appWidgetId, false);

		// trigger also onDataStackChanged, as the intent will not update the service once created.
		if (updateType == UpdateType.NEW_LIST || updateType == UpdateType.NEW_IMAGE_BY_USER || updateType == UpdateType.NEW_IMAGE_AUTOMATIC) {
			PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_requires_update, appWidgetId, true);
			appWidgetManager.notifyAppWidgetViewDataChanged(new int[] {appWidgetId}, R.id.stackViewWidget);
		}

		if (updateType == UpdateType.NEW_LIST || updateType == UpdateType.NEW_IMAGE_BY_USER) {
			// re-trigger timer - just in case that timer is not valid any more.
			StackedImageWidget.updateTimers(appWidgetId);
		}
	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
												final int appWidgetId, final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		configureButtons(context, appWidgetManager, appWidgetId, false);

		determineWidgetDimensions(appWidgetManager, appWidgetId);

		PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_requires_update, appWidgetId, true);
		appWidgetManager.notifyAppWidgetViewDataChanged(new int[] {appWidgetId}, R.id.stackViewWidget);
	}

	/**
	 * Determine and store the widget dimensions.
	 *
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId      The appWidgetId for which an update is needed.
	 */
	private void determineWidgetDimensions(final AppWidgetManager appWidgetManager, final int appWidgetId) {
		Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);

		String widthOption = SystemUtil.isLandscape() ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH;
		String heightOption = SystemUtil.isLandscape() ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT;
		int width = (int) Math.ceil(DENSITY * options.getInt(widthOption));
		int height = (int) Math.ceil(DENSITY * options.getInt(heightOption));

		if (width > 0) {
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_view_width, appWidgetId, width);
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_view_height, appWidgetId, height);
		}
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @param listName    The list name to be used by the widget.
	 * @param updateType  flag indicating what should be updated.
	 */
	public static final void configure(final int appWidgetId, final String listName, final UpdateType updateType) {
		PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatestackedimagewidget);

		long interval = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId,
				Long.parseLong(Application.getAppContext().getString(R.string.pref_default_widget_alarm_interval)));

		doBaseConfiguration(appWidgetId, listName, interval);
		updateInstances(updateType, appWidgetId);
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param updateType  flag indicating what should be updated.
	 * @param appWidgetId the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final UpdateType updateType, final int... appWidgetId) {
		updateInstances(StackedImageWidget.class, updateType, appWidgetId);
	}

	/**
	 * Update timers for instances of the widget.
	 *
	 * @param appWidgetIds the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateTimers(final int... appWidgetIds) {
		updateTimers(StackedImageWidget.class, appWidgetIds);
	}

	/**
	 * Check if there is an StackedImageWidget of this id.
	 *
	 * @param appWidgetId The widget id.
	 * @return true if there is an StackedImageWidget of this id.
	 */
	public static boolean hasWidgetOfId(final int appWidgetId) {
		return hasWidgetOfId(StackedImageWidget.class, appWidgetId);
	}

	/**
	 * Get the layout resource id for the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @return The layout resource id.
	 */
	@Override
	protected final int getWidgetLayoutId(final int appWidgetId) {
		ButtonStyle buttonStyle = ButtonStyle.fromWidgetId(appWidgetId);
		boolean viewAsList = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_view_as_list, appWidgetId, false);

		switch (buttonStyle) {
		case BOTTOM:
			return viewAsList ? R.layout.widget_list_image_bottom_buttons : R.layout.widget_stacked_image_bottom_buttons;
		case TOP:
			return viewAsList ? R.layout.widget_list_image_top_buttons : R.layout.widget_stacked_image_top_buttons;
		default:
			return viewAsList ? R.layout.widget_list_image_centered_buttons : R.layout.widget_stacked_image_centered_buttons;
		}
	}

	@Override
	protected final Class<? extends GenericWidgetConfigurationActivity> getConfigurationActivityClass() {
		return StackedImageWidgetConfigurationActivity.class;
	}
}
