package de.jeisfeld.randomimage.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
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
	 * Notification tag for unmounted paths when loading an image list.
	 */
	public static final int ID_UNMOUNTED_PATH = 5;

	/**
	 * A dot used at the end of messages.
	 */
	private static final String DOT = ".";

	/**
	 * Prefix used before a list name.
	 */
	private static final String LIST_PREFIX = "L";

	/**
	 * Prefix used before a path name.
	 */
	private static final String PATH_PREFIX = "P";

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
	 * A map storing information on mounting issues while loading image lists.
	 */
	private static Map<String, Set<String>> mMountingIssues = new HashMap<>();

	static {
		restoreMountingIssues();
	}

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
	 * @param notificationTag the tag for the notification, which is also used in the title. Typically the list name.
	 * @param notificationId  the unique id of the notification.
	 * @param titleResource   the title resource
	 * @param messageResource the message resource
	 * @param args            arguments for the error message
	 */
	public static void displayNotification(final Context context, final String notificationTag, final int notificationId,
										   final int titleResource, final int messageResource, final Object... args) {
		String message = DialogUtil.capitalizeFirst(String.format(context.getString(messageResource), args));
		String title = String.format(context.getString(titleResource), notificationTag);

		if (notificationId == ID_UPDATED_LIST) {
			if (!message.endsWith(DOT)) {
				message += DOT;
			}

			// Construct message as collection of all prior messages
			List<String> existingMessages = mListUpdateMap.get(notificationTag);
			if (existingMessages == null) {
				existingMessages = new ArrayList<>();
				mListUpdateMap.put(notificationTag, existingMessages);
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
						.setSmallIcon(R.drawable.ic_notification_white)
						.setContentTitle(title)
						.setContentText(message)
						.setStyle(new Notification.BigTextStyle().bigText(message));

		if (notificationId == ID_UPDATED_LIST || notificationId == ID_BACKUP_RESTORE || notificationId == ID_UNMOUNTED_PATH) {
			Intent intent = new Intent(context, NotificationBroadcastReceiver.class);
			intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
			if (notificationTag != null) {
				intent.putExtra(EXTRA_LIST_NAME, notificationTag);
			}
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
			notificationBuilder.setDeleteIntent(pendingIntent);
		}

		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(notificationTag, notificationId, notificationBuilder.build());
	}

	/**
	 * Cancel a notification.
	 *
	 * @param context         the current activity or context
	 * @param notificationTag the tag for the notification, which is also used in the title. Typically the list name.
	 * @param notificationId  the unique id of the notification.
	 */
	public static void cancelNotification(final Context context, final String notificationTag, final int notificationId) {
		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (notificationId == ID_UPDATED_LIST) {
			mListUpdateMap.remove(notificationTag);
		}
		else if (notificationId == ID_BACKUP_RESTORE) {
			mBackupMessages.clear();
		}

		notificationManager.cancel(notificationTag, notificationId);
	}

	/**
	 * Notify about not found files for a list.
	 *
	 * @param context       the current activity or context
	 * @param listName      the list name - optional tag for the notification.
	 * @param notFoundFiles the list of files which have not been found.
	 */
	public static void notifyNotFoundFiles(final Context context, final String listName, final List<String> notFoundFiles) {
		// Ignore not found files while booting.
		if (PreferenceUtil.getSharedPreferenceBoolean(R.string.key_device_shut_down)) {
			return;
		}

		Set<String> oldMissingMounts = mMountingIssues.get(listName);

		if (notFoundFiles.size() > 0) {
			int notFoundFilesCount = 0;
			Set<String> missingMounts = new HashSet<>();
			for (String file : notFoundFiles) {
				String missingMount = FileUtil.getUnmountedSdCardPath(new File(file));
				if (missingMount != null) {
					missingMounts.add(missingMount);
				}
				else {
					// Re-check if the file still does not exist.
					if (!new File(file).exists()) {
						notFoundFilesCount++;
					}
				}
			}
			if (notFoundFilesCount > 0) {
				NotificationUtil.displayNotification(context, listName, NotificationUtil.ID_MISSING_FILES,
						R.string.title_notification_missing_files,
						notFoundFilesCount == 1 ? R.string.toast_failed_to_load_files_single : R.string.toast_failed_to_load_files,
						listName, notFoundFilesCount);
			}
			if (missingMounts.size() > 0) {
				mMountingIssues.put(listName, missingMounts);
			}
		}
		else {
			NotificationUtil.cancelNotification(context, listName, NotificationUtil.ID_MISSING_FILES);
			mMountingIssues.remove(listName);
		}
		saveMountingIssues();
		updateMissingMountNotifications(context, oldMissingMounts);
	}

	/**
	 * Update the notifications on missing mount points.
	 *
	 * @param context               the current activity or context
	 * @param previousMissingMounts A previous list of missing mounts that may need to be removed.
	 */
	private static void updateMissingMountNotifications(final Context context, final Set<String> previousMissingMounts) {
		Map<String, List<String>> missingMountReverseMap = new HashMap<>();
		for (String listName : mMountingIssues.keySet()) {
			for (String mountName : mMountingIssues.get(listName)) {
				List<String> listsForMount = missingMountReverseMap.get(mountName);
				if (listsForMount == null) {
					listsForMount = new ArrayList<>();
				}
				listsForMount.add(listName);
				Collections.sort(listsForMount);
				missingMountReverseMap.put(mountName, listsForMount);
			}
		}

		// Add/update notifications.
		for (String missingMount : missingMountReverseMap.keySet()) {
			StringBuilder affectedListString = new StringBuilder();
			for (String listName : missingMountReverseMap.get(missingMount)) {
				if (affectedListString.length() > 0) {
					affectedListString.append(", ");
				}
				affectedListString.append(String.format(context.getString(R.string.partial_quoted_string), listName));
			}
			NotificationUtil.displayNotification(context, missingMount, ID_UNMOUNTED_PATH, R.string.title_notification_unmounted_paths,
					R.string.notification_unmounted_path, missingMount, affectedListString.toString());
		}

		// Cancel notifications which are not needed any more.
		if (previousMissingMounts != null) {
			for (String previousMissingMount : previousMissingMounts) {
				if (!missingMountReverseMap.keySet().contains(previousMissingMount)) {
					NotificationUtil.cancelNotification(context, previousMissingMount, ID_UNMOUNTED_PATH);
				}
			}
		}
	}

	/**
	 * Save the mounting issues in shared preferences.
	 */
	private static void saveMountingIssues() {
		ArrayList<String> storageArray = new ArrayList<>();
		for (String listName : mMountingIssues.keySet()) {
			storageArray.add(LIST_PREFIX + listName);
			for (String pathName : mMountingIssues.get(listName)) {
				storageArray.add(PATH_PREFIX + pathName);
			}
		}
		PreferenceUtil.setSharedPreferenceStringList(R.string.key_image_list_mount_issues, storageArray);
	}

	/**
	 * Restore the mounting issues from shared preferences.
	 */
	private static void restoreMountingIssues() {
		ArrayList<String> storageArray = PreferenceUtil.getSharedPreferenceStringList(R.string.key_image_list_mount_issues);
		mMountingIssues = new HashMap<>();
		if (storageArray == null) {
			return;
		}
		String listName = null;
		for (String entry : storageArray) {
			if (entry.startsWith(LIST_PREFIX)) {
				listName = entry.substring(LIST_PREFIX.length());
				// Do not restore entries from outdated lists.
				if (!ImageRegistry.getImageListNames(ListFiltering.ALL_LISTS).contains(listName)) {
					listName = null;
				}
				if (listName != null) {
					mMountingIssues.put(listName, new HashSet<String>());
				}
			}
			else if (entry.startsWith(PATH_PREFIX)) {
				if (listName != null) {
					String pathName = entry.substring(PATH_PREFIX.length());
					mMountingIssues.get(listName).add(pathName);
				}
			}
		}
	}

	/**
	 * Clean the mounting issues from shared preferences.
	 */
	protected static void cleanupMountingIssues() {
		mMountingIssues = new HashMap<>();
		PreferenceUtil.removeSharedPreference(R.string.key_image_list_mount_issues);
	}
}
