package de.jeisfeld.randomimage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import de.jeisfeld.randomimage.MainConfigurationActivity.ListAction;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display the info for an image list.
 */
public class DisplayListInfoActivity extends Activity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 1;
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
	 * Number 100. Used for percentage calculation.
	 */
	private static final int HUNDRED = 100;

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
	public static final void startActivity(final Activity activity, final String listName, final String parentListName) {
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
		final TextView title = (TextView) findViewById(android.R.id.title);
		if (title != null) {
			int horizontalMargin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
			title.setPadding(horizontalMargin * 9 / 10, 0, 0, 0); // MAGIC_NUMBER
			title.setCompoundDrawablePadding(horizontalMargin * 2 / 3); // MAGIC_NUMBER
			title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_title_info, 0, 0, 0);
		}

		mListName = getIntent().getStringExtra(STRING_EXTRA_LIST_NAME);
		((TextView) findViewById(R.id.textViewNestedListName)).setText(mListName);


		if (parentListName == null) {
			configureActionButtons();
		}
		else {
			setTitle(R.string.title_activity_display_nested_list_info);
			displayNestedListInfo(parentListName);
		}
	}

	/**
	 * Display the info of the list as nested list of some parent list and configure editing of frequency.
	 *
	 * @param parentListName The name of the parent list.
	 */
	private void displayNestedListInfo(final String parentListName) {
		final StandardImageList imageList = ImageRegistry.getStandardImageListByName(parentListName, true);
		if (imageList == null || !imageList.isReady()) {
			finish();
			return;
		}
		((TextView) findViewById(R.id.textViewNumberOfImages)).setText(
				String.format(getString(R.string.info_nested_list_images), imageList.getNestedListImageCount(mListName)));

		((TextView) findViewById(R.id.textViewWeight)).setText(
				String.format(getString(R.string.info_nested_list_image_proportion),
						getPercentageString(imageList.getImagePercentage(mListName))));

		final EditText editTextViewFrequency = (EditText) findViewById(R.id.editTextViewFrequency);
		Double customNestedListWeight = imageList.getCustomNestedListWeight(mListName);
		if (customNestedListWeight == null) {
			double nestedListWeight = imageList.getNestedListWeight(mListName);
			editTextViewFrequency.setHint(getPercentageString(nestedListWeight));
		}
		else {
			editTextViewFrequency.setText(getPercentageString(customNestedListWeight));
		}

		ImageButton buttonSave = (ImageButton) findViewById(R.id.button_save);

		buttonSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				String percentageString = editTextViewFrequency.getText().toString().replace(',', '.');

				try {
					Double weight = null;
					if (percentageString.length() > 0) {
						weight = Double.parseDouble(percentageString) / HUNDRED;
						if (weight > 1) {
							weight = 1.0;
						}
						if (weight < 0) {
							weight = 0.0;
						}
					}

					imageList.setCustomNestedListWeight(mListName, weight);
				}
				catch (NumberFormatException e) {
					// do not change weight
				}
				finish();
			}
		});
	}

	/**
	 * Configure the action buttons for the list.
	 */
	private void configureActionButtons() {
		findViewById(R.id.buttonDelete).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				sendResult(ListAction.DELETE);
			}
		});

		findViewById(R.id.buttonRename).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				sendResult(ListAction.RENAME);
			}
		});

		findViewById(R.id.buttonClone).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				sendResult(ListAction.CLONE);
			}
		});

		findViewById(R.id.buttonBackup).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				sendResult(ListAction.BACKUP);
			}
		});

		if (ImageRegistry.getBackupImageListNames(ListFiltering.ALL_LISTS).contains(mListName)) {
			View buttonRestore = findViewById(R.id.buttonRestore);
			buttonRestore.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					sendResult(ListAction.RESTORE);
				}
			});
			buttonRestore.setVisibility(View.VISIBLE);
		}

		final TextView textViewNumberOfImages = (TextView) findViewById(R.id.textViewNumberOfImages);
		new Thread() {
			@Override
			public void run() {
				final int numberOfImages = ImageRegistry.getImageListByName(mListName, true).getAllImageFiles().size();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						textViewNumberOfImages.setText(String.format(getString(R.string.info_nested_list_images), numberOfImages));
					}
				});
			}
		}.start();
	}

	/**
	 * Get a rounded percentage String out of a probability.
	 *
	 * @param probability The probability.
	 * @return The percentage String.
	 */
	public static final String getPercentageString(final double probability) {
		if (probability >= 0.1) { // MAGIC_NUMBER
			return String.format("%1$,.1f", probability * HUNDRED);
		}
		else {
			return String.format("%1$,.2G", probability * HUNDRED);
		}
	}

	/**
	 * Static helper method to extract the name of the list.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the name of the list.
	 */
	public static final String getResultListName(final int resultCode, final Intent data) {
		return data == null ? null : data.getStringExtra(STRING_RESULT_LIST_NAME);
	}

	/**
	 * Static helper method to extract the the action to be done.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the action to be done.
	 */
	public static final ListAction getResultListAction(final int resultCode, final Intent data) {
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
	private void sendResult(final ListAction listAction) {
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
