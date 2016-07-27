package de.jeisfeld.randomimage.util;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

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
				alarmMgr.setAlarmClock(new AlarmClockInfo(alarmTime, null), alarmIntent);
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
			if (alarmType != AlarmType.INEXACT && VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
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
	 * @param frequency The alarm frequency in seconds.
	 * @return The alarm type.
	 */
	protected static AlarmType getAlarmType(final long frequency) {
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
