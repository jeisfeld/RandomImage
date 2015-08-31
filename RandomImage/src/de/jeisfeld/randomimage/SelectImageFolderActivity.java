package de.jeisfeld.randomimage;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.FileUtil;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.ImageUtil.OnImageFoldersFoundListener;
import de.jeisfeld.randomimage.util.Logger;

/**
 * Activity to display the list of images of a folder.
 */
public class SelectImageFolderActivity extends DisplayImageListActivity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 7;

	/**
	 * The resource key for the name of the selected folder.
	 */
	private static final String STRING_RESULT_SELECTED_FOLDER = "de.jeisfeld.randomimage.SELECTED_FOLDER";

	/**
	 * Static helper method to start the activity to display the contents of a folder.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 */
	public static final void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, SelectImageFolderActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// This step initializes the adapter.
		fillListOfFolders();
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}
	}

	/**
	 * Fill the view with the list of all image folders.
	 */
	private void fillListOfFolders() {
		ImageUtil.getAllImageFolders(new OnImageFoldersFoundListener() {

			@Override
			public void handleImageFolders(final ArrayList<String> imageFolders) {
				if (imageFolders.size() == 0) {
					imageFolders.add(FileUtil.getDefaultCameraFolder());
				}
				if (getAdapter() != null) {
					getAdapter().cleanupCache();
				}
				setAdapter(null, imageFolders, null);
				getAdapter().setSelectionMode(SelectionMode.ONE);
			}

			@Override
			public void handleImageFolder(final String imageFolder) {
				Logger.log("Found image folder " + imageFolder);
			}
		});
	}

	/**
	 * Static helper method to extract the selected folder.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the selected folder
	 */
	public static final String getSelectedFolder(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getString(STRING_RESULT_SELECTED_FOLDER);
		}
		else {
			return null;
		}
	}

	/**
	 * Helper method: Return the selected folder.
	 *
	 * @param selectedFolder
	 *            The selected folder.
	 */
	protected final void returnResult(final String selectedFolder) {
		Bundle resultData = new Bundle();
		resultData.putString(STRING_RESULT_SELECTED_FOLDER, selectedFolder);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

}
