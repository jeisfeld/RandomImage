package de.jeisfeld.randomimage;

import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.samsung.android.sdk.penremote.ButtonEvent;
import com.samsung.android.sdk.penremote.SpenEvent;
import com.samsung.android.sdk.penremote.SpenEventListener;
import com.samsung.android.sdk.penremote.SpenRemote;
import com.samsung.android.sdk.penremote.SpenRemote.ConnectionResultCallback;
import com.samsung.android.sdk.penremote.SpenUnit;
import com.samsung.android.sdk.penremote.SpenUnitManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import de.jeisfeld.randomimage.notifications.NotificationAlarmReceiver;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.notifications.NotificationUtil.NotificationType;
import de.jeisfeld.randomimage.util.CachedRandomFileProvider;
import de.jeisfeld.randomimage.util.DateUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageAnalyzer;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.Logger;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.RandomFileListProvider;
import de.jeisfeld.randomimage.util.RandomFileProvider;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
import de.jeisfeld.randomimage.view.PinchImageView;
import de.jeisfeld.randomimage.view.PinchImageView.ScaleType;
import de.jeisfeld.randomimage.view.PinchImageView.UpDownListener;
import de.jeisfeld.randomimage.widgets.MiniWidget;
import de.jeisfeld.randomimage.widgets.WidgetAlarmReceiver;
import de.jeisfeld.randomimagelib.R;

