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
 * Add images sent to the app via intent.
 */
public class SelectDirectoryActivity extends Activity {
	/**
	 * The resource key for the flag telling if only folders should be queried.
	 */
	public static final String STRING_EXTRA_ONLY_FOLDER = "de.jeisfeld.randomimage.ONLY_FOLDER";
	/**
	 * The resource key for the returned folder.
	 */
	private static final String STRING_RESULT_FOLDER = "de.jeisfeld.randomimage.FOLDER";
	/**
	 * The resource key for the flag indicating if the image list was updated.
	 */
	private static final String STRING_RESULT_UPDATED_LIST = "de.jeisfeld.randomimage.UPDATED_LIST";
	/**
	 * The resource key for the flag indicating that the event was handled by this class.
	 */
	private static final String STRING_RESULT_WAS_SELECT_DIRECTORY_ACTIVITY =
			"de.jeisfeld.randomimage.WAS_SELECT_DIRECTORY_ACTIVITY";

	/**
	 * Flag storing information if the image list was updated.
	 */
	private boolean updatedList = false;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			updatedList = getIntent().getBooleanExtra("updatedList", false);
		}

		boolean onlyFolder = getIntent().getBooleanExtra(STRING_EXTRA_ONLY_FOLDER, false);

		if (Intent.ACTION_GET_CONTENT.equals(getIntent().getAction()) && getIntent().getType() != null
				&& getIntent().getType().startsWith("image/") && onlyFolder) {
			DirectoryChooserDialogFragment.displayDirectoryChooserDialog(SelectDirectoryActivity.this,
					new ChosenDirectoryListener() {
						/**
						 * The serial version id.
						 */
						private static final long serialVersionUID = 1L;

						@Override
						public void onChosenDir(final String chosenDir) {
							sendResult(chosenDir);
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
							final ImageList imageList = ImageRegistry.getCurrentImageList();

							String folderShortName = new File(folderName).getName();

							if (ImageUtil.getImagesInFolder(folderName).size() > 0
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
													imageList.update();
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
		else {
			finish();
		}

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
	 * Static helper method to extract the flag indicating if the intent was handled by this class.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the flag indicating if the intent was handled by this class.
	 */
	public static final boolean getResultWasSelectDirectoryActivity(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			return data.getBooleanExtra(STRING_RESULT_WAS_SELECT_DIRECTORY_ACTIVITY, false);
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
		resultData.putBoolean(STRING_RESULT_WAS_SELECT_DIRECTORY_ACTIVITY, true);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(chosenDir != null || updatedList ? RESULT_OK : RESULT_CANCELED, intent);
		finish();
	}

}
