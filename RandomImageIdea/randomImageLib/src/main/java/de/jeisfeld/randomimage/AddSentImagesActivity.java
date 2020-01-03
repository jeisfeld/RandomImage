package de.jeisfeld.randomimage;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.notifications.NotificationUtil.NotificationType;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimagelib.R;

/**
 * Add images sent to the app via intent.
 */
public class AddSentImagesActivity extends Activity {

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ArrayList<String> imageListsNames = ImageRegistry.getStandardImageListNames(ListFiltering.HIDE_BY_REGEXP);

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
							}, R.drawable.ic_launcher, R.string.app_name, imageListsNames, R.string.button_cancel,
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
			NotificationUtil.displayNotification(this, listName, NotificationType.ERROR_LOADING_LIST,
					R.string.title_notification_failed_loading, R.string.toast_error_while_loading, listName);
			return;
		}
		NotificationUtil.cancelNotification(this, listName, NotificationType.ERROR_LOADING_LIST);

		Intent intent = getIntent();

		if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
			// Application was started from other application by passing one image
			Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
			String addedFileName = imageList.add(imageUri);
			if (addedFileName == null) {
				DialogUtil.displayToast(this, R.string.toast_added_image_none);
			}
			else {
				String shortFileName = new File(addedFileName).getName();
				DialogUtil.displayToast(this, R.string.toast_added_images_single_external, shortFileName, listName);
				NotificationUtil.notifyUpdatedList(this, listName, false, null, null, Collections.singletonList(addedFileName));
				imageList.update(true);
			}
		}
		else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) && intent.getType() != null
				&& intent.hasExtra(Intent.EXTRA_STREAM)) {
			// Application was started from other application by passing a list of images
			ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (imageUris != null) {
				List<String> addedImages = new ArrayList<>();
				String addedFileName;
				for (int i = 0; i < imageUris.size(); i++) {
					addedFileName = imageList.add(imageUris.get(i));
					if (addedFileName != null) {
						addedImages.add(addedFileName);
					}
				}
				if (addedImages.size() > 1) {
					DialogUtil.displayToast(this, R.string.toast_added_images_count_external, addedImages.size(), listName);
					NotificationUtil.notifyUpdatedList(this, listName, false, null, null, addedImages);
					imageList.update(true);
				}
				else if (addedImages.size() == 1) {
					String shortFileName = new File(addedImages.get(0)).getName();
					DialogUtil.displayToast(this, R.string.toast_added_images_single_external, shortFileName, listName);
					NotificationUtil.notifyUpdatedList(this, listName, false, null, null, addedImages);
					imageList.update(true);
				}
				else {
					DialogUtil.displayToast(this, R.string.toast_added_images_none_external, listName);
				}
			}
		}
	}

}
