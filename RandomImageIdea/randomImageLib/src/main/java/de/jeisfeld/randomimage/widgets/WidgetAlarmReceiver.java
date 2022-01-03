package de.jeisfeld.randomimage.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.notifications.NotificationSettingsActivity;
import de.jeisfeld.randomimage.util.AlarmReceiver;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.widgets.GenericWidget.UpdateType;
import de.jeisfeld.randomimagelib.R;

/**
 * Receiver for the alarm triggering the update of the image widget.
 */
public class WidgetAlarmReceiver extends AlarmReceiver {
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

		boolean isCancellation = intent.getBooleanExtra(STRING_IS_CANCELLATION, false);
		if (isCancellation) {
			DisplayRandomImageActivity.finishActivityForWidget(context, appWidgetId);
		}
		else {
			if (ImageWidget.hasWidgetOfId(appWidgetId)) {
				ImageWidget.updateInstances(UpdateType.NEW_IMAGE_AUTOMATIC, appWidgetId);
			}
			if (StackedImageWidget.hasWidgetOfId(appWidgetId)) {
				StackedImageWidget.updateInstances(UpdateType.NEW_IMAGE_AUTOMATIC, appWidgetId);
			}
		}
	}

	/**
	 * Sets a repeating alarm that runs at the given interval. When the alarm fires, the app broadcasts an
	 * Intent to this WidgetAlarmReceiver.
	 *
	 * @param context     The context in which the alarm is set.
	 * @param appWidgetId the widget id.
	 * @param interval    the interval (in milliseconds) in which the alarm triggers.
	 */
	public static void setAlarm(final Context context, final int appWidgetId, final long interval) {
		if (interval <= 0) {
			return;
		}

		PendingIntent alarmIntent = createAlarmIntent(context, appWidgetId, false);

		// Set the alarm
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		if (interval < AlarmManager.INTERVAL_FIFTEEN_MINUTES) {
			alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval,
					interval, alarmIntent);
		}
		else if (interval < AlarmManager.INTERVAL_DAY) {
			alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval,
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

			alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval, alarmIntent);
		}

		// Enable SdMountReceiver to automatically restart the alarm when the device is rebooted.
		reEnableAlarmsOnBoot(context);
	}

	/**
	 * Cancels the alarm.
	 *
	 * @param context     The context in which the alarm is set.
	 * @param appWidgetId the widget id.
	 * @param isCancellationAlarm flag indicating if the regular alarm or the cancellation alarm should be cancelled.
	 */
	public static void cancelAlarm(final Context context, final int appWidgetId, final boolean isCancellationAlarm) {
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(createAlarmIntent(context, appWidgetId, isCancellationAlarm));

		if (!isCancellationAlarm) {
			ArrayList<Integer> allWidgetIds = GenericWidget.getAllWidgetIds();
			if ((allWidgetIds.size() == 0 || allWidgetIds.size() == 1 && allWidgetIds.get(0) == appWidgetId)
					&& NotificationSettingsActivity.getNotificationIds().size() == 0) {
				reEnableAlarmsOnBoot(context);
			}
		}
	}

	/**
	 * Sets an alarm that runs at the given interval in order to cancel the triggered widget. When the alarm fires, the app broadcasts an
	 * Intent to this WidgetAlarmReceiver.
	 *
	 * @param context     The context in which the alarm is set.
	 * @param appWidgetId the widget id.
	 */
	public static void setCancellationAlarm(final Context context, final int appWidgetId) {
		long timeout = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_timeout, appWidgetId, 0);
		if (timeout == 0) {
			return;
		}

		PendingIntent alarmIntent = createAlarmIntent(context, appWidgetId, true);
		long alarmTimeMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeout);

		setAlarm(context, alarmTimeMillis, alarmIntent, getAlarmType(timeout));
	}

	/**
	 * Create a PendingIntent which can be used for creating or cancelling an alarm for a widget.
	 *
	 * @param context     The context in which the alarm is set.
	 * @param appWidgetId the widget id.
	 * @param isCancellation flag indicating if the alarm should cancel the widget activity.
	 * @return The PendingIntent.
	 */
	private static PendingIntent createAlarmIntent(final Context context, final int appWidgetId, final boolean isCancellation) {
		Intent intent = new Intent(context, WidgetAlarmReceiver.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		if (isCancellation) {
			intent.putExtra(STRING_IS_CANCELLATION, true);
		}
		int uniqueId = isCancellation ? -appWidgetId : appWidgetId;
		return PendingIntent.getBroadcast(context, uniqueId, intent, PendingIntent.FLAG_CANCEL_CURRENT | SystemUtil.IMMUTABLE_FLAG);
	}
}