import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;
import static android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
import static android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;

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
	 * A listener for up down touch events.
	 */
	private UpDownListener mUpDownListener;

	/**
	 * Flag indicating if the next image should be preloaded, and if the previous imageView should be retained.
	 */
	private final boolean mDoPreload = SystemUtil.getLargeMemoryClass() >= 256; // MAGIC_NUMBER

	/**
	 * Flag indicating if the list is currently parsed backward.
	 */
	private boolean mIsGoingBackward = false;

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
	 * Flag indicating if current view has dark background.
	 */
	private boolean mIsDark = false;

	/**
	 * Flag indicating if the navigation bar should be hidden.
	 */
	private boolean mHideNavigationBar = false;

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
	 * The way in which the background color is selected.
	 */
	private BackgroundColor mBackgroundColor = BackgroundColor.AVERAGE_IMAGE_COLOR;

	/**
	 * The way in which the new random image is selected on flipping.
	 */
	private FlipType mFlipType = FlipType.AVOID_REPETITIONS;

	/**
	 * The frequency of automatic image change.
	 */
	private long mChangeFrequency = 0;

	/**
	 * Flag indicating if change of image should be possible via single tap.
	 */
	private boolean mChangeImageWithSingleTap = false;

	/**
	 * Flag indicating if screen lock should be prevented.
	 */
	private boolean mPreventScreenLock = false;

	/**
	 * Flag helping to detect if a destroy is final or only temporary.
	 */
	private boolean mSavingInstanceState = false;

	/**
	 * Flag indicating if the triggering widget was locked.
	 */
	private boolean mIsLocked = false;

	/**
	 * The imageList used by the activity.
	 */
	private RandomFileListProvider mRandomFileProvider = null;

	/**
	 * Duration of usage of the screen.
	 */
	private long mTrackingDuration = 0;
	/**
	 * Timestamp for measuring the tracking duration.
	 */
	private long mTrackingTimestamp = 0;
	/**
	 * Number of images viewed.
	 */
	private long mTrackingImages = 0;
	/**
	 * Indicator if the activity was recreated after saving instance state.
	 */
	private boolean mRecreatedAfterSavingInstanceState = false;
	/**
	 * Flag indicating if a usage hint is still to be displayed.
	 */
	private boolean mDisplayHint = true;
	/**
	 * The manager of S-Pen gestures.
	 */
	private SpenUnitManager mSpenUnitManager = null;

	/**
	 * A Handler used for automatically changing the image by timeout.
	 */
	private final ChangeByTimeoutHandler mChangeByTimeoutHandler = new ChangeByTimeoutHandler();

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
	public static Intent createIntent(final Context context, final String listName, final String fileName,
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
	public static void finishActivityForNotification(final Context context, final int notificationId) {
		DisplayRandomImageActivity activity = NOTIFICATION_MAP.get(notificationId);
		if (activity != null) {
			TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, "Auto_Close", "Display Image from Notification");
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
	public static void finishActivityForWidget(final Context context, final int appWidgetId) {
		DisplayRandomImageActivity activity = WIDGET_MAP.get(appWidgetId);
		if (activity != null) {
			TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, "Auto_Close", "Display Image from Widget");
			if (MiniWidget.hasWidgetOfId(appWidgetId)) {
				PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_current_file_name, appWidgetId);
			}
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
	public static void startActivityForFolder(final Context context, final String folderName,
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
		Logger.log("Starting activity");
		mChangeImageWithSingleTap = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_detail_change_with_tap);
		mPreventScreenLock = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_detail_prevent_screen_timeout);

		// determine parameters if activity is started via ACTION_VIEW intent
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			Uri imageUri = getIntent().getData();
			if (imageUri != null && ImageUtil.getMimeType(imageUri).startsWith("image/")) {
				String fileName = MediaStoreUtil.getRealPathFromUri(imageUri);
				if (fileName != null) {
					File file = new File(fileName);

					String folderName = file.getParent();
					if (ImageUtil.isImage(file, true) && folderName != null) {
						getIntent().putExtra(STRING_EXTRA_FILENAME, fileName);
						getIntent().putExtra(STRING_EXTRA_FOLDERNAME, folderName);
						getIntent().putExtra(STRING_EXTRA_ALLOW_DISPLAY_MULTIPLE, true);
					}
				}
			}
		}

		if (savedInstanceState != null) {
			mListName = savedInstanceState.getString("listName");
			mCurrentFileName = savedInstanceState.getString("currentFileName");
			mPreviousFileName = savedInstanceState.getString("previousFileName");
			mLastFlingDirection = new FlingDirection(savedInstanceState.getFloat("lastFlingX", 0),
					savedInstanceState.getFloat("lastFlingY", 0));
			mPreviousCacheIndex = savedInstanceState.getInt("previousCacheIndex");
			mCurrentCacheIndex = savedInstanceState.getInt("currentCacheIndex");
			mNextCacheIndex = savedInstanceState.getInt("nextCacheIndex");
			mTrackingTimestamp = savedInstanceState.getLong("trackingTimestamp");
			mTrackingImages = savedInstanceState.getLong("trackingImages");
			mIsGoingBackward = savedInstanceState.getBoolean("isGoingBackward");
			mRandomFileProvider = savedInstanceState.getParcelable("randomFileProvider");
			mHideNavigationBar = savedInstanceState.getBoolean("hideNavigationBar");
			mChangeByTimeoutHandler.mActiveRuntime = savedInstanceState.getLong("changeByTimeoutHandler.activeRuntime");
			mRecreatedAfterSavingInstanceState = true;
			mDisplayHint = false;
		}
		if (mListName == null) {
			mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		}
		if (mCurrentFileName == null) {
			mCurrentFileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
			if (mCurrentFileName != null) {
				mTrackingImages++;
			}
		}

		mScaleType = ScaleType.fromResourceScaleType(
				PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_scale_type, R.string.pref_default_detail_scale_type));
		mBackgroundColor = BackgroundColor.fromResourceValue(
				PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_background, R.string.pref_default_detail_background));
		mFlipType = FlipType.fromResourceValue(
				PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_flip_behavior, R.string.pref_default_detail_flip_behavior));
		mChangeFrequency = PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_detail_change_timeout,
				R.string.pref_default_detail_change_timeout);

		mPreventDisplayAll = getIntent().getBooleanExtra(STRING_EXTRA_ALLOW_DISPLAY_MULTIPLE, false);

		mAppWidgetId = getIntent().getIntExtra(STRING_EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			mAppWidgetId = null;
			mNotificationId = getIntent().getIntExtra(STRING_EXTRA_NOTIFICATION_ID, -1);
			if (mNotificationId == -1) {
				mNotificationId = null;
			}
			else {
				configureNotificationProperties();
			}
		}
		else {
			configureWidgetProperties();
		}
		mHideNavigationBar = !mChangeImageWithSingleTap;

		if (mScaleType == ScaleType.TURN_FIT || mScaleType == ScaleType.TURN_STRETCH) {
			int orientation = getResources().getConfiguration().orientation;
			setRequestedOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE
					? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
					: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		createGestureDetector();
		createUpDownListener();
		createSPenListener();

		final String folderName = getIntent().getStringExtra(STRING_EXTRA_FOLDERNAME);
		if (folderName != null) {
			// If folderName is provided, then use the list of images in this folder.
			mRandomFileProvider = new CachedRandomFileProvider(new FolderRandomFileProvider(folderName, mCurrentFileName),
					mCurrentFileName, mFlipType, mRandomFileProvider);
		}
		else {
			// Otherwise, use the imageList.
			ImageList imageList;
			if (mListName == null) {
				if (mCurrentFileName == null) {
					ArrayList<String> listNames = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
					if (listNames.size() == 0) {
						// On first startup need to create default list.
						imageList = ImageRegistry.getCurrentImageListRefreshed(true);
						mListName = ImageRegistry.getCurrentListName();
					}
					else {
						DialogUtil.displayListSelectionDialog(this, new SelectFromListDialogListener() {
									@Override
									public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
										mListName = text;
										ImageRegistry.switchToImageList(mListName, CreationStyle.NONE, true);
										mRandomFileProvider = new CachedRandomFileProvider(ImageRegistry.getCurrentImageList(false),
												mCurrentFileName, mFlipType, mRandomFileProvider);
										displayImageListOnCreate(savedInstanceState, null);
									}

									@Override
									public void onDialogNegativeClick(final DialogFragment dialog) {
										MainConfigurationActivity.startActivity(DisplayRandomImageActivity.this);
									}
								}, R.string.title_dialog_select_list_name, listNames, R.string.title_activity_main_configuration,
								R.string.dialog_select_list_for_display);
						return;
					}
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
			mRandomFileProvider = new CachedRandomFileProvider(imageList, mCurrentFileName, mFlipType, mRandomFileProvider);
		}

		if (mPreventScreenLock) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		displayImageListOnCreate(savedInstanceState, folderName);

		test();
	}

	@Override
	public final void setContentView(final View view) {
		super.setContentView(view);
		if (view instanceof PinchImageView && VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			// Update navigation bar color
			int backgroundColor = ((PinchImageView) view).getBackgroundColor();
			String fileName = ((PinchImageView) view).getPathName();

			getWindow().setNavigationBarColor(backgroundColor);

			mIsDark = Color.red(backgroundColor) + Color.green(backgroundColor) + Color.blue(backgroundColor) <= 384; // MAGIC_NUMBER
			setNavigationBarFlags();

			sendNotificationBroadcast(mListName, fileName, backgroundColor, false);
		}
	}

	/**
	 * Send broadcast informing about the notification.
	 *
	 * @param listName        The list name
	 * @param fileName        The file name
	 * @param backgroundColor The background color
	 * @param isStop          Flag indicating if display is stopped
	 */
	private void sendNotificationBroadcast(final String listName, final String fileName, final int backgroundColor, final boolean isStop) {
		if (listName != null && (isStop || fileName != null)) {
			Intent intent = new Intent("de.jeisfeld.randomimage.DISPLAY_RANDOM_IMAGE");
			intent.putExtra("de.jeisfeld.randomimage.listName", listName);
			intent.putExtra("de.jeisfeld.randomimage.fileName", fileName);
			intent.putExtra("de.jeisfeld.randomimage.backgroundColor", backgroundColor);
			intent.putExtra("de.jeisfeld.randomimage.isStop", isStop);
			sendBroadcast(intent);
		}
	}


	/**
	 * Set system flags for navigation bar.
	 */
	private void setNavigationBarFlags() {
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			int flag = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;

			if (!mIsDark && VERSION.SDK_INT >= VERSION_CODES.O) {
				flag |= SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
			}

			if (mHideNavigationBar) {
				flag |= SYSTEM_UI_FLAG_HIDE_NAVIGATION | SYSTEM_UI_FLAG_IMMERSIVE;
			}

			getWindow().getDecorView().setSystemUiVisibility(flag);
		}
	}

	/**
	 * Final action on creation of activity - display the image list.
	 *
	 * @param savedInstanceState The saved instance state from onCreate
	 * @param folderName         The folder name if existing
	 */
	private void displayImageListOnCreate(final Bundle savedInstanceState, final String folderName) {
		if (mCurrentFileName == null) {
			displayRandomImage(false);
		}
		else {
			mCurrentImageView = createImageView(mCurrentFileName, mCurrentCacheIndex);
			setContentView(mCurrentImageView);
			mChangeByTimeoutHandler.start();
			if (mDoPreload && mRandomFileProvider.isReady()) {
				mRandomFileProvider.goForward();
				mNextFileName = mRandomFileProvider.getCurrentFileName();
				if (mNextFileName != null) {
					mNextImageView = createImageView(mNextFileName, mNextCacheIndex);
				}
				mRandomFileProvider.goBackward();
			}
		}

		if (savedInstanceState == null) {
			displayHint();
			PreferenceUtil.incrementCounter(R.string.key_statistics_countdisplayrandom);

			// Trigger data updates that should be run every now and then for sanity reasons
			if (mNotificationId == null) {
				NotificationAlarmReceiver.createNotificationAlarmsIfOutdated();
				ImageUtil.refreshStoredImageFoldersIfApplicable();
			}
		}

		sendInitialTrackingEvent(savedInstanceState != null, folderName != null);
	}


	/**
	 * Configure the properties defined by the notification that triggered this activity.
	 */
	private void configureNotificationProperties() {
		mNotificationId = getIntent().getIntExtra(STRING_EXTRA_NOTIFICATION_ID, -1);
		int notificationStyle = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_style, mNotificationId, -1);

		if (NotificationUtil.isLockScreenActivityNotificationStyle(notificationStyle)) {
			// Show on locked screen if required
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
				setTurnScreenOn(true);
				setShowWhenLocked(true);
			}
			else {
				Window window = getWindow();
				window.addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED);
				window.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON);
			}
		}

		// configurations if triggered from notification
		NOTIFICATION_MAP.put(mNotificationId, this);

		if (!PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_detail_use_default, mNotificationId, false)) {
			mScaleType = ScaleType.fromResourceScaleType(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_scale_type, mNotificationId, -1));
			mBackgroundColor = BackgroundColor.fromResourceValue(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_background, mNotificationId, -1));
			mFlipType = FlipType.fromResourceValue(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_flip_behavior, mNotificationId, -1));
			mChangeFrequency =
					PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_detail_change_timeout, mNotificationId, -1);
			mChangeImageWithSingleTap =
					PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_detail_change_with_tap, mNotificationId, false);
			mPreventScreenLock =
					PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_notification_detail_prevent_screen_timeout, mNotificationId, false);
		}

		if (!NotificationUtil.isActivityNotificationStyle(notificationStyle)) {
			// Stop auto-cancellation if a normal notification has been actively clicked
			NotificationAlarmReceiver.cancelAlarm(this, mNotificationId, true);
		}
	}

	/**
	 * Configure the properties defined by the widget that triggered this activity.
	 */
	private void configureWidgetProperties() {
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

		if (!PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_detail_use_default, mAppWidgetId, false)) {
			mScaleType = ScaleType.fromResourceScaleType(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, mAppWidgetId, -1));
			mBackgroundColor = BackgroundColor.fromResourceValue(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_background, mAppWidgetId, -1));
			mFlipType = FlipType.fromResourceValue(
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_detail_flip_behavior, mAppWidgetId, -1));
			mChangeFrequency =
					PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_widget_detail_change_timeout, mAppWidgetId, -1);
			mChangeImageWithSingleTap =
					PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_detail_change_with_tap, mAppWidgetId, false);
			mPreventScreenLock =
					PreferenceUtil.getIndexedSharedPreferenceBoolean(R.string.key_widget_detail_prevent_screen_timeout, mAppWidgetId, false);
		}

		if (MiniWidget.hasWidgetOfId(mAppWidgetId) && mFlipType.allowsGoingBack()) {
			mCurrentFileName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_current_file_name, mAppWidgetId);
		}

		WidgetAlarmReceiver.setCancellationAlarm(this, mAppWidgetId);
	}

	/**
	 * Display the hint message if required.
	 */
	private void displayHint() {
		if (mDisplayHint) {
			DialogUtil.displayInfo(DisplayRandomImageActivity.this, null,
					R.string.key_hint_display_image, R.string.dialog_hint_display_image);
			DialogUtil.displayFirstUseMessageIfRequired(this);
			mDisplayHint = false;
		}
	}

	/**
	 * Send the initial event to Google analytics.
	 *
	 * @param hasSavedInstanceState parameter indicating if there was a saved instance state.
	 * @param hasFolderName         parameter indicating if there was a folder name.
	 */
	private void sendInitialTrackingEvent(final boolean hasSavedInstanceState, final boolean hasFolderName) {
		if (hasSavedInstanceState) {
			TrackingUtil.sendEvent(Category.EVENT_VIEW, "Orientation_Change", "Display Images");
		}
		else {
			String trackingLabel;
			if (mNotificationId != null) {
				trackingLabel = "Notification";
			}
			else if (mAppWidgetId != null) {
				if (getIntent().getStringExtra(STRING_EXTRA_FILENAME) != null) {
					trackingLabel = "Image Widget";
				}
				else {
					trackingLabel = "Mini Widget";
				}
			}
			else if (hasFolderName) {
				trackingLabel = "Folder";
			}
			else if (getIntent().getStringExtra(STRING_EXTRA_FILENAME) != null) {
				trackingLabel = "List Configuration";
			}
			else {
				trackingLabel = "Launcher";
			}
			TrackingUtil.sendEvent(Category.EVENT_VIEW, "View_Images", trackingLabel);
		}
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
		mChangeByTimeoutHandler.stop();
		if (mNotificationId != null) {
			NOTIFICATION_MAP.delete(mNotificationId);
			if (!mSavingInstanceState) {
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
		if (!mSavingInstanceState) {
			sendStatistics();
		}

		try {
			if (SystemUtil.hasSamsungApi()) {
				SpenRemote spenRemote = SpenRemote.getInstance();
				if (spenRemote.isConnected()) {
					spenRemote.disconnect(this);
				}
				mSpenUnitManager = null;
			}
		}
		catch (IllegalArgumentException e) {
			// Ignore
		}

		sendNotificationBroadcast(mListName, null, 0, true);
	}

	@Override
	protected final void onResume() {
		super.onResume();
		TrackingUtil.sendScreen(this);
		if (!mRecreatedAfterSavingInstanceState) {
			if (mTrackingDuration > 0) {
				sendStatistics();
				TrackingUtil.sendEvent(Category.EVENT_VIEW, "View_Images", "Resuming");
			}
			mTrackingTimestamp = System.currentTimeMillis();
		}
		mSavingInstanceState = false;
		mRecreatedAfterSavingInstanceState = false;
		setNavigationBarFlags();
		mChangeByTimeoutHandler.resume();
		registerSPenListener();
	}

	@Override
	protected final void onPause() {
		super.onPause();
		mTrackingDuration = System.currentTimeMillis() - mTrackingTimestamp;
		mChangeByTimeoutHandler.pause();
		unregisterSPenListener();
	}

	/**
	 * Send the tracking statistics.
	 */
	private void sendStatistics() {
		TrackingUtil.sendTiming(Category.TIME_USAGE, "View_Images", null, mTrackingDuration);
		TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "Viewed_Images", null, mTrackingImages);
		mTrackingImages = 0;
		mTrackingDuration = 0;
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
		imageView.setUpDownListener(mUpDownListener);
		imageView.setScaleType(mScaleType);
		imageView.setImage(fileName, this, cacheIndex, findViewById(android.R.id.content));
		int backgroundColor = getBackgroundColor(fileName);
		imageView.setBackgroundColor(backgroundColor);

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
	 *
	 * @param goToNextImage flag indicating if the next image in the list should be selected.
	 */
	private void displayRandomImage(final boolean goToNextImage) {
		mRandomFileProvider.executeWhenReady(
				() -> setContentView(R.layout.text_view_loading),
				() -> {
					int tempCacheIndex = 0;
					mPreviousFileName = mCurrentFileName;
					if (mDoPreload) {
						mPreviousImageView = mCurrentImageView;
						tempCacheIndex = mPreviousCacheIndex;
						mPreviousCacheIndex = mCurrentCacheIndex;
					}
					if (goToNextImage) {
						// Except in very initial call, go one further
						if (mIsGoingBackward) {
							mRandomFileProvider.goBackward();
						}
						else {
							mRandomFileProvider.goForward();
						}
					}
					if (mNextImageView == null) {
						mCurrentFileName = mRandomFileProvider.getCurrentFileName();
						if (mDoPreload) {
							mCurrentCacheIndex = tempCacheIndex;
						}
						if (mCurrentFileName == null) {
							// Handle the case where the provider does not return any image.
							if (mListName != null) {
								if (DialogUtil.displaySearchForImageFoldersIfRequired(DisplayRandomImageActivity.this, false)) {
									return;
								}
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
					if (mAppWidgetId != null && MiniWidget.hasWidgetOfId(mAppWidgetId)) {
						PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_current_file_name, mAppWidgetId, mCurrentFileName);
					}

					if (goToNextImage) {
						mChangeByTimeoutHandler.stop();
					}
					mChangeByTimeoutHandler.start();

					mTrackingImages++;

					if (mDoPreload) {
						if (mIsGoingBackward) {
							mRandomFileProvider.goBackward();
						}
						else {
							mRandomFileProvider.goForward();
						}
						mNextFileName = mRandomFileProvider.getCurrentFileName();
						mNextImageView = createImageView(mNextFileName, mNextCacheIndex);
						if (mIsGoingBackward) {
							mRandomFileProvider.goForward();
						}
						else {
							mRandomFileProvider.goBackward();
						}
					}
				}, null);

	}

	/**
	 * Display the last image.
	 */
	private void displayLastImage() {
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
		mIsGoingBackward = !mIsGoingBackward;
		setContentView(mCurrentImageView);
		if (mAppWidgetId != null && MiniWidget.hasWidgetOfId(mAppWidgetId)) {
			PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_current_file_name, mAppWidgetId, mCurrentFileName);
		}

		// need to move mRandomFileProvider to new position.
		if (mIsGoingBackward) {
			mRandomFileProvider.goBackward();
		}
		else {
			mRandomFileProvider.goForward();
		}

		if (mDoPreload) {
			if (mIsGoingBackward) {
				mRandomFileProvider.goBackward();
				mNextFileName = mRandomFileProvider.getCurrentFileName();
				mRandomFileProvider.goForward();
			}
			else {
				mRandomFileProvider.goForward();
				mNextFileName = mRandomFileProvider.getCurrentFileName();
				mRandomFileProvider.goBackward();
			}
			mNextImageView = createImageView(mNextFileName, mNextCacheIndex);
		}
	}

	/**
	 * Create the gesture detector handling up/down movements.
	 */
	private void createUpDownListener() {
		mUpDownListener = new UpDownListener() {
			@Override
			public void onDown() {
				mChangeByTimeoutHandler.pause();
			}

			@Override
			public void onUp() {
				mChangeByTimeoutHandler.resume();
			}
		};
	}

	/**
	 * Create a listener for the Samsung S-Pen.
	 */
	private void createSPenListener() {
		if (SystemUtil.hasSamsungApi()) {
			final SpenRemote spenRemote = SpenRemote.getInstance();
			if (!spenRemote.isFeatureEnabled(SpenRemote.FEATURE_TYPE_BUTTON)) {
				return;
			}
			if (!spenRemote.isConnected()) {
				spenRemote.connect(this, new ConnectionResultCallback() {
					@Override
					public void onSuccess(final SpenUnitManager spenUnitManager) {
						mSpenUnitManager = spenUnitManager;
						registerSPenListener();
					}

					@Override
					public void onFailure(final int i) {
						// do nothing
					}
				});
			}
		}
	}

	/**
	 * Register the listener for SPen.
	 */
	private void registerSPenListener() {
		if (mSpenUnitManager != null) {
			try {
				mSpenUnitManager.registerSpenEventListener(new SpenEventListener() {
					/**
					 * The max difference of two clicks considered as double click
					 */
					private static final long LONG_CLICK_DURATION = 500;
					/**
					 * The last timestamp.
					 */
					private long mDownTimestamp = 0;

					@Override
					public void onEvent(final SpenEvent spenEvent) {
						if (mRandomFileProvider == null) {
							return;
						}
						final ButtonEvent buttonEvent = new ButtonEvent(spenEvent);
						switch (buttonEvent.getAction()) {
						case ButtonEvent.ACTION_DOWN:
							mDownTimestamp = buttonEvent.getTimeStamp();
							break;
						case ButtonEvent.ACTION_UP:
							runOnUiThread(() -> {
								if (mDownTimestamp > 0 && buttonEvent.getTimeStamp() - mDownTimestamp > LONG_CLICK_DURATION) {
									displayLastImage();
								}
								else {
									displayRandomImage(true);
								}
							});
							break;
						default:
							// do nothing
						}
					}
				}, mSpenUnitManager.getUnit(SpenUnit.TYPE_BUTTON));
			}
			catch (SecurityException e) {
				Log.e(Application.TAG, "Failed to register Samsung Pen", e);
			}
		}
	}

	/**
	 * Unregister the listener for SPen.
	 */
	private void unregisterSPenListener() {
		if (mSpenUnitManager != null) {
			mSpenUnitManager.unregisterSpenEventListener(mSpenUnitManager.getUnit(SpenUnit.TYPE_BUTTON));
		}
	}

	/**
	 * Create the gesture detector handling flinging.
	 */
	private void createGestureDetector() {
		mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			/**
			 * The speed which is accepted as fling.
			 */
			private static final int FLING_SPEED = 3000;

			@Override
			public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
				if (mFlipType == FlipType.NO_CHANGE || mChangeImageWithSingleTap) {
					return false;
				}

				if (Math.abs(velocityX) + Math.abs(velocityY) > FLING_SPEED) {
					Runnable runnable = () -> {
						if (mFlipType == FlipType.CLOSE) {
							finish();
							return;
						}
						FlingDirection newFlingDirection = new FlingDirection(velocityX, velocityY);
						if (newFlingDirection.isOpposite(mLastFlingDirection) && mPreviousFileName != null && mFlipType != FlipType.NEW_IMAGE) {
							displayLastImage();

							if (mAppWidgetId != null && MiniWidget.hasWidgetOfId(mAppWidgetId)) {
								PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_current_file_name,
										mAppWidgetId, mCurrentFileName);
							}

							mChangeByTimeoutHandler.stop();
							mChangeByTimeoutHandler.start();

							TrackingUtil.sendEvent(Category.EVENT_VIEW, "Fling", "Back");
						}
						else {
							displayRandomImage(true);
							TrackingUtil.sendEvent(Category.EVENT_VIEW, "Fling", "New");
						}

						if (mPreviousImageView != null) {
							mPreviousImageView.doScalingToFit();
						}

						mLastFlingDirection = newFlingDirection;
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
			public boolean onSingleTapUp(final MotionEvent e) {
				if (!mChangeImageWithSingleTap) {
					mHideNavigationBar = !mHideNavigationBar;
					setNavigationBarFlags();
					return true;
				}

				if (mFlipType == FlipType.CLOSE) {
					finish();
					return true;
				}

				float velocityX = e.getX() - mCurrentImageView.getWidth() / 2f;
				float velocityY = e.getY() - mCurrentImageView.getHeight() / 2f;
				FlingDirection newFlingDirection = new FlingDirection(velocityX, velocityY);

				if (newFlingDirection.isOpposite(mLastFlingDirection) && mPreviousFileName != null && mFlipType != FlipType.NEW_IMAGE) {
					displayLastImage();
					TrackingUtil.sendEvent(Category.EVENT_VIEW, "Fling", "Back");
				}
				else {
					displayRandomImage(true);
					TrackingUtil.sendEvent(Category.EVENT_VIEW, "Fling", "New");
				}

				if (mPreviousImageView != null) {
					mPreviousImageView.doScalingToFit();
				}

				mLastFlingDirection = newFlingDirection;

				return true;
			}

			@Override
			public void onLongPress(final MotionEvent e) {
				DisplayImageDetailsActivity.startActivity(DisplayRandomImageActivity.this, mCurrentFileName, mListName,
						mNotificationId, mAppWidgetId, mPreventDisplayAll, "Display random image");
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
			outState.putLong("trackingImages", mTrackingImages);
			outState.putLong("trackingTimestamp", mTrackingTimestamp);
			outState.putBoolean("isGoingBackward", mIsGoingBackward);
			outState.putParcelable("randomFileProvider", mRandomFileProvider);
			outState.putBoolean("hideNavigationBar", mHideNavigationBar);
			outState.putLong("changeByTimeoutHandler.activeRuntime", mChangeByTimeoutHandler.mActiveRuntime);
		}
	}

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		DisplayImageDetailsActivity.startActivity(this, mCurrentFileName, mListName,
				mNotificationId, mAppWidgetId, mPreventDisplayAll, "Display Random Image 2");
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
				if (mCurrentFileName.equals(mNextFileName)) {
					mNextImageView = null;
					mNextFileName = null;
				}
				if (mCurrentFileName.equals(mPreviousFileName)) {
					mPreviousImageView = null;
					mPreviousFileName = null;
				}
				mCurrentImageView = null;
				mCurrentFileName = null;
				mRandomFileProvider.removeCurrentFileName();
				displayRandomImage(false);
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
	public static boolean getResult(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res != null && res.getBoolean(STRING_RESULT_REFRESH_PARENT);
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
		mSavingInstanceState = false;
		finish();
	}

	@Override
	public final void updateAfterFirstImageListCreated() {
		mRandomFileProvider = new CachedRandomFileProvider(ImageRegistry.getCurrentImageList(false));
		displayRandomImage(false);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (VERSION.SDK_INT >= VERSION_CODES.S) {
			// Does not properly finish in Android 12, but this is required for proper behavior.
			finish();
		}
	}

	/**
	 * Method for temporary tests.
	 */
	private void test() {
	}

	/**
	 * A class allowing to select a random file name out of an image folder.
	 */
	private static final class FolderRandomFileProvider implements RandomFileProvider {
		/**
		 * The list of files in the folder, being the base of the provider.
		 */
		private final ArrayList<String> mFileNames;

		/**
		 * The file name returned if there is no image file in the folder.
		 */
		private final String mDefaultFileName;

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

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void waitUntilReady() {
		}

		@Override
		public void executeWhenReady(final Runnable whileLoading, final Runnable afterLoading, final Runnable ifError) {
			afterLoading.run();
		}

		@Override
		public List<String> getAllImageFiles() {
			return mFileNames;
		}
	}

	/**
	 * Class managing the automatic image change by timeout.
	 */
	private final class ChangeByTimeoutHandler {
		/**
		 * Flag if the timeout is started.
		 */
		private boolean mIsStarted = false;
		/**
		 * Flag if the timeout is paused.
		 */
		private boolean mIsPaused = false;
		/**
		 * The currently used timeout.
		 */
		private long mCurrentTimeout;
		/**
		 * The last resume time of the timeout.
		 */
		private long mLastResumeTime;
		/**
		 * The active runtime of the timeout.
		 */
		private long mActiveRuntime = 0;


		/**
		 * The Handler responsible for managing the thread..
		 */
		private final Handler mHandler = new Handler();

		/**
		 * The runnable used for automatically changing the image by timeout.
		 */
		private final Runnable mChangeByTimeoutRunnable = () -> {
			mActiveRuntime = 0;
			displayRandomImage(true);
		};

		/**
		 * Start the timeout for automatic image change.
		 */
		private synchronized void start() {
			if (!mIsStarted && mChangeFrequency > 0) {
				mIsStarted = true;
				mIsPaused = false;
				mCurrentTimeout = TimeUnit.SECONDS.toMillis(mChangeFrequency) - mActiveRuntime;
				mLastResumeTime = System.currentTimeMillis();
				mHandler.postDelayed(
						mChangeByTimeoutRunnable,
						mCurrentTimeout);
			}
		}

		/**
		 * Stop the timeout.
		 */
		private synchronized void stop() {
			if (mIsStarted) {
				mHandler.removeCallbacks(mChangeByTimeoutRunnable);
				mIsStarted = false;
				mIsPaused = false;
				mActiveRuntime = 0;
			}
		}

		/**
		 * Pause the timeout.
		 */
		private synchronized void pause() {
			if (mIsStarted && !mIsPaused) {
				mIsPaused = true;
				mActiveRuntime += System.currentTimeMillis() - mLastResumeTime;
				mHandler.removeCallbacks(mChangeByTimeoutRunnable);
			}
		}

		/**
		 * Resume the timeout.
		 */
		private synchronized void resume() {
			if (mIsPaused) {
				mIsPaused = false;
				mCurrentTimeout = TimeUnit.SECONDS.toMillis(mChangeFrequency) - mActiveRuntime;
				if (mCurrentTimeout > 0) {
					mLastResumeTime = System.currentTimeMillis();
					mHandler.postDelayed(
							mChangeByTimeoutRunnable,
							mCurrentTimeout);
				}
				else {
					mIsStarted = false;
					mCurrentTimeout = 0;
					mHandler.post(mChangeByTimeoutRunnable);
				}
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
		private final float mVelocityX;
		/**
		 * The y velocity of the movement.
		 */
		private final float mVelocityY;

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

	/**
	 * Helper class containing constants for flip types.
	 */
	public enum FlipType {
		// JAVADOC:OFF
		NEW_IMAGE(0, false),
		ONE_BACK(1, true),
		MULTIPLE_BACK(2, true),
		AVOID_REPETITIONS(3, true),
		CYCLICAL(4, true),
		NO_CHANGE(5, false),
		CLOSE(6, false);
		// JAVADOC:ON

		/**
		 * The value by which the color is specified in the resources.
		 */
		private final int mResourceValue;
		/**
		 * Flag indicating if it is possible to go back to the last image via flipping back.
		 */
		private final boolean mAllowsGoingBack;

		/**
		 * A map from the resourceValue to the color.
		 */
		private static final SparseArray<FlipType> FLIP_TYPE_MAP = new SparseArray<>();

		static {
			for (FlipType flipType : FlipType.values()) {
				FLIP_TYPE_MAP.put(flipType.mResourceValue, flipType);
			}
		}

		/**
		 * Constructor giving only the resourceValue (for random colors).
		 *
		 * @param resourceValue   The resource value.
		 * @param allowsGoingBack The indicator if it is possible to go back to the last image.
		 */
		FlipType(final int resourceValue, final boolean allowsGoingBack) {
			mResourceValue = resourceValue;
			mAllowsGoingBack = allowsGoingBack;
		}

		/**
		 * Get the color from its resource value.
		 *
		 * @param resourceValue The resource value.
		 * @return The corresponding BackgroundColor.
		 */
		public static FlipType fromResourceValue(final int resourceValue) {
			FlipType result = FLIP_TYPE_MAP.get(resourceValue);
			return result == null ? FlipType.AVOID_REPETITIONS : result;
		}

		/**
		 * Get the resource value.
		 *
		 * @return The resource value.
		 */
		public int getResourceValue() {
			return mResourceValue;
		}

		/**
		 * Get information if it is possible to go back to the last image via flipping back.
		 *
		 * @return true if it is possible to go back to last image via flipping back.
		 */
		public boolean allowsGoingBack() {
			return mAllowsGoingBack;
		}
	}
}
