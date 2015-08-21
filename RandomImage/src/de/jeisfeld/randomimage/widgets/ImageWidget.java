package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;

/**
 * The extended widget, also displaying a changing image.
 */
public class ImageWidget extends GenericWidget {
	/**
	 * The file names of the currently displayed images - mapped from appWidgetId.
	 */
	private static SparseArray<String> currentFileNames = new SparseArray<String>();

	@Override
	public final void onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId, final String listName, final boolean changeImage) {
		boolean requireNewImage = changeImage || currentFileNames.get(appWidgetId) == null;

		String fileName = null;
		if (requireNewImage) {
			ImageList imageList = ImageRegistry.getImageListByName(listName);
			if (imageList == null) {
				Log.e(Application.TAG, "Could not load image list " + listName + "for ImageWidget update");
				DialogUtil.displayToast(context, R.string.toast_error_while_loading, listName);
			}

			if (imageList != null) {
				imageList.waitUntilReady();
				fileName = imageList.getRandomFileName();
			}
		}
		else {
			fileName = currentFileNames.get(appWidgetId);
		}

		setImage(context, appWidgetManager, appWidgetId, listName, fileName);
		configureButtons(context, appWidgetManager, appWidgetId);

		if (requireNewImage) {
			new ButtonAnimator(context, appWidgetManager, appWidgetId, R.layout.widget_image,
					R.id.buttonNextImage, R.id.buttonSettings).start();
		}
	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId, final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		String listName = getListName(appWidgetId);

		ImageList imageList = ImageRegistry.getImageListByName(listName);
		if (imageList == null) {
			Log.e(Application.TAG, "Could not load image list " + listName + " for ImageWidget option change");
			DialogUtil.displayToast(context, R.string.toast_error_while_loading, listName);
		}

		String fileName = currentFileNames.get(appWidgetId);

		if (fileName == null && imageList != null) {
			imageList.waitUntilReady();
			fileName = imageList.getRandomFileName();
		}

		setImage(context, appWidgetManager, appWidgetId, listName, fileName);
		configureButtons(context, appWidgetManager, appWidgetId);
		new ButtonAnimator(context, appWidgetManager, appWidgetId, R.layout.widget_image,
				R.id.buttonNextImage, R.id.buttonSettings).start();
	}

	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			currentFileNames.remove(appWidgetIds[i]);
		}
	}

	/**
	 * Set the image in an instance of the widget.
	 *
	 * @param context
	 *            The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager
	 *            A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId
	 *            The appWidgetId of the widget whose size changed.
	 * @param listName
	 *            The name of the image list from which the file is taken.
	 * @param fileName
	 *            The filename of the image to be displayed.
	 */
	private void setImage(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
			final String listName, final String fileName) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_image);

		Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
		int width = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
		int height = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
		if (width <= 0 || height <= 0) {
			return;
		}

		if (fileName == null) {
			remoteViews.setViewVisibility(R.id.textViewWidgetEmpty, View.VISIBLE);
		}
		else {
			currentFileNames.put(appWidgetId, fileName);
			remoteViews.setViewVisibility(R.id.textViewWidgetEmpty, View.GONE);

			remoteViews.setImageViewBitmap(
					R.id.imageViewWidget,
					ImageUtil.getImageBitmap(fileName,
							Math.min(ImageUtil.MAX_BITMAP_SIZE, Math.max(width, height))));
		}

		Intent intent = DisplayRandomImageActivity.createIntent(context, listName, fileName, false);
		PendingIntent pendingIntent =
				PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		remoteViews.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
	}

	/**
	 * Set the intents for the action buttons on the widget.
	 *
	 * @param context
	 *            The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager
	 *            A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId
	 *            The appWidgetId of the widget whose size changed.
	 */
	private void configureButtons(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_image); // STORE_PROPERTY

		// Set the onClick intent for the "next" button
		Intent nextIntent = new Intent(context, ImageWidget.class);
		nextIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		nextIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
		nextIntent.putExtra(EXTRA_NEW_IMAGE, true);
		PendingIntent pendingNextIntent =
				PendingIntent.getBroadcast(context, appWidgetId, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.buttonNextImage, pendingNextIntent);

		// Set the onClick intent for the "settings" button
		Intent settingsIntent = new Intent(context, ImageWidgetConfigurationActivity.class);
		settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		PendingIntent pendingSettingsIntent =
				PendingIntent.getActivity(context, appWidgetId, settingsIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.buttonSettings, pendingSettingsIntent);

		appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId
	 *            The widget id.
	 * @param listName
	 *            The list name to be used by the widget.
	 * @param interval
	 *            The update interval.
	 */
	public static final void configure(final int appWidgetId, final String listName, final long interval) {
		PreferenceUtil.incrementCounter(R.string.key_statistics_countcreateimagewidget);

		doBaseConfiguration(appWidgetId, listName, interval);

		updateInstances(appWidgetId);
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param appWidgetId
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final int... appWidgetId) {
		updateInstances(ImageWidget.class, appWidgetId);
	}

	/**
	 * Update timers for instances of the widget.
	 *
	 * @param appWidgetIds
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateTimers(final int... appWidgetIds) {
		updateTimers(ImageWidget.class, appWidgetIds);
	}

	/**
	 * Check if there is an ImageWidget of this id.
	 *
	 * @param appWidgetId
	 *            The widget id.
	 * @return true if there is an ImageWidget of this id.
	 */
	public static boolean hasWidgetOfId(final int appWidgetId) {
		return hasWidgetOfId(ImageWidget.class, appWidgetId);
	}

}
