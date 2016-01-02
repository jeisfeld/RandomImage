package de.jeisfeld.randomimage.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import de.jeisfeld.randomimagelib.R;

/**
 * Helper class to show notifications.
 */
public final class NotificationUtil {
	/**
	 * Notification tag for update of an image list.
	 */
	public static final int TAG_UPDATED_LIST = 1;

	/**
	 * Notification tag for errors when loading an image list.
	 */
	public static final int TAG_ERROR_LOADING_LIST = 2;

	/**
	 * Notification tag for errors when saving an image list.
	 */
	public static final int TAG_ERROR_SAVING_LIST = 3;

	/**
	 * Notification tag for backup or restore.
	 */
	public static final int TAG_BACKUP_RESTORE = 4;

	/**
	 * Notification tag for missing files when loading an image list.
	 */
	public static final int TAG_MISSING_FILES = 5;

	/**
	 * Hide default constructor.
	 */
	private NotificationUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Display a notification.
	 *
	 * @param context         the current activity or context
	 * @param listName        the list name - optional tag for the notification.
	 * @param notificationId  the unique id of the notification.
	 * @param titleResource   the title resource
	 * @param messageResource the message resource
	 * @param args            arguments for the error message
	 */
	public static void displayNotification(final Context context, final String listName, final int notificationId,
										   final int titleResource, final int messageResource, final Object... args) {
		String message = DialogUtil.capitalizeFirst(String.format(context.getString(messageResource), args));
		String title = String.format(context.getString(titleResource), listName);

		Notification.Builder notificationBuilder =
				new Notification.Builder(context)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle(title)
						.setContentText(message);

		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(listName, notificationId, notificationBuilder.build());
	}
}
