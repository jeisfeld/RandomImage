package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import de.jeisfeld.randomimage.util.DateUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
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
	 * The resource key for the flag if the parent activity should be finished.
	 */
	private static final String STRING_RESULT_FILE_REMOVED = "de.jeisfeld.randomimage.FILE_REMOVED";

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

	@SuppressLint("InflateParams")
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_image_details);

		// Enable icon
		final TextView title = (TextView) findViewById(android.R.id.title);
		if (title != null) {
			int horizontalMargin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
			title.setPadding(horizontalMargin * 9 / 10, 0, 0, 0); // MAGIC_NUMBER
			title.setCompoundDrawablePadding(horizontalMargin * 2 / 3); // MAGIC_NUMBER
			title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_title_info, 0, 0, 0);
		}

		fileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		listName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		preventDisplayAll = getIntent().getBooleanExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, false);

		ListMenuView listMenu = (ListMenuView) findViewById(R.id.listViewImageDetailsMenu);

		// put the textView into the listView, so that it scrolls with the list.
		TextView textView =
				(TextView) getLayoutInflater().inflate(R.layout.listview_display_image_details_header, null);
		textView.setText(getImageInfo());
		listMenu.addHeaderView(textView, null, false);

		final ImageList imageList = ImageRegistry.getImageListByName(listName);
		if (imageList != null && imageList.contains(fileName)) {
			listMenu.addItem(R.string.menu_remove_from_list, new OnClickListener() {
				@Override
				public void onClick(final View v) {
					ArrayList<String> filesToBeRemoved = new ArrayList<String>();
					filesToBeRemoved.add(fileName);
					String filesString =
							DialogUtil.createFileFolderMessageString(new ArrayList<String>(), filesToBeRemoved);

					DialogUtil.displayConfirmationMessage(DisplayImageDetailsActivity.this,
							new ConfirmDialogListener() {
								/**
								 * The serial version id.
								 */
								private static final long serialVersionUID = 1L;

								@Override
								public void onDialogPositiveClick(final DialogFragment dialog) {
									imageList.removeFile(fileName);
									imageList.update();
									returnResult(preventDisplayAll, true);
								}

								@Override
								public void onDialogNegativeClick(final DialogFragment dialog) {
									returnResult(false, false);
								}
							}, R.string.button_remove, R.string.dialog_confirmation_remove, listName, filesString);
				}
			});
		}

		if (listName != null) {
			listMenu.addItem(R.string.menu_display_list, new OnClickListener() {
				@Override
				public void onClick(final View v) {
					if (preventDisplayAll) {
						returnResult(true, false);
					}
					else {
						DisplayAllImagesActivity.startActivity(DisplayImageDetailsActivity.this, listName);
						returnResult(false, false);
					}
				}
			});
		}

		// Adapter must be set after header, so better set it in the and.
		listMenu.setAdapter();
	}

	/**
	 * Static helper method to extract the finishParent flag.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the flag if the parent activity should be finished.
	 */
	public static final boolean getResultFinishParent(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_FINISH_PARENT);
		}
		else {
			return false;
		}
	}

	/**
	 * Static helper method to extract the fileRemoved flag.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the flag if the file was removed.
	 */
	public static final boolean getResultFileRemoved(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_FILE_REMOVED);
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
	 * @param fileRemoved
	 *            The flag if the file has been removed from the list.
	 */
	private void returnResult(final boolean finishParent, final boolean fileRemoved) {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_FINISH_PARENT, finishParent);
		resultData.putBoolean(STRING_RESULT_FILE_REMOVED, fileRemoved);
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
