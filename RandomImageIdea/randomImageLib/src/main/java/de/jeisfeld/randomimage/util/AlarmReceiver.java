package de.jeisfeld.randomimage.util;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import de.jeisfeld.randomimage.SdMountReceiver;

/**
 * Receiver for the alarm triggering the update of the image widget.
 */
public abstract class AlarmReceiver extends BroadcastReceiver {
	/**
	 * The resource key for the flag indicating a cancellation.
	 */
	protected static final String STRING_IS_CANCELLATION = "de.eisfeldj.randomimage.IS_CANCELLATION";
	/**
	 * Threshold (in seconds) below which alarms should be exact.
	 */
	private static final int EXACT_THRESHOLD = 900;
	/**
	 * Threshold (in seconds) below which alarms should use AlarmClock (which is the only real exact alarm in Android 6).
	 */
	private static final int CLOCK_THRESHOLD = 300;

	/**
	 * Set an alarm.
	 *
	 * @param context     The context
	 * @param alarmTime   The alarm time
	 * @param alarmIntent The alarm intent
	 * @param alarmType   flag indicating if the alarm time should be exact.
	 */
	protected static void setAlarm(final Context context, final long alarmTime, final PendingIntent alarmIntent, final AlarmType alarmType) {
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (VERSION.SDK_INT >= VERSION_CODES.M) {
			switch (alarmType) {
			case CLOCK:
				alarmMgr.setAlarmClock(new AlarmClockInfo(alarmTime, alarmIntent), alarmIntent);
				break;
			case EXACT:
				alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
				break;
			case INEXACT:
			default:
				alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
				break;
			}
		}
		else {
			if (alarmType != AlarmType.INEXACT) {
				alarmMgr.setExact(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
			}
			else {
				alarmMgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
			}
		}
	}

	/**
	 * Get the alarm type indicating the exactness of the alarm.
	 *
	 * @param context     The context in which the alarm is set.
	 * @param frequency The alarm frequency in seconds.
	 * @return The alarm type.
	 */
	protected static AlarmType getAlarmType(final Context context, final long frequency) {
		if (SystemUtil.isAtLeastVersion(VERSION_CODES.UPSIDE_DOWN_CAKE)) {
			AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			if (alarmMgr == null || !alarmMgr.canScheduleExactAlarms()) {
				return AlarmType.INEXACT;
			}
		}

		if (frequency < CLOCK_THRESHOLD) {
			return AlarmType.CLOCK;
		}
		else if (frequency < EXACT_THRESHOLD) {
			return AlarmType.EXACT;
		}
		else {
			return AlarmType.INEXACT;
		}
	}

	/**
	 * Enable SdMountReceiver to automatically restart the alarm when the device is rebooted.
	 *
	 * @param context The context.
	 */
	protected static void reEnableAlarmsOnBoot(final Context context) {
		ComponentName receiver = new ComponentName(context, SdMountReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);

	}

	/**
	 * The type of alarm to be set.
	 */
	private enum AlarmType {
		/**
		 * Inexact alarm.
		 */
		INEXACT,
		/**
		 * Exact alarm.
		 */
		EXACT,
		/**
		 * Clock alarm.
		 */
		CLOCK
	}
}
