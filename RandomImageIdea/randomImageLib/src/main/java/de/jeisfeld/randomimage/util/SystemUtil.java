package de.jeisfeld.randomimage.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.annotation.RequiresApi;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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

	/**
	 * Get information about the list of applications installed on the device.
	 *
	 * @return The map from package name to application label, for non-system apps.
	 */
	public static Map<String, String> getInstalledApplications() {
		Context context = Application.getAppContext();
		PackageManager pm = context.getPackageManager();
		Map<String, String> result = new HashMap<>();

		for (ApplicationInfo appInfo : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
				result.put(appInfo.packageName, appInfo.loadLabel(pm).toString());
			}
		}
		return result;
	}

	/**
	 * Check if usage statistics can be retrieved.
	 *
	 * @return true if usage statistics are available.
	 */
	public static boolean isUsageStatsAvailable() {
		if (android.os.Build.VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
			return false;
		}
		Context context = Application.getAppContext();
		AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
		int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
		if (mode == AppOpsManager.MODE_DEFAULT) {
			return context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED;
		}
		else {
			return mode == AppOpsManager.MODE_ALLOWED;
		}
	}

	/**
	 * Get the usage statistics from the last day.
	 *
	 * @return The usage statistics from the last day.
	 */
	@RequiresApi(api = VERSION_CODES.LOLLIPOP_MR1)
	public static List<UsageStats> getUsageStats() {
		Context context = Application.getAppContext();
		UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
		long time = System.currentTimeMillis();
		List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - TimeUnit.DAYS.toMillis(1), time);

		if (appList != null && appList.size() > 0) {
			List<UsageStats> sortedAppList = new ArrayList<>();
			SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
			for (UsageStats usageStats : appList) {
				mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
			}
			for (Long timestamp : mySortedMap.keySet()) {
				sortedAppList.add(mySortedMap.get(timestamp));
			}
			return sortedAppList;
		}

		else {
			return null;
		}
	}

	/**
	 * Get the package name of the last used app.
	 *
	 * @return The package name of the last used app.
	 */
	@RequiresApi(api = VERSION_CODES.LOLLIPOP_MR1)
	public static String getLastPackageUsed() {
		List<UsageStats> usageStats = getUsageStats();
		if (usageStats == null || usageStats.size() == 0) {
			return "";
		}
		else {
			return usageStats.get(usageStats.size() - 1).getPackageName();
		}
	}

}
