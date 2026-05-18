package de.jeisfeld.randomimage.util;

import android.Manifest.permission;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION_CODES;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.jeisfeld.randomimagelib.R;

/**
 * Utility class for handling the notification runtime permission.
 */
public final class NotificationPermissionUtil {
	/**
	 * Hide default constructor.
	 */
	private NotificationPermissionUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Check if the notification runtime permission applies on this device.
	 *
	 * @return true if notification permission must be checked/requested.
	 */
	public static boolean isNotificationPermissionRequired() {
		return SystemUtil.isAtLeastVersion(VERSION_CODES.TIRAMISU);
	}

	/**
	 * Check if the app may post notifications.
	 *
	 * @param context The context.
	 * @return true if notification permission is granted or not required on this device.
	 */
	public static boolean hasNotificationPermission(final Context context) {
		return !isNotificationPermissionRequired()
				|| ContextCompat.checkSelfPermission(context, permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Check if startup should ask for the notification permission.
	 *
	 * @param context The context.
	 * @return true if startup should ask for notification permission.
	 */
	public static boolean shouldRequestStartupNotificationPermission(final Context context) {
		return isNotificationPermissionRequired()
				&& !hasNotificationPermission(context)
				&& !PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_notification_permission_requested);
	}

	/**
	 * Request notification permission from an activity.
	 *
	 * @param activity    The activity.
	 * @param requestCode The request code.
	 */
	public static void requestNotificationPermission(final Activity activity, final int requestCode) {
		markNotificationPermissionRequested();
		ActivityCompat.requestPermissions(activity, new String[]{permission.POST_NOTIFICATIONS}, requestCode);
	}

	/**
	 * Request notification permission from a fragment.
	 *
	 * @param fragment    The fragment.
	 * @param requestCode The request code.
	 */
	public static void requestNotificationPermission(final Fragment fragment, final int requestCode) {
		markNotificationPermissionRequested();
		fragment.requestPermissions(new String[]{permission.POST_NOTIFICATIONS}, requestCode);
	}

	/**
	 * Mark that notification permission has been requested.
	 */
	public static void markNotificationPermissionRequested() {
		PreferenceUtil.setSharedPreferenceBoolean(R.string.key_pref_notification_permission_requested, true);
	}
}
