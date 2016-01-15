package de.jeisfeld.randomimage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import de.jeisfeld.randomimage.DisplayImageListAdapter.ItemType;
import de.jeisfeld.randomimage.DisplayImageListAdapter.SelectionMode;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.ImageUtil.OnImageFoldersFoundListener;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to add images to the list via display of all image folders on the device.
 */
public class SelectImageFolderActivity extends DisplayImageListActivity {
	/**
	 * The resource key for the name of the image list to be displayed.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";

	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 7;

	/**
	 * The resource key for the flag indicating that the list was updated.
	 */
	private static final String STRING_RESULT_UPDATED = "de.jeisfeld.randomimage.UPDATED";

	/**
	 * Recreate all thumbs every 12 weeks.
	 */
	private static final long THUMB_CREATION_FREQUENCY = DateUtils.WEEK_IN_MILLIS * 12;

	/**
	 * A filter for the displayed folders.
	 */
	private EditText mEditTextFilter;

	/**
	 * A filtered list of image folders to be displayed.
	 */
	private List<String> mFilteredImageFolders = new ArrayList<>();

	/**
	 * A list of all image folders to be displayed.
	 */
	private List<String> mAllImageFolders = null;

	/**
	 * A filtered list of image lists to be displayed.
	 */
	private List<String> mFilteredImageLists = new ArrayList<>();

	/**
	 * A list of all image lists to be displayed.
	 */
	private List<String> mAllImageLists = null;

	/**
	 * The current action within this activity.
	 */
	private CurrentAction mCurrentAction = CurrentAction.DISPLAY;

	/**
	 * The name of the image list to which folders should be added.
	 */
	private String mListName;

	/**
	 * Static helper method to start the activity to display the contents of a folder.
	 *
	 * @param activity The activity starting this activity.
	 * @param listName the triggering image list to which folders should be added.
	 */
	public static final void startActivity(final Activity activity, final String listName) {
		Intent intent = new Intent(activity, SelectImageFolderActivity.class);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final int getLayoutId() {
		return R.layout.activity_select_image_folder;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);

		mEditTextFilter = (EditText) findViewById(R.id.editTextFilterString);
		String lastFilterValue = PreferenceUtil.getSharedPreferenceString(R.string.key_folder_selection_filter);
		if (lastFilterValue != null) {
			mEditTextFilter.setText(lastFilterValue);
		}
		if (savedInstanceState != null) {
			mCurrentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
		}

		// This step initializes the adapter.
		fillListOfFolders();

		mEditTextFilter.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
				fillListOfFolders();
				PreferenceUtil.setSharedPreferenceString(R.string.key_folder_selection_filter, s.toString());
			}

