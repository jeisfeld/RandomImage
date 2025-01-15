package de.jeisfeld.randomimage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.RequiresApi;
import de.jeisfeld.randomimage.MainConfigurationActivity.ListAction;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.FormattingUtil;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimagelib.R;

import static de.jeisfeld.randomimage.util.ListElement.Type.NESTED_LIST;

/**
 * Activity to display the info for an image list.
 */
public class DisplayListInfoActivity extends BaseActivity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 8;
	/**
	 * The resource key for the name of the list whose details should be displayed.
	 */
	private static final String STRING_EXTRA_LIST_NAME = "de.jeisfeld.randomimage.LIST_NAME";
	/**
	 * The resource key for the name of the parent list from which this is a nested list.
	 */
	private static final String STRING_EXTRA_PARENT_LISTNAME = "de.jeisfeld.randomimage.PARENT_LISTNAME";
	/**
	 * The resource key for the list name to be passed back.
	 */
	private static final String STRING_RESULT_LIST_NAME = "de.jeisfeld.randomimage.LIST_NAME";
	/**
	 * The resource key for the list action to be done.
	 */
	private static final String STRING_RESULT_LIST_ACTION = "de.jeisfeld.randomimage.LIST_ACTION";

	/**
	 * The name of the list whose details should be displayed.
	 */
	private String mListName;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity       The activity starting this activity.
	 * @param listName       The name of the list whose details should be displayed.
	 * @param parentListName The name of the parent list from which this is a nested list.
	 */
	public static void startActivity(final Activity activity, final String listName, final String parentListName) {
		Intent intent = new Intent(activity, DisplayListInfoActivity.class);
		intent.putExtra(STRING_EXTRA_LIST_NAME, listName);
		if (parentListName != null) {
			intent.putExtra(STRING_EXTRA_PARENT_LISTNAME, parentListName);
		}
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@SuppressLint("InflateParams")
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String parentListName = getIntent().getStringExtra(STRING_EXTRA_PARENT_LISTNAME);

		setContentView(parentListName == null ? R.layout.activity_display_list_info : R.layout.activity_display_nested_list_info);

		// Enable icon
		final TextView title = findViewById(android.R.id.title);
		if (title != null) {
			int horizontalMargin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
			title.setPadding(horizontalMargin * 9 / 10, 0, 0, 0); // MAGIC_NUMBER
			title.setCompoundDrawablePadding(horizontalMargin * 2 / 3); // MAGIC_NUMBER
			title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_title_info, 0, 0, 0);
		}

		mListName = getIntent().getStringExtra(STRING_EXTRA_LIST_NAME);
		((TextView) findViewById(R.id.textViewNestedListName)).setText(mListName);


		if (parentListName == null) {
			displayMainListInfo();
		}
		else {
			setTitle(R.string.title_activity_display_nested_list_info);
			displayNestedListInfo(parentListName);
		}
	}

	@Override
	protected final void onResume() {
		super.onResume();
	}

	/**
	 * Display the info of the list as nested list of some parent list and configure editing of frequency.
	 *
	 * @param parentListName The name of the parent list.
	 */
	private void displayNestedListInfo(final String parentListName) {
		final StandardImageList imageList = ImageRegistry.getStandardImageListByName(parentListName, true);
		if (imageList == null) {
			finish();
			return;
		}

		findViewById(R.id.buttonRemove).setOnClickListener(v -> {
			final String listString = DialogUtil.createFileFolderMessageString(Collections.singletonList(mListName), null, null);
			DialogUtil.displayConfirmationMessage(DisplayListInfoActivity.this,
					new ConfirmDialogListener() {
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog) {
							imageList.remove(NESTED_LIST, mListName);
							NotificationUtil.notifyUpdatedList(DisplayListInfoActivity.this, parentListName, true,
									Collections.singletonList(mListName), null, null);
							imageList.update(true);
							DialogUtil.displayToast(DisplayListInfoActivity.this, R.string.toast_removed_single, listString);
							returnResult(ListAction.REFRESH);
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {
							returnResult(ListAction.NONE);
						}
					}, null, R.string.button_remove, R.string.dialog_confirmation_remove, parentListName, listString);
		});

		imageList.executeWhenReady(
				null,
				() -> {
					((TextView) findViewById(R.id.textViewNumberOfImages)).setText(
							DialogUtil.fromHtml(getString(R.string.info_number_of_images_and_proportion,
									imageList.getNestedListImageCount(mListName),
									FormattingUtil.getPercentageString(imageList.getPercentage(NESTED_LIST, mListName)))));

					final EditText editTextViewFrequency = findViewById(R.id.editTextViewFrequency);
					Double customNestedListWeight = imageList.getCustomNestedElementWeight(NESTED_LIST, mListName);
					if (customNestedListWeight == null) {
						double nestedListWeight = imageList.getProbability(NESTED_LIST, mListName);
						editTextViewFrequency.setHint(FormattingUtil.getPercentageString(nestedListWeight));
					}
					else {
						editTextViewFrequency.setText(FormattingUtil.getPercentageString(customNestedListWeight));
					}

					ImageButton buttonSave = findViewById(R.id.button_save);
					buttonSave.setOnClickListener(v -> {
						try {
							imageList.setCustomWeight(NESTED_LIST, mListName, FormattingUtil.getPercentageValue(editTextViewFrequency));
						}
						catch (NumberFormatException e) {
							// do not change weight
						}
						finish();
					});
				},
				this::finish
		);
	}

	/**
	 * Configure the action buttons for the list.
	 */
	private void displayMainListInfo() {
		findViewById(R.id.buttonDelete).setOnClickListener(v -> returnResult(ListAction.DELETE));

		findViewById(R.id.buttonRename).setOnClickListener(v -> returnResult(ListAction.RENAME));

		findViewById(R.id.buttonClone).setOnClickListener(v -> returnResult(ListAction.CLONE));

		findViewById(R.id.buttonBackup).setOnClickListener(v -> returnResult(ListAction.BACKUP));

		if (ImageRegistry.getBackupImageListNames(ListFiltering.ALL_LISTS).contains(mListName)) {
			View buttonRestore = findViewById(R.id.buttonRestore);
			buttonRestore.setOnClickListener(v -> returnResult(ListAction.RESTORE));
			buttonRestore.setVisibility(View.VISIBLE);
		}

		if (VERSION.SDK_INT >= VERSION_CODES.O) {
			ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
			if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
				View buttonCreateShortcut = findViewById(R.id.buttonCreateShortcut);
				buttonCreateShortcut.setOnClickListener(v -> {
					// Build the shortcut
					int shortcutId = PreferenceUtil.getSharedPreferenceInt(R.string.key_shortcut_max_id, 0) + 1;
					PreferenceUtil.setSharedPreferenceInt(R.string.key_shortcut_max_id, shortcutId);
					List<Integer> shortcutIds = getShortcutIds(shortcutManager);
					shortcutIds.add(shortcutId);
					setShortcutIds(shortcutIds);
					setDefaultShortcutParameters(shortcutId, mListName);

					Intent intent = DisplayRandomImageActivity.createIntent(DisplayListInfoActivity.this, mListName,
							null, false, getWidgetIdFromShortcutId(shortcutId), null);
					intent.setAction(Intent.ACTION_VIEW);

					ShortcutInfo shortcutInfo = getShortcutInfo(this, shortcutId, mListName, mListName, null);

					// Request the shortcut to be pinned
					Intent pinnedShortcutCallbackIntent =
							shortcutManager.createShortcutResultIntent(shortcutInfo);

					PendingIntent successCallback = PendingIntent.getBroadcast(DisplayListInfoActivity.this, 0,
							pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE);

					shortcutManager.requestPinShortcut(shortcutInfo, successCallback.getIntentSender());
					returnResult(null);
				});
				buttonCreateShortcut.setVisibility(View.VISIBLE);
			}
		}

		new Thread() {
			@Override
			public void run() {
				final int numberOfImages = ImageRegistry.getImageListByName(mListName, true).getAllImageFiles().size();
				runOnUiThread(() -> {
					TextView textViewNumberOfImages = findViewById(R.id.textViewNumberOfImages);
					textViewNumberOfImages.setText(
							DialogUtil.fromHtml(getString(R.string.info_number_of_images, numberOfImages)));
				});
			}
		}.start();
	}

	/**
	 * Static helper method to extract the name of the list.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the name of the list.
	 */
	public static String getResultListName(final int resultCode, final Intent data) {
		return data == null ? null : data.getStringExtra(STRING_RESULT_LIST_NAME);
	}

	/**
	 * Static helper method to extract the the action to be done.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the action to be done.
	 */
	public static ListAction getResultListAction(final int resultCode, final Intent data) {
		if (data == null) {
			return ListAction.NONE;
		}
		ListAction result = (ListAction) data.getSerializableExtra(STRING_RESULT_LIST_ACTION);
		return result == null ? ListAction.NONE : result;
	}

	/**
	 * Send the result to the calling activity.
	 *
	 * @param listAction the action to be done.
	 */
	private void returnResult(final ListAction listAction) {
		Bundle resultData = new Bundle();
		if (listAction != null) {
			resultData.putSerializable(STRING_RESULT_LIST_ACTION, listAction);
			resultData.putString(STRING_RESULT_LIST_NAME, mListName);
		}
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Get the ids of existing shortcuts.
	 *
	 * @param shortcutManager The shortcut manager
	 * @return The shortcut ids.
	 */
	@RequiresApi(api = VERSION_CODES.O)
	public static List<Integer> getShortcutIds(final ShortcutManager shortcutManager) {
		List<String> shortcutIdStrings = PreferenceUtil.getSharedPreferenceStringList(R.string.key_shortcut_ids);
		List<Integer> shortcutIds = new ArrayList<>();
		for (String idString : shortcutIdStrings) {
			shortcutIds.add(Integer.valueOf(idString));
		}

		// cleanup outdated ids
		if (shortcutManager != null) {
			List<Integer> pinnedShortcutIds = new ArrayList<>();
			boolean deletedId = false;
			for (ShortcutInfo shortcutInfo : shortcutManager.getPinnedShortcuts()) {
				pinnedShortcutIds.add(Integer.valueOf(shortcutInfo.getId()));
			}
			List<Integer> outdatedShortcutIds = new ArrayList<>();
			for (Integer shortcutId : shortcutIds) {
				if (!pinnedShortcutIds.contains(shortcutId)) {
					outdatedShortcutIds.add(shortcutId);
				}
			}
			for (Integer shortcutId : outdatedShortcutIds) {
				shortcutIds.remove(shortcutId);
				deleteShortcutParameters(shortcutId);
				deletedId = true;
			}
			if (deletedId) {
				setShortcutIds(shortcutIds);
			}
		}

		return shortcutIds;
	}

	/**
	 * Get the shortcutInfo for a shortcutId and a listName.
	 *
	 * @param context     the context
	 * @param shortcutId  The shortcutId
	 * @param listName    The list name
	 * @param displayName The display name
	 * @param widgetIcon  The widget icon
	 * @return The shortcutInfo.
	 */
	@RequiresApi(api = VERSION_CODES.O)
	private static ShortcutInfo getShortcutInfo(final Context context, final int shortcutId, final String listName, final String displayName,
												final String widgetIcon) {
		Intent intent = DisplayRandomImageActivity.createIntent(context, listName,
				null, false, getWidgetIdFromShortcutId(shortcutId), null);
		intent.setAction(Intent.ACTION_VIEW);

		Icon icon = Icon.createWithResource(context, R.drawable.ic_launcher);
		if (widgetIcon != null) {
			Bitmap bitmap = ImageUtil.getImageBitmap(widgetIcon, MediaStoreUtil.MINI_THUMB_SIZE);
			icon = Icon.createWithBitmap(bitmap);
		}

		return new ShortcutInfo.Builder(context, Integer.toString(shortcutId))
				.setShortLabel(displayName)
				.setLongLabel(displayName)
				.setIcon(icon)
				.setIntent(intent)
				.build();
	}

	/**
	 * Get the dummy widgetIds for shortcuts
	 *
	 * @return The dummy widgetIds for shortcuts
	 */
	public static List<Integer> getWidgetIdsForShortcuts() {
		List<Integer> widgetIds = new ArrayList<>();
		if (VERSION.SDK_INT >= VERSION_CODES.O) {
			ShortcutManager shortcutManager = Application.getAppContext().getSystemService(ShortcutManager.class);
			List<Integer> shortcutIds = getShortcutIds(shortcutManager);
			for (int shortcutId : shortcutIds) {
				widgetIds.add(getWidgetIdFromShortcutId(shortcutId));
			}
		}
		return widgetIds;
	}

	/**
	 * Set the ids of existing shortcuts.
	 *
	 * @param shortcutIds the shortcut ids.
	 */
	private static void setShortcutIds(final List<Integer> shortcutIds) {
		List<String> shortcutIdStrings = new ArrayList<>();
		for (int id : shortcutIds) {
			shortcutIdStrings.add(Integer.toString(id));
		}
		PreferenceUtil.setSharedPreferenceStringList(R.string.key_shortcut_ids, shortcutIdStrings);
	}

	/**
	 * Delete stored parameters for a shortcutId.
	 *
	 * @param shortcutId The shortcutId.
	 */
	private static void deleteShortcutParameters(final int shortcutId) {
		int appWidgetId = getWidgetIdFromShortcutId(shortcutId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_timeout, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_allowed_call_frequency, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_icon_image, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_list_name, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_display_name, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_use_default, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_scale_type, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_background, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_flip_behavior, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_change_timeout, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_change_with_tap, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_detail_prevent_screen_timeout, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_timer_duration, appWidgetId);
		PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_last_usage_time, appWidgetId);
	}

	/**
	 * Set the default parameters for a shortcut.
	 *
	 * @param shortcutId The shortcutId.
	 * @param listName   The list name.
	 */
	private void setDefaultShortcutParameters(final int shortcutId, final String listName) {
		int appWidgetId = getWidgetIdFromShortcutId(shortcutId);
		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId, mListName);
		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_timer_duration, appWidgetId, 0);

		PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_icon_image, appWidgetId,
				getString(R.string.pref_default_widget_icon_image));
		PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_use_default, appWidgetId, true);
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_scale_type, appWidgetId,
				PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_scale_type,
						R.string.pref_default_detail_scale_type));
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_background, appWidgetId,
				PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_background,
						R.string.pref_default_detail_background));
		PreferenceUtil.setIndexedSharedPreferenceInt(R.string.key_widget_detail_flip_behavior, appWidgetId,
				PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_detail_flip_behavior,
						R.string.pref_default_detail_flip_behavior));
		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_detail_change_timeout, appWidgetId,
				PreferenceUtil.getSharedPreferenceLongString(R.string.key_pref_detail_change_timeout,
						R.string.pref_default_notification_duration));
		PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_change_with_tap, appWidgetId,
				PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_detail_change_with_tap));
		PreferenceUtil.setIndexedSharedPreferenceBoolean(R.string.key_widget_detail_prevent_screen_timeout, appWidgetId,
				PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_detail_prevent_screen_timeout));
		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_timeout, appWidgetId, 0);
		PreferenceUtil.setIndexedSharedPreferenceLong(R.string.key_widget_allowed_call_frequency, appWidgetId, 0);
	}

	/**
	 * Update a shortcut linked to a dummy appWidgetId with stored data.
	 *
	 * @param context     The context.
	 * @param appWidgetId The dummy appWidgetId.
	 */
	public static void updateShortcut(final Context context, final int appWidgetId) {
		if (VERSION.SDK_INT >= VERSION_CODES.O) {
			int shortcutId = -(appWidgetId - 1) / 10;
			String listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);
			String displayName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_display_name, appWidgetId);
			if (displayName == null || displayName.isEmpty()) {
				displayName = listName;
			}

			String widgetIcon = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_icon_image, appWidgetId);
			if (context.getResources().getStringArray(R.array.icon_image_values)[0].equals(widgetIcon)) {
				widgetIcon = null;
			}

			ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

			if (shortcutManager != null) {
				List<ShortcutInfo> shortcuts = new ArrayList<>();
				shortcuts.add(getShortcutInfo(context, shortcutId, listName, displayName, widgetIcon));
				shortcutManager.updateShortcuts(shortcuts);
			}
		}
	}

	/**
	 * Get the dummy widgetId which is used to store paramters of a shortcutId.
	 *
	 * @param shortcutId the shortcutId.
	 * @return the related dummy widgetId.
	 */
	private static int getWidgetIdFromShortcutId(int shortcutId) {
		return -10 * shortcutId + 1;
	}

}
