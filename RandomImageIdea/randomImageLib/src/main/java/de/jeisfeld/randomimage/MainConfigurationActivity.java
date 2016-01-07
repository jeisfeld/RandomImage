package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.ItemType;
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
import de.jeisfeld.randomimage.view.ThumbImageView;
import de.jeisfeld.randomimage.widgets.GenericWidget;
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
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		context.startActivity(intent);
	}

	@Override
	protected final int getLayoutId() {
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

	@Override
	public final void onItemClick(final ItemType itemType, final String name) {
		// itemType is always list.
		ConfigureImageListActivity.startActivity(this, name);
	}

	@Override
	public final void onItemLongClick(final ItemType itemType, final String name) {
		// itemType is always list.
		DisplayListInfoActivity.startActivity(this, name, null);
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
		invalidateOptionsMenu();
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		switch (mCurrentAction) {
		case DISPLAY:
			getMenuInflater().inflate(R.menu.main_configuration, menu);

			if (mListNames.size() == 0) {
				menu.findItem(R.id.action_backup_lists).setEnabled(false);
			}

			return true;
		case BACKUP:
			getMenuInflater().inflate(R.menu.backup_multiple_lists, menu);
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
		case BACKUP:
			return onOptionsItemsSelectedMultiSelect(id);
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
		if (menuId == R.id.action_backup_lists) {
			changeAction(CurrentAction.BACKUP);
			return true;
		}
		else if (menuId == R.id.action_restore_lists) {
			PreferenceUtil.incrementCounter(R.string.key_statistics_countrestore);
			restoreImageList();
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
	 * Handler for options selected while in multi-select mode.
	 *
	 * @param menuId The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemsSelectedMultiSelect(final int menuId) {
		if (menuId == R.id.action_cancel) {
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
		else if (menuId == R.id.action_do_backup) {
			backupImageLists(getAdapter().getSelectedNestedLists());
			changeAction(CurrentAction.DISPLAY);
			return true;
		}
		return false;
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
									ConfigureImageListActivity.startActivity(MainConfigurationActivity.this, name);
									fillListOfLists();
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
	 * Delete an image list after selecting the list.
	 *
	 * @param listName The name of the list to be deleted.
	 */
	private void deleteImageList(final String listName) {
		DialogUtil.displayConfirmationMessage(MainConfigurationActivity.this,
				new ConfirmDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog1) {
						ImageRegistry.deleteImageList(listName);

						if (GenericWidget.getWidgetIdsForName(listName).size() > 0) {
							DialogUtil.displayInfo(MainConfigurationActivity.this, null, 0,
									R.string.dialog_info_delete_widgets, listName);
						}

						fillListOfLists();
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog1) {
						// do nothing.
					}
				}, null, R.string.button_delete, R.string.dialog_confirmation_delete_list, listName);
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
									ImageRegistry.renameCurrentList(name);
									fillListOfLists();
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
	 * Backup a set of image lists, warning in case of overwriting.
	 *
	 * @param listNames the name of the lists.
	 */
	private void backupImageLists(final List<String> listNames) {
		if (listNames == null || listNames.size() == 0) {
			return;
		}
		final List<String> existingBackups = ImageRegistry.getBackupImageListNames();
		existingBackups.retainAll(listNames);
		int existingCount = existingBackups.size();

		if (existingCount > 0) {
			DialogUtil.displayConfirmationMessage(MainConfigurationActivity.this,
					new ConfirmDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog2) {
							for (String listName : listNames) {
								doBackup(listName);
							}
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog2) {
							for (String listName : listNames) {
								if (!existingBackups.contains(listName)) {
									doBackup(listName);
								}
							}
						}
					}, null, R.string.button_overwrite,
					existingCount == 1 ? R.string.dialog_confirmation_overwrite_backup_single
							: R.string.dialog_confirmation_overwrite_backup_multiple,
					existingCount, existingBackups.get(0));

		}
		else {
			for (String listName : listNames) {
				doBackup(listName);
			}
		}
	}

	/**
	 * Make a backup of the list without querying.
	 *
	 * @param listToBeBackedUp The list name.
	 */
	private void doBackup(final String listToBeBackedUp) {
		String backupFile = ImageRegistry.backupImageList(listToBeBackedUp);
		DialogUtil.displayToast(MainConfigurationActivity.this,
				backupFile == null ? R.string.toast_failed_to_backup_list : R.string.toast_backup_of_list, listToBeBackedUp);
		if (backupFile != null) {
			NotificationUtil.notifyBackupRestore(this, listToBeBackedUp, new File(backupFile).getParent(), false);
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
	private void restoreSingleList(final List<String> existingLists, final String listName) {
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
			setSelectionMode(action == CurrentAction.BACKUP ? SelectionMode.MULTIPLE_ADD : SelectionMode.ONE);
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
		case DisplayListInfoActivity.REQUEST_CODE:
			handleListInfoResult(resultCode, data);
			break;
		default:
			break;
		}
	}

	/**
	 * Handle the result of DisplayListInfoActivity.
	 *
	 * @param resultCode The integer result code returned by the child activity through its setResult().
	 * @param data       An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
	 */
	private void handleListInfoResult(final int resultCode, final Intent data) {
		ListAction listAction = DisplayListInfoActivity.getResultListAction(resultCode, data);
		String selectedListName = DisplayListInfoActivity.getResultListName(resultCode, data);
		switch (listAction) {
		case DELETE:
			deleteImageList(selectedListName);
			break;
		case RENAME:
			renameImageList(selectedListName);
			break;
		case CLONE:
			if (selectedListName != null && checkIfMoreListsAllowed()) {
				if (ImageRegistry.switchToImageList(selectedListName, CreationStyle.NONE, true)) {
					PreferenceUtil.incrementCounter(R.string.key_statistics_countcreatelist);
					createNewImageList(CreationStyle.CLONE_CURRENT);
				}
			}
			break;
		case BACKUP:
			backupImageLists(Collections.singletonList(selectedListName));
			break;
		case RESTORE:
			restoreSingleList(Collections.singletonList(selectedListName), selectedListName);
			break;
		case NONE:
		default:
			break;
		}
	}

	/**
	 * The current action in this activity.
	 */
	private enum CurrentAction {
		/**
		 * Display the image lists.
		 */
		DISPLAY,
		/**
		 * Backup image lists.
		 */
		BACKUP
	}

	/**
	 * Action to be done with the list whose details are displayed.
	 */
	public enum ListAction {
		/**
		 * Do nothing.
		 */
		NONE,
		/**
		 * Clone list.
		 */
		CLONE,
		/**
		 * Rename list.
		 */
		RENAME,
		/**
		 * Delete list.
		 */
		DELETE,
		/**
		 * Backup list.
		 */
		BACKUP,
		/**
		 * Restore list.
		 */
		RESTORE
	}

}
