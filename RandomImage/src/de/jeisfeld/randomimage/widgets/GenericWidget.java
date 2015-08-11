package de.jeisfeld.randomimage.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;

/**
 * The extended widget, also displaying a changing image.
 */
public abstract class GenericWidget extends AppWidgetProvider {
	/**
	 * Number of pixels per dip.
	 */
	protected static final float DENSITY = Application.getAppContext().getResources().getDisplayMetrics().density;

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

			onUpdateWidget(context, appWidgetManager, appWidgetId, listName);
		}
	}

	/**
	 * Called whenever a widget is updated.
	 *
	 * @param context
	 *            The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager
	 *            A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetIds
	 *            The appWidgetIds for which an update is needed. Note that this may be all of the AppWidget instances
	 *            for this provider, or just a subset of them.
	 * @param listName
	 *            the list name of the widget.
	 */
	protected abstract void onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId, final String listName);

	// OVERRIDABLE
	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];
			WidgetAlarmReceiver.cancelAlarm(context, appWidgetId);
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
	protected final String getListName(final int appWidgetId) {
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
	 *            The update interval.
	 */
	public static final void doBaseConfiguration(final int appWidgetId, final String listName, final long interval) {
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, listName);
		listNames.put(appWidgetId, listName);

		if (interval > 0) {
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId, interval);
			WidgetAlarmReceiver.setAlarm(Application.getAppContext(), appWidgetId, interval);
		}
	}

	/**
	 * Update instances of the widgets of a specific class.
	 *
	 * @param widgetClass
	 *            the widget class
	 * @param appWidgetId
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	protected static final void updateInstances(final Class<?> widgetClass, final int... appWidgetId) {
		Context context = Application.getAppContext();
		Intent intent = new Intent(context, widgetClass);
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

		int[] ids;
		if (appWidgetId.length == 0) {
			ids = getAllWidgetIds(widgetClass);
		}
		else {
			ids = appWidgetId;
		}
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		context.sendBroadcast(intent);
	}

	/**
	 * Update timers for instances of the widgets of a specific class.
	 *
	 * @param widgetClass
	 *            the widget class
	 * @param appWidgetIds
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	protected static final void updateTimers(final Class<?> widgetClass, final int... appWidgetIds) {
		Context context = Application.getAppContext();
		int[] ids;
		if (appWidgetIds.length == 0) {
			ids = getAllWidgetIds(widgetClass);
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
	 * @param widgetClass
	 *            the widget class
	 * @return The ids of all widgets of this class.
	 */
	protected static int[] getAllWidgetIds(final Class<?> widgetClass) {
		Context context = Application.getAppContext();
		return AppWidgetManager.getInstance(context).getAppWidgetIds(
				new ComponentName(context, widgetClass));
	}

	/**
	 * Get the list of all widgetIds of any widget of this app.
	 *
	 * @return The list of widgetIds of widgets of this app.
	 */
	public static int[] getAllWidgetIds() {
		int[] allImageWidgetIds = ImageWidget.getAllWidgetIds();
		int[] allStackedImageWidgetIds = StackedImageWidget.getAllWidgetIds();
		int[] allMiniWidgetIds = MiniWidget.getAllWidgetIds();

		int[] allWidgetIds =
				new int[allImageWidgetIds.length + allStackedImageWidgetIds.length + allMiniWidgetIds.length];
		System.arraycopy(allImageWidgetIds, 0, allWidgetIds, 0, allImageWidgetIds.length);
		System.arraycopy(allStackedImageWidgetIds, 0, allWidgetIds, allImageWidgetIds.length,
				allStackedImageWidgetIds.length);
		System.arraycopy(allMiniWidgetIds, 0, allWidgetIds, allImageWidgetIds.length + allStackedImageWidgetIds.length,
				allMiniWidgetIds.length);
		return allWidgetIds;
	}

	/**
	 * Check if there is a widget of the given class of this id.
	 *
	 * @param widgetClass
	 *            the widget class
	 * @param appWidgetId
	 *            The widget id.
	 * @return true if there is a widget of the given class of this id.
	 */
	public static boolean hasWidgetOfId(final Class<?> widgetClass, final int appWidgetId) {
		int[] allAppWidgetIds = getAllWidgetIds(widgetClass);

		for (int i = 0; i < allAppWidgetIds.length; i++) {
			if (allAppWidgetIds[i] == appWidgetId) {
				return true;
			}
		}
		return false;
	}

}