			@Override
			public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
				// do nothing
			}

			@Override
			public void afterTextChanged(final Editable s) {
				// do nothing
			}
		});

		if (savedInstanceState == null) {
			DialogUtil.displayInfo(this, null, R.string.key_hint_select_folder, R.string.dialog_hint_select_folder);
		}
	}

	@Override
	public final void onItemClick(final ItemType itemType, final String name) {
		switch (itemType) {
		case LIST:
			addNestedList(name);
			break;
		case FOLDER:
			DisplayImagesFromFolderActivity.startActivity(this, name, mListName, true);
			break;
		default:
			break;
		}
	}

	@Override
	public final void onItemLongClick(final ItemType itemType, final String name) {
		switch (itemType) {
		case FOLDER:
			DisplayImageDetailsActivity.startActivity(this, name, null, true);
			break;
		default:
			break;
		}
	}

	/**
	 * Fill the view with the list of all image folders.
	 */
	private void fillListOfFolders() {
		boolean firstStart = mAllImageFolders == null;
		List<String> selectedLists = null;
		List<String> selectedFolders = null;
		if (getAdapter() != null) {
			selectedLists = getAdapter().getSelectedLists();
			selectedFolders = getAdapter().getSelectedFolders();
			getAdapter().cleanupCache();
		}

		if (firstStart) {
			mAllImageFolders = PreferenceUtil.getSharedPreferenceStringList(R.string.key_all_image_folders);
			mAllImageLists = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
			mAllImageLists.remove(mListName);
		}

		mFilteredImageFolders.clear();
		mFilteredImageLists.clear();
		String filterString = mEditTextFilter.getText().toString();

		mFilteredImageFolders = new ArrayList<>();
		for (String name : mAllImageFolders) {
			if (matchesFilter(name, filterString)) {
				mFilteredImageFolders.add(name);
			}
		}
		mFilteredImageLists = new ArrayList<>();
		for (String name : mAllImageLists) {
			if (matchesFilter(name, filterString)) {
				mFilteredImageLists.add(name);
			}
		}

		setAdapter(mFilteredImageLists, mFilteredImageFolders, null, true);
		changeAction(mCurrentAction);
		if (selectedFolders != null) {
			getAdapter().setSelectedFolders(selectedFolders);
		}
		if (selectedLists != null) {
			getAdapter().setSelectedLists(selectedLists);
		}

		if (firstStart) {
			parseAllImageFolders();
		}
	}

	/**
	 * Add the given list as nested list after first querying.
	 *
	 * @param listName The list to be added.
	 */
	private void addNestedList(final String listName) {
		DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						ImageList imageList = ImageRegistry.getImageListByName(mListName, false);
						boolean success = imageList.addNestedList(listName);
						if (success) {
							String addedItemString = DialogUtil.createFileFolderMessageString(Collections.singletonList(listName), null, null);
							DialogUtil.displayToast(SelectImageFolderActivity.this, R.string.toast_added_single, addedItemString);
							NotificationUtil.notifyUpdatedList(SelectImageFolderActivity.this, mListName, false,
									Collections.singletonList(listName), null, null);
							imageList.update(true);
							returnResult(true);
						}
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing.
					}
				}, R.string.title_dialog_add_list, R.string.button_add_nested_list, R.string.dialog_confirmation_add_nested_list,
				listName, mListName);
	}


	/**
	 * Parse all image folders and add missing image folders to the adapter.
	 */
	private void parseAllImageFolders() {
		ImageUtil.getAllImageFolders(new OnImageFoldersFoundListener() {

			@Override
			public void handleImageFolders(final ArrayList<String> imageFolders) {
				// Every now and then, trigger thumbnail creation.
				long lastThumbCreationTime = PreferenceUtil.getSharedPreferenceLong(R.string.key_last_thumb_creation_time, -1);
				if (System.currentTimeMillis() > lastThumbCreationTime + THUMB_CREATION_FREQUENCY) {
					new Thread() {
						@Override
						public void run() {
							for (String imageFolder : imageFolders) {
								List<String> images = ImageUtil.getImagesInFolder(imageFolder);
								if (images != null && images.size() > 0) {
									MediaStoreUtil.getThumbnailFromPath(images.get(0), MediaStoreUtil.MINI_THUMB_SIZE);
								}
							}
							PreferenceUtil.setSharedPreferenceLong(R.string.key_last_thumb_creation_time, System.currentTimeMillis());
						}
					}.start();
				}
			}

			@Override
			public void handleImageFolder(final String imageFolder) {
				if (!mAllImageFolders.contains(imageFolder)) {
					mAllImageFolders.add(imageFolder);
					if (matchesFilter(imageFolder, mEditTextFilter.getText().toString())) {
						mFilteredImageFolders.add(imageFolder);
					}
				}
				if (mFilteredImageFolders.contains(imageFolder)) {
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
			}
		});
	}

	/**
	 * Check if a file path matches a filter string.
	 *
	 * @param path         The file path.
	 * @param filterString The filter string.
	 * @return true if it matches.
	 */
	private boolean matchesFilter(final String path, final String filterString) {
		String hiddenFoldersPattern = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_hidden_folders_pattern);
		return path != null  // BOOLEAN_EXPRESSION_COMPLEXITY
				// Exclude if matches regexp filter
				&& (hiddenFoldersPattern == null || hiddenFoldersPattern.length() == 0 || !path.matches(hiddenFoldersPattern))
				// Include if there is no filter
				&& (filterString == null || filterString.length() == 0
				// Include if matches filter
				|| path.toLowerCase(Locale.getDefault()).contains(filterString.toLowerCase(Locale.getDefault()))
				// Include if selected
				|| (getAdapter() != null && (getAdapter().getSelectedFolders().contains(path) || getAdapter().getSelectedLists().contains(path))));
	}

	/**
	 * Change the action within this activity (display or remove).
	 *
	 * @param action the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action != null) {
			mCurrentAction = action;
			TextView textViewInfo = (TextView) findViewById(R.id.textViewMessage);

			switch (action) {
			case DISPLAY:
				setTitle(R.string.title_activity_add_images);
				textViewInfo.setText(R.string.text_info_select_image_folder_for_add);
				break;
			case SELECT:
				setTitle(R.string.title_activity_add_folders);
				textViewInfo.setText(R.string.text_info_select_image_folders_for_add);
				break;
			default:
				break;
			}

			setSelectionMode(action == CurrentAction.SELECT ? SelectionMode.MULTIPLE : SelectionMode.ONE);
			invalidateOptionsMenu();
		}
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		switch (mCurrentAction) {
		case DISPLAY:
			getMenuInflater().inflate(R.menu.select_image_folder, menu);
			return true;
		case SELECT:
			getMenuInflater().inflate(R.menu.select_image_folders, menu);
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
			return onOptionsItemSelectedDisplay(id);
		case SELECT:
			return onOptionsItemSelectedSelect(id);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Handler for options selected while in display mode.
	 *
	 * @param menuId The selected menu item.
	 * @return true if menu item was consumed.
	 */
	public final boolean onOptionsItemSelectedDisplay(final int menuId) {
		if (menuId == R.id.action_browse_folders) {
			SelectDirectoryActivity.startActivity(this, mListName);
			return true;
		}
		else if (menuId == R.id.action_select_multiple) {
			changeAction(CurrentAction.SELECT);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Handler for options selected while in sekect mode.
	 *
	 * @param menuId The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemSelectedSelect(final int menuId) {
		if (menuId == R.id.action_add_folders) {
			final List<String> nestedListsToBeAdded = getAdapter().getSelectedLists();
			final List<String> foldersToBeAdded = getAdapter().getSelectedFolders();
			if (foldersToBeAdded.size() > 0 || nestedListsToBeAdded.size() > 0) {
				ImageList imageList = ImageRegistry.getImageListByName(mListName, true);

				List<String> addedFolders = new ArrayList<>();
				for (String folderName : foldersToBeAdded) {
					boolean isAdded = imageList.addFolder(folderName);
					if (isAdded) {
						addedFolders.add(folderName);
					}
				}
				List<String> addedLists = new ArrayList<>();
				for (String listName : nestedListsToBeAdded) {
					boolean isAdded = imageList.addNestedList(listName);
					if (isAdded) {
						addedLists.add(listName);
					}
				}

				String folderMessageString = DialogUtil.createFileFolderMessageString(addedLists, addedFolders, null);
				int totalAddedCount = addedLists.size() + addedFolders.size();
				int messageId;
				if (totalAddedCount == 0) {
					messageId = R.string.toast_added_folders_none;
				}
				else if (totalAddedCount == 1) {
					messageId = R.string.toast_added_single;
				}
				else {
					messageId = R.string.toast_added_multiple;
				}

				DialogUtil.displayToast(SelectImageFolderActivity.this, messageId, folderMessageString);
				if (totalAddedCount > 0) {
					imageList.update(true);
					NotificationUtil.notifyUpdatedList(SelectImageFolderActivity.this, mListName, false, addedLists, addedFolders, null);
				}

				returnResult(totalAddedCount > 0);
			}
			else {
				DialogUtil.displayToast(SelectImageFolderActivity.this, R.string.toast_add_no_folder_selected);
				changeAction(CurrentAction.DISPLAY);
			}
			return true;
		}
		else if (menuId == R.id.action_cancel) {
			changeAction(CurrentAction.DISPLAY);
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
	 * Static helper method to extract the selected folder.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the flag indicating that the list was updated
	 */
	public static final boolean getUpdatedFlag(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_UPDATED, false);
		}
		else {
			return false;
		}
	}

	/**
	 * Helper method: Return the flag indicating that the list was updated.
	 *
	 * @param isUpdated The flag indicating that the list was updated.
	 */
	protected final void returnResult(final boolean isUpdated) {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_UPDATED, isUpdated);
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

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case DisplayImagesFromFolderActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				boolean isUpdated = DisplayImagesFromFolderActivity.getResultFilesAdded(resultCode, data);
				if (isUpdated) {
					returnResult(true);
				}
			}
			break;
		case SelectDirectoryActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				boolean isUpdated = SelectDirectoryActivity.getUpdatedFlag(resultCode, data);
				if (isUpdated) {
					returnResult(true);
				}
			}
			break;
		default:
			break;
		}
	}

	/**
	 * The current action in this activity.
	 */
	private enum CurrentAction {
		/**
		 * Just display the folders.
		 */
		DISPLAY,
		/**
		 * Select folders for addition.
		 */
		SELECT
	}
}
