package de.jeisfeld.randomimage.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * The extended widget, also displaying a changing image.
 */
public abstract class GenericWidget extends AppWidgetProvider {
	/**
	 * The list of all widget types.
	 */
	protected static final List<Class<? extends GenericWidget>> WIDGET_TYPES = new ArrayList<>();

	/**
	 * Number of pixels per dip.
	 */
	protected static final float DENSITY = Application.getAppContext().getResources().getDisplayMetrics().density;

	/**
	 * Intent flag to indicate the type of widget update.
	 */
	protected static final String EXTRA_UPDATE_TYPE = "de.jeisfeld.randomimage.UPDATE_TYPE";

	/**
	 * The number of milliseconds per second.
	 */
	private static final int MILLIS_PER_SECOND = (int) TimeUnit.SECONDS.toMillis(1);

	/**
	 * A temporary storage for the update types of the widgets.
	 */
	private static Map<Integer, UpdateType> mWidgetUpdateTypes = new HashMap<>();

	static {
		WIDGET_TYPES.add(MiniWidget.class);
		WIDGET_TYPES.add(ImageWidget.class);
		WIDGET_TYPES.add(StackedImageWidget.class);
	}

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
			int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			if (appWidgetIds != null && appWidgetIds.length > 0) {
				UpdateType updateType = (UpdateType) intent.getSerializableExtra(EXTRA_UPDATE_TYPE);
				for (int appWidgetId : appWidgetIds) {
					mWidgetUpdateTypes.put(appWidgetId, updateType);
				}
			}
		}

		super.onReceive(context, intent);
	}

	@Override
	public final void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (final int appWidgetId : appWidgetIds) {
			final UpdateType updateType = mWidgetUpdateTypes.get(appWidgetId);
			mWidgetUpdateTypes.remove(appWidgetId);

			final PendingResult result = goAsync();
			new Thread() {
				@Override
				public void run() {
					Looper.prepare();
					onUpdateWidget(context, appWidgetManager, appWidgetId, updateType);
					if (result != null) {
						result.finish();
					}
				}
			}.start();
		}
	}

	/**
	 * Called whenever a widget is updated.
	 *
	 * @param context          The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId      The appWidgetId for which an update is needed.
	 * @param updateType       flag indicating what should be updated.
	 */
	protected abstract void onUpdateWidget(Context context, AppWidgetManager appWidgetManager,
										   int appWidgetId, UpdateType updateType);

	// OVERRIDABLE
	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int appWidgetId : appWidgetIds) {
			WidgetAlarmReceiver.cancelAlarm(context, appWidgetId, false);

			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_list_name, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_display_name, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_use_default, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_scale_type, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_background, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_flip_behavior, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_change_timeout, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_change_with_tap, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_prevent_screen_timeout, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_timer_duration, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_last_usage_time, appWidgetId);
		}
	}

	/**
	 * Get the list name associated to an instance of the widget.
	 *
	 * @param appWidgetId The app widget id.
	 * @return The list name.
	 */
	protected static String getListName(final int appWidgetId) {
		String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);
		if (listName == null || listName.length() == 0) {
			listName = ImageRegistry.getCurrentListName();
		}
		return listName;
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @param listName    The list name to be used by the widget.
	 * @param interval    The update interval.
	 */
	public static void doBaseConfiguration(final int appWidgetId, final String listName, final long interval) {
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, listName);

		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, appWidgetId, interval);
		if (interval > 0) {
			WidgetAlarmReceiver.setAlarm(Application.getAppContext(), appWidgetId, interval * MILLIS_PER_SECOND);
		}
		else {
			WidgetAlarmReceiver.cancelAlarm(Application.getAppContext(), appWidgetId, false);
		}
	}

	/**
	 * Update instances of the widgets of a specific class.
	 *
	 * @param widgetClass the widget class (required if no appWidgetIds are given)
	 * @param updateType  flag indicating what should be updated.
	 * @param appWidgetId the list of instances to be updated. If empty, then all instances will be updated.
	 */
	protected static void updateInstances(final Class<? extends GenericWidget> widgetClass, final UpdateType updateType,
										  final int... appWidgetId) {
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
		intent.putExtra(EXTRA_UPDATE_TYPE, updateType);
		context.sendBroadcast(intent);
	}

	/**
	 * Update all instances of all widgets.
	 *
	 * @param updateType flag indicating what should be updated.
	 */
	public static void updateAllInstances(final UpdateType updateType) {
		for (Class<? extends GenericWidget> widgetType : WIDGET_TYPES) {
			updateInstances(widgetType, updateType);
		}
	}

	/**
	 * Update all instances of all widgets.
	 */
	public static void updateAllInstances() {
		updateAllInstances(UpdateType.NEW_LIST);
	}

	/**
	 * Update timers for instances of the widgets of a specific class.
	 *
	 * @param widgetClass  the widget class
	 * @param appWidgetIds the list of instances to be updated. If empty, then all instances will be updated.
	 */
	protected static void updateTimers(final Class<? extends GenericWidget> widgetClass, final int... appWidgetIds) {
		Context context = Application.getAppContext();
		int[] ids;
		if (appWidgetIds.length == 0) {
			ids = getAllWidgetIds(widgetClass);
		}
		else {
			ids = appWidgetIds;
		}

		for (int appWidgetId : ids) {
			long interval = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, appWidgetId, 0);

			if (interval > 0) {
				WidgetAlarmReceiver.setAlarm(context, appWidgetId, interval * MILLIS_PER_SECOND);
			}
		}
	}

	/**
	 * Get the ids of all widgets of this class.
	 *
	 * @param widgetClass the widget class
	 * @return The ids of all widgets of this class.
	 */
	protected static int[] getAllWidgetIds(final Class<? extends GenericWidget> widgetClass) {
		Context context = Application.getAppContext();
		return AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, widgetClass));
	}

	/**
	 * Get the list of all widgetIds of any widget of this app.
	 *
	 * @return The list of widgetIds of widgets of this app.
	 */
	public static ArrayList<Integer> getAllWidgetIds() {
		ArrayList<Integer> allWidgetIds = new ArrayList<>();

		for (Class<? extends GenericWidget> widgetClass : WIDGET_TYPES) {
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
	 * @param widgetClass the widget class
	 * @param appWidgetId The widget id.
	 * @return true if there is a widget of the given class of this id.
	 */
	public static boolean hasWidgetOfId(final Class<? extends GenericWidget> widgetClass, final int appWidgetId) {
		int[] allAppWidgetIds = getAllWidgetIds(widgetClass);

		for (int allAppWidgetId : allAppWidgetIds) {
			if (allAppWidgetId == appWidgetId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Retrieve all widgetIds for widgets related to a specific list name.
	 *
	 * @param listName The list name.
	 * @return The ids of widgets configured for this list name.
	 */
	public static ArrayList<Integer> getWidgetIdsForName(final String listName) {
		ArrayList<Integer> widgetIdsForName = new ArrayList<>();

		for (int appWidgetId : getAllWidgetIds()) {
			if (listName.equals(getListName(appWidgetId))) {
				widgetIdsForName.add(appWidgetId);
			}
		}

		return widgetIdsForName;
	}

	/**
	 * Get the fragment class of the widget.
	 *
	 * @return The fragment class.
	 */
	protected abstract Class<? extends GenericWidgetConfigurationActivity> getConfigurationActivityClass(); // SUPPRESS_CHECKSTYLE

	/**
	 * Types in which the widget can be updated.
	 */
	public enum UpdateType {
		/**
		 * Update the image automatically.
		 */
		NEW_IMAGE_AUTOMATIC,
		/**
		 * Update of the image triggered by the user.
		 */
		NEW_IMAGE_BY_USER,
		/**
		 * Update of the image list.
		 */
		NEW_LIST,
		/**
		 * Update of the scaling of the image.
		 */
		SCALING,
		/**
		 * Update the widget buttons and background.
		 */
		BUTTONS_BACKGROUND,
	}
}
