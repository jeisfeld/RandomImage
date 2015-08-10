package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;

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
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";

	/**
	 * Number of pixels per dip.
	 */
	private static final float DENSITY = Application.getAppContext().getResources().getDisplayMetrics().density;

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

			Intent intent = new Intent(context, StackedImageWidgetService.class);
			// Add the app widget ID to the intent extras.
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
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

			Intent nestedIntent = DisplayRandomImageActivity.createIntent(context, listNames.get(appWidgetId), null);
			PendingIntent pendingIntent =
					PendingIntent.getActivity(context, appWidgetId, nestedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			remoteViews.setPendingIntentTemplate(R.id.stackViewWidget, pendingIntent);

			appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

			// trigger also onDataStackChanged, as the intent will not update the service once created.
			appWidgetManager.notifyAppWidgetViewDataChanged(new int[] { appWidgetId }, R.id.stackViewWidget);
		}
	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId,
			final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		onUpdate(context, appWidgetManager, new int[] { appWidgetId });
	}

	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];
			WidgetAlarmReceiver.cancelAlarm(context, appWidgetId);
			listNames.remove(appWidgetId);

			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_list_name, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_alarm_interval, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_view_width, appWidgetId);
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
	 * @param interval
	 *            The shuffle interval.
	 */
	public static final void configure(final int appWidgetId, final String listName, final long interval) {
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, listName);
		listNames.put(appWidgetId, listName);

		if (interval > 0) {
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId, interval);
			WidgetAlarmReceiver.setAlarm(Application.getAppContext(), appWidgetId, interval);
		}
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

	/**
	 * Get the ids of all widgets of this class.
	 *
	 * @return The ids of all widgets of this class.
	 */
	public static int[] getAllWidgetIds() {
		Context context = Application.getAppContext();
		return AppWidgetManager.getInstance(context).getAppWidgetIds(
				new ComponentName(context, StackedImageWidget.class));
	}

	/**
	 * Check if there is an StackedImageWidget of this id.
	 *
	 * @param appWidgetId
	 *            The widget id.
	 * @return true if there is an StackedImageWidget of this id.
	 */
	public static boolean hasWidgetOfId(final int appWidgetId) {
		int[] allAppWidgetIds = getAllWidgetIds();
		for (int i = 0; i < allAppWidgetIds.length; i++) {
			if (allAppWidgetIds[i] == appWidgetId) {
				return true;
			}
		}
		return false;
	}
}
