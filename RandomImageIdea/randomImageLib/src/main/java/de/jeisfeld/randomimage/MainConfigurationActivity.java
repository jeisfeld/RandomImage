package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.AuthorizationHelper;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.NotificationUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display and configure the list of images in an image list.
 */
public class MainConfigurationActivity extends DisplayImageListActivity {
	/**
	 * The number of lists that may be created without premium status.
	 */
	private static final int ALLOWED_LISTS_NON_PREMIUM = 3;

	/**
	 * The names of the image lists to be displayed.
	 */
	private ArrayList<String> mListNames;

	/**
	 * The current action within this activity.
	 */
	private CurrentAction mCurrentAction = CurrentAction.DISPLAY;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param context The context creating the intent.
	 */
	public static final void startActivity(final Activity context) {
		Intent intent = new Intent(context, MainConfigurationActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.activity_main_configuration;
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fillListOfLists();

		if (savedInstanceState != null) {
			mCurrentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
		}

		changeAction(mCurrentAction);
	}

	/**
	 * Fill the view with the current list of images.
	 */
	private void fillListOfLists() {
		mListNames = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}
		setAdapter(mListNames, null, null, false);
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		switch (mCurrentAction) {
		case DISPLAY:
			getMenuInflater().inflate(R.menu.display_image_list, menu);

			if (ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP).size() < 2) {
				menu.findItem(R.id.action_switch_list).setEnabled(false);
				menu.findItem(R.id.action_delete_list).setEnabled(false);
			}

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
			DialogUtil.displayInfo(this, null, R.string.key_info_delete_images, R.string.dialog_info_delete_images);
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
		else if (menuId == R.id.action_clone_list) {
			if (checkIfMoreListsAllowed()) {
				PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatelist);
				createNewImageList(CreationStyle.CLONE_CURRENT);
			}
			return true;
		}
		else if (menuId == R.id.action_create_list) {
			if (checkIfMoreListsAllowed()) {
				PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatelist);
				createNewImageList(CreationStyle.CREATE_EMPTY);
			}
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
	 * Check if premium status and number of lists allows to add another image list.
	 *
	 * @return true if it is allowed to add another list.
	 */
	private boolean checkIfMoreListsAllowed() {
		boolean hasPremium = AuthorizationHelper.hasPremium();
		if (!hasPremium) {
			boolean moreListsAllowed = ImageRegistry.getImageListNames(ListFiltering.ALL_LISTS).size() < ALLOWED_LISTS_NON_PREMIUM;

			if (moreListsAllowed) {
				return true;
			}
			else {
				DialogUtil.displayInfo(this, new MessageDialogListener() {
					@Override
					public void onDialogFinished() {
						SettingsActivity.startActivity(MainConfigurationActivity.this);
					}
				}, 0, R.string.dialog_info_need_premium);
				return false;
			}
		}
		return true;
	}

