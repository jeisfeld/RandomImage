package de.jeisfeld.randomimage.notifications;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * An activity which is intended as a popup with an image. Used in notifications.
 */
public class DisplayImagePopupActivity extends Activity {
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
	private static final Map<Integer, DisplayImagePopupActivity> NOTIFICATION_MAP = new HashMap<>();

	/**
	 * Flag helping to detect if a destroy is final or only temporary.
	 */
	private boolean mSavingInstanceState = false;
	/**
	 * Flag helping to detect if the user puts the activity into the background.
	 */
	private boolean mUserIsLeaving = false;

	/**
	 * The id of the notification triggering this activity.
	 */
	private Integer mNotificationId = null;


	/**
	 * Static helper method to create an intent for this activity.
	 *
	 * @param context        The context in which this activity is started.
	 * @param listName       the image list which should be taken.
	 * @param fileName       the image file name which should be displayed first.
	 * @param notificationId the id of the notification triggering this activity.
	 * @return the intent.
	 */
	public static final Intent createIntent(final Context context, final String listName, final String fileName, final Integer notificationId) {
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

		final String fileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		final String listName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		mNotificationId = getIntent().getIntExtra(STRING_EXTRA_NOTIFICATION_ID, -1);
		if (fileName == null || mNotificationId == -1) {
			finish();
			return;
		}
		NOTIFICATION_MAP.put(mNotificationId, this);

		Bitmap bitmap = ImageUtil.getImageBitmap(fileName, ImageUtil.MAX_BITMAP_SIZE);
		ImageView imageView = (ImageView) findViewById(R.id.imageViewMicroImage);

		imageView.setImageBitmap(bitmap);

		imageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				Intent intent = DisplayRandomImageActivity.createIntent(DisplayImagePopupActivity.this, listName, fileName, true, null,
						mNotificationId);
				startActivity(intent);
				finish();
			}
		});
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
		if (mNotificationId != null) {
			NOTIFICATION_MAP.remove(mNotificationId);
			if (mUserIsLeaving || !mSavingInstanceState) {
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
	public static final void finishActivity(final Context context, final int notificationId) {
		DisplayImagePopupActivity activity = NOTIFICATION_MAP.get(notificationId);
		if (activity != null) {
			activity.finish();
			NOTIFICATION_MAP.remove(notificationId);
		}
	}

}
