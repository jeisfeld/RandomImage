package de.jeisfeld.randomimage.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.concurrent.TimeUnit;

import de.jeisfeld.randomimage.BaseActivity;
import de.jeisfeld.randomimage.DisplayImageDetailsActivity;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.DisplayRandomImageActivity.FlipType;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
import de.jeisfeld.randomimage.view.PinchImageView;
import de.jeisfeld.randomimage.view.PinchImageView.ScaleType;
import de.jeisfeld.randomimagelib.R;

/**
 * An activity which is intended as a popup with an image. Used in notifications.
 */
public class DisplayImagePopupActivity extends BaseActivity {
	/**
	 * The resource key for the image list.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";
	/**
	 * The resource key for the file name.
	 */
	public static final String STRING_EXTRA_FILENAME = "de.jeisfeld.randomimage.FILENAME";
	/**
	 * The resource key for the id of the notification triggering this activity.
	 */
	private static final String STRING_EXTRA_NOTIFICATION_ID = "de.jeisfeld.randomimage.NOTIFICATION_ID";

	/**
	 * Map storing the activities triggered by notifications.
	 */
	private static final SparseArray<DisplayImagePopupActivity> NOTIFICATION_MAP = new SparseArray<>();

	/**
	 * Flag helping to detect if a destroy is final or only temporary.
	 */
	private boolean mSavingInstanceState = false;
	/**
	 * Flag helping to detect if the user puts the activity into the background.
	 */
	private boolean mUserIsLeaving = false;
	/**
	 * Flag indicating that the user started the DisplayRandomImageActivity.
	 */
	private boolean mUserStartedRandomImageActivity = false;

	/**
	 * The id of the notification triggering this activity.
	 */
	private Integer mNotificationId = null;

	/**
	 * The view where the image is displayed.
	 */
	private PinchImageView mImageView;

	/**
	 * The file to be displayed.
	 */
	private String mFileName;
	/**
	 * The list being displayed.
	 */
	private String mListName;

	/**
	 * The frequency of automatic image change.
	 */
	private Long mChangeFrequency;
	/**
	 * A Handler used for automatically changing the image by timeout.
	 */
	private ChangeByTimeoutHandler mChangeByTimeoutHandler;


	/**
	 * Static helper method to create an intent for this activity.
	 *
	 * @param context        The context in which this activity is started.
	 * @param listName       the image list which should be taken.
	 * @param fileName       the image file name which should be displayed first.
	 * @param notificationId the id of the notification triggering this activity.
	 * @return the intent.
	 */
	public static Intent createIntent(final Context context, final String listName, final String fileName, final Integer notificationId) {
		Intent intent = new Intent(context, DisplayImagePopupActivity.class);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		if (fileName != null) {
			intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		}
		if (notificationId != null) {
			intent.putExtra(STRING_EXTRA_NOTIFICATION_ID, notificationId);
			finishActivity(context, notificationId);
		}
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		return intent;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_image_popup);

		mFileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		mNotificationId = getIntent().getIntExtra(STRING_EXTRA_NOTIFICATION_ID, -1);
		if (mFileName == null || mNotificationId == -1) {
			mNotificationId = null;
			finish();
			return;
		}
		NOTIFICATION_MAP.put(mNotificationId, this);

		mImageView = findViewById(R.id.imageViewMicroImage);
		mImageView.setScaleType(ScaleType.HALF_SIZE);
		mImageView.setImage(mFileName, this, 0);
		mImageView.setGestureDetector(getGestureDetector());

