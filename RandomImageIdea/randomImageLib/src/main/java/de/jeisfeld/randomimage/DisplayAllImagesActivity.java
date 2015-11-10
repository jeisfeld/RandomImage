package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.AuthorizationHelper;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.view.ThumbImageView;
import de.jeisfeld.randomimage.widgets.GenericWidget;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display the list of images configured for this app.
 */
public class DisplayAllImagesActivity extends DisplayImageListActivity {
	/**
	 * The resource key for the name of the image list to be displayed.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";

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
	 * The current action within this activity.
	 */
	private CurrentAction mCurrentAction = CurrentAction.DISPLAY;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param listName the image list which should be displayed first.
	 * @param activity The activity starting this activity.
	 */
	public static final void startActivity(final Activity activity, final String listName) {
		Intent intent = new Intent(activity, DisplayAllImagesActivity.class);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		activity.startActivity(intent);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mListName = savedInstanceState.getString("listName");
		}
		else {
			mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		}

		if (mListName == null) {
			mListName = ImageRegistry.getCurrentListName();
		}

		// This step initializes the adapter.
		switchToImageList(mListName, CreationStyle.NONE);

		if (savedInstanceState != null) {
			mCurrentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
			changeAction(mCurrentAction);
		}

		if (isEmpty(mFileNames) && isEmpty(mFolderNames) && isEmpty(mNestedListNames)) {
			DialogUtil.displayInfo(this, null, R.string.key_info_new_list, R.string.dialog_info_new_list);
		}

		PreferenceUtil.incrementCounter(R.string.key_statistics_countdisplayall);
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
		if (getAdapter() != null) {
			getAdapter().cleanupCache();
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
		setAdapter(mNestedListNames, mFolderNames, mFileNames);
		setTitle(mListName);
		invalidateOptionsMenu();
	}

	/**
	 * Remove any DisplayRandomImageActivity that may be on top of this activity.
	 */
	protected final void bringOnTop() {
		Intent reorderIntent = new Intent(this, DisplayRandomImageActivity.class);
		reorderIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		Intent displayAllIntent = new Intent(this, DisplayAllImagesActivity.class);
		displayAllIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivities(new Intent[] {reorderIntent, displayAllIntent});
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		switch (mCurrentAction) {
		case DISPLAY:
			getMenuInflater().inflate(R.menu.display_image_list, menu);

			if (ImageRegistry.getImageListNames().size() < 2) {
				menu.findItem(R.id.action_switch_list).setEnabled(false);
				menu.findItem(R.id.action_delete_list).setEnabled(false);
			}
			if (isEmpty(mFileNames) && isEmpty(mFolderNames) && isEmpty(mNestedListNames)) {
				MenuItem menuItemRemove = menu.findItem(R.id.action_select_images_for_removal);
				menuItemRemove.setEnabled(false);
				Drawable icon =
						new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(),
								R.drawable.ic_action_minus));
				icon.setAlpha(128); // MAGIC_NUMBER
				menuItemRemove.setIcon(icon);
			}

