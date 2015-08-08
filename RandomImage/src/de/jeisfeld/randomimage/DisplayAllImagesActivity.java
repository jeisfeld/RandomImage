package de.jeisfeld.randomimage;

import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import de.jeisfeld.randomimage.DisplayAllImagesArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;

/**
 * Activity to display the list of images configured for this app.
 */
public class DisplayAllImagesActivity extends Activity {
	/**
	 * The resource key for the input folder.
	 */
	public static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";

	/**
	 * The names of the files to be displayed.
	 */
	private String[] fileNames;

	/**
	 * The view showing the photos.
	 */
	private GridView gridView;

	/**
	 * The view showing the name of the list.
	 */
	private TextView textViewListName;

	/**
	 * The adapter handling the list of images.
	 */
	private DisplayAllImagesArrayAdapter adapter;

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
		setContentView(R.layout.activity_display_images);

		String listName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		if (listName != null && !listName.equals(ImageRegistry.getCurrentListName())) {
			ImageRegistry.switchToImageList(listName, CreationStyle.NONE);
		}

		gridView = (GridView) findViewById(R.id.gridViewDisplayImages);
		textViewListName = (TextView) findViewById(R.id.textViewListName);

		fillListOfImages();

		if (savedInstanceState != null) {
			String[] selectedFiles = savedInstanceState.getStringArray("selectedFiles");
			adapter.setSelectedFiles(selectedFiles);
			currentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
			changeAction(currentAction);
		}

		if (fileNames.length == 0) {
			DialogUtil.displayInfo(this, null, R.string.key_info_new_list, R.string.dialog_info_new_list);
		}
	}

	/**
	 * Fill the view with the current list of images.
	 */
	private void fillListOfImages() {
		fileNames = ImageRegistry.getCurrentImageList().getFileNames();
		if (adapter != null) {
			adapter.cleanupCache();
		}
		adapter = new DisplayAllImagesArrayAdapter(this, fileNames);
		gridView.setAdapter(adapter);
		textViewListName.setText(ImageRegistry.getCurrentListName());
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
		case R.id.action_select_images_for_removal:
			changeAction(CurrentAction.DELETE);
			DialogUtil.displayInfo(this, null, R.string.key_info_delete_images, R.string.dialog_info_delete_images);
			return true;
		case R.id.action_add_images:
			AddImagesFromGalleryActivity.startActivity(this);
			return true;
		case R.id.action_backup_list:
			backupImageList();
			return true;
		case R.id.action_restore_list:
			restoreImageList();
			return true;
		case R.id.action_rename_list:
			renameImageList();
			return true;
		case R.id.action_clone_list:
			createNewImageList(CreationStyle.CLONE_CURRENT);
			return true;
		case R.id.action_create_list:
			createNewImageList(CreationStyle.CREATE_EMPTY);
			return true;
		case R.id.action_switch_list:
			switchImageList();
			return true;
		case R.id.action_delete_list:
			deleteImageList();
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

			final int imagesToBeRemoved = adapter.getSelectedFiles().length;
			if (imagesToBeRemoved > 0) {
				DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						int removedFileCount = 0;
						for (String fileName : adapter.getSelectedFiles()) {
							boolean isRemoved = imageList.remove(fileName);
							if (isRemoved) {
								removedFileCount++;
							}
						}
						DialogUtil.displayToast(DisplayAllImagesActivity.this, R.string.toast_removed_images_count,
								removedFileCount);
						imageList.save();
						fillListOfImages();
						changeAction(CurrentAction.DISPLAY);
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing.
					}
				}, R.string.button_remove, R.string.dialog_confirmation_remove_images, imagesToBeRemoved,
						ImageRegistry.getCurrentListName());

			}
			return true;
		case R.id.action_cancel:
			changeAction(CurrentAction.DISPLAY);
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
							ImageRegistry.switchToImageList(name, creationStyle);
							invalidateOptionsMenu();
							fillListOfImages();
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
	 * Switch to another image list after selecting the list.
	 */
	private void switchImageList() {
		ArrayList<String> listNames = ImageRegistry.getImageListNames();
		String currentName = ImageRegistry.getCurrentListName();
		listNames.remove(currentName);

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
					/**
					 * The serial version id.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void
							onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
						ImageRegistry.switchToImageList(text, CreationStyle.NONE);
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
		String currentName = ImageRegistry.getCurrentListName();
		listNames.remove(currentName);

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
								ImageRegistry.switchToImageList(name, CreationStyle.NONE);
								invalidateOptionsMenu();
								fillListOfImages();
							}
						}
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing
					}
				}, R.string.title_dialog_enter_list_name, R.string.button_ok, ImageRegistry.getCurrentListName(),
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
											boolean success = ImageRegistry.backupImageList(text);
											DialogUtil.displayToast(DisplayAllImagesActivity.this,
													success ? R.string.toast_backup_of_list
															: R.string.toast_failed_to_backup_list, text);
										}

										@Override
										public void onDialogNegativeClick(final DialogFragment dialog2) {
											// do nothing
										}
									}, R.string.button_overwrite, R.string.dialog_confirmation_overwrite_backup, text);

						}
						else {
							boolean success = ImageRegistry.backupImageList(text);
							DialogUtil.displayToast(DisplayAllImagesActivity.this,
									success ? R.string.toast_backup_of_list
											: R.string.toast_failed_to_backup_list, text);
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
											invalidateOptionsMenu();
											if (text.equals(ImageRegistry.getCurrentListName())) {
												fillListOfImages();
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
							invalidateOptionsMenu();
							if (text.equals(ImageRegistry.getCurrentListName())) {
								fillListOfImages();
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
		if (action != null) {
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
		adapter.setSelectionMode(markable ? SelectionMode.MULTIPLE : SelectionMode.NONE);

		for (int i = 0; i < gridView.getChildCount(); i++) {
			View imageView = gridView.getChildAt(i);
			if (imageView instanceof ThumbImageView) {
				((ThumbImageView) imageView).setMarkable(markable);
			}
		}
		adapter.setMarkabilityStatus(markable);
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("currentAction", currentAction);
		if (adapter != null) {
			outState.putStringArray("selectedFiles", adapter.getSelectedFiles());
		}
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == AddImagesFromGalleryActivity.REQUEST_CODE) {
			int addedImagesCount = AddImagesFromGalleryActivity.getResult(resultCode, data);
			if (addedImagesCount > 0) {
				DialogUtil.displayToast(this, R.string.toast_added_images_count, addedImagesCount);
				fillListOfImages();
			}
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
