package de.jeisfeld.randomimage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.jeisfeld.randomimage.notifications.NotificationAlarmReceiver;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
import de.jeisfeld.randomimage.widgets.GenericImageWidget;
import de.jeisfeld.randomimage.widgets.GenericWidget;
import de.jeisfeld.randomimage.widgets.ImageWidget;
import de.jeisfeld.randomimage.widgets.StackedImageWidget;
import de.jeisfeld.randomimagelib.R;

/**
 * A broadcast receiver being informed about SD card mounts.
 */
public class SdMountReceiver extends BroadcastReceiver {

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		String action = intent.getAction();

		// Keep track of shutdown/startup period.
		if (Intent.ACTION_SHUTDOWN.equals(action)) {
			PreferenceUtil.setSharedPreferenceBoolean(R.string.key_device_shut_down, true);
		}

		// Re-create all timers after boot or installation.
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			PreferenceUtil.setSharedPreferenceBoolean(R.string.key_device_shut_down, false);
			triggerAllTimers();
			TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, "Device_Change", "Boot completed");
		}
		if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
			triggerAllTimers();
			TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, "Device_Change", "Package replaced");
		}

		// Update widgets after changes in SD card availability.
		if (Intent.ACTION_MEDIA_MOUNTED.equals(action)
				|| Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
				|| Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			ImageRegistry.getCurrentImageList(false).load(false);
			GenericWidget.updateAllInstances();
		}

		// Update images on widgets with screen on if requested.
		if (Intent.ACTION_SCREEN_OFF.equals(action)) {
			GenericImageWidget.updateInstancesOnScreenOff();
		}
	}

	/**
	 * Trigger all widget or notification timers.
	 */
	private static void triggerAllTimers() {
		ImageWidget.updateTimers();
		StackedImageWidget.updateTimers();
		NotificationAlarmReceiver.createAllNotificationAlarms();
	}

}
