package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import de.jeisfeld.randomimage.DisplayImageListAdapter.ItemType;
import de.jeisfeld.randomimage.DisplayImageListAdapter.SelectionMode;
import de.jeisfeld.randomimage.MainConfigurationActivity.ListAction;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display and configure the list of images in an image list.
 */
public class ConfigureImageListActivity extends DisplayImageListActivity {
	/**
	 * The resource key for the name of the image list to be displayed.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";
	/**
	 * The resource key for the a tracking String.
	 */
	public static final String STRING_EXTRA_TRACKING = "de.jeisfeld.randomimage.TRACKING";

	/**
	 * Request code for getting images from gallery.
	 */
	private static final int REQUEST_CODE_GALLERY = 100;

	/**
	 * The names of the nested lists to be displayed.
	 */
	private ArrayList<String> mNestedListNames;

	/**
	 * The names of the folders to be displayed.
	 */
	private ArrayList<String> mFolderNames;

	/**
	 * The names of the files to be displayed.
	 */
	private ArrayList<String> mFileNames;

	/**
	 * The name of the image list to be displayed.
	 */
	private String mListName;

	protected final String getListName() {
		return mListName;
	}

	/**
	 * The TextView displaying the list name.
	 */
	private TextView mTextViewListName;

	/**
	 * The TextView displaying a message.
	 */
	private TextView mTextViewMessage;

	/**
	 * The current action within this activity.
	 */
	private CurrentAction mCurrentAction = CurrentAction.DISPLAY;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param listName     the image list which should be displayed first.
	 * @param context      The context creating the intent.
	 * @param trackingName A String indicating the starter of the activity.
	 */
	public static final void startActivity(final Activity context, final String listName, final String trackingName) {
		context.startActivity(createIntent(context, listName, trackingName));
	}

	/**
	 * Static helper method to create an intent for the activity.
	 *
	 * @param listName     the image list which should be displayed first.
	 * @param context      The context creating the intent.
	 * @param trackingName A String indicating the starter of the activity.
	 * @return the intent.
	 */
	public static final Intent createIntent(final Context context, final String listName, final String trackingName) {
		Intent intent = new Intent(context, ConfigureImageListActivity.class);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		if (trackingName != null) {
			intent.putExtra(STRING_EXTRA_TRACKING, trackingName);
		}
		return intent;
	}

	@Override
	protected final int getLayoutId() {
		return R.layout.activity_configure_image_list;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		mTextViewListName = (TextView) findViewById(R.id.textViewTitle);
		mTextViewMessage = (TextView) findViewById(R.id.textViewMessage);

		String trackingName = getIntent().getStringExtra(STRING_EXTRA_TRACKING);
		if (trackingName != null) {
			TrackingUtil.sendEvent(Category.EVENT_SETUP, "Configure List", trackingName);
		}

		if (savedInstanceState != null) {
			mListName = savedInstanceState.getString("listName");
		}
		else {
			mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		}

		if (mListName == null) {
			mListName = ImageRegistry.getCurrentListName();
		}

		if (mListName == null) {
			// On first startup need to create default list.
			ImageRegistry.getCurrentImageListRefreshed(true);
			mListName = ImageRegistry.getCurrentListName();
		}

		// This step initializes the adapter.
		switchToImageList(mListName, CreationStyle.NONE);

		if (savedInstanceState != null) {
			mCurrentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
			changeAction(mCurrentAction);
		}

		DialogUtil.displayInitialSearchForImageFoldersIfRequired(this);

		if (savedInstanceState == null && isEmpty(mFileNames) && isEmpty(mFolderNames) && isEmpty(mNestedListNames)) {
			DialogUtil.displayFirstUseMessageIfRequired(this);
		}

		PreferenceUtil.incrementCounter(R.string.key_statistics_countdisplayall);
	}

	@Override
	public final void onItemClick(final ItemType itemType, final String name) {
		switch (itemType) {
		case LIST:
			switchToImageList(name, CreationStyle.NONE);
			break;
		case FOLDER:
			DisplayImagesFromFolderActivity.startActivity(this, name, mListName, false);
			break;
		case FILE:
		default:
			startActivityForResult(DisplayRandomImageActivity.createIntent(this, null, name, true, null, null),
					DisplayRandomImageActivity.REQUEST_CODE);
			break;
		}
	}

