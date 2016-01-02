package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.NotificationUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimagelib.R;

/**
 * Add images sent to the app via intent.
 */
public class AddSentImagesActivity extends Activity {

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ArrayList<String> imageListsNames = ImageRegistry.getStandardImageListNames();

		if (imageListsNames == null || imageListsNames.size() == 0) {
			DialogUtil.displayToast(AddSentImagesActivity.this, R.string.toast_aborted_add_images_no_list);
			finish();
		}
		else if (imageListsNames.size() == 1) {
			addImages(imageListsNames.get(0));
			finish();
		}
		else {
			DialogUtil
					.displayListSelectionDialog(this, new SelectFromListDialogListener() {
								@Override
								public void onDialogPositiveClick(final DialogFragment dialog, final int position,
																  final String text) {
									addImages(text);
									finish();
								}

								@Override
								public void onDialogNegativeClick(final DialogFragment dialog) {
									DialogUtil.displayToast(AddSentImagesActivity.this, R.string.toast_cancelled_add_images);
									finish();
								}
							}, R.drawable.ic_launcher, R.string.app_name, imageListsNames,
							R.string.dialog_select_list_for_external_add);
		}

	}

	/**
	 * Add the sent images to the given list.
	 *
	 * @param listName The name of the list to which the images should be added.
	 */
	private void addImages(final String listName) {
		StandardImageList imageList = ImageRegistry.getStandardImageListByName(listName, true);
		if (imageList == null) {
			Log.e(Application.TAG, "Could not load image list");
			DialogUtil.displayToast(this, R.string.toast_error_while_loading, listName);
			NotificationUtil.displayNotification(this, listName, NotificationUtil.TAG_ERROR_LOADING_LIST,
					R.string.title_notification_failed_loading, R.string.toast_error_while_loading, listName);
			return;
		}
		NotificationUtil.cancelNotification(this, listName, NotificationUtil.TAG_ERROR_LOADING_LIST);

		Intent intent = getIntent();

		if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
			// Application was started from other application by passing one image
			Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
			String addedFileName = imageList.add(imageUri);
			if (addedFileName != null) {
				PreferenceUtil.incrementCounter(R.string.key_statistics_countaddexternal);
				String shortFileName = new File(addedFileName).getName();
				DialogUtil.displayToast(this, R.string.toast_added_images_single_external, shortFileName, listName);
				NotificationUtil.displayNotification(this, listName, NotificationUtil.TAG_UPDATED_LIST,
						R.string.title_notification_updated_list, R.string.toast_added_images_single_external, shortFileName, listName);
				imageList.update(true);
			}
		}
		else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) && intent.getType() != null
				&& intent.hasExtra(Intent.EXTRA_STREAM)) {
			// Application was started from other application by passing a list of images
			ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (imageUris != null) {
				int addedFileCount = 0;
				String addedFileName;
				String lastAddedFileName = null;
				for (int i = 0; i < imageUris.size(); i++) {
					addedFileName = imageList.add(imageUris.get(i));
					if (addedFileName != null) {
						addedFileCount++;
						lastAddedFileName = addedFileName;
					}
				}
				if (addedFileCount > 1) {
					PreferenceUtil.incrementCounter(R.string.key_statistics_countaddexternal);
					DialogUtil.displayToast(this, R.string.toast_added_images_count_external, addedFileCount, listName);
					NotificationUtil.displayNotification(this, listName, NotificationUtil.TAG_UPDATED_LIST,
							R.string.title_notification_updated_list, R.string.toast_added_images_count_external, addedFileCount, listName);
					imageList.update(true);
				}
				else if (addedFileCount == 1) {
					PreferenceUtil.incrementCounter(R.string.key_statistics_countaddexternal);
					String shortFileName = new File(lastAddedFileName).getName();
					DialogUtil.displayToast(this, R.string.toast_added_images_single_external, shortFileName, listName);
					NotificationUtil.displayNotification(this, listName, NotificationUtil.TAG_UPDATED_LIST,
							R.string.title_notification_updated_list, R.string.toast_added_images_single_external, shortFileName, listName);
					imageList.update(true);
				}
				else {
					DialogUtil.displayToast(this, R.string.toast_added_images_none_external, listName);
					NotificationUtil.displayNotification(this, listName, NotificationUtil.TAG_UPDATED_LIST,
							R.string.title_notification_updated_list, R.string.toast_added_images_none_external, listName);
				}
			}
		}
	}

}
