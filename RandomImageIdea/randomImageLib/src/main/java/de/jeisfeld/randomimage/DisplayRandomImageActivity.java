package de.jeisfeld.randomimage;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;

import de.jeisfeld.randomimage.notifications.NotificationAlarmReceiver;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.notifications.NotificationUtil.NotificationType;
import de.jeisfeld.randomimage.util.DateUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageAnalyzer;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.RandomFileProvider;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.view.PinchImageView;
import de.jeisfeld.randomimage.view.PinchImageView.ScaleType;
import de.jeisfeld.randomimage.widgets.WidgetAlarmReceiver;
import de.jeisfeld.randomimagelib.R;

/**
 * Display a random image.
 */
public class DisplayRandomImageActivity extends StartActivity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 2;
	/**
	 * The resource key for the image list.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";
	/**
	 * The resource key for the image folder.
	 */
	private static final String STRING_EXTRA_FOLDERNAME = "de.jeisfeld.randomimage.FOLDERNAME";
	/**
	 * The resource key for the file name.
	 */
	public static final String STRING_EXTRA_FILENAME = "de.jeisfeld.randomimage.FILENAME";
	/**
	 * The resource key for the flat indicating if the activity can be opened on top of existing activities.
	 */
	private static final String STRING_EXTRA_ALLOW_DISPLAY_MULTIPLE = "de.jeisfeld.randomimage.ALLOW_DISPLAY_MULTIPLE";
	/**
	 * The resource key for the id of the widget triggering this activity.
	 */
	private static final String STRING_EXTRA_APP_WIDGET_ID = "de.jeisfeld.randomimage.APP_WIDGET_ID";
	/**
	 * The resource key for the id of the notification triggering this activity.
	 */
	private static final String STRING_EXTRA_NOTIFICATION_ID = "de.jeisfeld.randomimage.NOTIFICATION_ID";

	/**
	 * The resource key for the flag if the parent activity should be refreshed.
	 */
	private static final String STRING_RESULT_REFRESH_PARENT = "de.jeisfeld.randomimage.REFRESH_PARENT";

	/**
	 * Map storing the activities triggered by notifications.
	 */
	private static final SparseArray<DisplayRandomImageActivity> NOTIFICATION_MAP = new SparseArray<>();

	/**
	 * Map storing the activities triggered by widgets.
	 */
	private static final SparseArray<DisplayRandomImageActivity> WIDGET_MAP = new SparseArray<>();

	/**
	 * The name of the used image list.
	 */
	private String mListName;

	/**
	 * The name of the previously displayed file.
	 */
	private String mPreviousFileName = null;

	/**
	 * The name of the displayed file.
	 */
	private String mCurrentFileName;

	/**
	 * The name of the preloaded file for next display.
	 */
	private String mNextFileName = null;

	/**
	 * The gesture detector used by this activity.
	 */
	private GestureDetector mGestureDetector;

	/**
	 * Flag indicating if the next image should be preloaded, and if the previous imageView should be retained.
	 */
	private boolean mDoPreload = SystemUtil.getLargeMemoryClass() >= 256; // MAGIC_NUMBER

	/**
	 * The view displaying the current file.
	 */
	private PinchImageView mCurrentImageView = null;

	/**
	 * The view that displayed the previous file.
	 */
	private PinchImageView mPreviousImageView = null;

	/**
	 * The view prepared to display the next file.
	 */
	private PinchImageView mNextImageView = null;

	/**
	 * The cache index of the previous image view.
	 */
	private int mPreviousCacheIndex = 3; // MAGIC_NUMBER

	/**
	 * The cache index of the current image view.
	 */
	private int mCurrentCacheIndex = 1;

	/**
	 * The cache index of the next image view.
	 */
	private int mNextCacheIndex = 2;

	/**
	 * The direction of the last fling movement.
	 */
	private FlingDirection mLastFlingDirection = null;

	/**
	 * flag indicating if the activity should prevent to trigger ConfigureImageListActivity.
	 */
	private boolean mPreventDisplayAll;

	/**
	 * The id of the widget triggering this activity.
	 */
	private Integer mAppWidgetId = null;

	/**
	 * The id of the notification triggering this activity.
	 */
	private Integer mNotificationId = null;

	/**
	 * The way in which the image gets initially scaled.
	 */
	private ScaleType mScaleType = ScaleType.FIT;
	/**
	 * The background color.
	 */
	private BackgroundColor mBackgroundColor = BackgroundColor.AVERAGE_IMAGE_COLOR;

	/**
	 * Flag helping to detect if a destroy is final or only temporary.
	 */
	private boolean mSavingInstanceState = false;
	/**
	 * Flag helping to detect if the user puts the activity into the background.
	 */
	private boolean mUserIsLeaving = false;

	/**
	 * Flag indicating if the triggering widget was locked.
	 */
	private boolean mIsLocked = false;

	/**
	 * The imageList used by the activity.
	 */
	private RandomFileProvider mRandomFileProvider;

	/**
	 * Static helper method to create an intent for this activity.
	 *
	 * @param context              The context in which this activity is started.
	 * @param listName             the image list which should be taken.
	 * @param fileName             the image file name which should be displayed first.
	 * @param allowDisplayMultiple flag indicating if the activity can be opened on top of existing activities.
	 * @param appWidgetId          the id of the widget triggering this activity.
	 * @param notificationId       the id of the notification triggering this activity.
	 * @return the intent.
	 */
	public static final Intent createIntent(final Context context, final String listName, final String fileName,
											final boolean allowDisplayMultiple, final Integer appWidgetId, final Integer notificationId) {
		Intent intent = new Intent(context, DisplayRandomImageActivity.class);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		if (fileName != null) {
			intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		}
		intent.putExtra(STRING_EXTRA_ALLOW_DISPLAY_MULTIPLE, allowDisplayMultiple);

		if (notificationId != null) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
			finishActivityForNotification(context, notificationId);
		}
		else if (allowDisplayMultiple) {
			intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		}
		else {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		}

		if (appWidgetId != null) {
			intent.putExtra(STRING_EXTRA_APP_WIDGET_ID, appWidgetId);
		}
		if (notificationId != null) {
			intent.putExtra(STRING_EXTRA_NOTIFICATION_ID, notificationId);
		}

		return intent;
	}

	/**
	 * Finish the activity started from a certain notification.
	 *
	 * @param context        The context.
	 * @param notificationId The notificationId that has triggered the activity.
	 */
	public static final void finishActivityForNotification(final Context context, final int notificationId) {
		DisplayRandomImageActivity activity = NOTIFICATION_MAP.get(notificationId);
		if (activity != null) {
			activity.finish();
			NOTIFICATION_MAP.delete(notificationId);
		}
	}

	/**
	 * Finish the activity started from a certain widget.
	 *
	 * @param context     The context.
	 * @param appWidgetId The appWidgetId that has triggered the activity.
	 */
	public static final void finishActivityForWidget(final Context context, final int appWidgetId) {
		DisplayRandomImageActivity activity = WIDGET_MAP.get(appWidgetId);
		if (activity != null) {
			activity.finish();
			WIDGET_MAP.delete(appWidgetId);
		}
	}

	/**
	 * Static helper method to start the activity for the contents of an image folder.
	 *
	 * @param context    The context starting this activity.
	 * @param folderName the name of the folder whose images should be displayed.
	 * @param fileName   the name of the file that should be displayed first
	 */
	public static final void startActivityForFolder(final Context context, final String folderName,
													final String fileName) {
		Intent intent = new Intent(context, DisplayRandomImageActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		intent.putExtra(STRING_EXTRA_FOLDERNAME, folderName);
		intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		intent.putExtra(STRING_EXTRA_ALLOW_DISPLAY_MULTIPLE, true);
		context.startActivity(intent);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Use this as trigger to verify if notification alarms are still present.
		NotificationAlarmReceiver.createNotificationAlarmsIfOutdated();

		if (savedInstanceState != null) {
			mListName = savedInstanceState.getString("listName");
			mCurrentFileName = savedInstanceState.getString("currentFileName");
			mPreviousFileName = savedInstanceState.getString("previousFileName");
			mLastFlingDirection =
					new FlingDirection(savedInstanceState.getFloat("lastFlingX", 0),
							savedInstanceState.getFloat("lastFlingY", 0));
			mPreviousCacheIndex = savedInstanceState.getInt("previousCacheIndex");
			mCurrentCacheIndex = savedInstanceState.getInt("currentCacheIndex");
			mNextCacheIndex = savedInstanceState.getInt("nextCacheIndex");
		}
		if (mListName == null) {
			mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		}
		if (mCurrentFileName == null) {
			mCurrentFileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		}

		mPreventDisplayAll = getIntent().getBooleanExtra(STRING_EXTRA_ALLOW_DISPLAY_MULTIPLE, false);

		mAppWidgetId = getIntent().getIntExtra(STRING_EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			mAppWidgetId = null;
			mNotificationId = getIntent().getIntExtra(STRING_EXTRA_NOTIFICATION_ID, -1);
			if (mNotificationId == -1) {
				mNotificationId = null;
			}
			else {
				NOTIFICATION_MAP.put(mNotificationId, this);
				mScaleType = ScaleType.fromResourceScaleType(
						PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId, -1));
				mBackgroundColor = BackgroundColor.fromResourceValue(
						PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_background, mNotificationId, -1));

				if (!NotificationUtil.isActivityNotificationStyle(
						PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, -1))) {
					// Stop auto-cancellation if a normal notification has been actively clicked
					NotificationAlarmReceiver.cancelAlarm(this, mNotificationId, true);
				}
			}
		}
		else {
			long lastClickTime = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_last_usage_time, mAppWidgetId, -1);
			long allowedCallFrequency = PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_allowed_call_frequency, mAppWidgetId, 0);
			long nextAllowedTime = lastClickTime + TimeUnit.SECONDS.toMillis(allowedCallFrequency);
			long currentTime = System.currentTimeMillis();

			if (allowedCallFrequency > 0 && currentTime < nextAllowedTime) {
				mIsLocked = true;
				DialogUtil.displayToast(this, R.string.toast_widget_locked, DateUtil.format(new Date(nextAllowedTime)));
				finish();
				return;
			}
			WIDGET_MAP.put(mAppWidgetId, this);

			PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_last_usage_time, mAppWidgetId, currentTime);
			mScaleType = ScaleType.fromResourceScaleType(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, mAppWidgetId, -1));
			mBackgroundColor = BackgroundColor.fromResourceValue(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_background, mAppWidgetId, -1));

			WidgetAlarmReceiver.setCancellationAlarm(this, mAppWidgetId);
		}

		if (mScaleType == ScaleType.TURN_FIT || mScaleType == ScaleType.TURN_STRETCH) {
			int orientation = getResources().getConfiguration().orientation;
			setRequestedOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE
					? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		createGestureDetector();

		String folderName = getIntent().getStringExtra(STRING_EXTRA_FOLDERNAME);
		if (folderName != null) {
			// If folderName is provided, then use the list of images in this folder.
			mRandomFileProvider = new FolderRandomFileProvider(folderName, mCurrentFileName);
		}
		else {
			// Otherwise, use the imageList.
			ImageList imageList;

			if (mListName == null) {
				mListName = ImageRegistry.getCurrentListName();
				if (mCurrentFileName == null) {
					// Reload the file when starting the activity.
					imageList = ImageRegistry.getCurrentImageListRefreshed(false);
				}
				else {
					imageList = ImageRegistry.getCurrentImageList(false);
				}
			}
			else {
				boolean foundList = ImageRegistry.switchToImageList(mListName, ImageRegistry.CreationStyle.NONE, false);
				if (!foundList) {
					Log.e(Application.TAG, "Could not load image list");
					DialogUtil.displayToast(this, R.string.toast_error_while_loading, mListName);
					NotificationUtil.displayNotification(this, mListName, NotificationType.ERROR_LOADING_LIST,
							R.string.title_notification_failed_loading, R.string.toast_error_while_loading, mListName);
					return;
				}
				NotificationUtil.cancelNotification(this, mListName, NotificationType.ERROR_LOADING_LIST);
				imageList = ImageRegistry.getCurrentImageList(false);
			}

			mListName = imageList.getListName();

			mRandomFileProvider = imageList;
		}

		if (mCurrentFileName == null) {
			displayRandomImage();
		}
		else {
			mCurrentImageView = createImageView(mCurrentFileName, mCurrentCacheIndex);
			setContentView(mCurrentImageView);
			if (mDoPreload && mRandomFileProvider.isReady()) {
				mNextFileName = mRandomFileProvider.getRandomFileName();
				if (mNextFileName != null) {
					mNextImageView = createImageView(mNextFileName, mNextCacheIndex);
				}
			}
		}

		PreferenceUtil.incrementCounter(R.string.key_statistics_countdisplayrandom);
		if (savedInstanceState == null) {
			DialogUtil.displayInfo(this, null, R.string.key_hint_display_image, R.string.dialog_hint_display_image);
		}

		test();
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
		if (mNotificationId != null) {
			NOTIFICATION_MAP.delete(mNotificationId);
			if (mUserIsLeaving || !mSavingInstanceState) {
				NotificationAlarmReceiver.cancelAlarm(this, mNotificationId, true);
				NotificationAlarmReceiver.setAlarm(this, mNotificationId, false);
			}
		}
		if (mAppWidgetId != null) {
			WIDGET_MAP.delete(mAppWidgetId);
			if (PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_timeout, mAppWidgetId, 0) > 0) {
				WidgetAlarmReceiver.cancelAlarm(this, mAppWidgetId, true);
			}
			if (!mIsLocked) {
				PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_last_usage_time, mAppWidgetId, System.currentTimeMillis());
			}
		}
	}

	@Override
	protected final void onUserLeaveHint() {
		if (mNotificationId != null) {
			mUserIsLeaving = true;
		}
	}

	@Override
	protected final void onStop() {
		super.onStop();
		if (mUserIsLeaving) {
			finish();
		}
	}

	@Override
	protected final void onResume() {
		super.onResume();
		mUserIsLeaving = false;
		mSavingInstanceState = false;
	}

	/**
	 * Create a PinchImageView displaying a given file.
	 *
	 * @param fileName   The name of the file.
	 * @param cacheIndex an index helping for caching the image for orientation change.
	 * @return The PinchImageView.
	 */
	private PinchImageView createImageView(final String fileName, final int cacheIndex) {
		PinchImageView imageView = new PinchImageView(this);
		imageView.setId(cacheIndex);
		imageView.setGestureDetector(mGestureDetector);
		imageView.setScaleType(mScaleType);
		imageView.setImage(fileName, this, cacheIndex);
		imageView.setBackgroundColor(getBackgroundColor(fileName));

		return imageView;
	}

	/**
	 * Get the background color to be set.
	 *
	 * @param fileName The file for which to set the background.
	 * @return the background color.
	 */
	private int getBackgroundColor(final String fileName) {
		int backgroundColor;
		switch (mBackgroundColor) {
		case LIGHT:
			backgroundColor = Color.WHITE;
			break;
		case COLOR_FROM_IMAGE:
			backgroundColor = ImageAnalyzer.getColorFromImageBorder(ImageUtil.getImageBitmap(fileName, MediaStoreUtil.MINI_THUMB_SIZE));
			break;
		case AVERAGE_IMAGE_COLOR:
			backgroundColor = ImageAnalyzer.getAverageImageColor(ImageUtil.getImageBitmap(fileName, MediaStoreUtil.MINI_THUMB_SIZE));
			break;
		case DARK:
		default:
			backgroundColor = Color.TRANSPARENT;
			break;
		}
		return backgroundColor;
	}

	/**
	 * Display a random image.
	 */
	private void displayRandomImage() {
		mRandomFileProvider.executeWhenReady(
				new Runnable() {
					@Override
					public void run() {
						setContentView(R.layout.text_view_loading);
					}
				},
				new Runnable() {
					@Override
					public void run() {
						int tempCacheIndex = 0;
						mPreviousFileName = mCurrentFileName;
						if (mDoPreload) {
							mPreviousImageView = mCurrentImageView;
							tempCacheIndex = mPreviousCacheIndex;
							mPreviousCacheIndex = mCurrentCacheIndex;
						}
						if (mNextImageView == null) {
							mCurrentFileName = mRandomFileProvider.getRandomFileName();
							if (mDoPreload) {
								mCurrentCacheIndex = tempCacheIndex;
							}
							if (mCurrentFileName == null) {
								// Handle the case where the provider does not return any image.
								if (mListName != null) {
									ConfigureImageListActivity.startActivity(DisplayRandomImageActivity.this, mListName);
								}
								finish();
								return;
							}
							mCurrentImageView = createImageView(mCurrentFileName, mCurrentCacheIndex);
						}
						else {
							mCurrentFileName = mNextFileName;
							mCurrentImageView = mNextImageView;
							if (mDoPreload) {
								mCurrentCacheIndex = mNextCacheIndex;
								mNextCacheIndex = tempCacheIndex;
							}
						}
						setContentView(mCurrentImageView);

						if (mDoPreload) {
							mNextFileName = mRandomFileProvider.getRandomFileName();
							mNextImageView = createImageView(mNextFileName, mNextCacheIndex);
						}

					}
				}, null);

	}

	/**
	 * Create the gesture detector handling flinging and double tapping.
	 */
	private void createGestureDetector() {
		final int flingType;
		if (mNotificationId == null) {
			flingType = 0;
		}
		else {
			flingType = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_flip_behavior, mNotificationId, 0);
		}

		mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			/**
			 * The speed which is accepted as fling.
			 */
			private static final int FLING_SPEED = 3000;

			@Override
			public boolean onDoubleTap(final MotionEvent e) {
				if (mPreventDisplayAll) {
					if (mListName != null) {
						finish();
					}
				}
				else {
					ConfigureImageListActivity.startActivity(DisplayRandomImageActivity.this, mListName);
				}
				return true;
			}

			@Override
			public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
				if (flingType == 1) {
					return false;
				}

				if (Math.abs(velocityX) + Math.abs(velocityY) > FLING_SPEED) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							if (flingType == 2) {
								finish();
								return;
							}
							FlingDirection newFlingDirection = new FlingDirection(velocityX, velocityY);
							if (newFlingDirection.isOpposite(mLastFlingDirection) && mPreviousFileName != null) {
								String tempFileName = mCurrentFileName;
								mCurrentFileName = mPreviousFileName;
								mPreviousFileName = tempFileName;
								if (mDoPreload) {
									int tempCacheIndex = mCurrentCacheIndex;
									mCurrentCacheIndex = mPreviousCacheIndex;
									mPreviousCacheIndex = tempCacheIndex;
								}
								if (mDoPreload && mPreviousImageView != null) {
									PinchImageView tempImageView = mCurrentImageView;
									mCurrentImageView = mPreviousImageView;
									mPreviousImageView = tempImageView;
								}
								else {
									mCurrentImageView = createImageView(mCurrentFileName, mCurrentCacheIndex);
								}
								setContentView(mCurrentImageView);
							}
							else {
								displayRandomImage();
							}

							if (mPreviousImageView != null) {
								mPreviousImageView.doScalingToFit();
							}

							mLastFlingDirection = newFlingDirection;
						}
					};

					mCurrentImageView.animateOut(velocityX, velocityY, runnable);

					PreferenceUtil.incrementCounter(R.string.key_statistics_countfling);
					return true;
				}
				else {
					return false;
				}
			}

			@Override
			public void onLongPress(final MotionEvent e) {
				DisplayImageDetailsActivity.startActivity(DisplayRandomImageActivity.this, mCurrentFileName, mListName, mPreventDisplayAll);
			}

		});
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		mSavingInstanceState = true;
		if (mCurrentFileName != null) {
			outState.putString("currentFileName", mCurrentFileName);
			outState.putString("listName", mListName);
			if (mLastFlingDirection != null) {
				outState.putFloat("lastFlingX", mLastFlingDirection.mVelocityX);
				outState.putFloat("lastFlingY", mLastFlingDirection.mVelocityY);
			}
			if (mPreviousFileName != null) {
				outState.putString("previousFileName", mPreviousFileName);
			}
			outState.putInt("previousCacheIndex", mPreviousCacheIndex);
			outState.putInt("currentCacheIndex", mCurrentCacheIndex);
			outState.putInt("nextCacheIndex", mNextCacheIndex);
		}
	}

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		DisplayImageDetailsActivity.startActivity(this, mCurrentFileName, mListName, mPreventDisplayAll);
		return true;
	}

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case DisplayImageDetailsActivity.REQUEST_CODE:
			boolean finishParent = DisplayImageDetailsActivity.getResultFinishParent(resultCode, data);
			boolean fileRemoved = DisplayImageDetailsActivity.getResultFileRemoved(resultCode, data);
			if (finishParent) {
				returnResult(fileRemoved);
			}
			else if (fileRemoved) {
				displayRandomImage();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Static helper method to extract the result flag.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the flag if the parent activity should be refreshed.
	 */
	public static final boolean getResult(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_REFRESH_PARENT);
		}
		else {
			return false;
		}
	}

	/**
	 * Helper method: Return the flag if the parent activity should be finished.
	 *
	 * @param refreshParent The flag if the parent activity should be refreshed.
	 */
	private void returnResult(final boolean refreshParent) {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_REFRESH_PARENT, refreshParent);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Method for temporary tests.
	 */
	private void test() {
	}

	/**
	 * A class allowing to select a random file name out of an image folder.
	 */
	private final class FolderRandomFileProvider extends RandomFileProvider {
		/**
		 * The list of files in the folder, being the base of the provider.
		 */
		private ArrayList<String> mFileNames;

		/**
		 * The file name returned if there is no image file in the folder.
		 */
		private String mDefaultFileName;

		/**
		 * Constructor initializing with the folder name.
		 *
		 * @param folderName      The folder name.
		 * @param defaultFileName The file name returned if there is no image file in the folder.
		 */
		private FolderRandomFileProvider(final String folderName, final String defaultFileName) {
			mFileNames = ImageUtil.getImagesInFolder(folderName);
			this.mDefaultFileName = defaultFileName;
		}

		@Override
		public String getRandomFileName() {
			if (mFileNames.size() > 0) {
				return mFileNames.get(new Random().nextInt(mFileNames.size()));
			}
			else {
				return mDefaultFileName;
			}
		}
	}

	/**
	 * A class holding the direction of a fling movement.
	 */
	private static final class FlingDirection {
		/**
		 * The x velocity of the movement.
		 */
		private float mVelocityX;
		/**
		 * The y velocity of the movement.
		 */
		private float mVelocityY;

		/**
		 * Constructor.
		 *
		 * @param velocityX The x velocity of the movement.
		 * @param velocityY The y velocity of the movement.
		 */
		private FlingDirection(final float velocityX, final float velocityY) {
			this.mVelocityX = velocityX;
			this.mVelocityY = velocityY;
		}

		/**
		 * Get the direction of the fling as angle.
		 *
		 * @return the angle of the movement.
		 */
		private double getAngle() {
			double angle;
			if (mVelocityX > 0) {
				angle = Math.atan(mVelocityY / mVelocityX);
			}
			else if (mVelocityX < 0) {
				angle = Math.PI + Math.atan(mVelocityY / mVelocityX);
			}
			else if (mVelocityY > 0) {
				angle = Math.PI / 2;
			}
			else {
				angle = -Math.PI / 2;
			}
			if (angle < 0) {
				angle = angle + 2 * Math.PI;
			}
			return angle;
		}

		/**
		 * Find out if two flings go into opposite direction.
		 *
		 * @param otherDirection The fling to be compared with.
		 * @return true if both go into opposite direction.
		 */
		private boolean isOpposite(final FlingDirection otherDirection) {
			if (otherDirection == null) {
				return false;
			}
			double angleDiff = Math.abs(getAngle() - otherDirection.getAngle());
			return angleDiff > Math.PI / 2 && angleDiff < 3 * Math.PI / 2; // MAGIC_NUMBER
		}
	}

	/**
	 * Helper class containing constants for background colors.
	 */
	protected enum BackgroundColor {

		// JAVADOC:OFF
		DARK(0),
		LIGHT(1),
		COLOR_FROM_IMAGE(2),
		AVERAGE_IMAGE_COLOR(3);
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
		private static BackgroundColor fromResourceValue(final int resourceValue) {
			BackgroundColor result = BACKGROUND_COLOR_MAP.get(resourceValue);
			return result == null ? BackgroundColor.AVERAGE_IMAGE_COLOR : result;
		}
	}

}
