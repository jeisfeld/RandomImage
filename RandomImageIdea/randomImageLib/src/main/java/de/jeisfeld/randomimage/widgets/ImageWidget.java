package de.jeisfeld.randomimage.widgets;

import java.io.File;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.NotificationUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * The extended widget, also displaying a changing image.
 */
public class ImageWidget extends GenericImageWidget {
	@Override
	public final void onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager,
									 final int appWidgetId, final UpdateType updateType) {
		final String listName = getListName(appWidgetId);
		if (listName == null) {
			return;
		}
		Log.i(Application.TAG, "Updating ImageWidget " + appWidgetId + " for list \"" + listName + "\" with type " + updateType);

		String currentFileName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_current_file_name, appWidgetId);
		final boolean requireNewImage = updateType == UpdateType.NEW_LIST // BOOLEAN_EXPRESSION_COMPLEXITY
				|| updateType == UpdateType.NEW_IMAGE_AUTOMATIC
				|| updateType == UpdateType.NEW_IMAGE_BY_USER
				|| currentFileName == null
				|| !new File(currentFileName).exists();

		boolean isVisibleToUser = updateType == UpdateType.NEW_IMAGE_BY_USER || updateType == UpdateType.NEW_LIST;

		if (requireNewImage) {
			if (isVisibleToUser) {
				ImageRegistry.switchToImageList(listName, CreationStyle.NONE, false);
			}
			if (updateType == UpdateType.NEW_IMAGE_BY_USER) {
				ButtonAnimator.interrupt(appWidgetId);
			}

			final ImageList imageList = ImageRegistry.getImageListByName(listName, false);

			if (imageList == null) {
				Log.e(Application.TAG, "Could not load image list " + listName + "for ImageWidget update");
				DialogUtil.displayToast(context, R.string.toast_error_while_loading, listName);
				NotificationUtil.displayNotification(context, listName, NotificationUtil.TAG_ERROR_LOADING_LIST,
						R.string.title_notification_failed_loading, R.string.toast_error_while_loading, listName);

				// Put view in good state again.
				RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId));
				remoteViews.setViewVisibility(R.id.textViewWidgetEmpty, View.GONE);
				appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
			}
			else {
				setImageAsynchronously(context, appWidgetManager, imageList, appWidgetId, listName, isVisibleToUser);
			}
		}
		else {
			setImage(context, appWidgetManager, appWidgetId, listName, currentFileName);
			configureButtons(context, appWidgetManager, appWidgetId, true);
		}

	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
												final int appWidgetId, final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		final String listName = getListName(appWidgetId);

		final ImageList imageList = ImageRegistry.getImageListByName(listName, false);
		if (imageList == null) {
			Log.e(Application.TAG, "Could not load image list " + listName + " for ImageWidget option change");
			DialogUtil.displayToast(context, R.string.toast_error_while_loading, listName);
			NotificationUtil.displayNotification(context, listName, NotificationUtil.TAG_ERROR_LOADING_LIST,
					R.string.title_notification_failed_loading, R.string.toast_error_while_loading, listName);
		}

		String fileName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_current_file_name, appWidgetId);

		if (fileName == null && imageList != null) {
			setImageAsynchronously(context, appWidgetManager, imageList, appWidgetId, listName, true);
		}
		else {
			configureButtons(context, appWidgetManager, appWidgetId, true);
		}

	}

	/**
	 * Put a random image onto the view in an asynchronous way (after ensuring that the list is loaded).
	 *
	 * @param context          The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param imageList        the list from which to get a random image.
	 * @param appWidgetId      The appWidgetId of the widget whose size changed.
	 * @param listName         The name of the image list from which the file is taken.
	 * @param userTriggered    flag indicating if the call was triggered by the user.
	 */
	private void setImageAsynchronously(final Context context, final AppWidgetManager appWidgetManager, final ImageList imageList,
										final int appWidgetId, final String listName, final boolean userTriggered) {
		imageList.executeWhenReady(new Runnable() {
			@Override
			public void run() {
				if (userTriggered) {
					RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId));
					remoteViews.setViewVisibility(R.id.textViewWidgetEmpty, View.VISIBLE);
					remoteViews.setTextViewText(R.id.textViewWidgetEmpty, context.getString(R.string.text_loading));
					appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
				}
			}
		}, new Runnable() {
			@Override
			public void run() {
				String fileName = imageList.getRandomFileName();

				setImage(context, appWidgetManager, appWidgetId, listName, fileName);
				configureButtons(context, appWidgetManager, appWidgetId, true);

				int buttonStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, appWidgetId,
						Integer.parseInt(context.getString(R.string.pref_default_widget_button_style)));
				if (buttonStyle > 1) {
					new ButtonAnimator(context, appWidgetManager, appWidgetId, getWidgetLayoutId(appWidgetId),
							R.id.buttonNextImage, R.id.buttonSettings).start();
				}
			}
		}, new Runnable() {
			@Override
			public void run() {
				if (userTriggered) {
					RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId));
					remoteViews.setViewVisibility(R.id.textViewWidgetEmpty, View.GONE);
					remoteViews.setTextViewText(R.id.textViewWidgetEmpty, context.getString(R.string.text_no_image));
					appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
				}
			}
		});

	}

	/**
	 * Set the image in an instance of the widget.
	 *
	 * @param context          The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId      The appWidgetId of the widget whose size changed.
	 * @param listName         The name of the image list from which the file is taken.
	 * @param fileName         The filename of the image to be displayed.
	 */
	private void setImage(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
						  final String listName, final String fileName) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId));

		Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
		int width = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
		int height = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
		if (width <= 0 || height <= 0) {
			return;
		}

		if (fileName == null) {
			Log.e(Application.TAG, "Did not find any file to display");
			remoteViews.setViewVisibility(R.id.textViewWidgetEmpty, View.VISIBLE);
			remoteViews.setTextViewText(R.id.textViewWidgetEmpty, context.getString(R.string.text_no_image));
		}
		else {
			PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_current_file_name, appWidgetId, fileName);
			remoteViews.setViewVisibility(R.id.textViewWidgetEmpty, View.GONE);

			remoteViews.setImageViewBitmap(
					R.id.imageViewWidget,
					ImageUtil.getImageBitmap(fileName,
							Math.min(ImageUtil.MAX_BITMAP_SIZE, Math.max(width, height))));
		}

		Intent intent = DisplayRandomImageActivity.createIntent(context, listName, fileName, false, appWidgetId);
		PendingIntent pendingIntent =
				PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		remoteViews.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @param listName    The list name to be used by the widget.
	 * @param updateType  flag indicating what should be updated.
	 */
	public static final void configure(final int appWidgetId, final String listName, final UpdateType updateType) {
		PreferenceUtil.incrementCounter(R.string.key_statistics_countcreateimagewidget);

		long interval = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId,
				Long.parseLong(Application.getAppContext().getString(R.string.pref_default_widget_alarm_interval)));

		doBaseConfiguration(appWidgetId, listName, interval);

		updateInstances(updateType, appWidgetId);
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param updateType  flag indicating what should be updated.
	 * @param appWidgetId the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final UpdateType updateType, final int... appWidgetId) {
		updateInstances(ImageWidget.class, updateType, appWidgetId);
	}

	/**
	 * Update timers for instances of the widget.
	 *
	 * @param appWidgetIds the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateTimers(final int... appWidgetIds) {
		updateTimers(ImageWidget.class, appWidgetIds);
	}

	/**
	 * Check if there is an ImageWidget of this id.
	 *
	 * @param appWidgetId The widget id.
	 * @return true if there is an ImageWidget of this id.
	 */
	public static boolean hasWidgetOfId(final int appWidgetId) {
		return hasWidgetOfId(ImageWidget.class, appWidgetId);
	}

	/**
	 * Get the layout resource id for the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @return The layout resource id.
	 */
	@Override
	protected final int getWidgetLayoutId(final int appWidgetId) {
		Context context = Application.getAppContext();
		int buttonStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, appWidgetId,
				Integer.parseInt(context.getString(R.string.pref_default_widget_button_style)));
		int backgroundStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, appWidgetId,
				Integer.parseInt(context.getString(R.string.pref_default_widget_background_style)));

		switch (buttonStyle) {
		case 0:
			return backgroundStyle == 0 ? R.layout.widget_image_crop_bottom_buttons : R.layout.widget_image_inside_bottom_buttons;
		case 1:
			return backgroundStyle == 0 ? R.layout.widget_image_crop_top_buttons : R.layout.widget_image_inside_top_buttons;
		default:
			return backgroundStyle == 0 ? R.layout.widget_image_crop_centered_buttons : R.layout.widget_image_inside_centered_buttons;
		}
	}

	@Override
	protected final Class<? extends GenericWidgetConfigurationActivity> getConfigurationActivityClass() {
		return ImageWidgetConfigurationActivity.class;
	}
}
