package de.jeisfeld.randomimage;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * Add images sent to the app via intent.
 */
public class AddSentImagesActivity extends Activity {

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ImageList imageList = ImageRegistry.getCurrentImageList();

		Intent intent = getIntent();
		if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
			// Application was started from other application by passing one image
			Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
			String addedFileName = imageList.add(imageUri);
			if (addedFileName != null) {
				DialogUtil.displayToast(this, R.string.toast_added_image, addedFileName);
				imageList.save();
			}
		}
		else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) && intent.getType() != null
				&& intent.hasExtra(Intent.EXTRA_STREAM)) {
			// Application was started from other application by passing a list of images
			ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (imageUris != null) {
				int addedFileCount = 0;
				for (int i = 0; i < imageUris.size(); i++) {
					String addedFileName = imageList.add(imageUris.get(i));
					if (addedFileName != null) {
						addedFileCount++;
					}
				}
				if (addedFileCount > 0) {
					DialogUtil.displayToast(this, R.string.toast_added_images_count, addedFileCount);
					imageList.save();
				}
			}
		}
		finish();
	}
}