	@Override
	public final void onItemLongClick(final ItemType itemType, final String name) {
		switch (itemType) {
		case LIST:
			DisplayListInfoActivity.startActivity(this, name, mListName);
			break;
		case FOLDER:
		case FILE:
		default:
			DisplayImageDetailsActivity.startActivity(this, name, mListName, true, "Configure image list");
			break;
		}
	}

	/**
	 * Fill the view with the current list of images.
	 */
	private void fillListOfImages() {
		ImageList imageList = ImageRegistry.getCurrentImageList(true);
		mFileNames = imageList.getFileNames();
		mFolderNames = imageList.getFolderNames();
		mNestedListNames = imageList.getNestedListNames();
		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}
		setAdapter(mNestedListNames, mFolderNames, mFileNames, false);
		mTextViewListName.setText(mListName);
		configureMissingImagesButton(imageList);
		invalidateOptionsMenu();
		displayEmptyListMessageIfRequired();
	}

	/**
	 * Display the message on empty list if required.
	 */
	private void displayEmptyListMessageIfRequired() {
		if (isEmpty(mFileNames) && isEmpty(mFolderNames) && isEmpty(mNestedListNames)) {
			mTextViewMessage.setVisibility(View.VISIBLE);
			mTextViewMessage.setText(R.string.text_info_empty_list);
		}
		else {
			mTextViewMessage.setVisibility(View.GONE);
		}
	}

	/**
	 * Configure the button to show missing images.
	 *
	 * @param imageList the image list.
	 */
	private void configureMissingImagesButton(final ImageList imageList) {
		Button missingImagesButton = (Button) findViewById(R.id.buttonShowMissing);
		boolean hasMissingImages = imageList.getMissingPathNames().size() > 0;
		missingImagesButton.setVisibility(hasMissingImages ? View.VISIBLE : View.GONE);
		if (hasMissingImages) {
			missingImagesButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					StringBuilder missingImagesString = new StringBuilder();
					for (String pathName : imageList.getMissingPathNames()) {
						missingImagesString.append(pathName).append("\n");
					}

					DialogUtil.displayConfirmationMessage(ConfigureImageListActivity.this, new ConfirmDialogListener() {
								@Override
								public void onDialogPositiveClick(final DialogFragment dialog) {
									imageList.cleanupMissingFiles();
									fillListOfImages();
								}

								@Override
								public void onDialogNegativeClick(final DialogFragment dialog) {
									// do nothing
								}
							}, R.string.title_dialog_missing_images, R.string.menu_remove_from_list, R.string.dialog_confirmation_missing_images,
							mListName, missingImagesString);
				}
			});
		}
	}

	/**
	 * Remove any DisplayRandomImageActivity that may be on top of this activity.
	 */
	protected final void bringOnTop() {
		Intent reorderIntent = new Intent(this, DisplayRandomImageActivity.class);
		reorderIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		Intent displayAllIntent = new Intent(this, ConfigureImageListActivity.class);
		displayAllIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivities(new Intent[] {reorderIntent, displayAllIntent});
	}

	@Override
	public final void onBackPressed() {
		if (mCurrentAction == CurrentAction.REMOVE) {
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
			getMenuInflater().inflate(R.menu.configure_image_list, menu);

			if (isEmpty(mFileNames) && isEmpty(mFolderNames) && isEmpty(mNestedListNames)) {
				MenuItem menuItemRemove = menu.findItem(R.id.action_select_images_for_removal);
				menuItemRemove.setEnabled(false);
				menuItemRemove.setIcon(ImageUtil.getTransparentIcon(R.drawable.ic_action_minus));
			}

			int folderSelectionMode = PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_folder_selection_mechanism,
					R.string.pref_default_folder_selection_mechanism);

			if (folderSelectionMode != 0) {
				menu.findItem(R.id.action_include_other_list).setVisible(true);
			}

			return true;
		case REMOVE:
			getMenuInflater().inflate(R.menu.delete_from_list, menu);
			return true;
		default:
			return false;
		}
	}

	/**
	 * Get information if a list is null or empty.
	 *
	 * @param list the list.
	 * @return true if null or empty.
	 */
	private static boolean isEmpty(final ArrayList<String> list) {
		return list == null || list.size() == 0;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home || id == R.id.action_home) {
			MainConfigurationActivity.startActivity(this);
			finish();
			return true;
		}

		switch (mCurrentAction) {
		case DISPLAY:
			return onOptionsItemSelectedDisplay(id);
		case REMOVE:
			return onOptionsItemSelectedDelete(id);
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
	private boolean onOptionsItemSelectedDisplay(final int menuId) {
		if (menuId == R.id.action_select_images_for_removal) {
			changeAction(CurrentAction.REMOVE);
			return true;
		}
		else if (menuId == R.id.action_add_images) {
			addImagesToList();
			return true;
		}
		else if (menuId == R.id.action_include_other_list) {
			includeOtherList();
			return true;
		}
		else if (menuId == R.id.action_settings) {
			SettingsActivity.startActivity(this);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Handler for options selected while in delete mode.
	 *
	 * @param menuId The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemSelectedDelete(final int menuId) {
		if (menuId == R.id.action_remove_images) {
			final ImageList imageList = ImageRegistry.getCurrentImageList(true);
			final ArrayList<String> imagesToBeRemoved = getAdapter().getSelectedFiles();
			final ArrayList<String> foldersToBeRemoved = getAdapter().getSelectedFolders();
			final ArrayList<String> nestedListsToBeRemoved = getAdapter().getSelectedLists();
			String imageFolderString =
					DialogUtil.createFileFolderMessageString(nestedListsToBeRemoved, foldersToBeRemoved,
							imagesToBeRemoved);
			if (nestedListsToBeRemoved.size() > 0 || imagesToBeRemoved.size() > 0 || foldersToBeRemoved.size() > 0) {

				DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog) {
								ArrayList<String> removedNestedLists = new ArrayList<>();
								ArrayList<String> removedFolders = new ArrayList<>();
								ArrayList<String> removedImages = new ArrayList<>();
								for (String nestedListName : nestedListsToBeRemoved) {
									boolean isRemoved = imageList.removeNestedList(nestedListName);
									if (isRemoved) {
										removedNestedLists.add(nestedListName);
									}
								}
								for (String removeFolderName : foldersToBeRemoved) {
									boolean isRemoved = imageList.removeFolder(removeFolderName);
									if (isRemoved) {
										removedFolders.add(removeFolderName);
									}
								}
								for (String fileName : imagesToBeRemoved) {
									boolean isRemoved = imageList.removeFile(fileName);
									if (isRemoved) {
										removedImages.add(fileName);
									}
								}

								String fileFolderMessageString =
										DialogUtil.createFileFolderMessageString(removedNestedLists, removedFolders, removedImages);
								int totalRemovedCount = removedNestedLists.size() + removedFolders.size() + removedImages.size();
								int messageId;
								if (totalRemovedCount == 0) {
									messageId = R.string.toast_removed_no_image;
								}
								else if (totalRemovedCount == 1) {
									messageId = R.string.toast_removed_single;
								}
								else {
									messageId = R.string.toast_removed_multiple;
								}

								DialogUtil.displayToast(ConfigureImageListActivity.this, messageId, fileFolderMessageString);
								NotificationUtil.notifyUpdatedList(ConfigureImageListActivity.this,
										mListName, true, removedNestedLists, removedFolders, removedImages);

								if (totalRemovedCount > 0) {
									imageList.update(true);
									fillListOfImages();
								}
								changeAction(CurrentAction.DISPLAY);
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing.
							}
						}, null, R.string.button_remove, R.string.dialog_confirmation_remove, mListName,
						imageFolderString);

			}
			else {
				DialogUtil.displayToast(ConfigureImageListActivity.this, R.string.toast_removed_no_image);
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
	 * Add images to the image list.
	 */
	private void addImagesToList() {
		int folderSelectionMode = PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_folder_selection_mechanism,
				R.string.pref_default_folder_selection_mechanism);

		switch (folderSelectionMode) {
		case 0:
			SelectImageFolderActivity.startActivity(this, mListName);
			break;
		case 1:
			SelectDirectoryActivity.startActivity(this, mListName);
			break;
		case 2:
			// via Gallery
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(intent, REQUEST_CODE_GALLERY);
			break;
		default:
			break;
		}
	}

	/**
	 * Switch to the image list with the given name.
	 *
	 * @param name          The name of the target image list.
	 * @param creationStyle Flag indicating if the list should be created if non-existing.
	 * @return true if successful.
	 */
	protected final boolean switchToImageList(final String name, final CreationStyle creationStyle) {
		mListName = name;
		boolean success = ImageRegistry.switchToImageList(name, creationStyle, true);
		if (success) {
			fillListOfImages();
		}
		return success;
	}

	/**
	 * Select another list to include in the current list.
	 */
	private void includeOtherList() {
		ArrayList<String> listNames = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
		listNames.remove(mListName);
		listNames.removeAll(mNestedListNames);

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
								ImageList imageList = ImageRegistry.getCurrentImageList(true);
								boolean success = imageList.addNestedList(text);
								if (success) {
									String addedItemString = DialogUtil.createFileFolderMessageString(Collections.singletonList(text), null, null);
									DialogUtil.displayToast(ConfigureImageListActivity.this, R.string.toast_added_single, addedItemString);
									NotificationUtil.notifyUpdatedList(ConfigureImageListActivity.this, mListName, false,
											Collections.singletonList(text), null, null);
									imageList.update(true);
									fillListOfImages();
								}
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_select_list_name, listNames,
						R.string.dialog_select_list_for_inclusion);
	}

	/**
	 * Change the action within this activity (display or remove).
	 *
	 * @param action the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action != null) {
			mCurrentAction = action;

			switch (action) {
			case DISPLAY:
				setTitle(R.string.title_activity_configure_image_list);
				displayEmptyListMessageIfRequired();
				break;
			case REMOVE:
				setTitle(R.string.title_activity_remove_images);
				mTextViewMessage.setVisibility(View.VISIBLE);
				mTextViewMessage.setText(R.string.text_info_select_images_for_remove);
				break;
			default:
				break;
			}

			setSelectionMode(action == CurrentAction.REMOVE ? SelectionMode.MULTIPLE : SelectionMode.ONE);
			invalidateOptionsMenu();
		}
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("currentAction", mCurrentAction);
		outState.putSerializable("listName", mListName);
	}

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		boolean needsRefresh = false;

		switch (requestCode) {
		case DisplayRandomImageActivity.REQUEST_CODE:
			needsRefresh = DisplayRandomImageActivity.getResult(resultCode, data);
			break;
		case SettingsActivity.REQUEST_CODE:
			boolean boughtPremium = SettingsActivity.getResultBoughtPremium(resultCode, data);
			if (boughtPremium) {
				invalidateOptionsMenu();
			}
			break;
		case DisplayImageDetailsActivity.REQUEST_CODE:
			needsRefresh = DisplayImageDetailsActivity.getResultFileRemoved(resultCode, data);
			if (needsRefresh) {
				changeAction(CurrentAction.DISPLAY);
			}
			break;
		case DisplayListInfoActivity.REQUEST_CODE:
			needsRefresh = DisplayListInfoActivity.getResultListAction(resultCode, data) == ListAction.REFRESH;
			if (needsRefresh) {
				changeAction(CurrentAction.DISPLAY);
			}
			break;
		case SelectDirectoryActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				needsRefresh = SelectDirectoryActivity.getUpdatedFlag(resultCode, data);
			}
			break;
		case SelectImageFolderActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				needsRefresh = SelectImageFolderActivity.getUpdatedFlag(resultCode, data);
			}
			break;
		case REQUEST_CODE_GALLERY:
			if (resultCode == RESULT_OK) {
				Uri selectedImageUri = data.getData();
				String fileName = MediaStoreUtil.getRealPathFromUri(selectedImageUri);

				if (fileName == null) {
					DialogUtil.displayToast(this, R.string.toast_error_select_folder);
					return;
				}

				String folderName = new File(fileName).getParent();
				if (folderName != null) {
					DisplayImagesFromFolderActivity.startActivity(this, folderName, mListName, true);
				}
			}
			break;
		case DisplayImagesFromFolderActivity.REQUEST_CODE:
			needsRefresh = DisplayImagesFromFolderActivity.getResultFilesAdded(resultCode, data);
			break;
		default:
			break;
		}
		if (needsRefresh) {
			fillListOfImages();
		}
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
		 * Delete photos.
		 */
		REMOVE
	}

}
