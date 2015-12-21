package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * The extended widget, also displaying a changing image.
 */
public abstract class GenericWidget extends AppWidgetProvider {
	/**
	 * The list of all widget types.
	 */
	private static final List<Class<? extends GenericWidget>> WIDGET_TYPES = new ArrayList<>();

	/**
	 * Number of pixels per dip.
	 */
	protected static final float DENSITY = Application.getAppContext().getResources().getDisplayMetrics().density;

	/**
	 * Intent flag to indicate that a new image should be displayed.
	 */
	protected static final String EXTRA_NEW_IMAGE = "de.jeisfeld.randomimage.NEW_IMAGE";

	/**
	 * Intent flag to indicate that this intent was triggered by the user.
	 */
	protected static final String EXTRA_USER_TRIGGERED = "de.jeisfeld.randomimage.USER_TRIGGERED";

	/**
	 * The names of the image lists associated to any widget.
	 */
	private static SparseArray<String> mListNames = new SparseArray<>();

	/**
	 * A temporary storage for ButtonAnimators in order to ensure that they are not garbage collected before they
	 * complete the animation.
	 */
	private static Set<ButtonAnimator> mButtonAnimators = new HashSet<>();

	/**
	 * A temporary storage listing appWidgetIds that should change the image with the next update.
	 */
	private static Set<Integer> mDirtyWidgets = new HashSet<>();

	/**
	 * Id of an app widget triggered for update by user.
	 */
	private static Integer mUserUpdatedAppWidgetId = null;

	static {
		WIDGET_TYPES.add(MiniWidget.class);
		WIDGET_TYPES.add(ImageWidget.class);
		WIDGET_TYPES.add(StackedImageWidget.class);
	}

	@Override
	public final void onReceive(final Context context, final Intent intent) {
		String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
			int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			if (appWidgetIds != null && appWidgetIds.length > 0) {
				boolean updateFlag = intent.getBooleanExtra(EXTRA_NEW_IMAGE, false);
				if (updateFlag) {
					for (int i = 0; i < appWidgetIds.length; i++) {
						mDirtyWidgets.add(appWidgetIds[i]);
					}
				}
			}
			if (appWidgetIds != null && appWidgetIds.length == 1 && mUserUpdatedAppWidgetId == null) {
				boolean updateFlag = intent.getBooleanExtra(EXTRA_USER_TRIGGERED, false);
				if (updateFlag) {
					mUserUpdatedAppWidgetId = appWidgetIds[0];
				}
			}
		}

