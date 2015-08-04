package de.jeisfeld.randomimage;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;

/**
 * The extended widget, also displaying a changing image.
 */
public class ImageWidget extends AppWidgetProvider {
	/**
	 * The file name of the currently displayed image.
	 */
	private String currentFileName;

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

			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_image);

			Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
			int width = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
			int height = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
			currentFileName = ImageRegistry.getCurrentImageList().getRandomFileName();

			if (currentFileName == null) {
				remoteViews.setImageViewResource(
						R.id.imageViewWidget,
						R.drawable.ic_launcher);
			}
			else {
				remoteViews.setImageViewBitmap(
						R.id.imageViewWidget,
						ImageUtil.getImageBitmap(currentFileName,
								Math.min(ImageUtil.MAX_BITMAP_SIZE, Math.max(width, height))));
			}

			Intent intent = DisplayRandomImageActivity.createIntent(context, currentFileName);
			PendingIntent pendingIntent =
					PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

			remoteViews.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntent);
			appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
		}
	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId,
			final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		onUpdate(context, appWidgetManager, new int[] { appWidgetId });
	}

}
