package de.jeisfeld.randomimage;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * The base widget, allowing to open the app for a specific list.
 */
public class MiniWidget extends AppWidgetProvider {
	@Override
	public final void
			onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_mini);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			Intent intent = new Intent(context, DisplayRandomImageActivity.class);
			PendingIntent pendingIntent =
					PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

			remoteViews.setOnClickPendingIntent(R.id.textViewWidget, pendingIntent);

			appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
		}
	}

}
