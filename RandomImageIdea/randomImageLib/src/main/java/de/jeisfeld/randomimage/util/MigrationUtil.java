package de.jeisfeld.randomimage.util;

import java.util.concurrent.TimeUnit;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.notifications.NotificationSettingsActivity;
import de.jeisfeld.randomimage.widgets.GenericWidget;
import de.jeisfeld.randomimagelib.R;

/**
 * Utility class used for version migrations.
 */
public final class MigrationUtil {

	/**
	 * The number of milliseconds per second.
	 */
	private static final int MILLIS_PER_SECOND = (int) TimeUnit.SECONDS.toMillis(1);

	/**
	 * Hide default constructor.
	 */
	private MigrationUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Check if there has been a version upgrade. If so, do required migration steps.
	 */
	public static void migrateAppVersion() {
		int currentVersion = Application.getVersion();
		int lastVersion = PreferenceUtil.getSharedPreferenceInt(R.string.key_last_app_version, currentVersion);

		if (currentVersion != lastVersion) {
			doMigration(lastVersion, currentVersion);
		}

		PreferenceUtil.setSharedPreferenceInt(R.string.key_last_app_version, currentVersion);
	}

	/**
	 * Do the migration into a certain app version from a prior version.
	 *
	 * @param fromVersion the prior version
	 * @param toVersion   the new version
	 */
	private static void doMigration(final int fromVersion, final int toVersion) {
		for (int i = fromVersion + 1; i <= toVersion; i++) {
			doMigration(i);
		}
	}

	/**
	 * Do the migration into a certain app version from the predecessor version.
	 *
	 * @param toVersion the new version
	 */
	private static void doMigration(final int toVersion) {
		switch (toVersion) {
		case 19: // MAGIC_NUMBER
			doMigrationToVersion19();
			break;
		case 20: // MAGIC_NUMBER
			doMigrationToVersion20();
			break;
		default:
			break;
		}
	}

	/**
	 * Do the migration steps for migration into app version 19.
	 */
	private static void doMigrationToVersion19() {
		for (int appWidgetId : GenericWidget.getAllWidgetIds()) {
			long frequency = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId, 0);
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, appWidgetId, frequency / MILLIS_PER_SECOND);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_alarm_interval, appWidgetId);
		}

		for (int notificationId : NotificationSettingsActivity.getNotificationIds()) {
			int frequency = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_frequency, notificationId, 0);
			long newFrequency;
			if (frequency < 0) {
				newFrequency = TimeUnit.DAYS.toSeconds(1) / (-frequency);
			}
			else {
				newFrequency = TimeUnit.DAYS.toSeconds(frequency);
			}
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_timer_duration, notificationId, newFrequency);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_notification_frequency, notificationId);
		}
	}

	/**
	 * Do the migration steps for migration into app version 20.
	 */
	private static void doMigrationToVersion20() {
		for (int notificationId : NotificationSettingsActivity.getNotificationIds()) {
			int duration = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_duration, notificationId, 0);
			long newDuration = duration * 60; // MAGIC_NUMBER
			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_notification_duration, notificationId, newDuration);

			int timerVariance = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, notificationId, 0);
			int newTimerVariance = timerVariance + 1;
			PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_notification_timer_variance, notificationId, newTimerVariance);
		}
	}

}
