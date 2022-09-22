package de.jeisfeld.randomimage.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.widgets.WidgetSettingsActivity;
import de.jeisfeld.randomimagelib.R;

/**
 * Receiver for triggering random image notifications from external.
 */
public class NotificationExternalTriggerReceiver extends BroadcastReceiver {
	/**
	 * The resource key for the notification name.
	 */
	private static final String STRING_NOTIFICATION_NAME = "de.eisfeldj.randomimage.NOTIFICATION_NAME";
	/**
	 * The resource key for the widget name.
	 */
	private static final String STRING_WIDGET_NAME = "de.eisfeldj.randomimage.WIDGET_NAME";

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		String action = intent.getAction();

		if ("de.jeisfeld.randomimage.DISPLAY_RANDOM_IMAGE_FROM_EXTERNAL".equals(action)) {
			String notificationName = intent.getStringExtra(STRING_NOTIFICATION_NAME);
			Integer notificationId = NotificationSettingsActivity.getNotificationIdByName(notificationName);
			if (notificationId != null) {
				NotificationUtil.displayRandomImageNotification(context, notificationId);
			}
			String widgetName = intent.getStringExtra(STRING_WIDGET_NAME);
			Integer appWidgetId = WidgetSettingsActivity.getWidgetIdByName(widgetName);
			if (appWidgetId != null) {
				String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);
				final ImageList imageList = ImageRegistry.getImageListByName(listName, false);
				if (imageList != null) {
					imageList.executeWhenReady(null,
							() -> {
								String fileName = imageList.getRandomFileName();
								try {
									Intent displayIntent = DisplayRandomImageActivity.createIntent(
											context, listName, fileName, true, appWidgetId, null);
									displayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
									context.startActivity(displayIntent);
								}
								catch (Exception e) {
									Log.e(Application.TAG, "Error while triggering widget from external", e);
								}
							}, null);
				}
			}
		}
	}
}
