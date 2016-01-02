package de.jeisfeld.randomimage.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;

import de.jeisfeld.randomimagelib.R;

/**
 * Helper class to show notifications.
 */
public final class NotificationUtil {
	/**
	 * Notification tag for update of an image list.
	 */
	public static final int ID_UPDATED_LIST = 1;

	/**
	 * Notification tag for errors when loading an image list.
	 */
	public static final int ID_ERROR_LOADING_LIST = 2;

	/**
	 * Notification tag for errors when saving an image list.
	 */
	public static final int ID_ERROR_SAVING_LIST = 3;

	/**
	 * Notification tag for backup or restore.
	 */
	public static final int ID_BACKUP_RESTORE = 4;

	/**
	 * Notification tag for missing files when loading an image list.
	 */
	public static final int ID_MISSING_FILES = 5;

	/**
	 * A dot used at the end of messages.
	 */
	private static final String DOT = ".";

	/**
	 * Key for the notification id within intent.
	 */
	public static final String EXTRA_NOTIFICATION_ID = "de.jeisfeld.randomimage.NOTIFICATION_ID";

	/**
	 * Key for the notification tag (list name) within intent.
	 */
	public static final String EXTRA_LIST_NAME = "de.jeisfeld.randomimage.NOTIFICATION_TAG";

	/**
	 * A map storing the update message texts.
	 */
	private static Map<String, List<String>> mListUpdateMap = new HashMap<>();

	/**
	 * A list storing the backup/restore messages.
	 */
	private static List<String> mBackupMessages = new ArrayList<>();

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
	@TargetApi(VERSION_CODES.KITKAT_WATCH)
	public static void displayNotification(final Context context, final String listName, final int notificationId,
										   final int titleResource, final int messageResource, final Object... args) {
		String message = DialogUtil.capitalizeFirst(String.format(context.getString(messageResource), args));
		String title = String.format(context.getString(titleResource), listName);

		if (notificationId == ID_UPDATED_LIST) {
			if (!message.endsWith(DOT)) {
				message += DOT;
			}

			// Construct message as collection of all prior messages
			List<String> existingMessages = mListUpdateMap.get(listName);
			if (existingMessages == null) {
				existingMessages = new ArrayList<>();
				mListUpdateMap.put(listName, existingMessages);
			}
			existingMessages.add(message);

			StringBuilder messageBuilder = new StringBuilder(existingMessages.get(0));
			for (String partialMessage : existingMessages.subList(1, existingMessages.size())) {
				messageBuilder.append("\n");
				messageBuilder.append(partialMessage);
			}
			message = messageBuilder.toString();
		}
		else if (notificationId == ID_BACKUP_RESTORE) {
			if (!message.endsWith(DOT)) {
				message += DOT;
			}
			mBackupMessages.add(message);

			StringBuilder messageBuilder = new StringBuilder(mBackupMessages.get(0));
			for (String partialMessage : mBackupMessages.subList(1, mBackupMessages.size())) {
				messageBuilder.append("\n");
				messageBuilder.append(partialMessage);
			}
			message = messageBuilder.toString();
		}

		Notification.Builder notificationBuilder =
				new Notification.Builder(context)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle(title)
						.setContentText(message)
						.setStyle(new Notification.BigTextStyle().bigText(message));

		if (notificationId == ID_UPDATED_LIST || notificationId == ID_BACKUP_RESTORE) {
			Intent intent = new Intent(context, NotificationBroadcastReceiver.class);
			intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
			if (listName != null) {
				intent.putExtra(EXTRA_LIST_NAME, listName);
			}
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
			notificationBuilder.setDeleteIntent(pendingIntent);
		}

		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(listName, notificationId, notificationBuilder.build());
	}

	/**
	 * Cancel a notification.
	 *
	 * @param context        the current activity or context
	 * @param listName       the list name - optional tag for the notification.
	 * @param notificationId the unique id of the notification.
	 */
	public static void cancelNotification(final Context context, final String listName, final int notificationId) {
		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (notificationId == ID_UPDATED_LIST) {
			mListUpdateMap.remove(listName);
		}
		else if (notificationId == ID_BACKUP_RESTORE) {
			mBackupMessages.clear();
		}

		notificationManager.cancel(listName, notificationId);
	}

}
