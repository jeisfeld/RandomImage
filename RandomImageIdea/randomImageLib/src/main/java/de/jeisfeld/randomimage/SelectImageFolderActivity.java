package de.jeisfeld.randomimage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.ItemType;
import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.ImageUtil.OnImageFoldersFoundListener;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.NotificationUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.view.ThumbImageView;
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
	 * The resource key for the flag indicating that the list was updated.
	 */
	private static final String STRING_RESULT_UPDATED = "de.jeisfeld.randomimage.UPDATED";

	/**
	 * The resource key for the flag to trigger SelectDirectoryActivity.
	 */
	private static final String STRING_RESULT_TRIGGER_SELECT_DIRECTORY_ACTIVITY = "de.jeisfeld.randomimage.TRIGGER_SELECT_DIRECTORY_ACTIVITY";

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
	 * The current action within this activity.
	 */
	private CurrentAction mCurrentAction = CurrentAction.DISPLAY;

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
	}

	@Override
	public final void onItemClick(final ItemType itemType, final String name) {
		// itemType is always folder.
		DisplayImagesFromFolderActivity.startActivity(this, name, true);
	}

	@Override
	public final void onItemLongClick(final ItemType itemType, final String name) {
		// itemType is always folder.
		DisplayImageDetailsActivity.startActivity(this, name, null, true);
	}

	/**
	 * Fill the view with the list of all image folders.
	 */
	private void fillListOfFolders() {
		boolean firstStart = mAllImageFolders == null;
		List<String> selectedFolders = null;
		if (getAdapter() != null) {
			selectedFolders = getAdapter().getSelectedFolders();
			getAdapter().cleanupCache();
		}

		if (firstStart) {
			mAllImageFolders = PreferenceUtil.getSharedPreferenceStringList(R.string.key_all_image_folders);
		}

		mFilteredImageFolders.clear();
		String filterString = mEditTextFilter.getText().toString();

		if (filterString.length() > 0) {
			mFilteredImageFolders = new ArrayList<>();
			for (String name : mAllImageFolders) {
				if (matchesFilter(name, filterString)) {
					mFilteredImageFolders.add(name);
				}
			}
		}
		else {
			mFilteredImageFolders = new ArrayList<>(mAllImageFolders);
		}

		setAdapter(null, mFilteredImageFolders, null, true);
		changeAction(mCurrentAction);
		if (selectedFolders != null) {
			getAdapter().setSelectedFolders(selectedFolders);
		}

		if (firstStart) {
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
	}

	/**
	 * Check if a file path matches a filter string.
	 *
	 * @param path         The file path.
	 * @param filterString The filter string.
	 * @return true if it matches.
	 */
	private boolean matchesFilter(final String path, final String filterString) {
		return filterString == null || filterString.length() == 0 // BOOLEAN_EXPRESSION_COMPLEXITY
				|| path.toLowerCase(Locale.getDefault()).contains(filterString.toLowerCase(Locale.getDefault()))
				|| (getAdapter() != null && getAdapter().getSelectedFolders().contains(path));
	}

	/**
	 * Change the action within this activity (display or remove).
	 *
	 * @param action the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action != null) {
			mCurrentAction = action;
			setSelectionMode(action == CurrentAction.SELECT ? SelectionMode.MULTIPLE_ADD : SelectionMode.ONE);
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
			triggerSelectDirectoryActivity();
			finish();
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
			final ArrayList<String> foldersToBeAdded = getAdapter().getSelectedFolders();
			if (foldersToBeAdded.size() > 0) {
				ImageList imageList = ImageRegistry.getCurrentImageList(false);

				ArrayList<String> addedFolders = new ArrayList<>();
				for (String folderName : foldersToBeAdded) {
					boolean isAdded = imageList.addFolder(folderName);
					if (isAdded) {
						addedFolders.add(folderName);
					}
				}

				String folderMessageString = DialogUtil.createFileFolderMessageString(null, addedFolders, null);
				int messageId;
				if (addedFolders.size() == 0) {
					messageId = R.string.toast_added_folders_none;
				}
				else if (addedFolders.size() == 1) {
					messageId = R.string.toast_added_single;
				}
				else {
					messageId = R.string.toast_added_multiple;
				}

				DialogUtil.displayToast(SelectImageFolderActivity.this, messageId, folderMessageString);
				if (addedFolders.size() > 0) {
					NotificationUtil.displayNotification(SelectImageFolderActivity.this, ImageRegistry.getCurrentListName(),
							NotificationUtil.ID_UPDATED_LIST, R.string.title_notification_updated_list, messageId, folderMessageString);
				}

				returnResult(addedFolders.size() > 0);
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
			boolean markingStatus = getAdapter().toggleSelectAll();
			for (int i = 0; i < getGridView().getChildCount(); i++) {
				View imageView = getGridView().getChildAt(i);
				if (imageView instanceof ThumbImageView) {
					((ThumbImageView) imageView).setMarked(markingStatus);
				}
			}
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
				returnResult(isUpdated);
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
