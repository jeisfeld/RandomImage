package de.jeisfeld.randomimage.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcase receiver that handles actions on notifications.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		int notificationId = intent.getIntExtra(NotificationUtil.EXTRA_NOTIFICATION_ID, -1);
		String listName = intent.getStringExtra(NotificationUtil.EXTRA_LIST_NAME);

		if (notificationId == NotificationUtil.ID_UPDATED_LIST) {
			NotificationUtil.cancelNotification(context, listName, notificationId);
		}
		else if (notificationId == NotificationUtil.ID_UNMOUNTED_PATH) {
			NotificationUtil.cleanupMountingIssues();
		}
	}
}
