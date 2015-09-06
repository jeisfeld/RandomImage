package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import de.jeisfeld.randomimage.DirectoryChooserDialogFragment.ChosenDirectoryListener;
import de.jeisfeld.randomimage.DirectoryChooserDialogFragment.OnFolderLongClickListener;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;

/**
 * Add images to the list via browsing folders.
 */
public class SelectDirectoryActivity extends Activity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 6;
	/**
	 * The resource key for the returned folder.
	 */
	private static final String STRING_RESULT_FOLDER = "de.jeisfeld.randomimage.FOLDER";
	/**
	 * The resource key for the flag indicating if the image list was updated.
	 */
	private static final String STRING_RESULT_UPDATED_LIST = "de.jeisfeld.randomimage.UPDATED_LIST";

	/**
	 * Flag storing information if the image list was updated.
	 */
	private boolean updatedList = false;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 */
	public static void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, SelectDirectoryActivity.class);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			updatedList = getIntent().getBooleanExtra("updatedList", false);
		}

		DirectoryChooserDialogFragment.displayDirectoryChooserDialog(SelectDirectoryActivity.this,
				new ChosenDirectoryListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void onAddImages(final String chosenDir) {
						sendResult(chosenDir);
					}

					@Override
					public void onAddFolder(final String chosenDir) {
						final ImageList imageList = ImageRegistry.getCurrentImageList(true);
						boolean success = imageList.addFolder(chosenDir);
						if (success) {
							imageList.update(true);
							updatedList = true;
						}
						sendResult(null);
					}

					@Override
					public void onCancelled() {
						sendResult(null);
					}
				},
				new OnFolderLongClickListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public boolean onFolderLongClick(final String folderName) {
						final ImageList imageList = ImageRegistry.getCurrentImageList(true);

						String folderShortName = new File(folderName).getName();

						if (ImageUtil.isImageFolder(folderName)
								&& !imageList.contains(folderName)) {
							DialogUtil.displayConfirmationMessage(SelectDirectoryActivity.this,
									new ConfirmDialogListener() {
								/**
								 * The serial version id.
								 */
								private static final long serialVersionUID = 1L;

								@Override
								public void onDialogPositiveClick(final DialogFragment dialog) {
									boolean success = imageList.addFolder(folderName);

									if (success) {
										ArrayList<String> addedFolderList = new ArrayList<String>();
										addedFolderList.add(folderName);
										String addedFoldersString =
												DialogUtil.createFileFolderMessageString(
														null, addedFolderList, null);
										DialogUtil.displayToast(SelectDirectoryActivity.this,
												R.string.toast_added_single, addedFoldersString);
										imageList.update(true);
										updatedList = true;
									}
								}

								@Override
								public void onDialogNegativeClick(final DialogFragment dialog) {
									// do nothing
								}
							}, R.string.button_add_folder, R.string.dialog_confirmation_add_folder,
									folderShortName);

						}
						return true;
					}
				},
				null);

	}

	@Override
	public final void onSaveInstanceState(final Bundle outState) {
		outState.putBoolean("updatedList", updatedList);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Static helper method to extract the result folder.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the result folder.
	 */
	public static final String getResultFolder(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			return data.getStringExtra(STRING_RESULT_FOLDER);
		}
		else {
			return null;
		}
	}

	/**
	 * Static helper method to extract the flag indicating if the image list was updated.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the flag indicating if the image list was updated.
	 */
	public static final boolean getResultUpdatedList(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			return data.getBooleanExtra(STRING_RESULT_UPDATED_LIST, false);
		}
		else {
			return false;
		}
	}

	/**
	 * Send the result to the calling activity.
	 *
	 * @param chosenDir
	 *            The selected directory.
	 */
	private void sendResult(final String chosenDir) {
		Bundle resultData = new Bundle();
		if (chosenDir != null) {
			resultData.putString(STRING_RESULT_FOLDER, chosenDir);
		}
		resultData.putBoolean(STRING_RESULT_UPDATED_LIST, updatedList);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(chosenDir != null || updatedList ? RESULT_OK : RESULT_CANCELED, intent);
		finish();
	}

}
