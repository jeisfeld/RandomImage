package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * The extended widget, also displaying a changing image.
 */
public class ImageWidget extends GenericWidget {
	/**
	 * The file names of the currently displayed images - mapped from appWidgetId.
	 */
	private static SparseArray<String> mCurrentFileNames = new SparseArray<>();

	/**
	 * Method name to set the background color.
	 */
	private static final String SET_BACKGROUND_COLOR = "setBackgroundColor";

	/**
	 * Method name to set the background resource.
	 */
	private static final String SET_BACKGROUND_RESOURCE = "setBackgroundResource";


	@Override
	public final void onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager,
									 final int appWidgetId, final UpdateType updateType) {
		final String listName = getListName(appWidgetId);
		Log.i(Application.TAG, "Updating ImageWidget " + appWidgetId + " for list \"" + listName + "\" with type " + updateType);

		final boolean requireNewImage = mCurrentFileNames.get(appWidgetId) == null
				|| updateType == UpdateType.NEW_LIST
				|| updateType == UpdateType.NEW_IMAGE_AUTOMATIC
				|| updateType == UpdateType.NEW_IMAGE_BY_USER;

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
			String fileName = mCurrentFileNames.get(appWidgetId);

			setImage(context, appWidgetManager, appWidgetId, listName, fileName);
			configureButtons(context, appWidgetManager, appWidgetId);
			configureBackground(context, appWidgetManager, appWidgetId);
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
		}

		String fileName = mCurrentFileNames.get(appWidgetId);

		if (fileName == null && imageList != null) {
			if (mCurrentFileNames.get(appWidgetId) == null) {
				setImageAsynchronously(context, appWidgetManager, imageList, appWidgetId, listName, true);
			}
		}
		else {
			if (mCurrentFileNames.get(appWidgetId) == null) {
				setImage(context, appWidgetManager, appWidgetId, listName, fileName);
				configureBackground(context, appWidgetManager, appWidgetId);
			}
			configureButtons(context, appWidgetManager, appWidgetId);
			configureBackground(context, appWidgetManager, appWidgetId);
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
				configureButtons(context, appWidgetManager, appWidgetId);
				configureBackground(context, appWidgetManager, appWidgetId);

				if (getWidgetLayoutId(appWidgetId) == R.layout.widget_image_inside_temp_buttons) {
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

	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		for (int appWidgetId : appWidgetIds) {
			mCurrentFileNames.remove(appWidgetId);
		}
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
			mCurrentFileNames.put(appWidgetId, fileName);
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
	 * @param context          The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId      The appWidgetId of the widget whose size changed.
	 */
	private void configureButtons(final Context context, final AppWidgetManager appWidgetManager,
								  final int appWidgetId) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId)); // STORE_PROPERTY

		// Set the onClick intent for the "next" button
		Intent nextIntent = new Intent(context, ImageWidget.class);
		nextIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		nextIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetId});
		nextIntent.putExtra(EXTRA_UPDATE_TYPE, UpdateType.NEW_IMAGE_BY_USER);
		PendingIntent pendingNextIntent =
				PendingIntent.getBroadcast(context, appWidgetId, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.buttonNextImage, pendingNextIntent);

		// Set the onClick intent for the "settings" button
		Intent settingsIntent = new Intent(context, ImageWidgetConfigurationActivity.class);
		settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		settingsIntent.putExtra(WidgetConfigurationActivity.EXTRA_RECONFIGURE_WIDGET, true);
		PendingIntent pendingSettingsIntent =
				PendingIntent.getActivity(context, appWidgetId, settingsIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.buttonSettings, pendingSettingsIntent);

		int buttonStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, appWidgetId,
				Integer.parseInt(context.getString(R.string.pref_default_widget_button_style)));
		if (buttonStyle > 0) {
			int padding = context.getResources().getDimensionPixelSize(
					buttonStyle == 1 ? R.dimen.widget_button_padding_narrow : R.dimen.widget_button_padding_wide);
			remoteViews.setViewPadding(R.id.buttonNextImage, padding, 0, padding, 0);
			remoteViews.setViewPadding(R.id.buttonSettings, padding, 0, padding, 0);
		}

		appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);

		if (buttonStyle > 0) {
			new ButtonAnimator(context, appWidgetManager, appWidgetId, getWidgetLayoutId(appWidgetId),
					R.id.buttonNextImage, R.id.buttonSettings).start();
		}
	}

	/**
	 * Configure the background of the widget.
	 *
	 * @param context          The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId      The appWidgetId of the widget whose size changed.
	 */
	private void configureBackground(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId));

		int backgroundStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, appWidgetId,
				Integer.parseInt(context.getString(R.string.pref_default_widget_background_style)));

		switch (backgroundStyle) {
		case 0:
		case 1:
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.TRANSPARENT);
			break;
		case 2:
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_RESOURCE, R.drawable.background_transparent_white);
			break;
		case 3: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_RESOURCE, R.drawable.background_transparent_black);
			break;
		case 4: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.LTGRAY);
			break;
		case 5: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.DKGRAY);
			break;
		case 6: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.rgb(0, 51, 141)); // MAGIC_NUMBER
			break;
		case 7: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.rgb(141, 0, 26)); // MAGIC_NUMBER
			break;
		case 8: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.rgb(173, 210, 149)); // MAGIC_NUMBER
			break;
		case 9: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.rgb(253, 240, 146)); // MAGIC_NUMBER
			break;
		case 10: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.rgb(91, 60, 26)); // MAGIC_NUMBER
			break;
		default:
			break;
		}

		appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
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
	private static int getWidgetLayoutId(final int appWidgetId) {
		Context context = Application.getAppContext();
		int buttonStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, appWidgetId,
				Integer.parseInt(context.getString(R.string.pref_default_widget_button_style)));
		int backgroundStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, appWidgetId,
				Integer.parseInt(context.getString(R.string.pref_default_widget_background_style)));

		if (backgroundStyle == 0) {
			return buttonStyle == 0 ? R.layout.widget_image_crop_fixed_buttons : R.layout.widget_image_crop_temp_buttons;
		}
		else {
			return buttonStyle == 0 ? R.layout.widget_image_inside_fixed_buttons : R.layout.widget_image_inside_temp_buttons;
		}
	}

	/**
	 * The style of the widget - defines the layout XML.
	 */
	protected enum WidgetStyle {
		/**
		 * Buttons are displayed temporarily on full height.
		 */
		BUTTONS_FULL_HEIGHT,
		/**
		 * Buttons are displayed permanently on the lower corners.
		 */
		BUTTONS_LOWER_CORNERS
	}
}
