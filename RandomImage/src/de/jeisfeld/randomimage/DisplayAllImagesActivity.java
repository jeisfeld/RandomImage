package de.jeisfeld.randomimage;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import de.jeisfeld.randomimage.DisplayAllImagesArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.GoogleBillingHelper;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.view.ThumbImageView;
import de.jeisfeld.randomimage.widgets.GenericWidget;

/**
 * Activity to display the list of images configured for this app.
 */
public class DisplayAllImagesActivity extends DisplayImageListActivity {
	/**
	 * The resource key for the name of the image list to be displayed.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";

	/**
	 * The names of the folders to be displayed.
	 */
	private ArrayList<String> folderNames;

	/**
	 * The names of the files to be displayed.
	 */
	private ArrayList<String> fileNames;

	/**
	 * The name of the image list to be displayed.
	 */
	private String listName;

	/**
	 * The current action within this activity.
	 */
	private CurrentAction currentAction = CurrentAction.DISPLAY;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param listName
	 *            the image list which should be displayed first.
	 * @param activity
	 *            The activity starting this activity.
	 *
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
			listName = savedInstanceState.getString("listName");
		}
		else {
			listName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		}

		if (listName == null) {
			listName = ImageRegistry.getCurrentListName();
		}

		switchToImageList(listName, CreationStyle.NONE);

		if (savedInstanceState != null) {
			String[] selectedFiles = savedInstanceState.getStringArray("selectedFiles");
			getAdapter().setSelectedFiles(new ArrayList<String>(Arrays.asList(selectedFiles)));
			String[] selectedFolders = savedInstanceState.getStringArray("selectedFolders");
			getAdapter().setSelectedFolders(new ArrayList<String>(Arrays.asList(selectedFolders)));
			currentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
			changeAction(currentAction);
		}

		if (fileNames.size() == 0 && folderNames.size() == 0) {
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
		fileNames = ImageRegistry.getCurrentImageList().getFileNames();
		folderNames = ImageRegistry.getCurrentImageList().getFolderNames();
		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}
		setAdapter(folderNames, fileNames, listName);
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
		startActivities(new Intent[] { reorderIntent, displayAllIntent });
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		switch (currentAction) {
		case DISPLAY:
			getMenuInflater().inflate(R.menu.display_images, menu);

			if (ImageRegistry.getImageListNames().size() < 2) {
				menu.findItem(R.id.action_switch_list).setEnabled(false);
				menu.findItem(R.id.action_delete_list).setEnabled(false);
			}
			if ((fileNames == null || fileNames.size() == 0) && (folderNames == null || folderNames.size() == 0)) {
				MenuItem menuItemRemove = menu.findItem(R.id.action_select_images_for_removal);
				menuItemRemove.setEnabled(false);
				Drawable icon =
						new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(),
								R.drawable.ic_action_minus));
				icon.setAlpha(128); // MAGIC_NUMBER
				menuItemRemove.setIcon(icon);
			}

			if (GoogleBillingHelper.hasPremium() || SystemUtil.isJeDevice()) {
				menu.findItem(R.id.action_need_premium).setVisible(false);
			}
			else {
				menu.findItem(R.id.action_manage_lists).setVisible(false);
			}
			return true;
		case DELETE:
			getMenuInflater().inflate(R.menu.delete_images, menu);
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
			return onOptionsItemSelectedDisplay(id);
		case DELETE:
			return onOptionsItemSelectedDelete(id);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Handler for options selected while in display mode.
	 *
	 * @param menuId
	 *            The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemSelectedDisplay(final int menuId) {
		switch (menuId) {
		case R.id.action_need_premium:
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
		case R.id.action_select_images_for_removal:
			changeAction(CurrentAction.DELETE);
			DialogUtil.displayInfo(this, null, R.string.key_info_delete_images, R.string.dialog_info_delete_images);
			return true;
		case R.id.action_add_single_images:
			AddImagesFromGalleryActivity.startActivity(this, false);
			return true;
		case R.id.action_add_image_folder:
			AddImagesFromGalleryActivity.startActivity(this, true);
			return true;
		case R.id.action_backup_list:
			PreferenceUtil.incrementCounter(R.string.key_statistics_countbackup);
			backupImageList();
			return true;
		case R.id.action_restore_list:
			PreferenceUtil.incrementCounter(R.string.key_statistics_countrestore);
			restoreImageList();
			return true;
		case R.id.action_rename_list:
			renameImageList();
			return true;
		case R.id.action_clone_list:
			PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatelist);
			createNewImageList(CreationStyle.CLONE_CURRENT);
			return true;
		case R.id.action_create_list:
			PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatelist);
			createNewImageList(CreationStyle.CREATE_EMPTY);
			return true;
		case R.id.action_switch_list:
			switchImageList();
			return true;
		case R.id.action_delete_list:
			deleteImageList();
			return true;
		case R.id.action_settings:
			SettingsActivity.startActivity(this);
			return true;
		default:
			return false;
		}
	}

	/**
	 * Handler for options selected while in delete mode.
	 *
	 * @param menuId
	 *            The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemSelectedDelete(final int menuId) {
		switch (menuId) {
		case R.id.action_remove_images:
			final ImageList imageList = ImageRegistry.getCurrentImageList();

			final ArrayList<String> imagesToBeRemoved = getAdapter().getSelectedFiles();
			final ArrayList<String> foldersToBeRemoved = getAdapter().getSelectedFolders();

			String imageFolderString = DialogUtil.createFileFolderMessageString(foldersToBeRemoved, imagesToBeRemoved);

			if (imagesToBeRemoved.size() > 0 || foldersToBeRemoved.size() > 0) {

				DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						ArrayList<String> removedFolders = new ArrayList<String>();
						ArrayList<String> removedImages = new ArrayList<String>();
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
								DialogUtil.createFileFolderMessageString(removedFolders, removedImages);
						int totalRemovedCount = removedFolders.size() + removedImages.size();
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
							imageList.update();
							fillListOfImages();
						}
						changeAction(CurrentAction.DISPLAY);
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing.
					}
				}, R.string.button_remove, R.string.dialog_confirmation_remove, listName,
						imageFolderString);

			}
			else {
				DialogUtil.displayToast(DisplayAllImagesActivity.this, R.string.toast_removed_no_image);
				changeAction(CurrentAction.DISPLAY);
			}
			return true;
		case R.id.action_cancel:
			changeAction(CurrentAction.DISPLAY);
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
	 * Create a new image list after requesting to enter its name.
	 *
	 * @param creationStyle
	 *            flag indicating if the list should be empty or cloned.
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
	 * @param name
	 *            The name of the target image list.
	 * @param creationStyle
	 *            Flag indicating if the list should be created if non-existing.
	 *
	 * @return true if successful.
	 */
	private boolean switchToImageList(final String name, final CreationStyle creationStyle) {
		listName = name;
		boolean success = ImageRegistry.switchToImageList(name, creationStyle);
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
		listNames.remove(listName);

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void
							onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
						switchToImageList(text, CreationStyle.NONE);
						fillListOfImages();
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
		listNames.remove(listName);

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void
							onDialogPositiveClick(final DialogFragment dialog, final int positin, final String text) {
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

						if (name == null || name.length() == 0) {
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
								fillListOfImages();
							}
						}
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing
					}
				}, R.string.title_dialog_enter_list_name, R.string.button_ok, listName,
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
					public void
							onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
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
	 * @param listToBeBackuped
	 *            The list name.
	 */
	private void doBackup(final String listToBeBackuped) {
		String backupFile = ImageRegistry.backupImageList(listToBeBackuped);
		DialogUtil.displayToast(DisplayAllImagesActivity.this,
				backupFile == null ? R.string.toast_failed_to_backup_list
						: R.string.toast_backup_of_list, listToBeBackuped);
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
					public void
							onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
						if (listNames.contains(text)) {
							DialogUtil.displayConfirmationMessage(DisplayAllImagesActivity.this,
									new ConfirmDialogListener() {
										/**
										 * The serial version id.
										 */
										private static final long serialVersionUID = 1L;

										@Override
										public void onDialogPositiveClick(final DialogFragment dialog2) {
											boolean success = ImageRegistry.restoreImageList(text);
											DialogUtil.displayToast(DisplayAllImagesActivity.this,
													success ? R.string.toast_restore_of_list
															: R.string.toast_failed_to_restore_list, text);
											if (success) {
												switchToImageList(text, CreationStyle.NONE);
											}
										}

										@Override
										public void onDialogNegativeClick(final DialogFragment dialog2) {
											// do nothing
										}
									}, R.string.button_overwrite, R.string.dialog_confirmation_overwrite_list, text);

						}
						else {
							boolean success = ImageRegistry.restoreImageList(text);
							DialogUtil.displayToast(DisplayAllImagesActivity.this,
									success ? R.string.toast_restore_of_list
											: R.string.toast_failed_to_restore_list, text);
							if (success) {
								switchToImageList(text, CreationStyle.NONE);
							}
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
	 * Change the action within this activity (display or delete).
	 *
	 * @param action
	 *            the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action == CurrentAction.DELETE || action == CurrentAction.DISPLAY) {
			currentAction = action;
			setMarkabilityStatus(action == CurrentAction.DELETE);
			invalidateOptionsMenu();
		}
	}

	/**
	 * Set the markability status of all views in the grid.
	 *
	 * @param markable
	 *            The markability status.
	 */
	private void setMarkabilityStatus(final boolean markable) {
		getAdapter().setSelectionMode(markable ? SelectionMode.MULTIPLE : SelectionMode.NONE);

		for (int i = 0; i < getGridView().getChildCount(); i++) {
			View imageView = getGridView().getChildAt(i);
			if (imageView instanceof ThumbImageView) {
				((ThumbImageView) imageView).setMarkable(markable);
			}
		}
		getAdapter().setMarkabilityStatus(markable);
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("currentAction", currentAction);
		outState.putSerializable("listName", listName);
		if (getAdapter() != null) {
			outState.putStringArray("selectedFiles", getAdapter().getSelectedFiles().toArray(new String[0]));
			outState.putStringArray("selectedFolders", getAdapter().getSelectedFolders().toArray(new String[0]));
		}
	}

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case AddImagesFromGalleryActivity.REQUEST_CODE:
			int addedImagesCount = AddImagesFromGalleryActivity.getAddedImageCountFromResult(resultCode, data);
			if (addedImagesCount > 1) {
				PreferenceUtil.incrementCounter(R.string.key_statistics_countaddfiles);
				DialogUtil.displayToast(this, R.string.toast_added_images_count, addedImagesCount);
			}
			else if (addedImagesCount == 1) {
				PreferenceUtil.incrementCounter(R.string.key_statistics_countaddfiles);
				DialogUtil.displayToast(this, R.string.toast_added_images_single);
			}
			String addedFolder = AddImagesFromGalleryActivity.getAddedFolderFromResult(resultCode, data);
			if (addedFolder != null) {
				PreferenceUtil.incrementCounter(R.string.key_statistics_countaddfolder);
				DialogUtil.displayToast(this, R.string.toast_added_folder, addedFolder);
			}
			if (addedImagesCount > 0 || addedFolder != null) {
				fillListOfImages();
			}
			break;
		case DisplayRandomImageActivity.REQUEST_CODE:
			boolean refreshParent = DisplayRandomImageActivity.getResult(resultCode, data);
			if (refreshParent) {
				fillListOfImages();
			}
			break;
		case SettingsActivity.REQUEST_CODE:
			boolean boughtPremium = SettingsActivity.getResultBoughtPremium(resultCode, data);
			if (boughtPremium) {
				invalidateOptionsMenu();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * The current action in this activity.
	 */
	public enum CurrentAction {
		/**
		 * Just display photos.
		 */
		DISPLAY,
		/**
		 * Delete photos.
		 */
		DELETE
	}

}
