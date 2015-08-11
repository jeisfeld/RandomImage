package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;

/**
 * The base widget, allowing to open the app for a specific list.
 */
public class MiniWidget extends GenericWidget {
	@Override
	public final void
			onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
					final String listName) {

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_mini);

		remoteViews.setTextViewText(R.id.textViewWidget, listName);

		Intent intent = DisplayRandomImageActivity.createIntent(context, listName, null, true);
		PendingIntent pendingIntent =
				PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		remoteViews.setOnClickPendingIntent(R.id.textViewWidget, pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId
	 *            The widget id.
	 * @param listName
	 *            The list name to be used by the widget.
	 */
	public static final void configure(final int appWidgetId, final String listName) {
		doBaseConfiguration(appWidgetId, listName, 0);
		updateInstances(appWidgetId);
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param appWidgetId
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final int... appWidgetId) {
		updateInstances(MiniWidget.class, appWidgetId);
	}

}
