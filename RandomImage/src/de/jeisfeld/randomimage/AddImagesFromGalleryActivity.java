package de.jeisfeld.randomimage;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.MediaStoreUtil;

/**
 * Activity to add images to the repository from the gallery.
 */
public class AddImagesFromGalleryActivity extends Activity {
	/**
	 * The resource key for the flag indicating if the activity is started to get a folder (instead of a list of files).
	 */
	public static final String STRING_EXTRA_FORFOLDER = "de.jeisfeld.randomimage.FORFOLDER";

	/**
	 * Request code with which this activity is started.
	 */
	public static final int REQUEST_CODE = 3;
	/**
	 * Request code for getting images from gallery.
	 */
	private static final int REQUEST_CODE_GALLERY = 2;
	/**
	 * The resource key for the number of added images.
	 */
	private static final String STRING_RESULT_ADDED_IMAGES_COUNT = "de.jeisfeld.randomimage.ADDED_IMAGES_COUNT";
	/**
	 * The resource key for the name of the added folder.
	 */
	private static final String STRING_RESULT_ADDED_FOLDER = "de.jeisfeld.randomimage.ADDED_FOLDER";

	/**
	 * The number of images that have been added.
	 */
	private int addedImageCount = 0;

	/**
	 * The folder that has been added.
	 */
	private String addedFolder = null;

	/**
	 * Flag indicating if the activity is started to get a folder (instead of a list of files).
	 */
	private boolean forFolder = false;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 * @param forFolder
	 *            flag indicating if the activity is started to get a folder (instead of a list of files).
	 */
	public static final void startActivity(final Activity activity, final boolean forFolder) {
		Intent intent = new Intent(activity, AddImagesFromGalleryActivity.class);
		intent.putExtra(STRING_EXTRA_FORFOLDER, forFolder);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			addedImageCount = savedInstanceState.getInt("addedImageCount");
			return;
		}

		forFolder = getIntent().getBooleanExtra(STRING_EXTRA_FORFOLDER, false);

		DialogUtil.displayInfo(this, new MessageDialogListener() {
			/**
			 * The serial version uid.
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onDialogFinished() {
				triggerAddImage();
			}
		}, forFolder ? R.string.key_info_add_folder : R.string.key_info_add_images,
				forFolder ? R.string.dialog_info_add_folder : R.string.dialog_info_add_images);
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
		final ImageList imageList = ImageRegistry.getCurrentImageList();

		if (requestCode != REQUEST_CODE_GALLERY) {
			return;
		}

		if (resultCode == RESULT_OK) {
			Uri selectedImageUri = data.getData();
			String fileName = MediaStoreUtil.getRealPathFromUri(selectedImageUri);
			if (forFolder) {
				if (fileName == null) {
					DialogUtil.displayToast(this, R.string.toast_added_folder_error);
				}
				else {
					String folderName = new File(fileName).getParent();
					boolean success = imageList.addFolder(folderName);
					if (success) {
						addedFolder = new File(folderName).getName();
					}
					else {
						if (imageList.contains(folderName)) {
							String shortFolderName = new File(folderName).getName();
							DialogUtil.displayToast(this, R.string.toast_added_folder_none, shortFolderName);
						}
						else {
							DialogUtil.displayToast(this, R.string.toast_added_folder_error);
						}
					}
				}
				if (addedFolder != null) {
					imageList.update();
				}
				returnResult();
			}
			else {
				boolean success = imageList.addFile(fileName);
				if (success) {
					String shortFileName = new File(fileName).getName();
					DialogUtil.displayToast(this, R.string.toast_added_image, shortFileName);
					addedImageCount++;
				}
				else {
					if (imageList.contains(fileName)) {
						String shortFileName = new File(fileName).getName();
						DialogUtil.displayToast(this, R.string.toast_added_image_none, shortFileName);
					}
					else {
						DialogUtil.displayToast(this, R.string.toast_added_image_error);
					}
				}
				triggerAddImage();
			}
		}
		else {
			// Finally, refresh list of images
			if (addedImageCount > 0) {
				imageList.update();
			}
			returnResult();
		}
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("addedImageCount", addedImageCount);
	}

	/**
	 * Static helper method to retrieve the number of added images from the activity response.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return The number of added images
	 */
	public static final int getAddedImageCountFromResult(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			return data.getExtras().getInt(STRING_RESULT_ADDED_IMAGES_COUNT);
		}
		else {
			return 0;
		}
	}

	/**
	 * Static helper method to retrieve the added folder name from the activity response.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return The name of the added folder.
	 */
	public static final String getAddedFolderFromResult(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			return data.getExtras().getString(STRING_RESULT_ADDED_FOLDER);
		}
		else {
			return null;
		}
	}

	/**
	 * Helper method: Return the information if images have been added and finish the activity.
	 */
	private void returnResult() {
		Bundle resultData = new Bundle();
		resultData.putInt(STRING_RESULT_ADDED_IMAGES_COUNT, addedImageCount);
		if (addedFolder != null) {
			resultData.putString(STRING_RESULT_ADDED_FOLDER, addedFolder);
		}
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

}
