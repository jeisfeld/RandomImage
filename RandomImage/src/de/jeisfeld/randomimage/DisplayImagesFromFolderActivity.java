package de.jeisfeld.randomimage;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageUtil;

/**
 * Activity to display the list of images of a folder.
 */
public class DisplayImagesFromFolderActivity extends DisplayImageListActivity {
	/**
	 * The resource key for the folder whose images should be displayed.
	 */
	public static final String STRING_EXTRA_FOLDERNAME = "de.jeisfeld.randomimage.FOLDERNAME";

	/**
	 * The names of the files to be displayed.
	 */
	private ArrayList<String> fileNames;

	/**
	 * The folder whose images should be displayed.
	 */
	private String folderName;

	/**
	 * Static helper method to start the activity to display the contents of a folder.
	 *
	 * @param context
	 *            The context starting this activity.
	 * @param folderName
	 *            the name of the folder which should be displayed.
	 *
	 */
	public static final void startActivity(final Context context, final String folderName) {
		Intent intent = new Intent(context, DisplayImagesFromFolderActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		if (folderName != null) {
			intent.putExtra(STRING_EXTRA_FOLDERNAME, folderName);
		}
		context.startActivity(intent);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		folderName = getIntent().getStringExtra(STRING_EXTRA_FOLDERNAME);

		fillListOfImagesFromFolder();

		if (fileNames.size() == 0) {
			DialogUtil.displayInfo(this, null, R.string.key_info_new_list, R.string.dialog_info_new_list);
		}
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}
	}

	/**
	 * Fill the view with the images of a folder.
	 */
	private void fillListOfImagesFromFolder() {
		fileNames = ImageUtil.getImagesInFolder(folderName);

		if (fileNames.size() == 0) {
			// do not show activity for empty folder.
			finish();
			return;
		}

		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}

		setAdapter(null, fileNames, folderName);
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
	}

}
