package de.jeisfeld.randomimage.notifications;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.SdMountReceiver;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.widgets.GenericWidget;
import de.jeisfeld.randomimagelib.R;

/**
 * Receiver for the alarm triggering the update of the image widget.
 */
public class NotificationAlarmReceiver extends BroadcastReceiver {
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		int notificationId = intent.getIntExtra(NotificationConfigurationFragment.STRING_NOTIFICATION_ID, -1);
		if (notificationId != -1) {
			NotificationUtil.displayRandomImageNotification(context, notificationId);
		}
	}

	/**
	 * Sets a repeating alarm that runs at the configured interval. When the alarm fires, the app broadcasts an
	 * Intent to this NotificationAlarmReceiver.
	 *
	 * @param context        The context in which the alarm is set.
	 * @param notificationId the notification id.
	 */
	public static final void setAlarm(final Context context, final int notificationId) {
		long frequency = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_frequency, notificationId, -1);
		if (frequency != -1) {
			NotificationAlarmReceiver.setAlarm(context, notificationId, frequency);
		}
	}

	/**
	 * Sets a repeating alarm that runs at the given interval. When the alarm fires, the app broadcasts an
	 * Intent to this NotificationAlarmReceiver.
	 *
	 * @param context        The context in which the alarm is set.
	 * @param notificationId the notification id.
	 * @param frequency      the frequency in which the alarm triggers.
	 */
	public static final void setAlarm(final Context context, final int notificationId, final long frequency) {
		if (frequency <= 0) {
			return;
		}
		Random random = new Random();

		PendingIntent alarmIntent = createAlarmIntent(context, notificationId);

		// Set the alarm
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		if (frequency < AlarmManager.INTERVAL_DAY) {
			alarmMgr.set(AlarmManager.ELAPSED_REALTIME, (int) (SystemClock.elapsedRealtime() + frequency * random.nextDouble()), alarmIntent);
		}
		else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(System.currentTimeMillis());

			calendar.set(Calendar.HOUR_OF_DAY, random.nextInt(24)); // MAGIC_NUMBER
			calendar.set(Calendar.MINUTE, random.nextInt(60)); // MAGIC_NUMBER
			calendar.set(Calendar.SECOND, random.nextInt(60)); // MAGIC_NUMBER

			// First ensure that it is in the past
			if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
				calendar.add(Calendar.DATE, -1);
			}

			// Then add the planned interval
			calendar.setTimeInMillis(calendar.getTimeInMillis() + frequency);

			alarmMgr.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), frequency, alarmIntent);
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
	 * @param context        The context in which the alarm is set.
	 * @param notificationId the notification id.
	 */
	public static final void cancelAlarm(final Context context, final int notificationId) {
		PendingIntent alarmIntent = createAlarmIntent(context, notificationId);

		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		alarmMgr.cancel(alarmIntent);

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

	/**
	 * Create a PendingIntent which can be used for creating or cancelling an alarm for a notification.
	 *
	 * @param context        The context in which the alarm is set.
	 * @param notificationId the notification id.
	 * @return The PendingIntent.
	 */
	private static PendingIntent createAlarmIntent(final Context context, final int notificationId) {
		Intent intent = new Intent(context, NotificationAlarmReceiver.class);
		intent.putExtra(NotificationConfigurationFragment.STRING_NOTIFICATION_ID, notificationId);
		return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	/**
	 * Create alarms for all configured notifications.
	 */
	public static void createAllNotificationAlarms() {
		List<Integer> notificationIds = NotificationSettingsActivity.getNotificationIds();
		for (int notificationId : notificationIds) {
			setAlarm(Application.getAppContext(), notificationId);
		}
	}
}
