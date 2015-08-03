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
	 * Number of pixels per dip.
	 */
	private static final int PIXELS_PER_DIP = Application.getAppContext().getResources()
			.getDimensionPixelSize(R.dimen.dip);

	@Override
	public final void
			onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_image);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
			int width = PIXELS_PER_DIP * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
			int height = PIXELS_PER_DIP * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
			remoteViews.setImageViewBitmap(
					R.id.imageViewWidget,
					ImageUtil.getImageBitmap(ImageRegistry
							.getInstance().getRandomFileName(),
							Math.min(ImageUtil.MAX_BITMAP_SIZE, Math.max(width, height))));

			Intent intent = new Intent(context, DisplayRandomImageActivity.class);
			PendingIntent pendingIntent =
					PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

			remoteViews.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntent);

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

}
