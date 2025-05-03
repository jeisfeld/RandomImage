package de.jeisfeld.randomimage.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.util.AlarmReceiver;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.widgets.GenericWidget;
import de.jeisfeld.randomimagelib.R;

/**
 * Receiver for the alarm triggering the update of the image widget.
 */
public class NotificationAlarmReceiver extends AlarmReceiver {
	/**
	 * The resource key for the notification id.
	 */
	private static final String STRING_NOTIFICATION_ID = "de.eisfeldj.randomimage.NOTIFICATION_ID";
	/**
	 * Time to wait with the alarm after boot.
	 */
	private static final int ALARM_WAIT_SECONDS = 60;
	/**
	 * The number of seconds per day.
	 */
	private static final int SECONDS_PER_DAY = (int) TimeUnit.DAYS.toSeconds(1);
	/**
	 * The threshold above which "number of days" are counted rather than "number of seconds".
	 */
	private static final int DAY_THRESHOLD = (int) TimeUnit.HOURS.toSeconds(12);
	/**
	 * Time (in milliseconds) after which timers are considered as outdated.
	 */
	private static final long OUTDATED_INTERVAL = TimeUnit.MINUTES.toMillis(10);

	/**
	 * Timer variance constant for no variance.
	 */
	private static final int TIMER_VARIANCE_NONE = 0;
	/**
	 * Timer variance constant for small variance.
	 */
	private static final int TIMER_VARIANCE_SMALL = 1;
	/**
	 * Timer variance constant for big variance.
	 */
	private static final int TIMER_VARIANCE_BIG = 2;

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
	public static void setAlarm(final Context context, final int notificationId, final boolean useLastAlarmTime) {
		long frequency = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_timer_duration, notificationId, 0);
		if (frequency == 0) {
			cancelAlarm(context, notificationId, false);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_notification_current_alarm_timestamp, notificationId);
			return;
		}

		String dailyStartTimeString = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_daily_start_time, notificationId);
		int dailyStartTime = timeStringToSeconds(dailyStartTimeString);
		String dailyEndTimeString = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_daily_end_time, notificationId);
		int dailyEndTime = timeStringToSeconds(dailyEndTimeString);
		int dailyDuration = dailyEndTime - dailyStartTime;
		if (dailyDuration <= 0) {
			dailyDuration = dailyDuration + 86400;
		}

		double expectedDaysUntilAlarm = (double) frequency / (frequency < DAY_THRESHOLD ? dailyDuration : SECONDS_PER_DAY);

		PendingIntent alarmIntent = createAlarmIntent(context, notificationId, false, true);
		Random random = new Random();

		if (useLastAlarmTime) {
			long oldAlarmTime = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_current_alarm_timestamp, notificationId, -1);

			if (oldAlarmTime >= 0) {
				if (oldAlarmTime > System.currentTimeMillis()) {
					setAlarm(context, oldAlarmTime, alarmIntent, getAlarmType(context, frequency));
					return;
				}

				long expectedDuration = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_duration, notificationId, 0);
				long duration;
				if (expectedDuration > 0) {
					duration = 0;
				}
				else {
					int timerVariance = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_duration_variance, notificationId, -1);
					duration = (long) getRandomizedDuration(expectedDuration, timerVariance);
				}
				long oldAlarmExpirationTime = oldAlarmTime + TimeUnit.SECONDS.toMillis(duration);

				if (frequency < 2 * TimeUnit.SECONDS.toMillis(ALARM_WAIT_SECONDS)) {
					// for short alarms, just create new alarm
					setAlarm(context, notificationId, false);
					return;
				}

				if (duration <= 0 || oldAlarmExpirationTime > System.currentTimeMillis()) {
					// Avoid showing the alarm immediately after startup, also in order to avoid issues while booting.
					long newAlarmTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ALARM_WAIT_SECONDS);

					// For alarms bigger than daily, add some random minutes.
					if (frequency >= SECONDS_PER_DAY) {
						newAlarmTime += (long) (random.nextDouble() * TimeUnit.MINUTES.toMillis(10)); // MAGIC_NUMBER
					}

					PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_current_alarm_timestamp, notificationId, newAlarmTime);
					setAlarm(context, newAlarmTime, alarmIntent, getAlarmType(context, frequency));
					return;
				}
			}
			// in case of non-existing old alarm time or if old notification is already expired,
			// continue to generate a new alarm time based on notification configuration.
		}

		// Set the alarm
		int timerVariance = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, notificationId, -1);
		double daysUntilAlarm = getRandomizedDuration(expectedDaysUntilAlarm, timerVariance);

		// Determine how much of "notification time" is already elapsed today.
		Calendar currentTimeCalendar = Calendar.getInstance();
		int secondsSinceMidnight = currentTimeCalendar.get(Calendar.HOUR_OF_DAY) * 3600 + currentTimeCalendar.get(Calendar.MINUTE) * 60 + currentTimeCalendar.get(Calendar.SECOND);
		Calendar startOfDayCalendar = getCalendarForToday(dailyStartTimeString);

		if (dailyStartTime > dailyEndTime && dailyEndTime > secondsSinceMidnight) {
			// after midnight. Therefore, the reference day is the previous day.
			startOfDayCalendar.add(Calendar.DATE, -1);
		}

		double usableMillisecondsPerDay = TimeUnit.SECONDS.toMillis(dailyDuration);
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
		setAlarm(context, alarmTimeMillis, alarmIntent, getAlarmType(context, frequency));

		reEnableAlarmsOnBoot(context);
	}

	/**
	 * Convert time String into number of seconds of the day.
	 *
	 * @param time The time String in format hh:mm
	 * @return The number of seconds of the day.
	 */
	private static int timeStringToSeconds(String time) {
		if (time == null) {
			return 0;
		}
		try {
			String[] parts = time.split(":");
			int hour = Integer.parseInt(parts[0]);
			int minute = Integer.parseInt(parts[1]);
			return hour * 3600 + minute * 60;
		}
		catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Get a calendar that represents a given time on current date
	 *
	 * @param time The time String in format hh:mm
	 * @return The calendar.
	 */
	private static Calendar getCalendarForToday(String time) {
		Calendar calendar = Calendar.getInstance();
		try {
			String[] parts = time.split(":");
			int hour = Integer.parseInt(parts[0]);
			int minute = Integer.parseInt(parts[1]);

			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minute);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
		}
		catch (Exception e) {
			// ignore
		}
		return calendar;
	}

	/**
	 * Sets an alarm that runs at the given interval in order to cancel a notification. When the alarm fires, the app broadcasts an
	 * Intent to this NotificationAlarmReceiver. The alarm policies are determined by the notification properties.
	 *
	 * @param context        The context in which the alarm is set.
	 * @param notificationId the notification id.
	 */
	public static void setCancellationAlarm(final Context context, final int notificationId) {
		long expectedDuration = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_duration, notificationId, 0);
		if (expectedDuration == 0) {
			return;
		}

		int timerVariance = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_duration_variance, notificationId, -1);
		double duration = getRandomizedDuration(expectedDuration, timerVariance);

		PendingIntent alarmIntent = createAlarmIntent(context, notificationId, true, true);
		long alarmTimeMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis((long) duration);

		setAlarm(context, alarmTimeMillis, alarmIntent, getAlarmType(context, expectedDuration));
	}

	/**
	 * Get a randomized timer duration.
	 *
	 * @param baseDuration The configured timer duration.
	 * @param variance     The configured timer variance.
	 * @return The randomized timer duration.
	 */
	private static double getRandomizedDuration(final double baseDuration, final int variance) {
		Random random = new Random();
		switch (variance) {
		case TIMER_VARIANCE_SMALL:
			// equal distribution at some distance from now
			return (0.5 + random.nextDouble()) * baseDuration; // MAGIC_NUMBER
		case TIMER_VARIANCE_BIG:
			// exponential distribution
			return -baseDuration * Math.log(random.nextDouble());
		case TIMER_VARIANCE_NONE:
		default:
			return baseDuration;
		}
	}

	/**
	 * Cancels the alarms for a notification.
	 *
	 * @param context             The context in which the alarm is set.
	 * @param notificationId      the notification id.
	 * @param isCancellationAlarm flag indicating if the regular alarm or the cancellation alarm should be cancelled.
	 */
	public static void cancelAlarm(final Context context, final int notificationId, final boolean isCancellationAlarm) {
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(createAlarmIntent(context, notificationId, isCancellationAlarm, false));

		if (!isCancellationAlarm) {
			List<Integer> allNotificationIds = NotificationSettingsActivity.getNotificationIds();
			if ((allNotificationIds.isEmpty() || allNotificationIds.size() == 1 && allNotificationIds.get(0) == notificationId)
					&& GenericWidget.getAllWidgetIds().isEmpty()) {
				reEnableAlarmsOnBoot(context);
			}
		}
	}

	/**
	 * Create a PendingIntent which can be used for creating or cancelling an alarm for a notification.
	 *
	 * @param context        The context in which the alarm is set.
	 * @param notificationId the notification id.
	 * @param isCancellation flag indicating if the alarm should cancel the notification.
	 * @param isNew          flag indicating if this is a new intent or if an existing intent should be reused.
	 * @return The PendingIntent.
	 */
	private static PendingIntent createAlarmIntent(final Context context, final int notificationId,
												   final boolean isCancellation, final boolean isNew) {
		Intent intent = new Intent(context, NotificationAlarmReceiver.class);
		intent.putExtra(STRING_NOTIFICATION_ID, notificationId);
		intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		if (isCancellation) {
			intent.putExtra(STRING_IS_CANCELLATION, true);
		}
		int uniqueId = isCancellation ? -notificationId : notificationId;
		return PendingIntent.getBroadcast(context, uniqueId, intent,
				isNew ? PendingIntent.FLAG_CANCEL_CURRENT | SystemUtil.IMMUTABLE_FLAG
						: PendingIntent.FLAG_UPDATE_CURRENT | SystemUtil.IMMUTABLE_FLAG);
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

	/**
	 * Check if there are outdated notification alarms (indicating that there were issues leading to lost timers).
	 * If so, then re-create notification alarms.
	 */
	public static void createNotificationAlarmsIfOutdated() {
		List<Integer> notificationIds = NotificationSettingsActivity.getNotificationIds();
		boolean isOutdated = false;
		for (int notificationId : notificationIds) {
			long oldAlarmTime = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_current_alarm_timestamp, notificationId, -1);
			if (oldAlarmTime < System.currentTimeMillis() - OUTDATED_INTERVAL) {
				isOutdated = true;
				break;
			}
		}
		if (isOutdated) {
			createAllNotificationAlarms();
		}
	}
}