			if (AuthorizationHelper.hasPremium()) {
				menu.findItem(R.id.action_need_premium).setVisible(false);
			}
			else {
				menu.findItem(R.id.action_manage_lists).setVisible(false);
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
		if (menuId == R.id.action_need_premium) {
			DialogUtil.displayInfo(this, new MessageDialogListener() {
				/**
				 * The serial version id.
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void onDialogFinished() {
					SettingsActivity.startActivity(DisplayAllImagesActivity.this);
				}
			}, 0, R.string.dialog_info_need_premium);
			return true;
		}
		else if (menuId == R.id.action_select_images_for_removal) {
			changeAction(CurrentAction.REMOVE);
			DialogUtil.displayInfo(this, null, R.string.key_info_delete_images, R.string.dialog_info_delete_images);
			return true;
		}
		else if (menuId == R.id.action_add_images) {
			addImagesToList();
			return true;
		}
		else if (menuId == R.id.action_backup_list) {
			PreferenceUtil.incrementCounter(R.string.key_statistics_countbackup);
			backupImageList();
			return true;
		}
		else if (menuId == R.id.action_restore_list) {
			PreferenceUtil.incrementCounter(R.string.key_statistics_countrestore);
			restoreImageList();
			return true;
		}
		else if (menuId == R.id.action_rename_list) {
			renameImageList();
			return true;
		}
		else if (menuId == R.id.action_clone_list) {
			PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatelist);
			createNewImageList(CreationStyle.CLONE_CURRENT);
			return true;
		}
		else if (menuId == R.id.action_create_list) {
			PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatelist);
			createNewImageList(CreationStyle.CREATE_EMPTY);
			return true;
		}
		else if (menuId == R.id.action_switch_list) {
			switchImageList();
			return true;
		}
		else if (menuId == R.id.action_delete_list) {
			deleteImageList();
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
			final ArrayList<String> nestedListsToBeRemoved = getAdapter().getSelectedNestedLists();
			String imageFolderString =
					DialogUtil.createFileFolderMessageString(nestedListsToBeRemoved, foldersToBeRemoved,
							imagesToBeRemoved);
			if (nestedListsToBeRemoved.size() > 0 || imagesToBeRemoved.size() > 0 || foldersToBeRemoved.size() > 0) {

				DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
							/**
							 * The serial version id.
							 */
							private static final long serialVersionUID = 1L;

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

								DialogUtil.displayToast(DisplayAllImagesActivity.this, messageId, fileFolderMessageString);

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
						}, R.string.button_remove, R.string.dialog_confirmation_remove, mListName,
						imageFolderString);

			}
			else {
				DialogUtil.displayToast(DisplayAllImagesActivity.this, R.string.toast_removed_no_image);
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
	 * Add images to the image list.
	 */
	private void addImagesToList() {
		int folderSelectionMode = Integer.parseInt(PreferenceUtil.getSharedPreferenceString(R.string.key_pref_folder_selection_mechanism,
				R.string.pref_default_folder_selection_mechanism));

		switch (folderSelectionMode) {
		case 0:
			SelectImageFolderActivity.startActivity(this);
			break;
		case 1:
			SelectDirectoryActivity.startActivity(this);
			break;
		case 2:
			// via Gallery
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(intent, REQUEST_CODE_GALLERY);
			break;
		default:
			break;
		}
	}

	/**
	 * Create a new image list after requesting to enter its name.
	 *
	 * @param creationStyle flag indicating if the list should be empty or cloned.
	 */
	private void createNewImageList(final CreationStyle creationStyle) {
		DialogUtil
				.displayInputDialog(this, new RequestInputDialogListener() {
							/**
							 * The serial version id.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
								String name = text == null ? null : text.trim();

								if (name == null || name.length() == 0) {
									DialogUtil.displayInfo(DisplayAllImagesActivity.this, new MessageDialogListener() {
										/**
										 * The serial version id.
										 */
										private static final long serialVersionUID = 1L;

										@Override
										public void onDialogFinished() {
											createNewImageList(creationStyle);
										}

									}, 0, R.string.dialog_info_name_too_short);
								}
								else if (ImageRegistry.getImageListNames().contains(name)) {
									DialogUtil.displayInfo(DisplayAllImagesActivity.this, new MessageDialogListener() {
										/**
										 * The serial version id.
										 */
										private static final long serialVersionUID = 1L;

										@Override
										public void onDialogFinished() {
											createNewImageList(creationStyle);
										}

									}, 0, R.string.dialog_info_name_already_existing, name);
								}
								else {
									switchToImageList(name, creationStyle);
								}
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_enter_list_name, R.string.button_ok, "",
						creationStyle == CreationStyle.CREATE_EMPTY ? R.string.dialog_input_enter_list_name_new
								: R.string.dialog_input_enter_list_name_cloned);
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
	 * Switch to another image list after selecting the list.
	 */
	private void switchImageList() {
		ArrayList<String> listNames = ImageRegistry.getImageListNames();
		listNames.remove(mListName);

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
							/**
							 * The serial version id.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
								switchToImageList(text, CreationStyle.NONE);
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_select_list_name, listNames,
						R.string.dialog_select_list_for_switch);
	}

	/**
	 * Delete an image list after selecting the list.
	 */
	private void deleteImageList() {
		ArrayList<String> listNames = ImageRegistry.getImageListNames();
		listNames.remove(mListName);

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
							/**
							 * The serial version id.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final int positin, final String text) {
								DialogUtil.displayConfirmationMessage(DisplayAllImagesActivity.this,
										new ConfirmDialogListener() {
											/**
											 * The serial version id.
											 */
											private static final long serialVersionUID = 1L;

											@Override
											public void onDialogPositiveClick(final DialogFragment dialog1) {
												ImageRegistry.deleteImageList(text);

												if (GenericWidget.getWidgetIdsForName(text).size() > 0) {
													DialogUtil.displayInfo(DisplayAllImagesActivity.this, null, 0,
															R.string.dialog_info_delete_widgets, text);
												}

												invalidateOptionsMenu();
											}

											@Override
											public void onDialogNegativeClick(final DialogFragment dialog1) {
												// do nothing.
											}
										}, R.string.button_delete, R.string.dialog_confirmation_delete_list, text);
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_select_list_name, listNames,
						R.string.dialog_select_list_for_delete);
	}

	/**
	 * Rename the current image list after selecting a name.
	 */
	private void renameImageList() {
		DialogUtil
				.displayInputDialog(this, new RequestInputDialogListener() {
							/**
							 * The serial version id.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
								String name = text == null ? null : text.trim();

								if (mListName.equals(name)) {
									// If name unchanged, then do nothing.
									return;
								}
								else if (name == null || name.length() == 0) {
									DialogUtil.displayInfo(DisplayAllImagesActivity.this, new MessageDialogListener() {
										/**
										 * The serial version id.
										 */
										private static final long serialVersionUID = 1L;

										@Override
										public void onDialogFinished() {
											renameImageList();
										}

									}, 0, R.string.dialog_info_name_too_short);
								}
								else if (ImageRegistry.getImageListNames().contains(name)) {
									DialogUtil.displayInfo(DisplayAllImagesActivity.this, new MessageDialogListener() {
										/**
										 * The serial version id.
										 */
										private static final long serialVersionUID = 1L;

										@Override
										public void onDialogFinished() {
											renameImageList();
										}

									}, 0, R.string.dialog_info_name_already_existing, name);
								}
								else {
									boolean success = ImageRegistry.renameCurrentList(name);
									if (success) {
										switchToImageList(name, CreationStyle.NONE);
									}
								}
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_enter_list_name, R.string.button_ok, mListName,
						R.string.dialog_input_enter_list_name_changed);
	}

	/**
	 * Backup an image list after selecting the list to backup.
	 */
	private void backupImageList() {
		final ArrayList<String> listNames = ImageRegistry.getImageListNames();
		final ArrayList<String> backupNames = ImageRegistry.getBackupImageListNames();

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
							/**
							 * The serial version id.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
								if (backupNames.contains(text)) {
									DialogUtil.displayConfirmationMessage(DisplayAllImagesActivity.this,
											new ConfirmDialogListener() {
												/**
												 * The serial version id.
												 */
												private static final long serialVersionUID = 1L;

												@Override
												public void onDialogPositiveClick(final DialogFragment dialog2) {
													doBackup(text);
												}

												@Override
												public void onDialogNegativeClick(final DialogFragment dialog2) {
													// do nothing
												}
											}, R.string.button_overwrite, R.string.dialog_confirmation_overwrite_backup, text);

								}
								else {
									doBackup(text);
								}
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_select_list_name, listNames,
						R.string.dialog_select_list_for_backup);
	}

	/**
	 * Make a backup of the list without querying.
	 *
	 * @param listToBeBackuped The list name.
	 */
	private void doBackup(final String listToBeBackuped) {
		String backupFile = ImageRegistry.backupImageList(listToBeBackuped);
		DialogUtil.displayToast(DisplayAllImagesActivity.this,
				backupFile == null ? R.string.toast_failed_to_backup_list
						: R.string.toast_backup_of_list,
				listToBeBackuped);
		if (backupFile != null) {
			DialogUtil.displayInfo(DisplayAllImagesActivity.this, null,
					R.string.key_info_backup, R.string.dialog_info_backup, listToBeBackuped,
					backupFile);
		}
	}

	/**
	 * Restore an image list after selecting the list to backup.
	 */
	private void restoreImageList() {
		final ArrayList<String> listNames = ImageRegistry.getImageListNames();
		final ArrayList<String> backupNames = ImageRegistry.getBackupImageListNames();

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
							/**
							 * The serial version id.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
								if (listNames.contains(text)) {
									DialogUtil.displayConfirmationMessage(DisplayAllImagesActivity.this,
											new ConfirmDialogListener() {
												/**
												 * The serial version id.
												 */
												private static final long serialVersionUID = 1L;

												@Override
												public void onDialogPositiveClick(final DialogFragment dialog2) {
													doRestore(text);
												}

												@Override
												public void onDialogNegativeClick(final DialogFragment dialog2) {
													// do nothing
												}
											}, R.string.button_overwrite, R.string.dialog_confirmation_overwrite_list, text);

								}
								else {
									doRestore(text);
								}
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_select_list_name, backupNames,
						R.string.dialog_select_list_for_restore);
	}

	/**
	 * Make a restore of the list without querying.
	 *
	 * @param listToBeRestored The list name.
	 */
	private void doRestore(final String listToBeRestored) {
		boolean success = ImageRegistry.restoreImageList(listToBeRestored);
		DialogUtil.displayToast(DisplayAllImagesActivity.this,
				success ? R.string.toast_restore_of_list
						: R.string.toast_failed_to_restore_list,
				listToBeRestored);
		if (success) {
			switchToImageList(listToBeRestored, CreationStyle.NONE);
		}
	}

	/**
	 * Select another list to include in the current list.
	 */
	private void includeOtherList() {
		ArrayList<String> listNames = ImageRegistry.getImageListNames();
		listNames.remove(mListName);
		listNames.removeAll(mNestedListNames);

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
							/**
							 * The serial version id.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
								ImageList imageList = ImageRegistry.getCurrentImageList(true);
								imageList.addNestedList(text);
								imageList.update(true);
								fillListOfImages();
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
			setSelectionMode(action == CurrentAction.REMOVE ? SelectionMode.MULTIPLE_REMOVE : SelectionMode.ONE);
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
		switch (requestCode) {
		case DisplayRandomImageActivity.REQUEST_CODE:
			boolean needsRefresh1 = DisplayRandomImageActivity.getResult(resultCode, data);
			if (needsRefresh1) {
				fillListOfImages();
			}
			break;
		case SettingsActivity.REQUEST_CODE:
			boolean boughtPremium = SettingsActivity.getResultBoughtPremium(resultCode, data);
			if (boughtPremium) {
				invalidateOptionsMenu();
			}
			break;
		case DisplayImageDetailsActivity.REQUEST_CODE:
			boolean needsRefresh2 = DisplayImageDetailsActivity.getResultFileRemoved(resultCode, data);
			if (needsRefresh2) {
				changeAction(CurrentAction.DISPLAY);
				fillListOfImages();
			}
			break;
		case SelectDirectoryActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				String folderName = SelectDirectoryActivity.getResultFolder(resultCode, data);
				if (SelectDirectoryActivity.getResultUpdatedList(resultCode, data)) {
					fillListOfImages();
				}

				if (folderName != null) {
					DisplayImagesFromFolderActivity.startActivity(this, folderName, true);
				}
			}
			break;
		case SelectImageFolderActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				String folderName = SelectImageFolderActivity.getSelectedFolder(resultCode, data);
				if (folderName != null) {
					DisplayImagesFromFolderActivity.startActivity(this, folderName, true);
				}
				else if (SelectImageFolderActivity.triggeredSelectDirectoryActivity(resultCode, data)) {
					SelectDirectoryActivity.startActivity(this);
				}
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
					DisplayImagesFromFolderActivity.startActivity(this, folderName, true);
				}
			}
			break;
		case DisplayImagesFromFolderActivity.REQUEST_CODE:
			boolean needsRefresh3 = DisplayImagesFromFolderActivity.getResultFilesAdded(resultCode, data);
			if (needsRefresh3) {
				fillListOfImages();
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
		 * Just display photos.
		 */
		DISPLAY,
		/**
		 * Delete photos.
		 */
		REMOVE
	}

}
