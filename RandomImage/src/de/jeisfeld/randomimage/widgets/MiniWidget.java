package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;

/**
 * The base widget, allowing to open the app for a specific list.
 */
public class MiniWidget extends AppWidgetProvider {
	/**
	 * The names of the image lists associated to the widget.
	 */
	private static SparseArray<String> listNames = new SparseArray<String>();

	@Override
	public final void
			onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			String listName = getListName(appWidgetId);

			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_mini);

			remoteViews.setTextViewText(R.id.textViewWidget, listName);

			Intent intent = DisplayRandomImageActivity.createIntent(context, listName, null);
			PendingIntent pendingIntent =
					PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

			remoteViews.setOnClickPendingIntent(R.id.textViewWidget, pendingIntent);
			appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
		}
	}

	/**
	 * Get the list name associated to an instance of the widget.
	 *
	 * @param appWidgetId
	 *            The app widget id.
	 * @return The list name.
	 */
	private String getListName(final int appWidgetId) {
		String listName = listNames.get(appWidgetId);
		if (listName == null) {
			listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);
			if (listName == null || listName.length() == 0) {
				listName = ImageRegistry.getCurrentListName();
			}
			listNames.put(appWidgetId, listName);
		}
		return listName;
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
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, listName);
		listNames.put(appWidgetId, listName);
		updateInstances(appWidgetId);
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param appWidgetId
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final int... appWidgetId) {
		Context context = Application.getAppContext();
		Intent intent = new Intent(context, MiniWidget.class);
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

		int[] ids;
		if (appWidgetId.length == 0) {
			ids =
					AppWidgetManager.getInstance(context).getAppWidgetIds(
							new ComponentName(Application.getAppContext(), MiniWidget.class));
		}
		else {
			ids = appWidgetId;
		}
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		context.sendBroadcast(intent);
	}

}
