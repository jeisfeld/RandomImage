package de.jeisfeld.randomimage.notifications;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.SdMountReceiver;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.widgets.GenericWidget;
import de.jeisfeld.randomimagelib.R;

/**
 * Receiver for the alarm triggering the update of the image widget.
 */
public class NotificationAlarmReceiver extends BroadcastReceiver {
	/**
	 * The resource key for the notification id.
	 */
	private static final String STRING_NOTIFICATION_ID = "de.eisfeldj.randomimage.NOTIFICATION_ID";
	/**
	 * The resource key for the flag indicating a cancellation.
	 */
	private static final String STRING_IS_CANCELLATION = "de.eisfeldj.randomimage.IS_CANCELLATION";
	/**
	 * The maximum number of days for a notification.
	 */
	private static final double MAX_ALARM_DAYS = 730;
	/**
	 * Time to wait with the alarm after boot.
	 */
	private static final int ALARM_WAIT_SECONDS = 60;
	/**
	 * The number of hours per day.
	 */
	private static final int HOURS_PER_DAY = (int) TimeUnit.DAYS.toHours(1);

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		int notificationId = intent.getIntExtra(STRING_NOTIFICATION_ID, -1);
		if (notificationId != -1) {
			boolean isCancellation = intent.getBooleanExtra(STRING_IS_CANCELLATION, false);
			if (isCancellation) {
				// Cancel the existing and trigger a new notification.
				NotificationUtil.cancelRandomImageNotification(context, notificationId);
				setAlarm(context, notificationId, false);
			}
			else {
				// Display the notification.
				cancelAlarm(context, notificationId, true);
				NotificationUtil.displayRandomImageNotification(context, notificationId);
			}
		}
	}

	/**
	 * Sets an alarm that runs at the given interval in order to trigger a notification. When the alarm fires, the app broadcasts an
	 * Intent to this NotificationAlarmReceiver. The alarm policies are determined by the notification properties.
	 *
	 * @param context          The context in which the alarm is set.
	 * @param notificationId   the notification id.
	 * @param useLastAlarmTime flag indicating if the last existing alarm time should be re-used.
	 */
	public static final void setAlarm(final Context context, final int notificationId, final boolean useLastAlarmTime) {
		int frequency = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_frequency, notificationId, 0);
		if (frequency == 0) {
			cancelAlarm(context, notificationId, false);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_notification_current_alarm_timestamp, notificationId);
			return;
		}

		int dailyStartTime = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_start_time, notificationId, -1);
		int dailyEndTime = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_daily_end_time, notificationId, -1);

		// Positive frequencies: expected numer of days
		double expectedDaysUntilAlarm = frequency;
		if (frequency < 0) {
			// Negative frequencies: portions of days
			expectedDaysUntilAlarm = -1.0 / frequency;

			if (frequency < -4) { // MAGIC_NUMBER
				// refer to hours rather than days if we are below quarter-day
				expectedDaysUntilAlarm *= HOURS_PER_DAY / (dailyEndTime - dailyStartTime);
			}
		}

		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent alarmIntent = createAlarmIntent(context, notificationId, false);
		Random random = new Random();

		if (useLastAlarmTime) {
			long oldAlarmTime = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_current_alarm_timestamp, notificationId, -1);

			if (oldAlarmTime >= 0) {
				int duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_duration, notificationId, 0);
				long oldAlarmExpirationTime = oldAlarmTime + TimeUnit.MINUTES.toMillis(duration);

				if (oldAlarmTime > System.currentTimeMillis()) {
					alarmMgr.set(AlarmManager.RTC, oldAlarmTime, alarmIntent);
				}
				else if (duration <= 0 || oldAlarmExpirationTime > System.currentTimeMillis()) {
					// Avoid showing the alarm immediately after startup, also in order to avoid issues while booting.
					long newAlarmTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ALARM_WAIT_SECONDS);

					// For alarms bigger than hourly, add some random minutes.
					if (frequency > -HOURS_PER_DAY) {
						newAlarmTime += random.nextDouble() * TimeUnit.MINUTES.toMillis(10); // MAGIC_NUMBER
					}

					PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_current_alarm_timestamp, notificationId, newAlarmTime);
					alarmMgr.set(AlarmManager.RTC, newAlarmTime, alarmIntent);
				}
				return;
			}
			// in case of non-existing old alarm time or if old notification is already expired,
			// continue to generate a new alarm time based on notification configuration.
		}

		// Set the alarm

		int timerVariance = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, notificationId, -1);

		double daysUntilAlarm;

		switch (timerVariance) {
		case 0:
			// equal distribution at some distance from now
			daysUntilAlarm = (0.25 + 1.5 * random.nextDouble()) * expectedDaysUntilAlarm; // MAGIC_NUMBER
			break;
		case 1:
		default:
			// exponential distribution
			daysUntilAlarm = -expectedDaysUntilAlarm * Math.log(random.nextDouble());
			if (daysUntilAlarm > MAX_ALARM_DAYS) {
				daysUntilAlarm = MAX_ALARM_DAYS - random.nextDouble();
			}
		}

		// Determine how much of "notification time" is already elapsed today.
		Calendar currentTimeCalendar = Calendar.getInstance();
		Calendar startOfDayCalendar = Calendar.getInstance();
		startOfDayCalendar.setTimeInMillis(currentTimeCalendar.getTimeInMillis());
		startOfDayCalendar.set(Calendar.HOUR_OF_DAY, dailyStartTime);
		startOfDayCalendar.set(Calendar.MINUTE, 0);
		startOfDayCalendar.set(Calendar.SECOND, 0);

		if (currentTimeCalendar.get(Calendar.HOUR_OF_DAY) + HOURS_PER_DAY < dailyEndTime) {
			// after midnight. Therefore, the reference day is the previous day.
			startOfDayCalendar.add(Calendar.DATE, -1);
		}

		double usableMillisecondsPerDay = TimeUnit.HOURS.toMillis(dailyEndTime - dailyStartTime);
		// quota of the available time used today
		double usedTimeToday = ((double) currentTimeCalendar.getTimeInMillis() - startOfDayCalendar.getTimeInMillis()) / usableMillisecondsPerDay;

		long alarmTimeMillis;
		if (usedTimeToday < 0) {
			// we are before the allowed time window.
			int fullDaysUntilAlarm = (int) Math.floor(daysUntilAlarm);
			double partialDaysUntilAlarm = daysUntilAlarm - fullDaysUntilAlarm;
			startOfDayCalendar.add(Calendar.DATE, fullDaysUntilAlarm);
			alarmTimeMillis = startOfDayCalendar.getTimeInMillis() + (long) (partialDaysUntilAlarm * usableMillisecondsPerDay);
		}
		else if (usedTimeToday > 1) {
			// we are after the allowed time window.
			int fullDaysUntilAlarm = (int) Math.floor(daysUntilAlarm);
			double partialDaysUntilAlarm = daysUntilAlarm - fullDaysUntilAlarm;
			startOfDayCalendar.add(Calendar.DATE, fullDaysUntilAlarm + 1);
			alarmTimeMillis = startOfDayCalendar.getTimeInMillis() + (long) (partialDaysUntilAlarm * usableMillisecondsPerDay);
		}
		else {
			// we are inside the allowed time window.
			double leftTimeInCurrentWindow = 1 - usedTimeToday;
			if (daysUntilAlarm < leftTimeInCurrentWindow) {
				alarmTimeMillis = currentTimeCalendar.getTimeInMillis() + (long) (daysUntilAlarm * usableMillisecondsPerDay);
			}
			else {
				double daysUntilAlarmAfterToday = daysUntilAlarm - leftTimeInCurrentWindow;
				int fullDaysUntilAlarm = (int) Math.floor(daysUntilAlarmAfterToday);
				double partialDaysUntilAlarm = daysUntilAlarmAfterToday - fullDaysUntilAlarm;
				startOfDayCalendar.add(Calendar.DATE, fullDaysUntilAlarm + 1);
				alarmTimeMillis = startOfDayCalendar.getTimeInMillis() + (long) (partialDaysUntilAlarm * usableMillisecondsPerDay);
			}
		}

		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_current_alarm_timestamp, notificationId, alarmTimeMillis);
		alarmMgr.set(AlarmManager.RTC, alarmTimeMillis, alarmIntent);

		// Enable SdMountReceiver to automatically restart the alarm when the device is rebooted.
		ComponentName receiver = new ComponentName(context, SdMountReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	/**
	 * Sets an alarm that runs at the given interval in order to cancel a notification. When the alarm fires, the app broadcasts an
	 * Intent to this NotificationAlarmReceiver. The alarm policies are determined by the notification properties.
	 *
	 * @param context        The context in which the alarm is set.
	 * @param notificationId the notification id.
	 */
	public static final void setCancellationAlarm(final Context context, final int notificationId) {
		int duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_duration, notificationId, 0);
		if (duration == 0) {
			return;
		}

		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent alarmIntent = createAlarmIntent(context, notificationId, true);
		long alarmTimeMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(duration);

		alarmMgr.set(AlarmManager.RTC, alarmTimeMillis, alarmIntent);
	}

	/**
	 * Cancels the alarms for a notification.
	 *
	 * @param context             The context in which the alarm is set.
	 * @param notificationId      the notification id.
	 * @param isCancellationAlarm flag indicating if the regular alarm or the cancellation alarm shoud be cancelled.
	 */
	public static final void cancelAlarm(final Context context, final int notificationId, final boolean isCancellationAlarm) {
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		alarmMgr.cancel(createAlarmIntent(context, notificationId, isCancellationAlarm));

		if (!isCancellationAlarm) {
			List<Integer> allNotificationIds = NotificationSettingsActivity.getNotificationIds();
			if ((allNotificationIds.size() == 0 || allNotificationIds.size() == 1 && allNotificationIds.get(0) == notificationId)
					&& GenericWidget.getAllWidgetIds().size() == 0) {
				ComponentName receiver = new ComponentName(context, SdMountReceiver.class);
				PackageManager pm = context.getPackageManager();

				pm.setComponentEnabledSetting(receiver,
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP);
			}
		}
	}

	/**
	 * Create a PendingIntent which can be used for creating or cancelling an alarm for a notification.
	 *
	 * @param context        The context in which the alarm is set.
	 * @param notificationId the notification id.
	 * @param isCancellation flag indicating if the alarm should cancel the notification.
	 * @return The PendingIntent.
	 */
	private static PendingIntent createAlarmIntent(final Context context, final int notificationId, final boolean isCancellation) {
		Intent intent = new Intent("de.jeisfeld.randomimage.NOTIFICATION_ALARM_RECEIVER");
		intent.putExtra(STRING_NOTIFICATION_ID, notificationId);
		if (isCancellation) {
			intent.putExtra(STRING_IS_CANCELLATION, true);
		}
		int uniqueId = isCancellation ? -notificationId : notificationId;
		return PendingIntent.getBroadcast(context, uniqueId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	/**
	 * Create alarms for all configured notifications.
	 */
	public static void createAllNotificationAlarms() {
		List<Integer> notificationIds = NotificationSettingsActivity.getNotificationIds();
		for (int notificationId : notificationIds) {
			setAlarm(Application.getAppContext(), notificationId, true);
		}
	}
}
