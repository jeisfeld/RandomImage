package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;

/**
 * The extended widget, also displaying a changing image.
 */
public class ImageWidget extends AppWidgetProvider {
	/**
	 * Number of pixels per dip.
	 */
	private static final float DENSITY = Application.getAppContext().getResources().getDisplayMetrics().density;

	/**
	 * The names of the image lists associated to the widget.
	 */
	private static SparseArray<String> listNames = new SparseArray<String>();

	/**
	 * The file names of the currently displayed images - mapped from appWidgetId.
	 */
	private static SparseArray<String> currentFileNames = new SparseArray<String>();

	@Override
	public final void
			onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			String listName = getListName(appWidgetId);

			String fileName = ImageRegistry.getImageListByName(listName).getRandomFileName();

			setImage(context, appWidgetManager, appWidgetId, listName, fileName);
		}
	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId, final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		String listName = getListName(appWidgetId);

		String fileName = currentFileNames.get(appWidgetId);
		if (fileName == null) {
			fileName = ImageRegistry.getImageListByName(listName).getRandomFileName();
		}

		setImage(context, appWidgetManager, appWidgetId, listName, fileName);
	}

	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];
			WidgetAlarmReceiver.cancelAlarm(context, appWidgetId);
			currentFileNames.remove(appWidgetId);
			listNames.remove(appWidgetId);

			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_list_name, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_alarm_interval, appWidgetId);
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
	 * Set the image in an instance of the widget.
	 *
	 * @param context
	 *            The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager
	 *            A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId
	 *            The appWidgetId of the widget whose size changed.
	 * @param listName
	 *            The name of the image list from which the file is taken.
	 * @param fileName
	 *            The filename of the image to be displayed.
	 */
	private void setImage(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
			final String listName, final String fileName) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_image);

		Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
		int width = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
		int height = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
		if (width <= 0 || height <= 0) {
			return;
		}

		if (fileName == null) {
			remoteViews.setImageViewResource(
					R.id.imageViewWidget,
					R.drawable.ic_launcher);
		}
		else {
			currentFileNames.put(appWidgetId, fileName);

			remoteViews.setImageViewBitmap(
					R.id.imageViewWidget,
					ImageUtil.getImageBitmap(fileName,
							Math.min(ImageUtil.MAX_BITMAP_SIZE, Math.max(width, height))));
		}

		Intent intent = DisplayRandomImageActivity.createIntent(context, listName, fileName, true);
		PendingIntent pendingIntent =
				PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		remoteViews.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId
	 *            The widget id.
	 * @param listName
	 *            The list name to be used by the widget.
	 * @param interval
	 *            The update interval.
	 */
	public static final void configure(final int appWidgetId, final String listName, final long interval) {
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, listName);
		listNames.put(appWidgetId, listName);
		currentFileNames.remove(appWidgetId);

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
		Intent intent = new Intent(context, ImageWidget.class);
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

		int[] ids;
		if (appWidgetId.length == 0) {
			ids = getAllWidgetIds();
		}
		else {
			ids = appWidgetId;
		}
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		context.sendBroadcast(intent);
	}

	/**
	 * Update timers for instances of the widget.
	 *
	 * @param appWidgetIds
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateTimers(final int... appWidgetIds) {
		Context context = Application.getAppContext();
		int[] ids;
		if (appWidgetIds.length == 0) {
			ids = getAllWidgetIds();
		}
		else {
			ids = appWidgetIds;
		}

		for (int appWidgetId : ids) {
			long interval =
					PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId, 0);

			if (interval > 0) {
				WidgetAlarmReceiver.setAlarm(context, appWidgetId, interval);
			}
		}
	}

	/**
	 * Get the ids of all widgets of this class.
	 *
	 * @return The ids of all widgets of this class.
	 */
	public static int[] getAllWidgetIds() {
		Context context = Application.getAppContext();
		return AppWidgetManager.getInstance(context).getAppWidgetIds(
				new ComponentName(context, ImageWidget.class));
	}

	/**
	 * Check if there is an ImageWidget of this id.
	 *
	 * @param appWidgetId
	 *            The widget id.
	 * @return true if there is an ImageWidget of this id.
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
