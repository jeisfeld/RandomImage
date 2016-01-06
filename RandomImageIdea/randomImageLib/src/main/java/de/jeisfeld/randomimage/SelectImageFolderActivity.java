package de.jeisfeld.randomimage;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.ItemType;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.ImageUtil.OnImageFoldersFoundListener;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to add images to the list via display of all image folders on the device.
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
	 * The resource key for the flag to trigger SelectDirectoryActivity.
	 */
	private static final String STRING_RESULT_TRIGGER_SELECT_DIRECTORY_ACTIVITY = "de.jeisfeld.randomimage.TRIGGER_SELECT_DIRECTORY_ACTIVITY";

	/**
	 * Static helper method to start the activity to display the contents of a folder.
	 *
	 * @param activity The activity starting this activity.
	 */
	public static final void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, SelectImageFolderActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final int getLayoutId() {
		return R.layout.activity_select_image_folder;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// This step initializes the adapter.
		fillListOfFolders();
	}

	@Override
	public final void onItemClick(final ItemType itemType, final String name) {
		// itemType is always folder.
		returnResult(name);
	}

	@Override
	public final void onItemLongClick(final ItemType itemType, final String name) {
		// itemType is always folder.
	}

	/**
	 * Fill the view with the list of all image folders.
	 */
	private void fillListOfFolders() {
		ArrayList<String> allImageFolders = PreferenceUtil.getSharedPreferenceStringList(R.string.key_all_image_folders);
		setAdapter(null, allImageFolders, null, true);

		ImageUtil.getAllImageFolders(new OnImageFoldersFoundListener() {

			@Override
			public void handleImageFolders(final ArrayList<String> imageFolders) {
				// No action required, as folders are added one by one.
			}

			@Override
			public void handleImageFolder(final String imageFolder) {
				if (getAdapter() == null) {
					ArrayList<String> folderNames = new ArrayList<>();
					folderNames.add(imageFolder);

					if (getAdapter() != null) {
						getAdapter().cleanupCache();
					}

					setAdapter(null, folderNames, null, true);
				}
				else {
					getAdapter().addFolder(imageFolder);
				}
			}
		});
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.select_image_folder, menu);
		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.action_browse_folders) {
			triggerSelectDirectoryActivity();
			finish();
			return true;
		}
		else {
			return false;
		}

	}

	/**
	 * Static helper method to extract the selected folder.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
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
	 * Static helper method to extract the flag indicating if SelectDirectoryActivity was triggered.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the flag indicating if SelectDirectoryActivity was triggered.
	 */
	public static final boolean triggeredSelectDirectoryActivity(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_TRIGGER_SELECT_DIRECTORY_ACTIVITY);
		}
		else {
			return false;
		}
	}

	/**
	 * Helper method: Return the selected folder.
	 *
	 * @param selectedFolder The selected folder.
	 */
	protected final void returnResult(final String selectedFolder) {
		Bundle resultData = new Bundle();
		resultData.putString(STRING_RESULT_SELECTED_FOLDER, selectedFolder);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Helper method: Return to parent in order to trigger SelectDirectoryActivity.
	 */
	private void triggerSelectDirectoryActivity() {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_TRIGGER_SELECT_DIRECTORY_ACTIVITY, true);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

}
