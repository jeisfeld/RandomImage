package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.view.ThumbImageView;

/**
 * Activity to display the list of images of a folder.
 */
public class DisplayImagesFromFolderActivity extends DisplayImageListActivity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 5;
	/**
	 * The resource key for the folder whose images should be displayed.
	 */
	private static final String STRING_EXTRA_FOLDERNAME = "de.jeisfeld.randomimage.FOLDERNAME";

	/**
	 * The resource key for the flag indicating if the activity is opened in order to add images to the current list.
	 */
	private static final String STRING_EXTRA_FORADDITION = "de.jeisfeld.randomimage.FORADDITION";

	/**
	 * The resource key for the flag if the parent activity should be finished.
	 */
	private static final String STRING_RESULT_FILES_ADDED = "de.jeisfeld.randomimage.FILES_ADDED";

	/**
	 * The names of the files to be displayed.
	 */
	private ArrayList<String> fileNames;

	/**
	 * The folder whose images should be displayed.
	 */
	private String folderName;

	/**
	 * The current action within this activity.
	 */
	private CurrentAction currentAction = CurrentAction.DISPLAY;

	/**
	 * Static helper method to start the activity to display the contents of a folder.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 * @param folderName
	 *            the name of the folder which should be displayed.
	 * @param forAddition
	 *            Flag indicating if the activity is opened in order to add images to the current list.
	 */
	public static final void startActivity(final Activity activity, final String folderName, final boolean forAddition) {
		Intent intent = new Intent(activity, DisplayImagesFromFolderActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		if (folderName != null) {
			intent.putExtra(STRING_EXTRA_FOLDERNAME, folderName);
		}
		if (forAddition) {
			intent.putExtra(STRING_EXTRA_FORADDITION, forAddition);
		}
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		folderName = getIntent().getStringExtra(STRING_EXTRA_FOLDERNAME);
		boolean forAddition = getIntent().getBooleanExtra(STRING_EXTRA_FORADDITION, false);

		// This step initializes the adapter.
		fillListOfImagesFromFolder();

		if (savedInstanceState != null) {
			currentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
		}
		else {
			currentAction = forAddition ? CurrentAction.ADD : CurrentAction.DISPLAY;
		}
		changeAction(currentAction);

		if (forAddition) {
			DialogUtil.displayInfo(this, null, R.string.key_info_add_images, R.string.dialog_info_add_images);
		}
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		switch (currentAction) {
		case DISPLAY:
			return false;
		case ADD:
			getMenuInflater().inflate(R.menu.add_to_list, menu);
			return true;
		default:
			return false;
		}
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		int id = item.getItemId();

		switch (currentAction) {
		case DISPLAY:
			return super.onOptionsItemSelected(item);
		case ADD:
			return onOptionsItemSelectedAdd(id);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Handler for options selected while in add mode.
	 *
	 * @param menuId
	 *            The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemSelectedAdd(final int menuId) {
		switch (menuId) {
		case R.id.action_add_images:
			final ImageList imageList = ImageRegistry.getCurrentImageList();
			final ArrayList<String> imagesToBeAdded = getAdapter().getSelectedFiles();
			if (imagesToBeAdded.size() > 0) {

				ArrayList<String> addedImages = new ArrayList<String>();
				for (String fileName : imagesToBeAdded) {
					boolean isAdded = imageList.addFile(fileName);
					if (isAdded) {
						addedImages.add(fileName);
					}
				}

				String addedImagesString =
						DialogUtil.createFileFolderMessageString(null, null, addedImages);

				int totalAddedCount = addedImages.size();
				if (totalAddedCount == 0) {
					DialogUtil.displayToast(DisplayImagesFromFolderActivity.this, R.string.toast_added_no_image);
				}
				else if (totalAddedCount == 1) {
					DialogUtil.displayToast(DisplayImagesFromFolderActivity.this, R.string.toast_added_single,
							addedImagesString);
				}
				else {
					DialogUtil.displayToast(DisplayImagesFromFolderActivity.this, R.string.toast_added_multiple,
							addedImagesString);
				}

				if (totalAddedCount > 0) {
					PreferenceUtil.incrementCounter(R.string.key_statistics_countaddfiles);
					imageList.update();
				}
				returnResult(totalAddedCount > 0);
			}
			else {
				DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						addFolderToImageList();
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						returnResult(false);
					}
				}, R.string.button_add_folder, R.string.dialog_confirmation_selected_no_image_add_folder,
						new File(folderName).getName());
			}
			return true;
		case R.id.action_add_image_folder:
			int selectedImagesCount = getAdapter().getSelectedFiles().size();

			if (selectedImagesCount == 0) {
				addFolderToImageList();
			}
			else {
				DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						addFolderToImageList();
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// stay in the activity.
						return;
					}
				}, R.string.button_add_folder, R.string.dialog_confirmation_add_folder_ignore_selection,
						new File(folderName).getName());
			}
			return true;
		case R.id.action_cancel:
			returnResult(false);
			return true;
		case R.id.action_select_all:
			boolean markingStatus = getAdapter().toggleSelectAll();
			for (int i = 0; i < getGridView().getChildCount(); i++) {
				View imageView = getGridView().getChildAt(i);
				if (imageView instanceof ThumbImageView) {
					((ThumbImageView) imageView).setMarked(markingStatus);
				}
			}
			return true;
		default:
			return false;
		}
	}

	/**
	 * Add the current folder to the current imageList.
	 */
	private void addFolderToImageList() {
		final ImageList imageList2 = ImageRegistry.getCurrentImageList();
		boolean success = imageList2.addFolder(folderName);
		if (success) {
			ArrayList<String> addedFolderList = new ArrayList<String>();
			addedFolderList.add(folderName);
			String addedFoldersString =
					DialogUtil.createFileFolderMessageString(null, addedFolderList, null);
			DialogUtil.displayToast(this, R.string.toast_added_single, addedFoldersString);
			PreferenceUtil.incrementCounter(R.string.key_statistics_countaddfolder);
			imageList2.update();
		}
		else {
			if (imageList2.contains(folderName)) {
				DialogUtil.displayToast(this, R.string.toast_added_folder_none, new File(folderName).getName());
			}
			else {
				DialogUtil.displayToast(this, R.string.toast_error_select_folder);
			}
		}
		returnResult(true);
	}

	/**
	 * Fill the view with the images of a folder.
	 */
	private void fillListOfImagesFromFolder() {
		fileNames = ImageUtil.getImagesInFolder(folderName);

		if (fileNames.size() == 0) {
			DialogUtil.displayInfo(this, new MessageDialogListener() {
				/**
				 * The serial version id.
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void onDialogFinished() {
					// Nothing to display.
					returnResult(false);
				}
			}, 0, R.string.dialog_info_no_images_in_folder, folderName);
			return;
		}

		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}

		setAdapter(null, null, fileNames);
		setTitle(folderName);
	}

	/**
	 * Change the action within this activity (display or add).
	 *
	 * @param action
	 *            the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action != null) {
			currentAction = action;
			setSelectionMode(action == CurrentAction.ADD ? SelectionMode.MULTIPLE_ADD : SelectionMode.ONE);
			invalidateOptionsMenu();
		}
	}

	/**
	 * Static helper method to extract the filesAdded flag.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the flag if the files were added.
	 */
	public static final boolean getResultFilesAdded(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_FILES_ADDED);
		}
		else {
			return false;
		}
	}

	/**
	 * Helper method: Return the flag if files have been added.
	 *
	 * @param filesAdded
	 *            The flag if files have been added to the list.
	 */
	private void returnResult(final boolean filesAdded) {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_FILES_ADDED, filesAdded);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("currentAction", currentAction);
	}

	/**
	 * The current action in this activity.
	 */
	private enum CurrentAction {
		/**
		 * Just display photos.
		 */
		DISPLAY,
		/**
		 * Add photos.
		 */
		ADD
	}

}
