package de.jeisfeld.randomimage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.StandardImageList;

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
	 * The name of the nested list whose details should be displayed.
	 */
	private String nestedListName;

	/**
	 * The name of the parent list from which the nested list is taken.
	 */
	private String parentListName;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 * @param nestedListName
	 *            The name of the nested list whose details should be displayed.
	 * @param parentListName
	 *            The name of the parent list from which the nested list is taken.
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

		nestedListName = getIntent().getStringExtra(STRING_EXTRA_NESTED_LISTNAME);
		parentListName = getIntent().getStringExtra(STRING_EXTRA_PARENT_LISTNAME);

		StandardImageList imageList = ImageRegistry.getStandardImageListByName(parentListName);
		if (imageList == null) {
			finish();
			return;
		}

		((TextView) findViewById(R.id.textViewNestedListName)).setText(nestedListName);

		((TextView) findViewById(R.id.textViewNumberOfImages)).setText(
				String.format(getString(R.string.info_nested_list_images), imageList.getNestedListImageCount(nestedListName)));

		((TextView) findViewById(R.id.textViewWeight)).setText(
				String.format(getString(R.string.info_nested_list_weight), 100 * imageList.getNestedListWeight(nestedListName))); // MAGIC_NUMBER

	}

}
