package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;

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
	 * The list of all widget types.
	 */
	private static final Class<?>[] WIDGET_TYPES = { MiniWidget.class, ImageWidget.class, StackedImageWidget.class };

	/**
	 * Number of pixels per dip.
	 */
	protected static final float DENSITY = Application.getAppContext().getResources().getDisplayMetrics().density;

	/**
	 * The names of the image lists associated to any widget.
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
	protected static final String getListName(final int appWidgetId) {
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
	 *            the widget class (required if no appWidgetIds are given)
	 * @param appWidgetId
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	protected static final void updateInstances(final Class<?> widgetClass, final int... appWidgetId) {
		if (widgetClass == null) {
			return;
		}

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
	public static ArrayList<Integer> getAllWidgetIds() {
		ArrayList<Integer> allWidgetIds = new ArrayList<Integer>();

		for (Class<?> widgetClass : WIDGET_TYPES) {
			int[] widgetIds = getAllWidgetIds(widgetClass);
			if (widgetIds != null) {
				for (int widgetId : widgetIds) {
					allWidgetIds.add(widgetId);
				}
			}
		}

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

	/**
	 * Retrieve all widgetIds for widgets related to a specific list name.
	 *
	 * @param listName
	 *            The list name.
	 * @return The ids of widgets configured for this list name.
	 */
	public static ArrayList<Integer> getWidgetIdsForName(final String listName) {
		ArrayList<Integer> widgetIdsForName = new ArrayList<Integer>();

		for (int appWidgetId : getAllWidgetIds()) {
			if (listName.equals(getListName(appWidgetId))) {
				widgetIdsForName.add(appWidgetId);
			}
		}

		return widgetIdsForName;
	}

	/**
	 * Update the list name in all widgets.
	 *
	 * @param oldName
	 *            The old name.
	 * @param newName
	 *            The new name.
	 */
	public static void updateListName(final String oldName, final String newName) {
		for (Class<?> widgetClass : WIDGET_TYPES) {
			int[] appWidgetIds = getAllWidgetIds(widgetClass);
			for (int appWidgetId : appWidgetIds) {
				if (oldName.equals(getListName(appWidgetId))) {
					PreferenceUtil
							.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, newName);
					listNames.put(appWidgetId, newName);
					updateInstances(widgetClass, appWidgetId);
				}
			}
		}
	}

}
