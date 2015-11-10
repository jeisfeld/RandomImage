package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import de.jeisfeld.randomimage.SdMountReceiver;

/**
 * Receiver for the alarm triggering the update of the image widget.
 */
public class WidgetAlarmReceiver extends BroadcastReceiver {
	/**
	 * The timer start hour.
	 */
	private static final int TIMER_HOUR = 3;

	/**
	 * The timer start minutes.
	 */
	private static final int TIMER_MINUTES = 30;

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

		if (StackedImageWidget.hasWidgetOfId(appWidgetId)) {
			StackedImageWidget.updateInstances(appWidgetId);
		}
	}

	/**
	 * Sets a repeating alarm that runs once a day at the given interval. When the alarm fires, the app broadcasts an
	 * Intent to this WakefulBroadcastReceiver.
	 *
	 * @param context     The context in which the alarm is set.
	 * @param appWidgetId the widget id.
	 * @param interval    the interval in which the alarm triggers.
	 */
	public static final void setAlarm(final Context context, final int appWidgetId, final long interval) {
		if (interval <= 0) {
			return;
		}

		PendingIntent alarmIntent = createAlarmIntent(context, appWidgetId);

		// Set the alarm
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		if (interval < AlarmManager.INTERVAL_FIFTEEN_MINUTES) {
			alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval,
					interval, alarmIntent);
		}
		else if (interval < AlarmManager.INTERVAL_DAY) {
			alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval,
					interval, alarmIntent);
		}
		else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(System.currentTimeMillis());

			calendar.set(Calendar.HOUR_OF_DAY, TIMER_HOUR);
			calendar.set(Calendar.MINUTE, TIMER_MINUTES);
			calendar.set(Calendar.SECOND, 0);

			// First ensure that it is in the past
			if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
				calendar.add(Calendar.DATE, -1);
			}

			// Then add the planned interval
			calendar.setTimeInMillis(calendar.getTimeInMillis() + interval);

			alarmMgr.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), interval, alarmIntent);
		}

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
	 * @param context     The context in which the alarm is set.
	 * @param appWidgetId the widget id.
	 */
	public static final void cancelAlarm(final Context context, final int appWidgetId) {
		PendingIntent alarmIntent = createAlarmIntent(context, appWidgetId);

		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		alarmMgr.cancel(alarmIntent);

		ArrayList<Integer> allWidgetIds = GenericWidget.getAllWidgetIds();
		if (allWidgetIds.size() == 0 || allWidgetIds.size() == 1 && allWidgetIds.get(0) == appWidgetId) {
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
	 * @param context     The context in which the alarm is set.
	 * @param appWidgetId the widget id.
	 * @return The PendingIntent.
	 */
	private static PendingIntent createAlarmIntent(final Context context, final int appWidgetId) {
		Intent intent = new Intent(context, WidgetAlarmReceiver.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

}
