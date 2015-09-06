package de.jeisfeld.randomimage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.widgets.ImageWidget;
import de.jeisfeld.randomimage.widgets.StackedImageWidget;

/**
 * A broadcast receiver being informed about SD card mounts.
 */
public class SdMountReceiver extends BroadcastReceiver {

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		String action = intent.getAction();

		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			ImageWidget.updateTimers();
		}

		if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
				|| action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
				|| action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			ImageRegistry.getCurrentImageList(false).load(false);
			ImageWidget.updateInstances();
			StackedImageWidget.updateInstances();
		}

	}

}
