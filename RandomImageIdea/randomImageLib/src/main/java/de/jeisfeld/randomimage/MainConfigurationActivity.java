package de.jeisfeld.randomimage;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.jeisfeld.randomimage.DisplayImageListAdapter.ItemType;
import de.jeisfeld.randomimage.DisplayImageListAdapter.SelectionMode;
import de.jeisfeld.randomimage.notifications.NotificationSettingsActivity;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.widgets.GenericWidget;
import de.jeisfeld.randomimage.widgets.WidgetSettingsActivity;
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
	 * The request code used to get permission for show activities in foreground.
	 */
	public static final int REQUEST_CODE_PERMISSION = 10;

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
	public static void startActivity(final Activity context) {
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
		setTitle(R.string.title_activity_main_configuration);
		fillListOfLists();

		if (savedInstanceState != null) {
			mCurrentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
		}

		configureButtons();

		changeAction(mCurrentAction);

		if (mListNames.size() == 0) {
			// On first startup need to create default list.
			ImageRegistry.getCurrentImageListRefreshed(true);
			fillListOfLists();
		}
		if (mListNames.size() == 1) {
			ImageList imageList = ImageRegistry.getCurrentImageList(false);
			if (imageList.isEmpty()) {
				if (DialogUtil.displaySearchForImageFoldersIfRequired(this, false)) {
					return;
				}
			}
		}

		if (savedInstanceState == null) {
			DialogUtil.displayInfo(this, null, R.string.key_hint_main_configuration, R.string.dialog_hint_main_configuration);
			DialogUtil.displayFirstUseMessageIfRequired(this);
		}
	}

	@Override
	public final void onItemClick(final ItemType itemType, final String name) {
		// itemType is always list.
		ConfigureImageListActivity.startActivity(this, name, "from Main Config");
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

	/**
	 * Configure the buttons for Widget and Notification setup.
	 */
	private void configureButtons() {
		Button buttonWidgets = findViewById(R.id.buttonWidgets);
		buttonWidgets.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				WidgetSettingsActivity.startActivity(MainConfigurationActivity.this);
			}
		});

		Button buttonNotifications = findViewById(R.id.buttonNotifications);
		buttonNotifications.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (VERSION.SDK_INT >= VERSION_CODES.M && !Settings.canDrawOverlays(MainConfigurationActivity.this)) {
					DialogUtil.displayInfo(MainConfigurationActivity.this, new MessageDialogListener() {
						@Override
						public void onDialogFinished() {
							Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
							startActivityForResult(permissionIntent, REQUEST_CODE_PERMISSION);
						}
					}, 0, R.string.dialog_info_permission_foreground);
				}
				else {
					NotificationSettingsActivity.startActivity(MainConfigurationActivity.this);
				}
			}
		});
	}

	/**
	 * Fill the view with the list of backups.
	 */
	private void fillListOfBackups() {
		mListNames = ImageRegistry.getBackupImageListNames(ListFiltering.HIDE_BY_REGEXP);
		if (getAdapter() != null) {
			getAdapter().cleanupCache();
		}
		setAdapter(mListNames, null, null, true);
		invalidateOptionsMenu();
	}

	@Override
	public final void onBackPressed() {
		if (mCurrentAction == CurrentAction.DISPLAY) {
			super.onBackPressed();
		}
		else {
			changeAction(CurrentAction.DISPLAY);
		}
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
		case RESTORE:
			getMenuInflater().inflate(R.menu.restore_multiple_lists, menu);
			return true;
		case DELETE:
			getMenuInflater().inflate(R.menu.delete_multiple_lists, menu);
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
		case RESTORE:
		case DELETE:
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
			changeAction(CurrentAction.RESTORE);
			return true;
		}
		else if (menuId == R.id.action_delete_lists) {
			changeAction(CurrentAction.DELETE);
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
			toggleSelectAll();
			return true;
		}
		else if (menuId == R.id.action_do_backup) {
			backupImageLists(getAdapter().getSelectedLists());
			return true;
		}
		else if (menuId == R.id.action_do_restore) {
			restoreImageLists(getAdapter().getSelectedLists());
			return true;
		}
		else if (menuId == R.id.action_do_delete) {
			deleteImageLists(getAdapter().getSelectedLists());
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
		boolean hasPremium = Boolean.parseBoolean(Application.getResourceString(R.string.has_premium));
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
								else if (name.startsWith("/")) {
									DialogUtil.displayInfo(MainConfigurationActivity.this, new MessageDialogListener() {
										@Override
										public void onDialogFinished() {
											createNewImageList(creationStyle);
										}

									}, 0, R.string.dialog_info_name_no_slash);
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
									ConfigureImageListActivity.startActivity(MainConfigurationActivity.this, name, "new");
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
	 * @param listNames The names of the lists to be deleted.
	 */
	private void deleteImageLists(final List<String> listNames) {
		if (listNames == null || listNames.size() == 0) {
			DialogUtil.displayToast(this, R.string.toast_delete_no_selection);
			changeAction(CurrentAction.DISPLAY);
			return;
		}
		if (listNames.size() == ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP).size()) {
			DialogUtil.displayInfo(this, R.string.dialog_info_not_allowed_to_delete_all_lists);
			return;
		}
		DialogUtil.displayConfirmationMessage(MainConfigurationActivity.this,
				new ConfirmDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						List<String> deletedLists = new ArrayList<>();
						for (String listName : listNames) {
							boolean success = ImageRegistry.deleteImageList(listName);
							if (success) {
								deletedLists.add(listName);
								if (GenericWidget.getWidgetIdsForName(listName).size() > 0) {
									DialogUtil.displayInfo(MainConfigurationActivity.this, R.string.dialog_info_delete_widgets, listName);
								}
								NotificationSettingsActivity.deleteListName(listName);
								if (listName.equals(ImageRegistry.getCurrentListName())) {
									List<String> remainingListNames = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
									if (remainingListNames.size() > 0) {
										ImageRegistry.switchToImageList(remainingListNames.get(0), CreationStyle.NONE, false);
									}
								}
							}
							else {
								DialogUtil.displayToast(MainConfigurationActivity.this, R.string.toast_failed_to_delete_list, listName);
							}
						}
						if (deletedLists.size() > 0) {
							DialogUtil.displayInfo(MainConfigurationActivity.this,
									deletedLists.size() == 1 ? R.string.dialog_info_delete_of_list_single : R.string.dialog_info_delete_of_lists,
									DialogUtil.createListNameString(deletedLists));
						}

						changeAction(CurrentAction.DISPLAY);
						fillListOfLists();
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog1) {
						// do nothing.
					}
				}, null, R.string.button_delete,
				listNames.size() == 1 ? R.string.dialog_confirmation_delete_list : R.string.dialog_confirmation_delete_lists,
				listNames.size(), listNames.get(0));
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
								else if (name.startsWith("/")) {
									DialogUtil.displayInfo(MainConfigurationActivity.this, new MessageDialogListener() {
										@Override
										public void onDialogFinished() {
											renameImageList(currentImageList);
										}

									}, 0, R.string.dialog_info_name_no_slash);
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
									ImageRegistry.renameImageList(currentImageList, name);
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
		PreferenceUtil.incrementCounter(R.string.key_statistics_countbackup);
		if (listNames == null || listNames.size() == 0) {
			DialogUtil.displayToast(this, R.string.toast_backup_no_selection);
			changeAction(CurrentAction.DISPLAY);
			return;
		}
		final List<String> existingBackups = ImageRegistry.getBackupImageListNames(ListFiltering.ALL_LISTS);
		existingBackups.retainAll(listNames);
		int existingCount = existingBackups.size();

		if (existingCount > 0) {
			DialogUtil.displayConfirmationMessage(MainConfigurationActivity.this,
					new ConfirmDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog) {
							doBackupAsynchroneously(listNames);
							changeAction(CurrentAction.DISPLAY);
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {
							List<String> listsToBeBackedUp = new ArrayList<>(listNames);
							listsToBeBackedUp.removeAll(existingBackups);
							doBackupAsynchroneously(listsToBeBackedUp);
							changeAction(CurrentAction.DISPLAY);
						}
					}, null, R.string.button_overwrite,
					existingCount == 1 ? R.string.dialog_confirmation_overwrite_backup_single
							: R.string.dialog_confirmation_overwrite_backup_multiple,
					existingCount, existingBackups.get(0));

		}
		else {
			doBackupAsynchroneously(listNames);
			changeAction(CurrentAction.DISPLAY);
		}
	}

	/**
	 * Make a backup of multiple lists in a background thread.
	 *
	 * @param listsToBeBackedUp The list names.
	 */
	private void doBackupAsynchroneously(final List<String> listsToBeBackedUp) {
		new Thread() {
			@Override
			public void run() {
				List<String> backedUpLists = new ArrayList<>();
				String backupFolder = null;
				for (String listName : listsToBeBackedUp) {
					String backupFile = ImageRegistry.backupImageList(listName);
					if (backupFile == null) {
						DialogUtil.displayToast(MainConfigurationActivity.this, R.string.toast_failed_to_backup_list, listName);
					}
					else {
						backedUpLists.add(listName);
						backupFolder = new File(backupFile).getParent();
					}
				}
				if (backedUpLists.size() > 0) {
					DialogUtil.displayInfo(MainConfigurationActivity.this,
							backedUpLists.size() == 1 ? R.string.dialog_info_backup_of_list_single : R.string.dialog_info_backup_of_lists,
							DialogUtil.createListNameString(backedUpLists), backupFolder);
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						changeAction(CurrentAction.DISPLAY);
					}
				});
			}
		}.start();
	}

	/**
	 * Restore image lists, warning in case of overwriting.
	 *
	 * @param listNames the names of the lists.
	 */
	private void restoreImageLists(final List<String> listNames) {
		PreferenceUtil.incrementCounter(R.string.key_statistics_countrestore);
		if (listNames == null || listNames.size() == 0) {
			DialogUtil.displayToast(this, R.string.toast_restore_no_selection);
			changeAction(CurrentAction.DISPLAY);
			return;
		}
		final List<String> existingLists = ImageRegistry.getImageListNames(ListFiltering.ALL_LISTS);
		existingLists.retainAll(listNames);
		int existingCount = existingLists.size();

		if (existingCount > 0) {
			DialogUtil.displayConfirmationMessage(MainConfigurationActivity.this,
					new ConfirmDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog) {
							doRestoreAsynchroneously(listNames);
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {
							List<String> listsToBeRestored = new ArrayList<>(listNames);
							listsToBeRestored.removeAll(existingLists);
							doRestoreAsynchroneously(listsToBeRestored);
						}
					}, null, R.string.button_overwrite,
					existingCount == 1 ? R.string.dialog_confirmation_overwrite_list_single
							: R.string.dialog_confirmation_overwrite_list_multiple,
					existingCount, existingLists.get(0));
		}
		else {
			doRestoreAsynchroneously(listNames);
		}
	}

	/**
	 * Restore multiple lists in a background thread.
	 *
	 * @param listsToBeRestored The list names.
	 */
	private void doRestoreAsynchroneously(final List<String> listsToBeRestored) {
		new Thread() {
			@Override
			public void run() {
				List<String> restoredLists = new ArrayList<>();
				for (String listName : listsToBeRestored) {
					boolean success = ImageRegistry.restoreImageList(listName);
					if (success) {
						restoredLists.add(listName);
					}
					else {
						DialogUtil.displayToast(MainConfigurationActivity.this, R.string.toast_failed_to_backup_list, listName);
					}
				}
				if (restoredLists.size() > 0) {
					DialogUtil.displayInfo(MainConfigurationActivity.this,
							restoredLists.size() == 1 ? R.string.dialog_info_restore_of_list_single : R.string.dialog_info_restore_of_lists,
							DialogUtil.createListNameString(restoredLists));
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						changeAction(CurrentAction.DISPLAY);
					}
				});
			}
		}.start();
	}

	/**
	 * Change the action within this activity (display or remove).
	 *
	 * @param action the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action != null && action != mCurrentAction) {
			TextView textViewInfo = findViewById(R.id.textViewMessage);
			LinearLayout layoutButtons = findViewById(R.id.layoutButtons);

			switch (action) {
			case DISPLAY:
				setTitle(R.string.title_activity_main_configuration);
				textViewInfo.setVisibility(View.GONE);
				layoutButtons.setVisibility(View.VISIBLE);
				break;
			case BACKUP:
				setTitle(R.string.title_activity_main_configuration_backup);
				textViewInfo.setText(R.string.text_info_select_lists_for_backup);
				textViewInfo.setVisibility(View.VISIBLE);
				layoutButtons.setVisibility(View.GONE);
				break;
			case RESTORE:
				setTitle(R.string.title_activity_main_configuration_restore);
				textViewInfo.setText(R.string.text_info_select_lists_for_restore);
				textViewInfo.setVisibility(View.VISIBLE);
				layoutButtons.setVisibility(View.GONE);
				break;
			case DELETE:
				setTitle(R.string.title_activity_main_configuration_delete);
				textViewInfo.setText(R.string.text_info_select_lists_for_delete);
				textViewInfo.setVisibility(View.VISIBLE);
				layoutButtons.setVisibility(View.GONE);
				break;
			default:
				break;
			}

			if (mCurrentAction == CurrentAction.RESTORE) {
				((TextView) findViewById(R.id.textViewTitle)).setText(R.string.text_info_image_lists);
				fillListOfLists();
			}
			else if (action == CurrentAction.RESTORE) {
				((TextView) findViewById(R.id.textViewTitle)).setText(R.string.text_info_image_list_backups);
				fillListOfBackups();
			}
			mCurrentAction = action;
			setSelectionMode(action == CurrentAction.DISPLAY ? SelectionMode.ONE : SelectionMode.MULTIPLE);
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
		case REQUEST_CODE_PERMISSION:
			NotificationSettingsActivity.startActivity(MainConfigurationActivity.this);
			break;
		default:
			break;
		}
	}

	@Override
	public final void updateAfterFirstImageListCreated() {
		fillListOfLists();
		DialogUtil.displayInfo(this, null, R.string.key_hint_main_configuration, R.string.dialog_hint_main_configuration);
		DialogUtil.displayFirstUseMessageIfRequired(this);
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
			deleteImageLists(Collections.singletonList(selectedListName));
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
			restoreImageLists(Collections.singletonList(selectedListName));
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
		BACKUP,
		/**
		 * Restore image lists.
		 */
		RESTORE,
		/**
		 * Delete image lists.
		 */
		DELETE
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
		RESTORE,
		/**
		 * Refresh the list display. (Used in different context.)
		 */
		REFRESH
	}

}
