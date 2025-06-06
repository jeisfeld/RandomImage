package de.jeisfeld.randomimage;

import android.app.Activity;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
	 * A list of all image folders to be displayed.
	 */
	private final ArrayList<String> mAllImageFolders = ImageUtil.getAllStoredImageFolders();

	/**
	 * A list of all image lists to be displayed.
	 */
	private ArrayList<String> mAllImageLists = null;

	/**
	 * The current action within this activity.
	 */
	private CurrentAction mCurrentAction = CurrentAction.DISPLAY;

	/**
	 * The name of the image list to which folders should be added.
	 */
	private String mListName;

	/**
	 * The thread queue filling the list of folders.
	 */
	private final List<Thread> mFillListThreads = new ArrayList<>();

	/**
	 * Static helper method to start the activity to display the contents of a folder.
	 *
	 * @param activity The activity starting this activity.
	 * @param listName the triggering image list to which folders should be added.
	 */
	public static void startActivity(final Activity activity, final String listName) {
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

		mEditTextFilter = findViewById(R.id.editTextFilterString);
		String lastFilterValue = PreferenceUtil.getSharedPreferenceString(R.string.key_folder_selection_filter);
		if (lastFilterValue != null) {
			mEditTextFilter.setText(lastFilterValue);
		}
		if (savedInstanceState != null) {
			mCurrentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
			mAllImageLists = savedInstanceState.getStringArrayList("allImageLists");
			List<String> allImageFolders = savedInstanceState.getStringArrayList("allImageFolders");
			if (allImageFolders != null) {
				mAllImageFolders.clear();
				mAllImageFolders.addAll(allImageFolders);
			}
		}

		// This step initializes the adapter.
		fillListOfFoldersAsync();

		mEditTextFilter.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
				fillListOfFoldersAsync();
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
			if (name.endsWith(ImageUtil.RECURSIVE_SUFFIX)) {
				addRecursiveFolder(name);
			}
			else {
				DisplayImagesFromFolderActivity.startActivity(this, name, mListName, true, AppWidgetManager.INVALID_APPWIDGET_ID);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public final void onItemLongClick(final ItemType itemType, final String name) {
		switch (itemType) {
		case FOLDER:
			DisplayImageDetailsActivity.startActivity(this, name, null, null, null, true);
			break;
		default:
			break;
		}
	}

	/**
	 * Fill the view with the list of all image folders, via background thread.
	 */
	private void fillListOfFoldersAsync() {
		Thread fillListThread = new Thread() {
			@Override
			public void run() {
				fillListOfFolders();
				synchronized (mFillListThreads) {
					mFillListThreads.remove(Thread.currentThread());
					if (!mFillListThreads.isEmpty()) {
						mFillListThreads.get(0).start();
					}
				}
			}
		};

		synchronized (mFillListThreads) {
			if (mFillListThreads.size() > 1) {
				mFillListThreads.remove(1);
			}
			mFillListThreads.add(fillListThread);
			if (mFillListThreads.size() == 1) {
				fillListThread.start();
			}
			else {
				mFillListThreads.get(0).interrupt();
			}
		}
	}


	/**
	 * Fill the view with the list of all image folders.
	 */
	private void fillListOfFolders() {
		final boolean firstStart = mAllImageLists == null;
		final List<String> selectedLists;
		final List<String> selectedFolders;
		if (getAdapter() != null) {
			selectedLists = getAdapter().getSelectedLists();
			selectedFolders = getAdapter().getSelectedFolders();
		}
		else {
			selectedLists = null;
			selectedFolders = null;
		}

		if (firstStart) {
			mAllImageLists = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
			mAllImageLists.remove(mListName);
		}

		String filterString = mEditTextFilter.getText().toString();

		final List<String> filteredImageFolders = new ArrayList<>();

		synchronized (mAllImageFolders) {
			for (String name : mAllImageFolders) {
				if (matchesFolderFilter(name, filterString)) {
					filteredImageFolders.add(name);
				}
				if (Thread.interrupted()) {
					return;
				}
			}
		}
		final List<String> filteredImageLists = new ArrayList<>();
		for (String name : mAllImageLists) {
			if (matchesListFilter(name, filterString)) {
				filteredImageLists.add(name);
			}
		}

		runOnUiThread(() -> {
			if (getAdapter() != null) {
				getAdapter().cleanupCache();
			}
			setAdapter(filteredImageLists, filteredImageFolders, null, true, selectedLists, selectedFolders, null);
			changeAction(mCurrentAction);

			if (firstStart) {
				parseAllImageFolders();
			}
		});
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
	 * Add the given folder as recursive folder after first querying.
	 *
	 * @param folderName The folder to be added.
	 */
	private void addRecursiveFolder(final String folderName) {
		DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						ImageList imageList = ImageRegistry.getImageListByName(mListName, false);
						boolean success = imageList.addFolder(folderName);
						if (success) {
							String addedItemString = DialogUtil.createFileFolderMessageString(null, Collections.singletonList(folderName), null);
							DialogUtil.displayToast(SelectImageFolderActivity.this, R.string.toast_added_single, addedItemString);
							NotificationUtil.notifyUpdatedList(SelectImageFolderActivity.this, mListName, false,
									null, Collections.singletonList(folderName), null);
							imageList.update(true);
							returnResult(true);
						}
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing.
					}
				}, R.string.title_dialog_add_folder, R.string.button_add_folder, R.string.dialog_confirmation_add_folder_recursively,
				folderName.substring(0, folderName.length() - 2), mListName);
	}

	/**
	 * Parse all image folders and add missing image folders to the adapter.
	 */
	private void parseAllImageFolders() {
		final String hiddenFoldersPattern = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_hidden_folders_pattern);
		final boolean useRegexp = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_use_regex_filter)
				&& hiddenFoldersPattern != null && !hiddenFoldersPattern.isEmpty();

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
								if (!images.isEmpty()) {
									MediaStoreUtil.getThumbnailFromPath(images.get(0));
								}
							}
							PreferenceUtil.setSharedPreferenceLong(R.string.key_last_thumb_creation_time, System.currentTimeMillis());
						}
					}.start();
				}
			}

			@Override
			public void handleImageFolder(final String imageFolder) {
				if (!mAllImageFolders.contains(imageFolder) && (!useRegexp || !imageFolder.matches(hiddenFoldersPattern))) {
					synchronized (mAllImageFolders) {
						mAllImageFolders.add(imageFolder);
					}
					if (matchesFolderFilter(imageFolder, mEditTextFilter.getText().toString())) {
						if (getAdapter() == null) {
							setAdapter(null, Collections.singletonList(imageFolder), null, true);
						}
						else {
							getAdapter().addFolder(imageFolder);
						}
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
	private boolean matchesFolderFilter(final String path, final String filterString) {
		ImageList imageList = ImageRegistry.getCurrentImageList(false);
		return path != null  // BOOLEAN_EXPRESSION_COMPLEXITY
				// Exclude if already contained
				&& !(imageList.getListName().equals(mListName) && imageList.contains(path))
				// Include if there is no filter
				&& (filterString == null || filterString.isEmpty()
				// Include if matches filter
				|| path.toLowerCase(Locale.getDefault()).contains(filterString.toLowerCase(Locale.getDefault()))
				// Include if selected
				|| getAdapter() != null && getAdapter().getSelectedFolders().contains(path));
	}

	/**
	 * Check if a list name matches a filter string.
	 *
	 * @param listName     The list name.
	 * @param filterString The filter string.
	 * @return true if it matches.
	 */
	private boolean matchesListFilter(final String listName, final String filterString) {
		ImageList imageList = ImageRegistry.getCurrentImageList(false);
		return listName != null  // BOOLEAN_EXPRESSION_COMPLEXITY
				// Exclude if already contained
				&& !(imageList.getListName().equals(mListName) && imageList.containsNestedList(listName, false))
				// Include if there is no filter
				&& (filterString == null || filterString.isEmpty()
				// Include if matches filter
				|| listName.toLowerCase(Locale.getDefault()).contains(filterString.toLowerCase(Locale.getDefault()))
				// Include if selected
				|| getAdapter() != null && getAdapter().getSelectedLists().contains(listName));
	}

	/**
	 * Change the action within this activity (display or remove).
	 *
	 * @param action the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action != null) {
			mCurrentAction = action;
			TextView textViewInfo = findViewById(R.id.textViewMessage);

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
	public final void onBackPressed() {
		if (mCurrentAction == CurrentAction.SELECT) {
			changeAction(CurrentAction.DISPLAY);
		}
		else {
			super.onBackPressed();
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
		else if (menuId == R.id.action_cancel) {
			finish();
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
			if (!foldersToBeAdded.isEmpty() || !nestedListsToBeAdded.isEmpty()) {
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

				changeAction(CurrentAction.DISPLAY);
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
	public static boolean getUpdatedFlag(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res != null && res.getBoolean(STRING_RESULT_UPDATED, false);
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
		//finish();
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("currentAction", mCurrentAction);
		outState.putStringArrayList("allImageLists", mAllImageLists);
		outState.putStringArrayList("allImageFolders", mAllImageFolders);
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
