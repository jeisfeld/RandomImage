package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import de.jeisfeld.randomimage.DisplayImageListAdapter.ItemType;
import de.jeisfeld.randomimage.DisplayImageListAdapter.SelectionMode;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display the list of images of a folder.
 */
public class DisplayImagesFromFolderActivity extends DisplayImageListActivity {
	/**
	 * The resource key for the name of the image list to be displayed.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";
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
	 * The folder whose images should be displayed.
	 */
	private String mFolderName;

	/**
	 * The TextView displaying the folder name.
	 */
	private TextView mTextViewFolderName;

	/**
	 * The current action within this activity.
	 */
	private CurrentAction mCurrentAction = CurrentAction.DISPLAY;

	/**
	 * The name of the image list to which files should be added.
	 */
	private String mListName;

	/**
	 * Static helper method to start the activity to display the contents of a folder.
	 *
	 * @param activity    The activity starting this activity.
	 * @param folderName  the name of the folder which should be displayed.
	 * @param listName the triggering image list to which files should be added.
	 * @param forAddition Flag indicating if the activity is opened in order to add images to the current list.
	 */
	public static final void startActivity(final Activity activity, final String folderName,
										   final String listName, final boolean forAddition) {
		Intent intent = new Intent(activity, DisplayImagesFromFolderActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		if (folderName != null) {
			intent.putExtra(STRING_EXTRA_FOLDERNAME, folderName);
		}
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		if (forAddition) {
			intent.putExtra(STRING_EXTRA_FORADDITION, true);
		}
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final int getLayoutId() {
		return R.layout.activity_display_images_from_folder;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTextViewFolderName = (TextView) findViewById(R.id.textViewTitle);

		mFolderName = getIntent().getStringExtra(STRING_EXTRA_FOLDERNAME);
		mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		boolean forAddition = getIntent().getBooleanExtra(STRING_EXTRA_FORADDITION, false);
		if (forAddition) {
			setTitle(R.string.title_activity_add_images);
		}

		// This step initializes the adapter.
		fillListOfImagesFromFolder();
		if (getAdapter() == null) {
			finish();
			return;
		}

		if (savedInstanceState != null) {
			mCurrentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
		}
		else {
			mCurrentAction = forAddition ? CurrentAction.ADD : CurrentAction.DISPLAY;
		}
		changeAction(mCurrentAction);

		if (forAddition) {
			DialogUtil.displayInfo(this, null, R.string.key_hint_add_images, R.string.dialog_hint_add_images);
		}
	}

	@Override
	public final void onItemClick(final ItemType itemType, final String name) {
		// itemType is always file.
		DisplayRandomImageActivity.startActivityForFolder(this, new File(name).getParent(), name);
	}

	@Override
	public final void onItemLongClick(final ItemType itemType, final String name) {
		// itemType is always file.
		DisplayImageDetailsActivity.startActivity(this, name, null, true);
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		switch (mCurrentAction) {
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

		switch (mCurrentAction) {
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
	 * @param menuId The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemSelectedAdd(final int menuId) {
		if (menuId == R.id.action_add_images) {
			final ImageList imageList = ImageRegistry.getImageListByName(mListName, true);
			final ArrayList<String> imagesToBeAdded = getAdapter().getSelectedFiles();
			if (imagesToBeAdded.size() > 0) {

				ArrayList<String> addedImages = new ArrayList<>();
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
					DialogUtil.displayToast(DisplayImagesFromFolderActivity.this, R.string.toast_added_images_none);
				}
				else if (totalAddedCount == 1) {
					DialogUtil.displayToast(DisplayImagesFromFolderActivity.this, R.string.toast_added_single, addedImagesString);
				}
				else {
					DialogUtil.displayToast(DisplayImagesFromFolderActivity.this, R.string.toast_added_multiple, addedImagesString);
				}
				NotificationUtil.notifyUpdatedList(DisplayImagesFromFolderActivity.this, mListName, false, null, null, addedImages);

				if (totalAddedCount > 0) {
					imageList.update(true);
				}
				returnResult(totalAddedCount > 0);
			}
			else {
				DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog) {
								addFolderToImageList();
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								returnResult(false);
							}
						}, null, R.string.button_add_folder, R.string.dialog_confirmation_selected_no_image_add_folder,
						new File(mFolderName).getName());
			}
			return true;
		}
		else if (menuId == R.id.action_add_image_folder) {
			int selectedImagesCount = getAdapter().getSelectedFiles().size();
			if (selectedImagesCount == 0) {
				addFolderToImageList();
			}
			else {
				DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog) {
								addFolderToImageList();
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// stay in the activity.
							}
						}, null, R.string.button_add_folder, R.string.dialog_confirmation_add_folder_ignore_selection,
						new File(mFolderName).getName());
			}
			return true;
		}
		else if (menuId == R.id.action_cancel) {
			returnResult(false);
			return true;
		}
		else if (menuId == R.id.action_select_all) {
			toggleSelectAll();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Add the current folder to the current imageList.
	 */
	private void addFolderToImageList() {
		final ImageList imageList = ImageRegistry.getImageListByName(mListName, true);
		boolean success = imageList.addFolder(mFolderName);
		if (success) {
			String addedFoldersString =
					DialogUtil.createFileFolderMessageString(null, Collections.singletonList(mFolderName), null);
			DialogUtil.displayToast(this, R.string.toast_added_single, addedFoldersString);
			NotificationUtil.notifyUpdatedList(this, imageList.getListName(), false, null, Collections.singletonList(mFolderName), null);
			imageList.update(true);
		}
		else {
			if (imageList.contains(mFolderName)) {
				DialogUtil.displayToast(this, R.string.toast_added_folder_none, new File(mFolderName).getName());
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
		ArrayList<String> fileNames = ImageUtil.getImagesInFolder(mFolderName);

		if (fileNames.size() == 0) {
			DialogUtil.displayInfo(this, new MessageDialogListener() {
				@Override
				public void onDialogFinished() {
					// Nothing to display.
					returnResult(false);
				}
			}, 0, R.string.dialog_info_no_images_in_folder, mFolderName);
			return;
		}

		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}

		setAdapter(null, null, fileNames, false);
		mTextViewFolderName.setText(mFolderName);
	}

	/**
	 * Change the action within this activity (display or add).
	 *
	 * @param action the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action != null) {
			mCurrentAction = action;
			setSelectionMode(action == CurrentAction.ADD ? SelectionMode.MULTIPLE : SelectionMode.ONE);
			invalidateOptionsMenu();
		}
	}

	/**
	 * Static helper method to extract the filesAdded flag.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
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
	 * @param filesAdded The flag if files have been added to the list.
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
		outState.putSerializable("currentAction", mCurrentAction);
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
