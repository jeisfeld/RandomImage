package de.jeisfeld.randomimage.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.jeisfeld.randomimage.notifications.NotificationUtil.NotificationType;

/**
 * Broadcast receiver that handles actions on notifications.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		NotificationType notificationType = (NotificationType) intent.getSerializableExtra(NotificationUtil.EXTRA_NOTIFICATION_TYPE);
		String notificationTag = intent.getStringExtra(NotificationUtil.EXTRA_NOTIFICATION_TAG);

		if (notificationType == NotificationType.UPDATED_LIST) {
			NotificationUtil.cancelNotification(context, notificationTag, notificationType);
		}
		else if (notificationType == NotificationType.UNMOUNTED_PATH) {
			NotificationUtil.cleanupMountingIssues();
		}
		else if (notificationType == NotificationType.RANDOM_IMAGE) {
			int notificationId = Integer.parseInt(notificationTag);
			NotificationAlarmReceiver.cancelAlarm(context, notificationId, true);
			NotificationAlarmReceiver.setAlarm(context, notificationId, false);
		}
	}
}
