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

import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display the details of an image.
 */
public class DisplayNestedListDetailsActivity extends Activity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 1;
	/**
	 * The resource key for the name of the nested list whose details should be displayed.
	 */
	private static final String STRING_EXTRA_NESTED_LISTNAME = "de.jeisfeld.randomimage.NESTED_LISTNAME";
	/**
	 * The resource key for the name of the parent list from which the nested list is taken.
	 */
	private static final String STRING_EXTRA_PARENT_LISTNAME = "de.jeisfeld.randomimage.PARENT_LISTNAME";

	/**
	 * Number 100. Used for percentage calculation.
	 */
	private static final int HUNDRED = 100;

	/**
	 * The name of the nested list whose details should be displayed.
	 */
	private String mNestedListName;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity       The activity starting this activity.
	 * @param nestedListName The name of the nested list whose details should be displayed.
	 * @param parentListName The name of the parent list from which the nested list is taken.
	 */
	public static final void startActivity(final Activity activity, final String nestedListName, final String parentListName) {
		Intent intent = new Intent(activity, DisplayNestedListDetailsActivity.class);
		intent.putExtra(STRING_EXTRA_NESTED_LISTNAME, nestedListName);
		intent.putExtra(STRING_EXTRA_PARENT_LISTNAME, parentListName);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@SuppressLint("InflateParams")
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_nested_list_details);

		// Enable icon
		final TextView title = (TextView) findViewById(android.R.id.title);
		if (title != null) {
			int horizontalMargin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
			title.setPadding(horizontalMargin * 9 / 10, 0, 0, 0); // MAGIC_NUMBER
			title.setCompoundDrawablePadding(horizontalMargin * 2 / 3); // MAGIC_NUMBER
			title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_title_info, 0, 0, 0);
		}

		mNestedListName = getIntent().getStringExtra(STRING_EXTRA_NESTED_LISTNAME);
		String parentListName = getIntent().getStringExtra(STRING_EXTRA_PARENT_LISTNAME);

		final StandardImageList imageList = ImageRegistry.getStandardImageListByName(parentListName, true);
		if (imageList == null || !imageList.isReady()) {
			finish();
			return;
		}

		((TextView) findViewById(R.id.textViewNestedListName)).setText(mNestedListName);

		((TextView) findViewById(R.id.textViewNumberOfImages)).setText(
				String.format(getString(R.string.info_nested_list_images), imageList.getNestedListImageCount(mNestedListName)));

		((TextView) findViewById(R.id.textViewWeight)).setText(
				String.format(getString(R.string.info_nested_list_image_proportion),
						getPercentageString(imageList.getImagePercentage(mNestedListName))));

		final EditText editTextViewFrequency = (EditText) findViewById(R.id.editTextViewFrequency);
		Double customNestedListWeight = imageList.getCustomNestedListWeight(mNestedListName);
		if (customNestedListWeight == null) {
			double nestedListWeight = imageList.getNestedListWeight(mNestedListName);
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

					imageList.setCustomNestedListWeight(mNestedListName, weight);
				}
				catch (NumberFormatException e) {
					// do not change weight
				}
				finish();
			}
		});

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
}
