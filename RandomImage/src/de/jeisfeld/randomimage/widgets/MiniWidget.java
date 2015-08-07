package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * The base widget, allowing to open the app for a specific list.
 */
public class MiniWidget extends AppWidgetProvider {
	@Override
	public final void
			onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_mini);

			remoteViews.setTextViewText(R.id.textViewWidget, ImageRegistry.getCurrentListName());

			Intent intent = new Intent(context, DisplayRandomImageActivity.class);
			PendingIntent pendingIntent =
					PendingIntent.getActivity(context, 0, intent, 0);

			remoteViews.setOnClickPendingIntent(R.id.textViewWidget, pendingIntent);
			appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
		}
	}

}
