package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * Display a random image.
 */
public class DisplayRandomImageActivity extends Activity {
	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";
	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_FILENAME = "de.jeisfeld.randomimage.FILENAME";
	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_FIXED_IMAGE = "de.jeisfeld.randomimage.FIXED_IMAGE";

	/**
	 * The name of the used image list.
	 */
	private String listName;

	/**
	 * The name of the displayed file.
	 */
	private String currentFileName;

	/**
	 * flag indicating if the activity should prevent to trigger DisplayAllImagesActivity.
	 */
	private boolean preventDisplayAll;

	/**
	 * The imageList used by the activity.
	 */
	private ImageList imageList;

	/**
	 * Static helper method to create an intent for this activitz.
	 *
	 * @param context
	 *            The context in which this activity is started.
	 * @param listName
	 *            the image list which should be taken.
	 * @param fileName
	 *            the image file name which should be displayed first.
	 * @param preventDisplayAll
	 *            flag indicating if the activity should prevent to trigger DisplayAllImagesActivity.
	 * @return the intent.
	 */
	public static final Intent createIntent(final Context context, final String listName, final String fileName,
			final boolean preventDisplayAll) {
		Intent intent = new Intent(context, DisplayRandomImageActivity.class);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		if (fileName != null) {
			intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		}
		intent.putExtra(STRING_EXTRA_FIXED_IMAGE, preventDisplayAll);

		if (preventDisplayAll) {
			intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		}
		else {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		}
		return intent;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			listName = savedInstanceState.getString("listName");
			currentFileName = savedInstanceState.getString("currentFileName");
			preventDisplayAll = savedInstanceState.getBoolean("preventDisplayAll");
		}

		if (listName == null) {
			listName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		}
		if (currentFileName == null) {
			currentFileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		}

		preventDisplayAll = getIntent().getBooleanExtra(STRING_EXTRA_FIXED_IMAGE, false);

		if (listName == null) {
			listName = ImageRegistry.getCurrentListName();
			imageList = ImageRegistry.getCurrentImageList();
			if (currentFileName == null) {
				// Reload the file when starting the app.
				imageList.load();
			}
		}
		else {
			imageList = ImageRegistry.getImageListByName(listName);
			if (imageList == null) {
				Log.e(Application.TAG, "Could not load image list");
				DialogUtil.displayToast(this, R.string.toast_error_while_loading, listName);
				return;
			}
		}

		if (currentFileName == null) {
			boolean success = displayRandomImage();
			if (success) {
				DialogUtil.displayInfo(this, null, R.string.key_info_display_image, R.string.dialog_info_display_image);
			}
		}
		else {
			displayCurrentImage();
		}

		test();
	}

	/**
	 * Display the current Image defined by currentFileName on this view.
	 */
	private void displayCurrentImage() {
		PinchImageView imageView = new PinchImageView(this);
		imageView.setGestureDetector(createGestureDetector());
		setContentView(imageView);
		imageView.setImage(currentFileName, this, 1);
	}

	/**
	 * Display a random image.
	 *
	 * @return false if there was no image to be displayed.
	 */
	private boolean displayRandomImage() {
		currentFileName = imageList.getRandomFileName();
		if (currentFileName == null) {
			DisplayAllImagesActivity.startActivity(this, listName);
			finish();
			return false;
		}
		else {
			displayCurrentImage();
			return true;
		}
	}

	/**
	 * Create a gesture detector that displays a new image on double tap.
	 *
	 * @return The gesture detector.
	 */
	private GestureDetector createGestureDetector() {
		GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			/**
			 * The speed which is accepted as fling.
			 */
			private static final int FLING_SPEED = 3000;

			@Override
			public boolean onDoubleTap(final MotionEvent e) {
				if (preventDisplayAll) {
					finish();
				}
				else {
					DisplayAllImagesActivity.startActivity(DisplayRandomImageActivity.this, listName);
				}
				return true;
			}

			@Override
			public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX,
					final float velocityY) {
				if (Math.abs(velocityX) + Math.abs(velocityY) > FLING_SPEED) {
					displayRandomImage();
					return true;
				}
				else {
					return false;
				}
			}

			@Override
			public void onLongPress(final MotionEvent e) {
				// TODO: menu with further options.
				if (preventDisplayAll) {
					finish();
				}
				else {
					DisplayAllImagesActivity.startActivity(DisplayRandomImageActivity.this, listName);
				}
			}

		});

		return gestureDetector;
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (currentFileName != null) {
			outState.putString("currentFileName", currentFileName);
			outState.putString("listName", listName);
			outState.putBoolean("preventDisplayAll", preventDisplayAll);
		}
	}

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		if (preventDisplayAll) {
			finish();
		}
		else {
			DisplayAllImagesActivity.startActivity(this, listName);
		}
		return true;
	}

	/**
	 * Method for temporary tests.
	 */
	private void test() {
	}
}
