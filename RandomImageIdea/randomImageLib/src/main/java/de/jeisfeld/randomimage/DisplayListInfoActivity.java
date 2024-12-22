package de.jeisfeld.randomimage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Collections;

import de.jeisfeld.randomimage.MainConfigurationActivity.ListAction;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.FormattingUtil;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimage.util.TrackingUtil;
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
		TrackingUtil.sendScreen(this);
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
					Intent intent = DisplayRandomImageActivity.createIntent(DisplayListInfoActivity.this, mListName,
							null, true, null, null);
					intent.setAction(Intent.ACTION_VIEW);
					intent.putExtra("key", "value"); // Add any extras if needed

					// Build the shortcut
					ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(DisplayListInfoActivity.this, "unique_shortcut_id")
							.setShortLabel(mListName)
							.setLongLabel(mListName)
							.setIcon(Icon.createWithResource(DisplayListInfoActivity.this, R.drawable.ic_launcher))
							.setIntent(intent)
							.build();

					// Request the shortcut to be pinned
					Intent pinnedShortcutCallbackIntent =
							shortcutManager.createShortcutResultIntent(shortcutInfo);

					PendingIntent successCallback = PendingIntent.getBroadcast(DisplayListInfoActivity.this, 0,
							pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE);

					shortcutManager.requestPinShortcut(shortcutInfo, successCallback.getIntentSender());
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
}
