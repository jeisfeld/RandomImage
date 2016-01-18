package de.jeisfeld.randomimage.notifications;

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
import android.graphics.Bitmap;

import de.jeisfeld.randomimage.ConfigureImageListActivity;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.FileUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Helper class to show notifications.
 */
public final class NotificationUtil {
	/**
	 * Prefix used before a list name.
	 */
	private static final String LIST_PREFIX = "L";

	/**
	 * Prefix used before a path name.
	 */
	private static final String PATH_PREFIX = "P";

	/**
	 * A dot used at the end of messages.
	 */
	private static final String DOT = ".";

	/**
	 * Key for the notification id within intent.
	 */
	public static final String EXTRA_NOTIFICATION_TYPE = "de.jeisfeld.randomimage.NOTIFICATION_TYPE";

	/**
	 * Key for the notification tag within intent.
	 */
	public static final String EXTRA_NOTIFICATION_TAG = "de.jeisfeld.randomimage.NOTIFICATION_TAG";

	/**
	 * A map storing information on mounting issues while loading image lists.
	 */
	private static Map<String, Set<String>> mMountingIssues = new HashMap<>();

	/**
	 * The information about added or deleted lists/folders/files per image list.
	 */
	private static Map<String, ListUpdateInfo> mListUpdateInfo = new HashMap<>();

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
	 * @param context          the current activity or context
	 * @param notificationTag  the tag for the notification, which is also used in the title. Typically the list name.
	 * @param notificationType the type of the notification.
	 * @param titleResource    the title resource
	 * @param messageResource  the message resource
	 * @param args             arguments for the error message
	 */
	public static void displayNotification(final Context context, final String notificationTag, final NotificationType notificationType,
										   final int titleResource, final int messageResource, final Object... args) {
		String message = DialogUtil.capitalizeFirst(context.getString(messageResource, args));
		String title = context.getString(titleResource, notificationTag);

		Notification.Builder notificationBuilder =
				new Notification.Builder(context)
						.setSmallIcon(R.drawable.ic_notification_white)
						.setContentTitle(title)
						.setContentText(message)
						.setStyle(new Notification.BigTextStyle().bigText(message));

		if (notificationType == NotificationType.MISSING_FILES || notificationType == NotificationType.UPDATED_LIST
				|| notificationType == NotificationType.ERROR_LOADING_LIST || notificationType == NotificationType.ERROR_SAVING_LIST) {
			Intent actionIntent = ConfigureImageListActivity.createIntent(context, notificationTag);
			actionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			int uniqueId = getUniqueId(notificationTag, notificationType);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueId, actionIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			notificationBuilder.setContentIntent(pendingIntent);
		}

		if (notificationType == NotificationType.UPDATED_LIST || notificationType == NotificationType.UNMOUNTED_PATH) {
			notificationBuilder.setDeleteIntent(createDismissalIntent(context, notificationType, notificationTag));
		}

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(notificationTag, notificationType.intValue(), notificationBuilder.build());
	}

	/**
	 * Get a unique id of a notification instance.
	 *
	 * @param notificationTag  the tag for the notification, which is also used in the title. Typically the list name.
	 * @param notificationType the type of the notification.
	 * @return The unique id.
	 */
	private static int getUniqueId(final String notificationTag, final NotificationType notificationType) {
		if (notificationTag == null) {
			return notificationType.intValue();
		}
		else {
			return (notificationTag + notificationType.toString()).hashCode();
		}
	}

	/**
	 * Display a Random Image notification for a certain image list.
	 *
	 * @param context        the current activity or context
	 * @param notificationId the id of the configured notification.
	 */
	public static void displayRandomImageNotification(final Context context, final int notificationId) {
		String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_list_name, notificationId);
		String notificationTag = Integer.toString(notificationId);
		NotificationType notificationType = NotificationType.RANDOM_IMAGE;
		ImageList imageList = ImageRegistry.getImageListByName(listName, false);
		if (imageList == null) {
			// Fatal error - it does not make sense to re-create the alarm.
			return;
		}

		String fileName = imageList.getRandomFileName();
		if (fileName == null) {
			// This is typically a temporary error - therefore re-create the alarm.
			NotificationAlarmReceiver.setAlarm(context, notificationId);
			return;
		}

		Bitmap bitmap = ImageUtil.getImageBitmap(fileName, MediaStoreUtil.MINI_THUMB_SIZE);

		Notification.Builder notificationBuilder =
				new Notification.Builder(context)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle(listName)
						.setLargeIcon(bitmap)
						.setAutoCancel(true)
						.setStyle(new Notification.BigPictureStyle().bigPicture(bitmap));

		Intent actionIntent = DisplayRandomImageActivity.createIntent(context, listName, fileName, false, null, notificationId);
		int uniqueId = getUniqueId(notificationTag, notificationType);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueId, actionIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		notificationBuilder.setContentIntent(pendingIntent);

