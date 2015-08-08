package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;

/**
 * The stacked widget, showing a stack of images.
 */
public class StackedImageWidget extends AppWidgetProvider {
	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_ITEM = "de.jeisfeld.randomimage.ITEM";

	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_WIDTH = "de.jeisfeld.randomimage.WIDTH";

	/**
	 * Number of pixels per dip.
	 */
	private static final float DENSITY = Application.getAppContext().getResources().getDisplayMetrics().density;

	@Override
	public final void
			onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			Intent intent = new Intent(context, StackedImageWidgetService.class);
			// Add the app widget ID to the intent extras.
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

			Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
			int width = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
			intent.putExtra(STRING_EXTRA_WIDTH, width);

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

			Intent nestedIntent = new Intent(context, DisplayRandomImageActivity.class);
			PendingIntent pendingIntent =
					PendingIntent.getActivity(context, appWidgetId, nestedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			remoteViews.setPendingIntentTemplate(R.id.stackViewWidget, pendingIntent);

			appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
		}
	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId,
			final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		onUpdate(context, appWidgetManager, new int[] { appWidgetId });
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param appWidgetId
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final int... appWidgetId) {
		Context context = Application.getAppContext();
		Intent intent = new Intent(context, StackedImageWidget.class);
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

		int[] ids;
		if (appWidgetId.length == 0) {
			ids =
					AppWidgetManager.getInstance(context).getAppWidgetIds(
							new ComponentName(Application.getAppContext(), StackedImageWidget.class));
		}
		else {
			ids = appWidgetId;
		}
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		context.sendBroadcast(intent);
	}
}
