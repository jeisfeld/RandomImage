package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.PreferenceUtil;

/**
 * The stacked widget, showing a stack of images.
 */
public class StackedImageWidget extends GenericWidget {
	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_ITEM = "de.jeisfeld.randomimage.ITEM";

	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_WIDTH = "de.jeisfeld.randomimage.WIDTH";

	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";

	@Override
	public final void
			onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
					final String listName) {

		Intent intent = new Intent(context, StackedImageWidgetService.class);
		// Add the app widget ID to the intent extras.
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

		Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
		int width = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
		if (width > 0) {
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_view_width, appWidgetId, width);
		}

		intent.putExtra(STRING_EXTRA_WIDTH, width);
		intent.putExtra(STRING_EXTRA_LISTNAME, getListName(appWidgetId));
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_stacked_image);

		// Set up the RemoteViews object to use a RemoteViews adapter.
		// This adapter connects
		// to a RemoteViewsService through the specified intent.
		// This is how you populate the data.
		remoteViews.setRemoteAdapter(R.id.stackViewWidget, intent);

		// The empty view is displayed when the collection has no items.
		// It should be in the same layout used to instantiate the RemoteViews
		// object above.
		remoteViews.setEmptyView(R.id.stackViewWidget, R.id.textViewWidgetEmpty);

		Intent nestedIntent = DisplayRandomImageActivity.createIntent(context, getListName(appWidgetId), null, false);
		PendingIntent pendingIntent =
				PendingIntent.getActivity(context, appWidgetId, nestedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setPendingIntentTemplate(R.id.stackViewWidget, pendingIntent);

		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

		// trigger also onDataStackChanged, as the intent will not update the service once created.
		appWidgetManager.notifyAppWidgetViewDataChanged(new int[] { appWidgetId }, R.id.stackViewWidget);
	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId,
			final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		appWidgetManager.notifyAppWidgetViewDataChanged(new int[] { appWidgetId }, R.id.stackViewWidget);
	}

	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_view_width, appWidgetIds[i]);
		}
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId
	 *            The widget id.
	 * @param listName
	 *            The list name to be used by the widget.
	 * @param interval
	 *            The shuffle interval.
	 */
	public static final void configure(final int appWidgetId, final String listName, final long interval) {
		PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatestackedimagewidget);
		doBaseConfiguration(appWidgetId, listName, interval);
		updateInstances(appWidgetId);
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param appWidgetId
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final int... appWidgetId) {
		updateInstances(StackedImageWidget.class, appWidgetId);
	}

	/**
	 * Check if there is an StackedImageWidget of this id.
	 *
	 * @param appWidgetId
	 *            The widget id.
	 * @return true if there is an StackedImageWidget of this id.
	 */
	public static boolean hasWidgetOfId(final int appWidgetId) {
		return hasWidgetOfId(StackedImageWidget.class, appWidgetId);
	}
}
