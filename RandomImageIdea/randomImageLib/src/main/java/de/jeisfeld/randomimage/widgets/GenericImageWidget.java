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
	 * @param context            The {@link Context Context} in which this method is called.
	 * @param appWidgetManager   A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId        The appWidgetId of the widget whose size changed.
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
		default:
			return ImageUtil.changeBitmapColor(sourceBitmap,
					ButtonColor.getButtonColor(buttonColor),
					ButtonColor.getButtonSecondaryColor(buttonColor));
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
		case 2:
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_RESOURCE, R.drawable.background_transparent_white);
			break;
		case 3: // MAGIC_NUMBER
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_RESOURCE, R.drawable.background_transparent_black);
			break;
		default:
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, BackgroundColor.getBackgroundColor(backgroundStyle));
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

	/**
	 * Helper class containing constants for background colors.
	 */
	private static class BackgroundColor {
		// JAVADOC:OFF
		private static final int TRANSPARENT = Color.TRANSPARENT;
		private static final int LIGHT_GREY = Color.parseColor("#DFDFDF");
		private static final int DARK_GREY = Color.parseColor("#1F1F1F");
		private static final int BLUE = Color.parseColor("#8EC4FA");
		private static final int RED = Color.parseColor("#8D001A");
		private static final int GREEN = Color.parseColor("#ADD295");
		private static final int YELLOW = Color.parseColor("#FDF092");
		private static final int BROWN = Color.parseColor("#5B3C1A");
		// JAVADOC:ON

		/**
		 * Get the background color value from the resource value.
		 *
		 * @param resourceValue The value from the resource array.
		 * @return The color.
		 */
		private static int getBackgroundColor(final int resourceValue) {
			switch (resourceValue) {
			case 4: // MAGIC_NUMBER
				return LIGHT_GREY;
			case 5: // MAGIC_NUMBER
				return DARK_GREY;
			case 6: // MAGIC_NUMBER
				return BLUE;
			case 7: // MAGIC_NUMBER
				return RED;
			case 8: // MAGIC_NUMBER
				return GREEN;
			case 9: // MAGIC_NUMBER
				return YELLOW;
			case 10: // MAGIC_NUMBER
				return BROWN;
			default:
				return TRANSPARENT;
			}
		}
	}

	/**
	 * Helper class containing constants for button colors.
	 */
	private static class ButtonColor {
		// JAVADOC:OFF
		private static final int BLACK = Color.parseColor("#000000");
		private static final int WHITE = Color.parseColor("#FFFFFF");
		private static final int BLUE = Color.parseColor("#0000FF");
		private static final int RED = Color.parseColor("#BF0000");
		private static final int GREEN = Color.parseColor("#00BF00");
		private static final int YELLOW = Color.parseColor("#FFFF00");
		private static final int CYAN = Color.parseColor("#3FFFFF");
		private static final int MAGENTA = Color.parseColor("#FF7FFF");
		private static final int BROWN = Color.parseColor("#65462E");
		private static final int BROWN_SECONDARY = Color.parseColor("#FFF6CE");
		// JAVADOC:ON

		/**
		 * Get the button color value from the resource value.
		 *
		 * @param resourceValue The value from the resource array.
		 * @return The color.
		 */
		private static int getButtonColor(final int resourceValue) {
			switch (resourceValue) {
			case 0:
				return BLACK;
			case 1:
				return WHITE;
			case 2:
				return BLUE;
			case 3: // MAGIC_NUMBER
				return RED;
			case 4: // MAGIC_NUMBER
				return GREEN;
			case 5: // MAGIC_NUMBER
				return YELLOW;
			case 6: // MAGIC_NUMBER
				return CYAN;
			case 7: // MAGIC_NUMBER
				return MAGENTA;
			case 8: // MAGIC_NUMBER
				return BROWN;
			default:
				return BLACK;
			}
		}

		/**
		 * Get the secondary button color value from the resource value.
		 *
		 * @param resourceValue The value from the resource array.
		 * @return The color.
		 */
		private static int getButtonSecondaryColor(final int resourceValue) {
			switch (resourceValue) {
			case 0:
			case 2:
			case 3:// MAGIC_NUMBER
			case 4: // MAGIC_NUMBER
				return WHITE;
			case 1:
			case 5: // MAGIC_NUMBER
			case 6: // MAGIC_NUMBER
			case 7: // MAGIC_NUMBER
				return BLACK;
			case 8: // MAGIC_NUMBER
				return BROWN_SECONDARY;
			default:
				return WHITE;
			}
		}
	}

}
