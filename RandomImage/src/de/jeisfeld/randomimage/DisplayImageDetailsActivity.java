package de.jeisfeld.randomimage;

import java.io.File;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.jeisfeld.randomimage.util.DateUtil;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.view.ListMenuView;

/**
 * Activity to display the details of an image.
 */
public class DisplayImageDetailsActivity extends Activity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 1;
	/**
	 * The resource key for the file name.
	 */
	private static final String STRING_EXTRA_FILENAME = "de.jeisfeld.randomimage.FILENAME";
	/**
	 * The resource key for the name of the image list to be displayed.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";
	/**
	 * The resource key for the flat indicating if it should be prevented to trigger the DisplayAllImagesActivity.
	 */
	private static final String STRING_EXTRA_PREVENT_DISPLAY_ALL = "de.jeisfeld.randomimage.PREVENT_DISPLAY_ALL";
	/**
	 * The resource key for the flag if the parent activity should be finished.
	 */
	private static final String STRING_RESULT_FINISH_PARENT = "de.jeisfeld.randomimage.FINISH_PARENT";

	/**
	 * The name of the file whose details should be displayed.
	 */
	private String fileName;

	/**
	 * The name of the list from which this file is taken.
	 */
	private String listName;

	/**
	 * flag indicating if the activity should prevent to trigger DisplayAllImagesActivity.
	 */
	private boolean preventDisplayAll;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 * @param fileName
	 *            The name of the file whose details should be displayed.
	 * @param listName
	 *            The name of the list from which this file is taken.
	 * @param preventDisplayAll
	 *            flag indicating if the activity should prevent to trigger DisplayAllImagesActivity.
	 */
	public static final void startActivity(final Activity activity, final String fileName, final String listName,
			final boolean preventDisplayAll) {
		Intent intent = new Intent(activity, DisplayImageDetailsActivity.class);
		intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		intent.putExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, preventDisplayAll);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_image_details);

		fileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		listName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		preventDisplayAll = getIntent().getBooleanExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, false);

		// Move the textView into the listView, so that it scrolls with the list.
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout_display_image_details);
		TextView textView = (TextView) findViewById(R.id.textViewImageDetails);
		ListMenuView listMenu = (ListMenuView) findViewById(R.id.listViewImageDetailsMenu);
		layout.removeView(textView);
		listMenu.addHeaderView(textView, null, false);

		textView.setText(getImageInfo());

		listMenu.addItem(R.string.menu_remove_from_list, new OnClickListener() {
			@Override
			public void onClick(final View v) {
				// TODO
			}
		});
		// if (!preventDisplayAll) {
		// listMenu.addItem(R.string.menu_display_list, new OnClickListener() {
		// @Override
		// public void onClick(final View v) {
		// DisplayAllImagesActivity.startActivity(DisplayImageDetailsActivity.this, listName);
		// }
		// });
		// }

		listMenu.addItem(R.string.menu_display_list, new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (preventDisplayAll) {
					returnResult(true);
				}
				else {
					DisplayAllImagesActivity.startActivity(DisplayImageDetailsActivity.this, listName);
					returnResult(false);
				}
			}
		});

	}

	/**
	 * Static helper method to extract the result flag.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the flag if the parent activity should be finished.
	 */
	public static final boolean getResult(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_FINISH_PARENT);
		}
		else {
			return false;
		}
	}

	/**
	 * Helper method: Return the flag if the parent activity should be finished.
	 *
	 * @param finishParent
	 *            The flag if the parent activity should be finished.
	 */
	private void returnResult(final boolean finishParent) {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_FINISH_PARENT, finishParent);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Get the image information.
	 *
	 * @return The image information.
	 */
	private CharSequence getImageInfo() {
		StringBuffer imageInfo = new StringBuffer();
		File file = new File(fileName);

		imageInfo.append(formatImageInfoLine(this, R.string.info_file_name, file.getName()));
		imageInfo.append(formatImageInfoLine(this, R.string.info_file_location, file.getParent()));

		Date imageDate = ImageUtil.getExifDate(fileName);
		if (imageDate != null) {
			imageInfo.append(formatImageInfoLine(this, R.string.info_file_date, DateUtil.format(imageDate)));

		}

		return Html.fromHtml(imageInfo.toString());
	}

	/**
	 * Format one line of the image display.
	 *
	 * @param activity
	 *            the triggering activity.
	 * @param resource
	 *            The resource containing the label of the line.
	 * @param value
	 *            The value of the parameter.
	 * @return The formatted line.
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private static String formatImageInfoLine(final Activity activity, final int resource, final String value) {
		StringBuilder line = new StringBuilder("<br><b>");
		line.append(activity.getString(resource));
		line.append("</b><br>");

		if (SystemUtil.isAtLeastVersion(Build.VERSION_CODES.JELLY_BEAN)) {
			// Workaround to escape html, but transfer line breaks to HTML
			line.append(Html.escapeHtml(value.replace("\n", "|||LINEBREAK|||")).replace("|||LINEBREAK|||", "<br>"));
		}
		else {
			line.append(value.replace("&", "&amp;").replace("\n", "<br>").replace("<", "&lt;").replace(">", "&gt;"));
		}
		line.append("<br>");

		return line.toString();
	}

}
