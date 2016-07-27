package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * The base widget, allowing to open the app for a specific list.
 */
public class MiniWidget extends GenericWidget {
	@Override
	public final void onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
									 final UpdateType updateType) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_mini);
		final String listName = getListName(appWidgetId);
		final String widgetName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_display_name, appWidgetId);

		if (widgetName != null && widgetName.length() > 0) {
			remoteViews.setTextViewText(R.id.textViewWidget, widgetName);
		}
		else {
			remoteViews.setTextViewText(R.id.textViewWidget, listName);
		}

		Intent intent = DisplayRandomImageActivity.createIntent(context, listName, null, false, appWidgetId, null);
		PendingIntent pendingIntent =
				PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		remoteViews.setOnClickPendingIntent(R.id.layoutWidget, pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @param listName    The list name to be used by the widget.
	 */
	public static final void configure(final int appWidgetId, final String listName) {
		PreferenceUtil.incrementCounter(R.string.key_statistics_countcreateminiwidget);
		doBaseConfiguration(appWidgetId, listName, 0);
		updateInstances(UpdateType.NEW_LIST, appWidgetId);
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param updateType  flag indicating what should be updated.
	 * @param appWidgetId the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final UpdateType updateType, final int... appWidgetId) {
		updateInstances(MiniWidget.class, updateType, appWidgetId);
	}

	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int appWidgetId : appWidgetIds) {
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_timeout, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_allowed_call_frequency, appWidgetId);
		}
	}
}
