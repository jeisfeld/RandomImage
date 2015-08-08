package de.jeisfeld.randomimage.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import de.jeisfeld.randomimage.SdMountReceiver;

/**
 * Receiver for the alarm triggering the update of the image widget.
 */
public class WidgetAlarmReceiver extends BroadcastReceiver {
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		int appWidgetId =
				intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			return;
		}

		if (ImageWidget.hasWidgetOfId(appWidgetId)) {
			ImageWidget.updateInstances(appWidgetId);
		}
	}

	/**
	 * Sets a repeating alarm that runs once a day at the given interval. When the alarm fires, the app broadcasts an
	 * Intent to this WakefulBroadcastReceiver.
	 *
	 * @param context
	 *            The context in which the alarm is set.
	 * @param appWidgetId
	 *            the widget id.
	 * @param widgetType
	 *            the widget class.
	 * @param interval
	 *            the interval in which the alarm triggers.
	 */
	public static final void setAlarm(final Context context, final int appWidgetId,
			final Class<?> widgetType, final long interval) {
		PendingIntent alarmIntent = createAlarmIntent(context, appWidgetId);

		// Set the alarm
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, interval, interval, alarmIntent);

		// Enable SdMountReceiver to automatically restart the alarm when the device is rebooted.
		ComponentName receiver = new ComponentName(context, SdMountReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	/**
	 * Cancels the alarm.
	 *
	 * @param context
	 *            The context in which the alarm is set.
	 * @param appWidgetId
	 *            the widget id.
	 */
	public static final void cancelAlarm(final Context context, final int appWidgetId) {
		PendingIntent alarmIntent = createAlarmIntent(context, appWidgetId);

		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		alarmMgr.cancel(alarmIntent);

		int[] allWidgetIds = ImageWidget.getAllWidgetIds();
		if (allWidgetIds.length == 0 || allWidgetIds.length == 1 && allWidgetIds[0] == appWidgetId) {
			ComponentName receiver = new ComponentName(context, SdMountReceiver.class);
			PackageManager pm = context.getPackageManager();

			pm.setComponentEnabledSetting(receiver,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
		}
	}

	/**
	 * Create a PendingIntent which can be used for creating or cancelling an alarm for a widget.
	 *
	 * @param context
	 *            The context in which the alarm is set.
	 * @param appWidgetId
	 *            the widget id.
	 * @return The PendingIntent.
	 */
	private static PendingIntent createAlarmIntent(final Context context, final int appWidgetId) {
		Intent intent = new Intent(context, WidgetAlarmReceiver.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

}
