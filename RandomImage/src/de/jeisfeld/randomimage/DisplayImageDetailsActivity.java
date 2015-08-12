package de.jeisfeld.randomimage;

import java.io.File;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
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

/**
 * Activity to display the details of an image.
 */
public class DisplayImageDetailsActivity extends Activity {
	/**
	 * The resource key for the file name.
	 */
	public static final String STRING_EXTRA_FILENAME = "de.jeisfeld.randomimage.FILENAME";
	/**
	 * The resource key for the name of the image list to be displayed.
	 */
	public static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";

	/**
	 * The name of the file whose details should be displayed.
	 */
	private String fileName;

	/**
	 * The name of the list from which this file is taken.
	 */
	private String listName;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param context
	 *            The context starting this activity.
	 * @param fileName
	 *            The name of the file whose details should be displayed.
	 * @param listName
	 *            The name of the list from which this file is taken.
	 *
	 */
	public static final void startActivity(final Context context, final String fileName, final String listName) {
		Intent intent = new Intent(context, DisplayImageDetailsActivity.class);
		intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		context.startActivity(intent);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_image_details);

		fileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		listName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);

		// Move the textView into the listView, so that it scrolls with the list.
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout_display_image_details);
		TextView textView = (TextView) findViewById(R.id.textViewImageDetails);
		ListMenu listMenu = (ListMenu) findViewById(R.id.listViewImageDetailsMenu);
		layout.removeView(textView);
		listMenu.addHeaderView(textView, null, false);

		textView.setText(getImageInfo());

		listMenu.addItem(R.string.menu_remove_from_list, new OnClickListener() {
			@Override
			public void onClick(final View v) {
				// TODO
			}
		});
		listMenu.addItem(R.string.menu_display_list, new OnClickListener() {
			@Override
			public void onClick(final View v) {
				DisplayAllImagesActivity.startActivity(DisplayImageDetailsActivity.this, listName);
			}
		});

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
