package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * Activity to add images to the repository from the gallery.
 */
public class AddImagesActivity extends Activity {
	/**
	 * Request code with which this activity is started.
	 */
	public static final int REQUEST_CODE = 1;
	/**
	 * Request code for getting images from gallery.
	 */
	private static final int REQUEST_CODE_GALLERY = 2;
	/**
	 * The resource key for the name of the first selected file.
	 */
	private static final String STRING_RESULT_ADDED_IMAGES = "de.jeisfeld.randomimage.NEEDSREFRESH";

	/**
	 * The number of images that have been added.
	 */
	private int addedImageCount = 0;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 */
	public static final void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, AddImagesActivity.class);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			addedImageCount = savedInstanceState.getInt("addedImageCount");
			return;
		}

		DialogUtil.displayInfo(this, new MessageDialogListener() {
			/**
			 * The serial version uid.
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onDialogFinished() {
				triggerAddImage();
			}
		}, R.string.dialog_info_add_images, R.string.key_info_add_images);
	}

	/**
	 * Trigger an intent for getting an image for addition.
	 */
	private void triggerAddImage() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, REQUEST_CODE_GALLERY);
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == REQUEST_CODE_GALLERY) {
			if (resultCode == RESULT_OK) {
				Uri selectedImageUri = data.getData();
				String addedFileName = ImageRegistry.getInstance().add(selectedImageUri);
				if (addedFileName != null) {
					DialogUtil.displayToast(this, R.string.toast_added_image, addedFileName);
					addedImageCount++;
				}
				triggerAddImage();
			}
			else {
				// Finally, refresh list of images
				if (addedImageCount > 0) {
					ImageRegistry.getInstance().save();
				}
				returnResult();
			}
		}
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("addedImageCount", addedImageCount);
	}

	/**
	 * Static helper method to retrieve the activity response.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return The number of added images
	 */
	public static final int getResult(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getInt(STRING_RESULT_ADDED_IMAGES);
		}
		else {
			return 0;
		}
	}

	/**
	 * Helper method: Return the information if images have been added and finish the activity.
	 */
	private void returnResult() {
		Bundle resultData = new Bundle();
		resultData.putInt(STRING_RESULT_ADDED_IMAGES, addedImageCount);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

}