		notificationBuilder.setDeleteIntent(createDismissalIntent(context, notificationType, notificationTag));

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(listName, notificationType.intValue(), notificationBuilder.build());
	}

	/**
	 * Create a dismissal intent for a notification.
	 *
	 * @param context          the current activity or context
	 * @param notificationTag  the tag for the notification, which is also used in the title. Typically the list name.
	 * @param notificationType the type of the notification.
	 * @return The dismissal intent.
	 */
	private static PendingIntent createDismissalIntent(final Context context, final NotificationType notificationType, final String notificationTag) {
		Intent dismissalIntent = new Intent(context, NotificationBroadcastReceiver.class);
		dismissalIntent.putExtra(EXTRA_NOTIFICATION_TYPE, notificationType);
		int uniqueId = getUniqueId(notificationTag, notificationType);
		if (notificationTag != null) {
			dismissalIntent.putExtra(EXTRA_NOTIFICATION_TAG, notificationTag);
			uniqueId += notificationTag.hashCode();
		}
		return PendingIntent.getBroadcast(context.getApplicationContext(), uniqueId,
				dismissalIntent, PendingIntent.FLAG_ONE_SHOT);
	}

	/**
	 * Cancel a notification.
	 *
	 * @param context          the current activity or context
	 * @param notificationTag  the tag for the notification, which is also used in the title. Typically the list name.
	 * @param notificationType the type of the notification.
	 */
	public static void cancelNotification(final Context context, final String notificationTag, final NotificationType notificationType) {
		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (notificationType == NotificationType.UPDATED_LIST) {
			mListUpdateInfo.remove(notificationTag);
		}

		notificationManager.cancel(notificationTag, notificationType.intValue());
	}

	/**
	 * Notify about updates of an image list.
	 *
	 * @param context     The current context.
	 * @param listName    The name of the updated list.
	 * @param isRemove    true for removed elements, false for added elements.
	 * @param nestedLists The nested lists that have been added/removed.
	 * @param folders     The folders that have been added/removed.
	 * @param files       The image files that have been added/removed.
	 */
	public static void notifyUpdatedList(final Context context, final String listName, final boolean isRemove,
										 final List<String> nestedLists, final List<String> folders, final List<String> files) {

		ListUpdateInfo listUpdateInfo = mListUpdateInfo.get(listName);
		if (listUpdateInfo == null) {
			listUpdateInfo = new ListUpdateInfo();
			mListUpdateInfo.put(listName, listUpdateInfo);
		}

		ListUpdateInfo.updateSets(listUpdateInfo.getAddedLists(), listUpdateInfo.getRemovedLists(), nestedLists, isRemove);
		ListUpdateInfo.updateSets(listUpdateInfo.getAddedFolders(), listUpdateInfo.getRemovedFolders(), folders, isRemove);
		ListUpdateInfo.updateSets(listUpdateInfo.getAddedFiles(), listUpdateInfo.getRemovedFiles(), files, isRemove);

		int totalAdded = listUpdateInfo.getAddedLists().size() + listUpdateInfo.getAddedFolders().size() + listUpdateInfo.getAddedFiles().size();
		int totalRemoved = listUpdateInfo.getRemovedLists().size() + listUpdateInfo.getRemovedFolders().size()
				+ listUpdateInfo.getRemovedFiles().size();

		String addedFileFolderMessageString = null;
		if (totalAdded > 0) {
			String addedElementsString = DialogUtil.createFileFolderMessageString(new ArrayList<>(listUpdateInfo.getAddedLists()),
					new ArrayList<>(listUpdateInfo.getAddedFolders()), new ArrayList<>(listUpdateInfo.getAddedFiles()));
			int addedFileFolderMessageId = totalAdded == 1 ? R.string.toast_added_single : R.string.toast_added_multiple;
			addedFileFolderMessageString = context.getString(addedFileFolderMessageId, addedElementsString);
			if (!addedFileFolderMessageString.endsWith(DOT)) {
				addedFileFolderMessageString += DOT;
			}
			addedFileFolderMessageString = DialogUtil.capitalizeFirst(addedFileFolderMessageString);
		}
		String removedFileFolderMessageString = null;
		if (totalRemoved > 0) {
			String removedElementsString = DialogUtil.createFileFolderMessageString(new ArrayList<>(listUpdateInfo.getRemovedLists()),
					new ArrayList<>(listUpdateInfo.getRemovedFolders()), new ArrayList<>(listUpdateInfo.getRemovedFiles()));
			int removedFileFolderMessageId = totalRemoved == 1 ? R.string.toast_removed_single : R.string.toast_removed_multiple;
			removedFileFolderMessageString = context.getString(removedFileFolderMessageId, removedElementsString);
			if (!removedFileFolderMessageString.endsWith(DOT)) {
				removedFileFolderMessageString += DOT;
			}
			removedFileFolderMessageString = DialogUtil.capitalizeFirst(removedFileFolderMessageString);
		}

		if (totalAdded == 0 && totalRemoved == 0) {
			cancelNotification(context, listName, NotificationType.UPDATED_LIST);
		}
		else {
			StringBuilder message = new StringBuilder();
			if (totalAdded > 0) {
				message.append(addedFileFolderMessageString).append("\n");
			}
			if (totalRemoved > 0) {
				message.append(removedFileFolderMessageString).append("\n");
			}
			displayNotification(context, listName, NotificationType.UPDATED_LIST, R.string.title_notification_updated_list,
					R.string.dummy_original_string, message.toString());
		}

	}


	/**
	 * Notify about not found files for a list.
	 *
	 * @param context       the current activity or context
	 * @param listName      the list name.
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
				NotificationUtil.displayNotification(context, listName, NotificationType.MISSING_FILES,
						R.string.title_notification_missing_files,
						notFoundFilesCount == 1 ? R.string.toast_failed_to_load_files_single : R.string.toast_failed_to_load_files,
						listName, notFoundFilesCount);
			}
			if (missingMounts.size() > 0) {
				mMountingIssues.put(listName, missingMounts);
			}
		}
		else {
			NotificationUtil.cancelNotification(context, listName, NotificationType.MISSING_FILES);
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
				affectedListString.append(context.getString(R.string.partial_quoted_string, listName));
			}
			NotificationUtil.displayNotification(context, missingMount, NotificationType.UNMOUNTED_PATH, R.string.title_notification_unmounted_paths,
					R.string.notification_unmounted_path, missingMount, affectedListString.toString());
		}

		// Cancel notifications which are not needed any more.
		if (previousMissingMounts != null) {
			for (String previousMissingMount : previousMissingMounts) {
				if (!missingMountReverseMap.keySet().contains(previousMissingMount)) {
					NotificationUtil.cancelNotification(context, previousMissingMount, NotificationType.UNMOUNTED_PATH);
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

	/**
	 * Information about items added or removed from a list.
	 */
	private static class ListUpdateInfo {
		/**
		 * Nested lists added.
		 */
		private Set<String> mAddedLists = new HashSet<>();

		public Set<String> getAddedLists() {
			return mAddedLists;
		}

		/**
		 * Folders added.
		 */
		private Set<String> mAddedFolders = new HashSet<>();

		public Set<String> getAddedFolders() {
			return mAddedFolders;
		}

		/**
		 * Files added.
		 */
		private Set<String> mAddedFiles = new HashSet<>();

		public Set<String> getAddedFiles() {
			return mAddedFiles;
		}

		/**
		 * Nested lists removed.
		 */
		private Set<String> mRemovedLists = new HashSet<>();

		public Set<String> getRemovedLists() {
			return mRemovedLists;
		}

		/**
		 * Folders removed.
		 */
		private Set<String> mRemovedFolders = new HashSet<>();

		public Set<String> getRemovedFolders() {
			return mRemovedFolders;
		}

		/**
		 * Files removed.
		 */
		private Set<String> mRemovedFiles = new HashSet<>();

		public Set<String> getRemovedFiles() {
			return mRemovedFiles;
		}

		/**
		 * Update the mAddedFolders etc. based on old and new information
		 *
		 * @param addedItems   The mAddedLists/mAddedFolders/mAddedFiles set.
		 * @param removedItems The mRemovedLists/mRemovedFolders/mRemovedFiles set.
		 * @param updatedItems The list of updated items of this type.
		 * @param isRemove     Flag indicating if items are added (false) or removed (true).
		 */
		private static void updateSets(final Set<String> addedItems, final Set<String> removedItems, final List<String> updatedItems,
									   final boolean isRemove) {
			if (updatedItems == null || updatedItems.size() == 0) {
				return;
			}
			Set<String> updatedItemsClone = new HashSet<>(updatedItems);
			if (isRemove) {
				updatedItemsClone.removeAll(addedItems);
				addedItems.removeAll(updatedItems);
				removedItems.addAll(updatedItemsClone);
			}
			else {
				updatedItemsClone.removeAll(removedItems);
				removedItems.removeAll(updatedItems);
				addedItems.addAll(updatedItemsClone);
			}
		}

	}

	/**
	 * Types of notifications.
	 */
	public enum NotificationType {
		/**
		 * Notification type for update of an image list.
		 */
		UPDATED_LIST,

		/**
		 * Notification type for errors when loading an image list.
		 */
		ERROR_LOADING_LIST,

		/**
		 * Notification type for errors when saving an image list.
		 */
		ERROR_SAVING_LIST,

		/**
		 * Notification type for missing files when loading an image list.
		 */
		MISSING_FILES,

		/**
		 * Notification type for unmounted paths when loading an image list.
		 */
		UNMOUNTED_PATH,

		/**
		 * Notification type for a random image notification.
		 */
		RANDOM_IMAGE;

		/**
		 * Get the value used as "notificationId" in the Android notification framework.
		 *
		 * @return the notificationId.
		 */
		private int intValue() {
			switch (this) {
			case UPDATED_LIST:
				return 1;
			case ERROR_LOADING_LIST:
				return 2;
			case ERROR_SAVING_LIST:
				return 3; // MAGIC_NUMBER
			case MISSING_FILES:
				return 4; // MAGIC_NUMBER
			case UNMOUNTED_PATH:
				return 5; // MAGIC_NUMBER
			case RANDOM_IMAGE:
				return 6; // MAGIC_NUMBER
			default:
				return 0;
			}
		}
	}

}
