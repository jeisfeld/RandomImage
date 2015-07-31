package de.jeisfeld.randomimage;

import android.app.Activity;
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
	 * The name of the displayed file.
	 */
	private String displayFileName;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			displayFileName = savedInstanceState.getString("displayFileName");

			if (displayFileName != null) {
				displayImage(displayFileName);
				return;
			}
		}

		displayRandomImage();

		test();
	}

	/**
	 * Display an Image on this view.
	 *
	 * @param fileName
	 *            The image file.
	 */
	private void displayImage(final String fileName) {
		PinchImageView imageView = new PinchImageView(this);
		imageView.setGestureDetector(createGestureDetector());
		setContentView(imageView);
		imageView.setImage(fileName, this, 1);
	}

	/**
	 * Display a random image.
	 */
	private void displayRandomImage() {
		String fileToDisplay = ImageRegistry.getInstance().getRandomFileName();
		if (fileToDisplay == null) {
			AddImagesFromGalleryActivity.startActivity(this);
		}
		else {
			displayImage(fileToDisplay);
		}
	}

	/**
	 * Create a gesture detector that displays a new image on double tap.
	 *
	 * @return The gesture detector.
	 */
	private GestureDetector createGestureDetector() {
		GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener());

		gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {

			@Override
			public boolean onSingleTapConfirmed(final MotionEvent e) {
				// do nothing
				return false;
			}

			@Override
			public boolean onDoubleTapEvent(final MotionEvent e) {
				// do nothing
				return false;
			}

			@Override
			public boolean onDoubleTap(final MotionEvent e) {
				displayImage(ImageRegistry.getInstance().getRandomFileName());
				return true;
			}
		});

		return gestureDetector;
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (displayFileName != null) {
			outState.putString("displayFileName", displayFileName);
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