		super.onReceive(context, intent);
	}

	@Override
	public final void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			String listName = getListName(appWidgetId);
			boolean userTriggered = mUserUpdatedAppWidgetId != null && mUserUpdatedAppWidgetId == appWidgetId;

			onUpdateWidget(context, appWidgetManager, appWidgetId, listName, mDirtyWidgets.contains(appWidgetId), userTriggered);

			mDirtyWidgets.remove(appWidgetId);
			mUserUpdatedAppWidgetId = null;
		}
	}

	/**
	 * Called whenever a widget is updated.
	 *
	 * @param context The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId The appWidgetId for which an update is needed.
	 * @param listName the list name of the widget.
	 * @param changeImage flag indicating if the image should be changed.
	 * @param userTriggered flag indicating if the call was triggered by the user.
	 */
	protected abstract void onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId, final String listName, final boolean changeImage, final boolean userTriggered);

	// OVERRIDABLE
	@Override
	public void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			WidgetAlarmReceiver.cancelAlarm(context, appWidgetId);
			mListNames.remove(appWidgetId);

			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_list_name, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_alarm_interval, appWidgetId);
		}
	}

	/**
	 * Get the list name associated to an instance of the widget.
	 *
	 * @param appWidgetId The app widget id.
	 * @return The list name.
	 */
	protected static final String getListName(final int appWidgetId) {
		String listName = mListNames.get(appWidgetId);
		if (listName == null) {
			listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);
			if (listName == null || listName.length() == 0) {
				listName = ImageRegistry.getCurrentListName();
			}
			mListNames.put(appWidgetId, listName);
		}
		return listName;
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @param listName The list name to be used by the widget.
	 * @param interval The update interval.
	 */
	public static final void doBaseConfiguration(final int appWidgetId, final String listName, final long interval) {
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, listName);
		mListNames.put(appWidgetId, listName);

		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId, interval);
		if (interval > 0) {
			WidgetAlarmReceiver.setAlarm(Application.getAppContext(), appWidgetId, interval);
		}
		else {
			WidgetAlarmReceiver.cancelAlarm(Application.getAppContext(), appWidgetId);
		}
	}

	/**
	 * Update instances of the widgets of a specific class.
	 *
	 * @param widgetClass the widget class (required if no appWidgetIds are given)
	 * @param appWidgetId the list of instances to be updated. If empty, then all instances will be updated.
	 */
	protected static final void updateInstances(final Class<? extends GenericWidget> widgetClass, final int... appWidgetId) {
		if (widgetClass == null) {
			return;
		}

		Context context = Application.getAppContext();
		Intent intent = new Intent(context, widgetClass);
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

		int[] ids;
		if (appWidgetId.length == 0) {
			ids = getAllWidgetIds(widgetClass);
		}
		else {
			ids = appWidgetId;
		}
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		intent.putExtra(EXTRA_NEW_IMAGE, true);
		context.sendBroadcast(intent);
	}

	/**
	 * Update timers for instances of the widgets of a specific class.
	 *
	 * @param widgetClass the widget class
	 * @param appWidgetIds the list of instances to be updated. If empty, then all instances will be updated.
	 */
	protected static final void updateTimers(final Class<? extends GenericWidget> widgetClass, final int... appWidgetIds) {
		Context context = Application.getAppContext();
		int[] ids;
		if (appWidgetIds.length == 0) {
			ids = getAllWidgetIds(widgetClass);
		}
		else {
			ids = appWidgetIds;
		}

		for (int appWidgetId : ids) {
			long interval =
					PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_alarm_interval, appWidgetId, 0);

			if (interval > 0) {
				WidgetAlarmReceiver.setAlarm(context, appWidgetId, interval);
			}
		}
	}

	/**
	 * Get the ids of all widgets of this class.
	 *
	 * @param widgetClass the widget class
	 * @return The ids of all widgets of this class.
	 */
	protected static int[] getAllWidgetIds(final Class<? extends GenericWidget> widgetClass) {
		Context context = Application.getAppContext();
		return AppWidgetManager.getInstance(context).getAppWidgetIds(
				new ComponentName(context, widgetClass));
	}

	/**
	 * Get the list of all widgetIds of any widget of this app.
	 *
	 * @return The list of widgetIds of widgets of this app.
	 */
	public static ArrayList<Integer> getAllWidgetIds() {
		ArrayList<Integer> allWidgetIds = new ArrayList<>();

		for (Class<? extends GenericWidget> widgetClass : WIDGET_TYPES) {
			int[] widgetIds = getAllWidgetIds(widgetClass);
			if (widgetIds != null) {
				for (int widgetId : widgetIds) {
					allWidgetIds.add(widgetId);
				}
			}
		}

		return allWidgetIds;
	}

	/**
	 * Check if there is a widget of the given class of this id.
	 *
	 * @param widgetClass the widget class
	 * @param appWidgetId The widget id.
	 * @return true if there is a widget of the given class of this id.
	 */
	public static boolean hasWidgetOfId(final Class<? extends GenericWidget> widgetClass, final int appWidgetId) {
		int[] allAppWidgetIds = getAllWidgetIds(widgetClass);

		for (int i = 0; i < allAppWidgetIds.length; i++) {
			if (allAppWidgetIds[i] == appWidgetId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Retrieve all widgetIds for widgets related to a specific list name.
	 *
	 * @param listName The list name.
	 * @return The ids of widgets configured for this list name.
	 */
	public static ArrayList<Integer> getWidgetIdsForName(final String listName) {
		ArrayList<Integer> widgetIdsForName = new ArrayList<>();

		for (int appWidgetId : getAllWidgetIds()) {
			if (listName.equals(getListName(appWidgetId))) {
				widgetIdsForName.add(appWidgetId);
			}
		}

		return widgetIdsForName;
	}

	/**
	 * Update the list name in all widgets.
	 *
	 * @param oldName The old name.
	 * @param newName The new name.
	 */
	public static void updateListName(final String oldName, final String newName) {
		for (Class<? extends GenericWidget> widgetClass : WIDGET_TYPES) {
			int[] appWidgetIds = getAllWidgetIds(widgetClass);
			for (int appWidgetId : appWidgetIds) {
				if (oldName.equals(getListName(appWidgetId))) {
					PreferenceUtil
							.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, newName);
					mListNames.put(appWidgetId, newName);
					updateInstances(widgetClass, appWidgetId);
				}
			}
		}
	}

	/**
	 * A class handling the animation of the widget buttons.
	 */
	protected final class ButtonAnimator {
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
		private AnimatorSet mAnimatorSet;

		/**
		 * Create and animate the ButtonAnimator.
		 *
		 * @param context The {@link android.content.Context Context} in which this receiver is running.
		 * @param appWidgetManager A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
		 * @param appWidgetId The appWidgetId of the widget whose buttons should be animated.
		 * @param widgetResource The resourceId of the widget layout.
		 * @param buttonIds The buttonIds to be animated.
		 */
		protected ButtonAnimator(final Context context, final AppWidgetManager appWidgetManager,
				final int appWidgetId, final int widgetResource, final int... buttonIds) {
			mButtonAnimators.add(this);

			this.mAppWidgetId = appWidgetId;
			this.mAppWidgetManager = appWidgetManager;
			this.mButtonIds = buttonIds;
			mRemoteViews = new RemoteViews(context.getPackageName(), widgetResource);

			final ObjectAnimator fadeOut =
					ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofInt("alpha", 255, 0));
			fadeOut.setDuration(1000); // MAGIC_NUMBER
			fadeOut.addListener(new AnimatorListener() {
				@Override
				public void onAnimationStart(final Animator animation) {
					for (int buttonId : buttonIds) {
						mRemoteViews.setViewVisibility(buttonId, View.VISIBLE);
					}
				}

				@Override
				public void onAnimationRepeat(final Animator animation) {
					// do nothing
				}

				@Override
				public void onAnimationEnd(final Animator animation) {
					for (int buttonId : buttonIds) {
						mRemoteViews.setViewVisibility(buttonId, View.INVISIBLE);
					}
					mButtonAnimators.remove(this);
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
			mAppWidgetManager.partiallyUpdateAppWidget(mAppWidgetId, mRemoteViews);
		}

		/**
		 * Start the animation.
		 */
		public void start() {
			mAnimatorSet.start();
		}
	}

}