		mChangeFrequency =
				PreferenceUtil.getIndexedSharedPreferenceLong(R.string.key_notification_detail_change_timeout, mNotificationId, -1);
		if (mChangeFrequency > 0) {
			mChangeByTimeoutHandler = new ChangeByTimeoutHandler();
			mChangeByTimeoutHandler.start();
		}
	}

	/**
	 * Create the gesture detector handling flinging.
	 *
	 * @return the gesture detector.
	 */
	private GestureDetector getGestureDetector() {
		final FlipType flingType = FlipType.fromResourceValue(
				PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_flip_behavior, mNotificationId, -1));


		PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_notification_detail_flip_behavior, mNotificationId, 0);

		return new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			/**
			 * The speed which is accepted as fling.
			 */
			private static final int FLING_SPEED = 3000;

			@Override
			public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
				if (flingType == FlipType.NO_CHANGE) {
					return false;
				}

				if (Math.abs(velocityX) + Math.abs(velocityY) > FLING_SPEED) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							finish();
						}
					};

					mImageView.animateOut(velocityX, velocityY, runnable);

					PreferenceUtil.incrementCounter(R.string.key_statistics_countfling);
					TrackingUtil.sendEvent(Category.EVENT_VIEW, "Fling", "Popup");
					return true;
				}
				else {
					return false;
				}
			}

			@Override
			public boolean onSingleTapUp(final MotionEvent e) {
				startActivity(DisplayRandomImageActivity.createIntent(DisplayImagePopupActivity.this,
						mListName, mFileName, true, null, mNotificationId));
				mUserStartedRandomImageActivity = true;
				finish();
				return true;
			}

			@Override
			public void onLongPress(final MotionEvent e) {
				DisplayImageDetailsActivity.startActivity(DisplayImagePopupActivity.this, mFileName, mListName, null, true,
						"Display image popup");
			}
		});
	}


	@Override
	protected final void onDestroy() {
		super.onDestroy();
		if (mNotificationId != null) {
			NOTIFICATION_MAP.remove(mNotificationId);
			if ((mUserIsLeaving || !mSavingInstanceState) && !mUserStartedRandomImageActivity) {
				NotificationAlarmReceiver.cancelAlarm(this, mNotificationId, true);
				NotificationAlarmReceiver.setAlarm(this, mNotificationId, false);
			}
		}
	}

	@Override
	protected final void onUserLeaveHint() {
		mUserIsLeaving = true;
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
		TrackingUtil.sendScreen(this);
		mUserIsLeaving = false;
		mSavingInstanceState = false;
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		mSavingInstanceState = true;
	}

	/**
	 * Finish the activity started from a certain notification.
	 *
	 * @param context        The context.
	 * @param notificationId The notificationId that has triggered the activity.
	 */
	public static void finishActivity(final Context context, final int notificationId) {
		DisplayImagePopupActivity activity = NOTIFICATION_MAP.get(notificationId);
		if (activity != null) {
			TrackingUtil.sendEvent(Category.EVENT_BACKGROUND, "Auto_Close", "Popup");
			activity.finish();
			NOTIFICATION_MAP.remove(notificationId);
		}
	}


	/**
	 * Class managing the automatic image change by timeout.
	 */
	private final class ChangeByTimeoutHandler {
		/**
		 * The currently used timeout.
		 */
		private long mCurrentTimeout;

		/**
		 * The Handler responsible for managing the thread..
		 */
		private Handler mHandler = new Handler();

		/**
		 * The runnable used for automatically changing the image by timeout.
		 */
		private Runnable mChangeByTimeoutRunnable = new Runnable() {
			@Override
			public void run() {
				mFileName = ImageRegistry.getImageListByName(mListName, false).getRandomFileName();
				mImageView.reset();
				mImageView.setImage(mFileName, DisplayImagePopupActivity.this, 0);
				mChangeByTimeoutHandler.start();
			}
		};

		/**
		 * Start the timeout for automatic image change.
		 */
		private synchronized void start() {
			if (mChangeFrequency > 0) {
				mCurrentTimeout = TimeUnit.SECONDS.toMillis(mChangeFrequency);
				mHandler.postDelayed(
						mChangeByTimeoutRunnable,
						mCurrentTimeout);
			}
		}
	}

}
