package de.jeisfeld.randomimage.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import de.jeisfeld.randomimage.Application;

/**
 * Utility class for getting system information.
 */
public final class SystemUtil {

	/**
	 * Hide default constructor.
	 */
	private SystemUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get information if Android version is Kitkat (4.4).
	 *
	 * @return true if Kitkat.
	 */
	public static boolean isKitkat() {
		return Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;
	}

	/**
	 * Get information if Android version is Lollipop (5.0) or higher.
	 *
	 * @return true if Lollipop or higher.
	 */
	public static boolean isAndroid5() {
		return isAtLeastVersion(Build.VERSION_CODES.LOLLIPOP);
	}

	/**
	 * Check if Android version is at least the given version.
	 *
	 * @param version The version
	 * @return true if Android version is at least the given version
	 */
	public static boolean isAtLeastVersion(final int version) {
		return Build.VERSION.SDK_INT >= version;
	}

	/**
	 * Determine if an app is installed.
	 *
	 * @param appPackage the app package name.
	 * @return true if the app is installed.
	 */
	public static boolean isAppInstalled(final String appPackage) {
		Intent appIntent = Application.getAppContext().getPackageManager().getLaunchIntentForPackage(appPackage);
		return appIntent != null;
	}

	/**
	 * Determine if the screen is shown in landscape mode (i.e. width &gt; height)
	 *
	 * @return true if the app runs in landscape mode
	 */
	public static boolean isLandscape() {
		// use screen width as criterion rather than getRotation
		WindowManager wm = (WindowManager) Application.getAppContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		return width > height;
	}

	/**
	 * Determine if the device is a tablet (i.e. it has a large screen).
	 *
	 * @return true if the app is running on a tablet.
	 */
	public static boolean isTablet() {
		return (Application.getAppContext().getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	/**
	 * Retrieve the default display.
	 *
	 * @return the default display.
	 */
	private static Display getDefaultDisplay() {
		WindowManager wm = (WindowManager) Application.getAppContext().getSystemService(Context.WINDOW_SERVICE);
		return wm.getDefaultDisplay();
	}

	/**
	 * Retrieve the display size in pixels (max of x and y value).
	 *
	 * @return the display size.
	 */
	public static int getDisplaySize() {
		Point p = new Point();
		getDefaultDisplay().getSize(p);
		return Math.max(p.x, p.y);
	}

	/**
	 * Get the large memory class of the device.
	 *
	 * @return the memory class - the maximal available memory for the app (in MB).
	 */
	public static int getLargeMemoryClass() {
		ActivityManager manager =
				(ActivityManager) Application.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);

		return manager.getLargeMemoryClass();
	}

	/**
	 * Lock or unlock the screen orientation programmatically.
	 *
	 * @param activity The triggering activity.
	 * @param lock     if true, the orientation is locked, otherwise it's unlocked.
	 */
	public static void lockOrientation(final Activity activity, final boolean lock) {
		if (activity == null) {
			return;
		}
		if (lock) {
			int currentOrientation = activity.getResources().getConfiguration().orientation;
			if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			}
			else {
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
			}
		}
		else {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}
	}

}
