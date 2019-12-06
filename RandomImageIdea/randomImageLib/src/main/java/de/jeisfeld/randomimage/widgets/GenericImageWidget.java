package de.jeisfeld.randomimage.widgets;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Random;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
import de.jeisfeld.randomimagelib.R;

/**
 * The extended widget, also displaying one or more changing images.
 */
public abstract class GenericImageWidget extends GenericWidget {
	/**
	 * Method name to set the background color.
	 */
	protected static final String SET_BACKGROUND_COLOR = "setBackgroundColor";

	/**
	 * Method name to set the background resource.
	 */
	private static final String SET_BACKGROUND_RESOURCE = "setBackgroundResource";

	/**
	 * Method name to set the image bitmap.
	 */
	private static final String SET_IMAGE_BITMAP = "setImageBitmap";

	/**
	 * The minimum saturation used for non-grey backgrounds.
	 */
	private static final int MIN_BACKGROUND_SATURATION = 64;

	/**
	 * Maximum difference of the color hue - value 0.5 just ensures that color boundaries do not overlap.
	 */
	private static final double MAX_HUE_DIFFERENCE = 0.3;

	/**
	 * A temporary storage for ButtonAnimators in order to ensure that they are not garbage collected before they
	 * complete the animation.
	 */
	private static final SparseArray<ButtonAnimator> BUTTON_ANIMATORS = new SparseArray<>();

	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		for (int appWidgetId : appWidgetIds) {
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_view_as_list, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_show_cyclically, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_background_style, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_button_style, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_button_color, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_view_width, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_view_height, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_current_file_name, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_current_list_of_file_names, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_requires_update, appWidgetId);
		}
	}

	/**
	 * Set the intents for the action buttons on the widget.
	 *
	 * @param context            The {@link Context Context} in which this method is called.
	 * @param appWidgetManager   A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId        The appWidgetId of the widget whose size changed.
	 * @param setBackgroundColor Flag indicating if the background color should be set as well.
	 * @param origRemoteViews    The remoteViews passed by caller.
	 */
	protected final void configureButtons(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
										  final boolean setBackgroundColor, final RemoteViews origRemoteViews) {
		// VARIABLE_DISTANCE:OFF
		RemoteViews remoteViews =
				origRemoteViews != null ? origRemoteViews : new RemoteViews(context.getPackageName(), getWidgetLayoutId(appWidgetId));
		// VARIABLE_DISTANCE:ON

		// Set the onClick intent for the "next" button
		Intent nextIntent = new Intent(context, getClass());
		nextIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		nextIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
		nextIntent.putExtra(EXTRA_UPDATE_TYPE, UpdateType.NEW_IMAGE_BY_USER);
		PendingIntent pendingNextIntent =
				PendingIntent.getBroadcast(context, appWidgetId, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.buttonNextImage, pendingNextIntent);

		// Set the onClick intent for the "settings" button
		Intent settingsIntent = new Intent(context, getConfigurationActivityClass());
		settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		settingsIntent.putExtra(GenericWidgetConfigurationActivity.EXTRA_RECONFIGURE_WIDGET, true);
		PendingIntent pendingSettingsIntent =
				PendingIntent.getActivity(context, appWidgetId, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.buttonSettings, pendingSettingsIntent);

		// Set the onClick intent for the view on empty widget
		Intent refreshIntent = new Intent(context, getClass());
		refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
		refreshIntent.putExtra(EXTRA_UPDATE_TYPE, UpdateType.SCALING);
		PendingIntent pendingRefreshIntent =
				PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, PendingIntent.FLAG_ONE_SHOT);
		remoteViews.setOnClickPendingIntent(R.id.textViewWidgetEmpty, pendingRefreshIntent);

		ButtonStyle buttonStyle = ButtonStyle.fromWidgetId(appWidgetId);
		if (buttonStyle == ButtonStyle.NARROW || buttonStyle == ButtonStyle.WIDE) {
			int padding = context.getResources().getDimensionPixelSize(
					buttonStyle == ButtonStyle.NARROW ? R.dimen.widget_button_padding_narrow : R.dimen.widget_button_padding_wide);
			remoteViews.setViewPadding(R.id.buttonNextImage, padding, 0, padding, 0);
			remoteViews.setViewPadding(R.id.buttonSettings, padding, 0, padding, 0);
		}
		else if (buttonStyle == ButtonStyle.GONE) {
			remoteViews.setViewVisibility(R.id.buttonNextImage, View.GONE);
			remoteViews.setViewVisibility(R.id.buttonSettings, View.GONE);
		}

		Bitmap[] buttonBitmaps = getColoredButtonBitmaps(context, appWidgetId, R.drawable.ic_widget_settings, R.drawable.ic_widget_next);
		remoteViews.setBitmap(R.id.buttonSettings, SET_IMAGE_BITMAP, buttonBitmaps[0]);
		remoteViews.setBitmap(R.id.buttonNextImage, SET_IMAGE_BITMAP, buttonBitmaps[1]);

		if (setBackgroundColor) {
			configureBackground(context, remoteViews, appWidgetManager, appWidgetId);
		}

		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

		if (buttonStyle == ButtonStyle.NARROW || buttonStyle == ButtonStyle.WIDE) {
			new ButtonAnimator(context, appWidgetManager, appWidgetId, remoteViews, getWidgetLayoutId(appWidgetId),
					R.id.buttonNextImage, R.id.buttonSettings).start();
		}
	}

	/**
	 * Get the coloured versions of button bitmaps.
	 *
	 * @param context         The {@link Context Context} in which this method is called.
	 * @param appWidgetId     The appWidgetId of the widget whose size changed.
	 * @param bitmapResources The resourceIds of the button bitmaps.
	 * @return The coloured button bitmaps.
	 */
	private static Bitmap[] getColoredButtonBitmaps(final Context context, final int appWidgetId, final int... bitmapResources) {
		ArrayList<Bitmap> resultList = new ArrayList<>();
		ButtonColor buttonColor = ButtonColor.fromResourceValue(
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_color, appWidgetId, -1));
		int color = buttonColor.getButtonColor();
		int secondaryColor = buttonColor.getSecondaryColor(color);

		for (int bitmapResource : bitmapResources) {
			Bitmap sourceBitmap = BitmapFactory.decodeResource(context.getResources(), bitmapResource);
			resultList.add(ImageUtil.changeBitmapColor(sourceBitmap, color, secondaryColor));
		}

		return resultList.toArray(new Bitmap[bitmapResources.length]);
	}

	/**
	 * Configure the background of the widget.
	 *
	 * @param context          The {@link Context Context} in which this method is called.
	 * @param remoteViews      The remoteViews via which the update should be made
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId      The appWidgetId of the widget whose size changed.
	 */
	protected static void configureBackground(final Context context, final RemoteViews remoteViews, final AppWidgetManager appWidgetManager,
											  final int appWidgetId) {
		BackgroundColor backgroundColor = BackgroundColor.fromWidgetId(appWidgetId);

		switch (backgroundColor) {
		case WHITE_SHADOW:
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_RESOURCE, R.drawable.background_transparent_white);
			break;
		case BLACK_SHADOW:
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_RESOURCE, R.drawable.background_transparent_black);
			break;
		case RANDOM_FROM_IMAGE:
		case COLOR_FROM_IMAGE:
		case AVERAGE_IMAGE_COLOR:
			// background can be set only after image is loaded.
			break;
		default:
			remoteViews.setInt(R.id.imageViewWidget, SET_BACKGROUND_COLOR, backgroundColor.getColorValue());
			break;
		}
	}

	/**
	 * Get the layout resource id for the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @return The layout resource id.
	 */
	protected abstract int getWidgetLayoutId(int appWidgetId);

	/**
	 * Get the fragment class of the widget.
	 *
	 * @return The fragment class.
	 */
	protected abstract Class<? extends GenericWidgetConfigurationActivity> getConfigurationActivityClass(); // SUPPRESS_CHECKSTYLE

	/**
	 * Report a widget image change to Google Analytics.
	 *
	 * @param action     The action to be reported
	 * @param updateType The updateType
	 */
	protected static void trackImageChange(final String action, final UpdateType updateType) {
		if (updateType == UpdateType.NEW_IMAGE_AUTOMATIC) {
			TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, action, "Timer");
		}
		else {
			TrackingUtil.sendEvent(Category.EVENT_VIEW, action,
					updateType == UpdateType.NEW_LIST ? "Updated List" : "Manual");
		}
	}


	/**
	 * Helper class containing constants for background colors.
	 */
	protected enum BackgroundColor {

		// JAVADOC:OFF
		FILL_FRAME(0),
		NO_BACKGROUND(1),
		WHITE_SHADOW(2),
		BLACK_SHADOW(3),
		LIGHT_GREY(4),
		DARK_GREY(5),
		BLUE(6),
		RED(7),
		GREEN(8),
		YELLOW(9),
		CYAN(10),
		MAGENTA(11),
		RANDOM(12),
		RANDOM_TRANSPARENT(13),
		RANDOM_FROM_IMAGE(14),
		COLOR_FROM_IMAGE(15),
		AVERAGE_IMAGE_COLOR(16);
		// JAVADOC:ON

		/**
		 * The value by which the color is specified in the resources.
		 */
		private final int mResourceValue;

		/**
		 * A map from the resourceValue to the color.
		 */
		private static final SparseArray<BackgroundColor> BACKGROUND_COLOR_MAP = new SparseArray<>();

		static {
			for (BackgroundColor backgroundColor : BackgroundColor.values()) {
				BACKGROUND_COLOR_MAP.put(backgroundColor.mResourceValue, backgroundColor);
			}
		}

		/**
		 * Constructor giving only the resourceValue (for random colors).
		 *
		 * @param resourceValue The resource value.
		 */
		BackgroundColor(final int resourceValue) {
			mResourceValue = resourceValue;
		}

		/**
		 * Get the color from its resource value.
		 *
		 * @param resourceValue The resource value.
		 * @return The corresponding BackgroundColor.
		 */
		protected static BackgroundColor fromResourceValue(final int resourceValue) {
			BackgroundColor result = BACKGROUND_COLOR_MAP.get(resourceValue);
			return result == null ? BackgroundColor.WHITE_SHADOW : result;
		}

		/**
		 * Get the background color style of a widget.
		 *
		 * @param appWidgetId The widget id.
		 * @return The BackgroundColor value of the widget.
		 */
		protected static BackgroundColor fromWidgetId(final int appWidgetId) {
			return fromResourceValue(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, appWidgetId,
							Integer.parseInt(Application.getAppContext().getString(R.string.pref_default_widget_background_style))));

		}

		/**
		 * Get the background color value.
		 *
		 * @return The color value.
		 */
		protected int getColorValue() {
			Random random = new Random();

			if (this == FILL_FRAME || this == NO_BACKGROUND || this == WHITE_SHADOW || this == BLACK_SHADOW) {
				return Color.TRANSPARENT;
			}

			int saturation;
			if (this == LIGHT_GREY || this == DARK_GREY) {
				saturation = 160; // MAGIC_NUMBER - difference between light grey and dark grey
			}
			else {
				saturation = MIN_BACKGROUND_SATURATION + random.nextInt(256 - MIN_BACKGROUND_SATURATION); // MAGIC_NUMBER
			}

			int valueMin = random.nextInt(256 - saturation); // MAGIC_NUMBER
			int valueMax = valueMin + saturation;

			int diff1 = 0;
			int diff2 = 0;
			if (random.nextBoolean()) {
				diff1 = random.nextInt((int) (MAX_HUE_DIFFERENCE * saturation));
			}
			else {
				diff2 = random.nextInt((int) (MAX_HUE_DIFFERENCE * saturation));
			}

			// generate RGB values based on valueMin, valueMax and the diff values.
			switch (this) {
			case LIGHT_GREY:
				return Color.rgb(valueMax, valueMax, valueMax);
			case DARK_GREY:
				return Color.rgb(valueMin, valueMin, valueMin);
			case BLUE:
				return Color.rgb(valueMin + diff1, valueMin + diff2, valueMax);
			case RED:
				return Color.rgb(valueMax, valueMin + diff1, valueMin + diff2);
			case GREEN:
				return Color.rgb(valueMin + diff1, valueMax, valueMin + diff2);
			case YELLOW:
				return Color.rgb(valueMax - diff1, valueMax - diff2, valueMin);
			case CYAN:
				return Color.rgb(valueMin, valueMax - diff1, valueMax - diff2);
			case MAGENTA:
				return Color.rgb(valueMax - diff1, valueMin, valueMax - diff2);
			case RANDOM:
				return Color.rgb(random.nextInt(0x100), random.nextInt(0x100), random.nextInt(0x100)); // MAGIC_NUMBER
			case RANDOM_TRANSPARENT:
				return Color.argb(0x10 + random.nextInt(0xE0), // MAGIC_NUMBER
						random.nextInt(0x100), random.nextInt(0x100), random.nextInt(0x100)); // MAGIC_NUMBER
			default:
				return Color.TRANSPARENT;
			}
		}
	}

	/**
	 * Helper class containing constants for button colors.
	 */
	private enum ButtonColor {

		// JAVADOC:OFF
		BLACK(0, "#000000"),
		WHITE(1, "#FFFFFF"),
		BLUE(2, "#0000FF"),
		RED(3, "#BF0000"),
		GREEN(4, "#00BF00"),
		YELLOW(5, "#FFFF00"),
		CYAN(6, "#3FFFFF"),
		MAGENTA(7, "#FF7FFF"),
		BROWN(8, "#65462E"),
		RANDOM_BW(9),
		RANDOM_COLOR(10);
		// JAVADOC:ON

		/**
		 * The value by which the color is specified in the resources.
		 */
		private final int mResourceValue;

		/**
		 * The value of the color.
		 */
		private final int mColorValue;

		/**
		 * A map from the resourceValue to the color.
		 */
		private static final SparseArray<ButtonColor> BUTTON_COLOR_MAP = new SparseArray<>();

		static {
			for (ButtonColor buttonColor : ButtonColor.values()) {
				BUTTON_COLOR_MAP.put(buttonColor.mResourceValue, buttonColor);
			}
		}

		/**
		 * Constructor giving the resourceValue and the color value.
		 *
		 * @param resourceValue The resource value.
		 * @param colorString   The color value.
		 */
		ButtonColor(final int resourceValue, final String colorString) {
			mResourceValue = resourceValue;
			mColorValue = Color.parseColor(colorString);
		}

		/**
		 * Constructor giving only the resourceValue (for random colors).
		 *
		 * @param resourceValue The resource value.
		 */
		ButtonColor(final int resourceValue) {
			mResourceValue = resourceValue;
			mColorValue = 0;
		}

		/**
		 * Get the color from its resource value.
		 *
		 * @param resourceValue The resource value.
		 * @return The corresponding ButtonColor.
		 */
		private static ButtonColor fromResourceValue(final int resourceValue) {
			ButtonColor result = BUTTON_COLOR_MAP.get(resourceValue);
			return result == null ? ButtonColor.BLUE : result;
		}

		/**
		 * Get the button color value.
		 *
		 * @return The color.
		 */
		private int getButtonColor() {
			if (this == RANDOM_BW || this == RANDOM_COLOR) {
				Random random = new Random();
				return Color.rgb(random.nextInt(0x100), random.nextInt(0x100), random.nextInt(0x100)); // MAGIC_NUMBER
			}
			else {
				return mColorValue;
			}
		}

		/**
		 * Get the secondary color value (for the button arrow).
		 *
		 * @param color the primary color.
		 * @return The secondary color.
		 */
		private int getSecondaryColor(final int color) {
			switch (this) {
			case BROWN:
			case RANDOM_COLOR:
				// "opposite" color
				return Color.rgb(
						((byte) Color.red(color)) + 0x80, // MAGIC_NUMBER
						((byte) Color.green(color)) + 0x80, // MAGIC_NUMBER
						((byte) Color.blue(color)) + 0x80 // MAGIC_NUMBER
				);
			default:
				// black/white color
				if (Color.red(color) + Color.green(color) + Color.green(color) >= 3 * 0x80) { // MAGIC_NUMBER
					return Color.BLACK;
				}
				else {
					return Color.WHITE;
				}
			}
		}
	}

	/**
	 * Helper class containing constants for button styles.
	 */
	protected enum ButtonStyle {
		// JAVADOC:OFF
		BOTTOM(0),
		TOP(1),
		NARROW(2),
		WIDE(3),
		GONE(4);
		// JAVADOC:ON

		/**
		 * The value by which the color is specified in the resources.
		 */
		private final int mResourceValue;

		/**
		 * A map from the resourceValue to the color.
		 */
		private static final SparseArray<ButtonStyle> BUTTON_STYLE_MAP = new SparseArray<>();

		static {
			for (ButtonStyle buttonStyle : ButtonStyle.values()) {
				BUTTON_STYLE_MAP.put(buttonStyle.mResourceValue, buttonStyle);
			}
		}

		/**
		 * Constructor giving the resourceValue.
		 *
		 * @param resourceValue The resource value.
		 */
		ButtonStyle(final int resourceValue) {
			mResourceValue = resourceValue;
		}

		/**
		 * Get the button style from its resource value.
		 *
		 * @param resourceValue The resource value.
		 * @return The corresponding ButtonStyle.
		 */
		protected static ButtonStyle fromResourceValue(final int resourceValue) {
			ButtonStyle result = BUTTON_STYLE_MAP.get(resourceValue);
			return result == null ? ButtonStyle.BOTTOM : result;
		}

		/**
		 * Get the background color style of a widget.
		 *
		 * @param appWidgetId The widget id.
		 * @return The BackgroundColor value of the widget.
		 */
		protected static ButtonStyle fromWidgetId(final int appWidgetId) {
			return fromResourceValue(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_button_style, appWidgetId,
							Integer.parseInt(Application.getAppContext().getString(R.string.pref_default_widget_button_style))));

		}
	}

	/**
	 * A class handling the animation of the widget buttons.
	 */
	protected static final class ButtonAnimator {
		/**
		 * The RemoteViews used for animating buttons.
		 */
		private RemoteViews mRemoteViews;

		/**
		 * A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
		 */
		private AppWidgetManager mAppWidgetManager;

		/**
		 * The appWidgetId of the widget whose buttons should be animated.
		 */
		private int mAppWidgetId;

		/**
		 * The buttonIds to be animated.
		 */
		private int[] mButtonIds;

		/**
		 * The AnimatorSet running the animation.
		 */
		private AnimatorSet mAnimatorSet = null;

		/**
		 * Create and animate the ButtonAnimator.
		 *
		 * @param context          The {@link Context Context} in which this receiver is running.
		 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
		 * @param remoteViews      The remote view for the widget
		 * @param appWidgetId      The appWidgetId of the widget whose buttons should be animated.
		 * @param widgetResource   The resourceId of the widget layout.
		 * @param buttonIds        The buttonIds to be animated.
		 */
		protected ButtonAnimator(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
								 final RemoteViews remoteViews, final int widgetResource, final int... buttonIds) {
			if (remoteViews == null) {
				return;
			}
			synchronized (BUTTON_ANIMATORS) {
				if (BUTTON_ANIMATORS.get(appWidgetId, null) != null) {
					return;
				}
				BUTTON_ANIMATORS.put(appWidgetId, this);
			}

			this.mAppWidgetId = appWidgetId;
			this.mAppWidgetManager = appWidgetManager;
			this.mButtonIds = buttonIds;
			mRemoteViews = remoteViews;

			final ObjectAnimator fadeOut =
					ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofInt("alpha", 255, 0));
			fadeOut.setDuration(1500); // MAGIC_NUMBER
			fadeOut.addListener(new AnimatorListener() {
				@Override
				public void onAnimationStart(final Animator animation) {
					for (int buttonId : buttonIds) {
						mRemoteViews.setViewVisibility(buttonId, View.VISIBLE);
					}
					setAlpha(255); // MAGIC_NUMBER
				}

				@Override
				public void onAnimationRepeat(final Animator animation) {
					// do nothing
				}

				@Override
				public void onAnimationEnd(final Animator animation) {
					setAlpha(0);
					synchronized (BUTTON_ANIMATORS) {
						BUTTON_ANIMATORS.remove(appWidgetId);
					}
				}

				@Override
				public void onAnimationCancel(final Animator animation) {
					// do nothing
				}
			});

			mAnimatorSet = new AnimatorSet();
			mAnimatorSet.play(fadeOut);
		}

		/**
		 * Set the opacity of the widget buttons.
		 *
		 * @param alpha The opacity.
		 */
		@SuppressWarnings("unused")
		private void setAlpha(final int alpha) {
			for (int buttonId : mButtonIds) {
				mRemoteViews.setInt(buttonId, "setAlpha", alpha);
				mRemoteViews.setInt(buttonId, "setBackgroundColor", Color.argb(alpha / 4, 0, 0, 0)); // MAGIC_NUMBER
			}
			mAppWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews);
		}

		/**
		 * Start the animation.
		 */
		public void start() {
			if (mAnimatorSet != null) {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						mAnimatorSet.start();
					}
				});
			}
		}

		/**
		 * Interrupt the animation.
		 */
		public void interrupt() {
			if (mAnimatorSet != null) {
				mAnimatorSet.cancel();
			}
		}

		/**
		 * Update the RemoteViews.
		 *
		 * @param remoteViews The new RemoteViews
		 */
		public void setRemoteViews(final RemoteViews remoteViews) {
			mRemoteViews = remoteViews;
		}


		/**
		 * Interrupt an animator instance.
		 *
		 * @param appWidgetId The app widget id of the widget to be interrupted.
		 */
		public static void interrupt(final int appWidgetId) {
			synchronized (BUTTON_ANIMATORS) {
				ButtonAnimator instance = BUTTON_ANIMATORS.get(appWidgetId);
				if (instance != null) {
					instance.interrupt();
				}
			}
		}

		/**
		 * Update the remoteViews of an animator instance.
		 *
		 * @param appWidgetId The app widget id of the widget to be interrupted.
		 * @param remoteViews the new RemoteViews.The app widget id of the widget to be interrupted.
		 */
		public static void setRemoteViews(final int appWidgetId, final RemoteViews remoteViews) {
			synchronized (BUTTON_ANIMATORS) {
				ButtonAnimator instance = BUTTON_ANIMATORS.get(appWidgetId);
				if (instance != null) {
					instance.setRemoteViews(remoteViews);
				}
			}
		}


	}
}