	/**
	 * Create a new image list after requesting to enter its name.
	 *
	 * @param creationStyle flag indicating if the list should be empty or cloned.
	 */
	private void createNewImageList(final CreationStyle creationStyle) {
		DialogUtil
				.displayInputDialog(this, new RequestInputDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
								String name = text == null ? null : text.trim();

								if (name == null || name.length() == 0) {
									DialogUtil.displayInfo(MainConfigurationActivity.this, new MessageDialogListener() {
										@Override
										public void onDialogFinished() {
											createNewImageList(creationStyle);
										}

									}, 0, R.string.dialog_info_name_too_short);
								}
								else if (ImageRegistry.getImageListNames(ListFiltering.ALL_LISTS).contains(name)) {
									DialogUtil.displayInfo(MainConfigurationActivity.this, new MessageDialogListener() {
										@Override
										public void onDialogFinished() {
											createNewImageList(creationStyle);
										}

									}, 0, R.string.dialog_info_name_already_existing, name);
								}
								else {
									ImageRegistry.switchToImageList(name, creationStyle, true);
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
	 * Rename the current image list after selecting a name.
	 *
	 * @param currentImageList The current image list.
	 */
	private void renameImageList(final String currentImageList) {
		DialogUtil
				.displayInputDialog(this, new RequestInputDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
								String name = text == null ? null : text.trim();

								if (currentImageList.equals(name)) {
									// If name unchanged, then do nothing.
									//noinspection UnnecessaryReturnStatement
									return;
								}
								else if (name == null || name.length() == 0) {
									DialogUtil.displayInfo(MainConfigurationActivity.this, new MessageDialogListener() {
										@Override
										public void onDialogFinished() {
											renameImageList(currentImageList);
										}

									}, 0, R.string.dialog_info_name_too_short);
								}
								else if (ImageRegistry.getImageListNames(ListFiltering.ALL_LISTS).contains(name)) {
									DialogUtil.displayInfo(MainConfigurationActivity.this, new MessageDialogListener() {
										@Override
										public void onDialogFinished() {
											renameImageList(currentImageList);
										}

									}, 0, R.string.dialog_info_name_already_existing, name);
								}
								else {
									boolean success = ImageRegistry.renameCurrentList(name);
								}
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_enter_list_name, R.string.button_ok, currentImageList,
						R.string.dialog_input_enter_list_name_changed);
	}

	/**
	 * Backup an image list after selecting the list to backup.
	 */
	private void backupImageList() {
		final ArrayList<String> listNames = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
		// Add entry to select all lists on first position.
		final String allListsText = Application.getAppContext().getString(R.string.menu_backup_all_lists);

		listNames.add(0, allListsText);

		final ArrayList<String> backupNames = ImageRegistry.getBackupImageListNames();

		DialogUtil.displayListSelectionDialog(this, new SelectFromListDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
						if (position == 0) {
							// Go through all lists in reverse order, so that dialog of first list appears on top.
							for (int i = listNames.size() - 1; i > 0; i--) {
								backupSingleList(backupNames, listNames.get(i));
							}
						}
						else {
							backupSingleList(backupNames, text);
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
	 * Backup an image list, warning in case of overwriting.
	 *
	 * @param existingBackups the list of existing backups.
	 * @param listName        the name of the list.
	 */
	private void backupSingleList(final ArrayList<String> existingBackups, final String listName) {
		if (existingBackups.contains(listName)) {
			DialogUtil.displayConfirmationMessage(MainConfigurationActivity.this,
					new ConfirmDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog2) {
							doBackup(listName);
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog2) {
							// do nothing
						}
					}, null, R.string.button_overwrite, R.string.dialog_confirmation_overwrite_backup, listName);

		}
		else {
			doBackup(listName);
		}
	}

	/**
	 * Make a backup of the list without querying.
	 *
	 * @param listToBeBackuped The list name.
	 */
	private void doBackup(final String listToBeBackuped) {
		String backupFile = ImageRegistry.backupImageList(listToBeBackuped);
		DialogUtil.displayToast(MainConfigurationActivity.this,
				backupFile == null ? R.string.toast_failed_to_backup_list : R.string.toast_backup_of_list, listToBeBackuped);
		if (backupFile != null) {
			NotificationUtil.notifyBackupRestore(this, listToBeBackuped, new File(backupFile).getParent(), false);
		}
	}

	/**
	 * Restore an image list after selecting the list to backup.
	 */
	private void restoreImageList() {
		final ArrayList<String> listNames = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
		final ArrayList<String> backupNames = ImageRegistry.getBackupImageListNames();
		// Add entry to select all lists on first position.
		final String allListsText = Application.getAppContext().getString(R.string.menu_restore_all_lists);
		backupNames.add(0, allListsText);

		DialogUtil
				.displayListSelectionDialog(this, new SelectFromListDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
								if (position == 0) {
									// Go through all lists in reverse order, so that dialog of first list appears on top.
									for (int i = backupNames.size() - 1; i > 0; i--) {
										restoreSingleList(listNames, backupNames.get(i));
									}
								}
								else {
									restoreSingleList(listNames, text);
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
	 * Restore an image list, warning in case of overwriting.
	 *
	 * @param existingLists the list of existing list names
	 * @param listName      the name of the list.
	 */
	private void restoreSingleList(final ArrayList<String> existingLists, final String listName) {
		if (existingLists.contains(listName)) {
			DialogUtil.displayConfirmationMessage(MainConfigurationActivity.this,
					new ConfirmDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog2) {
							doRestore(listName);
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog2) {
							// do nothing
						}
					}, null, R.string.button_overwrite, R.string.dialog_confirmation_overwrite_list, listName);

		}
		else {
			doRestore(listName);
		}
	}

	/**
	 * Make a restore of the list without querying.
	 *
	 * @param listToBeRestored The list name.
	 */
	private void doRestore(final String listToBeRestored) {
		boolean success = ImageRegistry.restoreImageList(listToBeRestored);
		DialogUtil.displayToast(MainConfigurationActivity.this,
				success ? R.string.toast_restore_of_list : R.string.toast_failed_to_restore_list, listToBeRestored);
		if (success) {
			NotificationUtil.notifyBackupRestore(this, listToBeRestored, null, true);
		}
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
	}

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
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
