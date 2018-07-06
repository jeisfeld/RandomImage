package de.jeisfeld.randomimage.notifications;

import android.app.Notification;
import android.app.Notification.BigPictureStyle;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.jeisfeld.randomimage.Application;
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
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
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
	 * Activity String for Google Analytics.
	 */
	private static final String IMAGE_NOTIFICATION = "Image Notification";

	/**
	 * The vibration pattern.
	 */
	private static final long[] VIBRATION_PATTERN = {0, 100, 200, 100, 200, 100, 200, 1000};
	/**
	 * The empty vibration pattern.
	 */
	private static final long[] VIBRATION_PATTERN_EMPTY = {0, 0, 0};

	/**
	 * Key for the notification id within intent.
	 */
	public static final String EXTRA_NOTIFICATION_TYPE = "de.jeisfeld.randomimage.NOTIFICATION_TYPE";

	/**
	 * Key for the notification tag within intent.
	 */
	public static final String EXTRA_NOTIFICATION_TAG = "de.jeisfeld.randomimage.NOTIFICATION_TAG";

	/**
	 * Notification channel for random image notifications.
	 */
	private static final String IMAGE_NOTIFICATION_CHANNEL = "ImageNotification";
	/**
	 * Notification channel for random image notifications.
	 */
	private static final String IMAGE_NOTIFICATION_CHANNEL_GROUP = "ImageNotificationGroup";
	/**
	 * Notification channel for support notifications.
	 */
	private static final String SYSTEM_NOTIFICATION_CHANNEL = "SystemNotification";

	/**
	 * A map storing information on mounting issues while loading image lists.
	 */
	private static Map<String, Set<String>> mMountingIssues = new HashMap<>();

	/**
	 * The information about added or deleted lists/folders/files per image list.
	 */
	private static Map<String, ListUpdateInfo> mListUpdateInfo = new HashMap<>();

	/**
	 * The height of notification large icons.
	 */
	private static final int NOTIFICATION_LARGE_ICON_HEIGHT;

	/**
	 * The width of notification large icons.
	 */
	private static final int NOTIFICATION_LARGE_ICON_WIDTH;

	/**
	 * The notification style for the special notification.
	 */
	private static final int NOTIFICATION_STYLE_SPECIAL_NOTIFICATION = 1;
	/**
	 * The notification style that triggers DisplayImagePopupActivity instead of a notification.
	 */
	private static final int NOTIFICATION_STYLE_START_MICRO_IMAGE_ACTIVITY = 2;
	/**
	 * The notification style that triggers DisplayRandomImageActivity instead of a notification.
	 */
	private static final int NOTIFICATION_STYLE_START_RANDOM_IMAGE_ACTIVITY = 3;

	static {
		restoreMountingIssues();

		NOTIFICATION_LARGE_ICON_HEIGHT =
				Application.getAppContext().getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
		NOTIFICATION_LARGE_ICON_WIDTH =
				Application.getAppContext().getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
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

		Notification.Builder notificationBuilder;
		if (VERSION.SDK_INT >= VERSION_CODES.O) {
			notificationBuilder = new Notification.Builder(context, SYSTEM_NOTIFICATION_CHANNEL);
		}
		else {
			notificationBuilder = new Notification.Builder(context);
		}
		notificationBuilder.setSmallIcon(R.drawable.ic_notification_white)
				.setContentTitle(title)
				.setContentText(message)
				.setStyle(new Notification.BigTextStyle().bigText(message));

		if (notificationType == NotificationType.MISSING_FILES || notificationType == NotificationType.UPDATED_LIST
				|| notificationType == NotificationType.ERROR_LOADING_LIST || notificationType == NotificationType.ERROR_SAVING_LIST) {
			Intent actionIntent = ConfigureImageListActivity.createIntent(context, notificationTag, "NT." + notificationType.toString());
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
	 * Display a Random Image notification.
	 *
	 * @param context        the current activity or context
	 * @param notificationId the id of the configured notification.
	 */
	public static void displayRandomImageNotification(final Context context, final int notificationId) {
		final String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_list_name, notificationId);
		final ImageList imageList = ImageRegistry.getImageListByName(listName, false);
		if (imageList == null) {
			// Fatal error - it does not make sense to re-create the alarm.
			return;
		}
		imageList.executeWhenReady(null,
				new Runnable() {
					@Override
					public void run() {
						try {
							doDisplayRandomImageNotification(context, notificationId, listName, imageList);
						}
						catch (Exception e) {
							// In case of error, trigger new alarm.
							Log.e(Application.TAG, "Failed to publish notification for list " + listName, e);
							NotificationAlarmReceiver.setAlarm(context, notificationId, false);
						}
					}
				},
				new Runnable() {
					@Override
					public void run() {
						NotificationAlarmReceiver.setAlarm(context, notificationId, false);
					}
				});
	}

	/**
	 * Display a Random Image notification - internal helper method.
	 *
	 * @param context        the current activity or context
	 * @param notificationId the id of the configured notification.
	 * @param listName       the name of the image list
	 * @param imageList      the image list
	 */
	@SuppressWarnings("deprecation")
	private static void doDisplayRandomImageNotification(final Context context, final int notificationId,
														 final String listName, final ImageList imageList) {
		String fileName = imageList.getRandomFileName();
		if (fileName == null) {
			// This is typically a temporary error - therefore re-create the alarm.
			NotificationAlarmReceiver.setAlarm(context, notificationId, false);
			return;
		}
		int notificationStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, notificationId, -1);
		boolean vibrate = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_vibration,
				notificationId, false);
		if (isActivityNotificationStyle(notificationStyle)) {
			if (SystemUtil.isUsageStatsAvailable() && VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
				String lastPackageUsed = SystemUtil.getLastPackageUsed();
				Set<String> packages = PreferenceUtil.getSharedPreferenceStringSet(R.string.key_pref_apps_without_popup_notifications);
				if (packages != null && packages.contains(lastPackageUsed)) {
					// skip notification and restart timer
					NotificationAlarmReceiver.setAlarm(context, notificationId, false);
					return;
				}
			}

			// open activity instead of notification
			if (notificationStyle == NOTIFICATION_STYLE_START_RANDOM_IMAGE_ACTIVITY) {
				context.startActivity(DisplayRandomImageActivity.createIntent(context, listName, fileName, true, null,
						notificationId));
				TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, IMAGE_NOTIFICATION, "Fullscreen");
			}
			else {
				context.startActivity(DisplayImagePopupActivity.createIntent(context, listName, fileName, notificationId));
				TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, IMAGE_NOTIFICATION, "Popup");
			}
			if (vibrate) {
				AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
				if (am.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
					Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
					vibrator.vibrate(VIBRATION_PATTERN, -1);
				}
			}
			NotificationAlarmReceiver.setCancellationAlarm(context, notificationId);

			return;
		}

		Builder notificationBuilder;
		if (VERSION.SDK_INT >= VERSION_CODES.O) {
			notificationBuilder = new Builder(context, getNotificationChannelId(notificationId));
		}
		else {
			notificationBuilder = new Builder(context);
		}
		boolean coloredIcon = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_colored_icon,
				notificationId, false);
		notificationBuilder.setSmallIcon(coloredIcon ? R.drawable.ic_launcher : R.drawable.ic_notification_white)
				.setAutoCancel(true);

		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
			notificationBuilder.setShowWhen(false);
		}

		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			notificationBuilder.setCategory(Notification.CATEGORY_ALARM);
		}

		Bitmap bitmap = ImageUtil.getImageBitmap(fileName, MediaStoreUtil.MINI_THUMB_SIZE);
		String title = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_notification_display_name, notificationId);
		if (title == null || title.length() == 0) {
			title = listName;
		}

		if (notificationStyle == NOTIFICATION_STYLE_SPECIAL_NOTIFICATION) {
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_special);
			remoteViews.setImageViewBitmap(R.id.imageViewNotification, bitmap);
			if (VERSION.SDK_INT >= VERSION_CODES.N) {
				notificationBuilder.setCustomContentView(remoteViews);
			}
			else {
				//noinspection deprecation
				notificationBuilder.setContent(remoteViews);
			}

			// Dummy intent will enable heads-up notifications if available
			Intent intent = DisplayImagePopupActivity.createIntent(context, null, null, null);
			PendingIntent fullScreenIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
			notificationBuilder.setFullScreenIntent(fullScreenIntent, false);


			if (VERSION.SDK_INT >= VERSION_CODES.N) {
				Notification publicNotification = new Builder(context).setCustomContentView(remoteViews).build();
				notificationBuilder.setPublicVersion(publicNotification);
			}
			else if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
				//noinspection deprecation
				Notification publicNotification = new Builder(context).setContent(remoteViews).build();
				notificationBuilder.setPublicVersion(publicNotification);
			}
			TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, IMAGE_NOTIFICATION, "Special");
		}
		else {
			Bitmap iconBitmap = ImageUtil.getBitmapOfExactSize(fileName,
					NOTIFICATION_LARGE_ICON_WIDTH, NOTIFICATION_LARGE_ICON_HEIGHT, 0);

			notificationBuilder.setContentTitle(title).setLargeIcon(iconBitmap).setStyle(new BigPictureStyle().bigPicture(bitmap));

			if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
				Notification publicNotification = new Builder(context)
						.setSmallIcon(coloredIcon ? R.drawable.ic_launcher : R.drawable.ic_notification_white)
						.setShowWhen(false).setContentTitle(title).build();
				notificationBuilder.setPublicVersion(publicNotification);
			}
			TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, IMAGE_NOTIFICATION, "Standard");
		}

		String notificationTag = Integer.toString(notificationId);
		NotificationType notificationType = NotificationType.RANDOM_IMAGE;
		Intent actionIntent = DisplayRandomImageActivity.createIntent(context, listName, fileName, true, null, notificationId);
		int uniqueId = getUniqueId(notificationTag, notificationType);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueId, actionIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		notificationBuilder.setContentIntent(pendingIntent);

		notificationBuilder.setDeleteIntent(createDismissalIntent(context, notificationType, notificationTag));

		int ledColor = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_led_color, notificationId, 0);
		if (ledColor > 0) {
			notificationBuilder.setLights(LedColor.getLedColor(ledColor), 1500, 3000); // MAGIC_NUMBER
			notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
			if (!vibrate) {
				notificationBuilder.setVibrate(VIBRATION_PATTERN_EMPTY);
			}
		}

		if (vibrate) {
			notificationBuilder.setVibrate(VIBRATION_PATTERN);
		}

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationTag, notificationType.intValue(), notificationBuilder.build());

		NotificationAlarmReceiver.setCancellationAlarm(context, notificationId);
	}

	/**
	 * Get information if a notificationStyle value leads to the start of an activity rather than to a notification.
	 *
	 * @param notificationStyle The notificationStyle value.
	 * @return True if this style leads to an activity.
	 */
	public static boolean isActivityNotificationStyle(final int notificationStyle) {
		return notificationStyle == NOTIFICATION_STYLE_START_RANDOM_IMAGE_ACTIVITY
				|| notificationStyle == NOTIFICATION_STYLE_START_MICRO_IMAGE_ACTIVITY;
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
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (notificationType == NotificationType.UPDATED_LIST) {
			mListUpdateInfo.remove(notificationTag);
		}

		notificationManager.cancel(notificationTag, notificationType.intValue());
	}

	/**
	 * Cancel a Random Image notification.
	 *
	 * @param context        the current activity or context
	 * @param notificationId the id of the configured notification.
	 */
	public static void cancelRandomImageNotification(final Context context, final int notificationId) {
		// Cancel both the normal notification and the triggered activity.
		cancelNotification(context, Integer.toString(notificationId), NotificationType.RANDOM_IMAGE);
		DisplayRandomImageActivity.finishActivityForNotification(context, notificationId);
		DisplayImagePopupActivity.finishActivity(context, notificationId);
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
		// Do not show notification if globally disabled.
		boolean showNotification = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_show_list_notification);
		if (!showNotification) {
			return;
		}

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
	 * Create the notification channels required for the app.
	 *
	 * @param context The context.
	 */
	@RequiresApi(VERSION_CODES.O)
	public static void createNotificationChannels(final Context context) {
		NotificationChannel systemNotificationChannel = new NotificationChannel(
				SYSTEM_NOTIFICATION_CHANNEL, context.getString(R.string.notification_channel_system), NotificationManager.IMPORTANCE_DEFAULT);
		systemNotificationChannel.setLightColor(Color.TRANSPARENT);
		systemNotificationChannel.setShowBadge(true);
		systemNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(systemNotificationChannel);
		notificationManager.createNotificationChannelGroup(
				new NotificationChannelGroup(IMAGE_NOTIFICATION_CHANNEL_GROUP, context.getString(R.string.notification_channel_image)));

		createImageNotificationChannels();
	}

	/**
	 * Create the image notification channels currently required.
	 */
	public static void createImageNotificationChannels() {
		if (VERSION.SDK_INT >= VERSION_CODES.O) {
			List<Integer> notificationIds = NotificationSettingsActivity.getNotificationIds();
			for (int notificationId : notificationIds) {
				createImageNotificationChannel(notificationId);
			}
		}
	}

	/**
	 * Create a notification channel for a specific notification.
	 *
	 * @param notificationId the notification id
	 */
	@RequiresApi(api = VERSION_CODES.O)
	private static void createImageNotificationChannel(final int notificationId) {
		String channelId = getNotificationChannelId(notificationId);
		if (channelId == null) {
			// Do not create anything in case of activity notifications.
			return;
		}
		String name = getNotificationChannelName(Application.getAppContext(), notificationId);

		NotificationChannel imageNotificationChannel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH);
		imageNotificationChannel.setShowBadge(false);
		imageNotificationChannel.setSound(null, null);
		imageNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		imageNotificationChannel.setGroup(IMAGE_NOTIFICATION_CHANNEL_GROUP);

		int ledColorResource = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_led_color, notificationId, -1);
		imageNotificationChannel.enableLights(ledColorResource > 0);
		imageNotificationChannel.setLightColor(LedColor.getLedColor(ledColorResource));

		boolean isVibrate = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_vibration, notificationId, false);
		imageNotificationChannel.enableVibration(isVibrate);
		imageNotificationChannel.setVibrationPattern(isVibrate ? VIBRATION_PATTERN : VIBRATION_PATTERN_EMPTY);

		NotificationManager notificationManager = (NotificationManager) Application.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(imageNotificationChannel);
	}

	/**
	 * Delete the image notification channels currently used.
	 */
	protected static void deleteImageNotificationChannels() {
		if (VERSION.SDK_INT >= VERSION_CODES.O) {
			List<Integer> notificationIds = NotificationSettingsActivity.getNotificationIds();
			for (int notificationId : notificationIds) {
				deleteImageNotificationChannel(notificationId);
			}
		}
	}

	/**
	 * Delete a notification channel for a specific notification.
	 *
	 * @param notificationId the notification id
	 */
	@RequiresApi(api = VERSION_CODES.O)
	private static void deleteImageNotificationChannel(final int notificationId) {
		String name = getNotificationChannelId(notificationId);
		if (name != null) {
			NotificationManager notificationManager =
					(NotificationManager) Application.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.deleteNotificationChannel(name);
		}
	}

	/**
	 * Get a notification channel id for image notifications based on LED color resource value and vibration enablement.
	 *
	 * @param ledColorResource the LED color resource value.
	 * @param isVibrate        The vibration enablement.
	 * @return The notification channel ID.
	 */
	private static String getNotificationChannelId(final int ledColorResource, final boolean isVibrate) {
		return IMAGE_NOTIFICATION_CHANNEL + "_" + ledColorResource + "_" + (isVibrate ? 1 : 0);
	}

	/**
	 * Get the notificationChannelId for a notification based on notificationId.
	 *
	 * @param notificationId The notificationId
	 * @return The notificationChannelId or null in case of an activity based notification
	 */
	private static String getNotificationChannelId(final int notificationId) {
		int notificationStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, notificationId, -1);
		if (isActivityNotificationStyle(notificationStyle)) {
			return null;
		}

		int ledColorResource = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_led_color, notificationId, -1);
		boolean isVibrate = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_vibration, notificationId, false);
		return getNotificationChannelId(ledColorResource, isVibrate);
	}

	/**
	 * Get a notification channel name for image notifications based on notificationId.
	 *
	 * @param context        the context
	 * @param notificationId The notificationId
	 * @return The notification channel ID.
	 */
	private static String getNotificationChannelName(final Context context, final int notificationId) {
		int ledColorResource = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_led_color, notificationId, -1);
		boolean isVibrate = PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_vibration, notificationId, false);
		String ledName = "";
		if (ledColorResource >= 0) {
			ledName = " " + context.getResources().getStringArray(R.array.notification_led_color_names)[ledColorResource];
		}
		return context.getString(R.string.notification_channel_led) + ledName + " "
				+ context.getString(isVibrate ? R.string.notification_channel_with_vibration : R.string.notification_channel_without_vibration);
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

	/**
	 * Helper class containing constants for LED colors.
	 */
	private static class LedColor {
		// JAVADOC:OFF
		private static final int FAINT = Color.parseColor("#070707");
		private static final int RED_GREEN = Color.parseColor("#7F7F00");
		private static final int GREEN_BLUE = Color.parseColor("#007F7F");
		private static final int BLUE_RED = Color.parseColor("#7F007F");
		// JAVADOC:ON

		/**
		 * Get the LED color value from the resource value.
		 *
		 * @param resourceValue The value from the resource array.
		 * @return The color.
		 */
		private static int getLedColor(final int resourceValue) {
			switch (resourceValue) {
			case 1:
				return FAINT;
			case 2:
				return Color.RED;
			case 3: // MAGIC_NUMBER
				return Color.GREEN;
			case 4: // MAGIC_NUMBER
				return Color.BLUE;
			case 5: // MAGIC_NUMBER
				return RED_GREEN;
			case 6: // MAGIC_NUMBER
				return GREEN_BLUE;
			case 7: // MAGIC_NUMBER
				return BLUE_RED;
			default:
				return 0;
			}
		}
	}

}
