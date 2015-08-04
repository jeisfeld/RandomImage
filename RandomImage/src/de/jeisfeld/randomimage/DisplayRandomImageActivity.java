package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * Display a random image.
 */
public class DisplayRandomImageActivity extends Activity {
	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_FILENAME = "de.jeisfeld.randomimage.FILENAME";

	/**
	 * The name of the displayed file.
	 */
	private String currentFileName;

	/**
	 * Static helper method to create an intent for this activitz.
	 *
	 * @param context
	 *            The context in which this activity is started.
	 * @param fileName
	 *            the image file name which should be displayed first.
	 * @return the intent.
	 */
	public static final Intent createIntent(final Context context, final String fileName) {
		Intent intent = new Intent(context, DisplayRandomImageActivity.class);
		if (fileName != null) {
			intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		}
		return intent;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			currentFileName = savedInstanceState.getString("currentFileName");
		}

		if (currentFileName == null) {
			currentFileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		}

		if (currentFileName == null) {
			// Reload the file when starting the app.
			ImageRegistry.getCurrentImageList().load();
			displayRandomImage();
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
	 */
	private void displayRandomImage() {
		currentFileName = ImageRegistry.getCurrentImageList().getRandomFileName();
		if (currentFileName == null) {
			AddImagesFromGalleryActivity.startActivity(this);
		}
		else {
			displayCurrentImage();
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
				displayRandomImage();
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
				DisplayAllImagesActivity.startActivity(DisplayRandomImageActivity.this);
			}

		});

		return gestureDetector;
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (currentFileName != null) {
			outState.putString("currentFileName", currentFileName);
		}
	}

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		DisplayAllImagesActivity.startActivity(this);
		return true;
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == AddImagesFromGalleryActivity.REQUEST_CODE) {
			int addedImagesCount = AddImagesFromGalleryActivity.getResult(resultCode, data);
			if (addedImagesCount > 0) {
				DialogUtil.displayToast(this, R.string.toast_added_images_count, addedImagesCount);
				displayRandomImage();
			}
			else {
				finish();
			}
		}
	}

	/**
	 * Method for temporary tests.
	 */
	private void test() {
		// PreferenceUtil.setSharedPreferenceBoolean(R.string.key_info_add_images, false);
	}
}
