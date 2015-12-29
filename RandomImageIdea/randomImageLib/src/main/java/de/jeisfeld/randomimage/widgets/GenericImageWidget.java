package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.RemoteViews;

import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * The extended widget, also displaying one or more changing images.
 */
public abstract class GenericImageWidget extends GenericWidget {
	/**
	 * Method name to set the background color.
	 */
	private static final String SET_BACKGROUND_COLOR = "setBackgroundColor";

	/**
	 * Method name to set the background resource.
	 */
	private static final String SET_BACKGROUND_RESOURCE = "setBackgroundResource";

	/**
	 * Method name to set the image bitmap.
	 */
	private static final String SET_IMAGE_BITMAP = "setImageBitmap";


	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		for (int appWidgetId : appWidgetIds) {
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_background_style, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_button_style, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_alarm_interval, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_current_file_name, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_list_name, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_view_width, appWidgetId);
		}
	}

	/**
	 * Set the intents for the action buttons on the widget.
	 *
	 * @param context          The {@link Context Context} in which this method is called.
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId      The appWidgetId of the widget whose size changed.
	 * @param setBackgroundColor Flag indicating if the background color should be set as well.
	 */
	protected final void configureButtons(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
										  final boolean setBackgroundColor) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId)); // STORE_PROPERTY

		// Set the onClick intent for the "next" button
		Intent nextIntent = new Intent(context, getClass());
		nextIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		nextIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetId});
		nextIntent.putExtra(EXTRA_UPDATE_TYPE, UpdateType.NEW_IMAGE_BY_USER);
		PendingIntent pendingNextIntent =
				PendingIntent.getBroadcast(context, appWidgetId, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.buttonNextImage, pendingNextIntent);

		// Set the onClick intent for the "settings" button
		Intent settingsIntent = new Intent(context, getConfigurationActivityClass());
		settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		settingsIntent.putExtra(GenericWidgetConfigurationActivity.EXTRA_RECONFIGURE_WIDGET, true);
		PendingIntent pendingSettingsIntent =
				PendingIntent.getActivity(context, appWidgetId, settingsIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.buttonSettings, pendingSettingsIntent);

		int buttonStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, appWidgetId,
				Integer.parseInt(context.getString(R.string.pref_default_widget_button_style)));
		if (buttonStyle > 1) {
			int padding = context.getResources().getDimensionPixelSize(
					buttonStyle == 2 ? R.dimen.widget_button_padding_narrow : R.dimen.widget_button_padding_wide);
			remoteViews.setViewPadding(R.id.buttonNextImage, padding, 0, padding, 0);
			remoteViews.setViewPadding(R.id.buttonSettings, padding, 0, padding, 0);
		}
		remoteViews.setBitmap(R.id.buttonNextImage, SET_IMAGE_BITMAP, getColoredButtonBitmap(context, appWidgetId, R.drawable.ic_widget_next));
		remoteViews.setBitmap(R.id.buttonSettings, SET_IMAGE_BITMAP, getColoredButtonBitmap(context, appWidgetId, R.drawable.ic_widget_settings));

		if (setBackgroundColor) {
			configureBackground(context, remoteViews, appWidgetManager, appWidgetId);
		}

		appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);

		if (buttonStyle > 1) {
			new ButtonAnimator(context, appWidgetManager, appWidgetId, getWidgetLayoutId(appWidgetId),
					R.id.buttonNextImage, R.id.buttonSettings).start();
		}
	}

	/**
	 * Get the coloured version of a button bitmap.
	 *
	 * @param context        The {@link Context Context} in which this method is called.
	 * @param appWidgetId    The appWidgetId of the widget whose size changed.
	 * @param bitmapResource The resourceId of the button bitmap.
	 * @return The coloured button bitmap.
	 */
	private static Bitmap getColoredButtonBitmap(final Context context, final int appWidgetId, final int bitmapResource) {
		Bitmap sourceBitmap = BitmapFactory.decodeResource(context.getResources(), bitmapResource);

		int buttonColor = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_color, appWidgetId, -1);

		switch (buttonColor) {
		case 0:
			return sourceBitmap;
		case 1:
			return ImageUtil.changeBitmapColor(sourceBitmap, Color.WHITE, Color.BLACK);
		case 2:
			return ImageUtil.changeBitmapColor(sourceBitmap, Color.BLUE, Color.WHITE);
		case 3: // MAGIC_NUMBER
			return ImageUtil.changeBitmapColor(sourceBitmap, Color.rgb(191, 0, 0), Color.WHITE); // MAGIC_NUMBER
		case 4: // MAGIC_NUMBER
			return ImageUtil.changeBitmapColor(sourceBitmap, Color.rgb(0, 191, 0), Color.WHITE); // MAGIC_NUMBER
		case 5: // MAGIC_NUMBER
			return ImageUtil.changeBitmapColor(sourceBitmap, Color.YELLOW, Color.BLACK);
		case 6: // MAGIC_NUMBER
			return ImageUtil.changeBitmapColor(sourceBitmap, Color.rgb(63, 255, 255), Color.BLACK); // MAGIC_NUMBER
		case 7: // MAGIC_NUMBER
			return ImageUtil.changeBitmapColor(sourceBitmap, Color.rgb(255, 127, 255), Color.BLACK); // MAGIC_NUMBER
		case 8: // MAGIC_NUMBER
			return ImageUtil.changeBitmapColor(sourceBitmap, Color.rgb(101, 70, 46), Color.rgb(254, 237, 160)); // MAGIC_NUMBER
		default:
			return null;
		}
	}

	/**
	 * Configure the background of the widget.
	 *
	 * @param context          The {@link Context Context} in which this method is called.
	 * @param remoteViews      The remoteViews via which the update should be made
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId      The appWidgetId of the widget whose size changed.
	 */
	protected static final void configureBackground(final Context context, final RemoteViews remoteViews, final AppWidgetManager appWidgetManager,
													final int appWidgetId) {
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
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, Color.rgb(142, 196, 250)); // MAGIC_NUMBER
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
	}

	/**
	 * Get the layout resource id for the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @return The layout resource id.
	 */
	protected abstract int getWidgetLayoutId(final int appWidgetId);

	/**
	 * Get the fragment class of the widget.
	 *
	 * @return The fragment class.
	 */
	protected abstract Class<? extends GenericWidgetConfigurationActivity> getConfigurationActivityClass(); // SUPPRESS_CHECKSTYLE
}
