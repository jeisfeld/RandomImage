package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.util.DateUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
import de.jeisfeld.randomimagelib.R;

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
	 * The resource key for the flat indicating if it should be prevented to trigger the ConfigureImageListActivity.
	 */
	private static final String STRING_EXTRA_PREVENT_DISPLAY_ALL = "de.jeisfeld.randomimage.PREVENT_DISPLAY_ALL";
	/**
	 * The resource key for the a tracking String.
	 */
	public static final String STRING_EXTRA_TRACKING = "de.jeisfeld.randomimage.TRACKING";
	/**
	 * The resource key for the flag if the parent activity should be finished.
	 */
	private static final String STRING_RESULT_FINISH_PARENT = "de.jeisfeld.randomimage.FINISH_PARENT";
	/**
	 * The resource key for the flag if the parent activity should be finished.
	 */
	private static final String STRING_RESULT_FILE_REMOVED = "de.jeisfeld.randomimage.FILE_REMOVED";

	/**
	 * The size of the image in the heading.
	 */
	private static final int HEADING_IMAGE_SIZE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32,
			Application.getAppContext().getResources().getDisplayMetrics());

	/**
	 * The name of the file whose details should be displayed.
	 */
	private String mFileName;

	/**
	 * The name of the list from which this file is taken.
	 */
	private String mListName;

	/**
	 * flag indicating if the activity should prevent to trigger ConfigureImageListActivity.
	 */
	private boolean mPreventDisplayAll;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity          The activity starting this activity.
	 * @param fileName          The name of the file whose details should be displayed.
	 * @param listName          The name of the list from which this file is taken.
	 * @param preventDisplayAll flag indicating if the activity should prevent to trigger ConfigureImageListActivity.
	 * @param trackingName      A String indicating the starter of the activity.
	 */
	public static final void startActivity(final Activity activity, final String fileName, final String listName,
										   final boolean preventDisplayAll, final String trackingName) {
		Intent intent = new Intent(activity, DisplayImageDetailsActivity.class);
		intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		if (trackingName != null) {
			intent.putExtra(STRING_EXTRA_TRACKING, trackingName);
		}
		intent.putExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, preventDisplayAll);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@SuppressLint("InflateParams")
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_image_details);
		// Ensure that this activity can be shown in Landscape mode, even if the parent activity is forced to Portrait.
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

		mFileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		mPreventDisplayAll = getIntent().getBooleanExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, false);

		String trackingName = getIntent().getStringExtra(STRING_EXTRA_TRACKING);
		if (trackingName != null) {
			TrackingUtil.sendEvent(Category.EVENT_VIEW, "Display Image Details", trackingName);
		}

		// Enable icon
		final TextView title = (TextView) findViewById(android.R.id.title);
		if (title != null) {
			int horizontalMargin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
			title.setPadding(horizontalMargin * 9 / 10, 0, 0, 0); // MAGIC_NUMBER
			title.setCompoundDrawablePadding(horizontalMargin * 2 / 3); // MAGIC_NUMBER
			if (mFileName != null && new File(mFileName).isFile()) {
				Drawable drawable = new BitmapDrawable(getResources(), ImageUtil.getImageBitmap(mFileName, HEADING_IMAGE_SIZE));
				title.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			}
			else {
				title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_title_info, 0, 0, 0);
			}
		}

		// put the textView into the listView, so that it scrolls with the list.
		displayImageInfo();

		final String galleryFileName;
		File file = new File(mFileName);
		if (file.isFile()) {
			galleryFileName = mFileName;
		}
		else if (file.isDirectory()) {
			setTitle(R.string.title_activity_display_folder_details);
			ArrayList<String> files = ImageUtil.getImagesInFolder(mFileName);
			if (files.size() > 0) {
				galleryFileName = files.get(0);
			}
			else {
				galleryFileName = null;
			}
		}
		else {
			galleryFileName = null;
		}
		if (galleryFileName != null) {
			Button btnViewInGallery = (Button) findViewById(R.id.buttonViewInGallery);
			btnViewInGallery.setVisibility(View.VISIBLE);
			btnViewInGallery.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					ImageUtil.showFileInGallery(DisplayImageDetailsActivity.this, galleryFileName);
					returnResult(false, false);
				}
			});
		}

		if (file.isFile()) {
			Button btnSendTo = (Button) findViewById(R.id.buttonSendTo);
			btnSendTo.setVisibility(View.VISIBLE);
			btnSendTo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					Intent intent = new Intent(Intent.ACTION_SEND);
					Uri uri = MediaStoreUtil.getUriFromFile(mFileName);
					intent.putExtra(Intent.EXTRA_STREAM, uri);
					intent.setType("image/*");
					startActivity(intent);
				}
			});
		}

		if (mListName != null) {
			configureButtonsForImageList();
		}
	}

	@Override
	protected final void onResume() {
		super.onResume();
		TrackingUtil.sendScreen(this);
	}

	/**
	 * Configure the buttons related to the image list.
	 */
	private void configureButtonsForImageList() {
		final StandardImageList imageList = ImageRegistry.getStandardImageListByName(mListName, false);
		if (imageList != null && imageList.contains(mFileName)) {
			final boolean isDirectory = new File(mFileName).isDirectory();

			Button btnRemoveFromList = (Button) findViewById(R.id.buttonRemoveFromList);
			btnRemoveFromList.setVisibility(View.VISIBLE);
			btnRemoveFromList.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					final String filesString;
					if (isDirectory) {
						filesString = DialogUtil.createFileFolderMessageString(null, Collections.singletonList(mFileName), null);
					}
					else {
						filesString = DialogUtil.createFileFolderMessageString(null, null, Collections.singletonList(mFileName));
					}

					DialogUtil.displayConfirmationMessage(DisplayImageDetailsActivity.this,
							new ConfirmDialogListener() {
								@Override
								public void onDialogPositiveClick(final DialogFragment dialog) {
									if (isDirectory) {
										imageList.removeFolder(mFileName);
										NotificationUtil.notifyUpdatedList(DisplayImageDetailsActivity.this, mListName, true,
												null, Collections.singletonList(mFileName), null);
									}
									else {
										imageList.removeFile(mFileName);
										NotificationUtil.notifyUpdatedList(DisplayImageDetailsActivity.this, mListName, true,
												null, null, Collections.singletonList(mFileName));
									}
									imageList.update(true);
									DialogUtil.displayToast(DisplayImageDetailsActivity.this, R.string.toast_removed_single, filesString);
									returnResult(mPreventDisplayAll, true);
								}

								@Override
								public void onDialogNegativeClick(final DialogFragment dialog) {
									returnResult(false, false);
								}
							}, null, R.string.button_remove, R.string.dialog_confirmation_remove, mListName,
							filesString);
				}
			});
		}

		if (mListName != null && !mPreventDisplayAll) {
			Button btnEditList = (Button) findViewById(R.id.buttonEditList);
			btnEditList.setVisibility(View.VISIBLE);
			btnEditList.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					ConfigureImageListActivity.startActivity(DisplayImageDetailsActivity.this, mListName, "from Image Details");
					returnResult(false, false);
				}
			});
		}

	}


	/**
	 * Static helper method to extract the finishParent flag.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
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
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
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
	 * @param finishParent The flag if the parent activity should be finished.
	 * @param fileRemoved  The flag if the file has been removed from the list.
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
	 * Display the image information.
	 */
	private void displayImageInfo() {
		File file = new File(mFileName);

		TextView textViewFileName = (TextView) findViewById(R.id.textViewFileName);
		textViewFileName.setText(file.getName());

		TextView textViewParentFolder = (TextView) findViewById(R.id.textViewParentFolder);
		textViewParentFolder.setText(fromHtml(getString(R.string.info_parent_folder, file.getParent())));

		TextView textViewImageDate = (TextView) findViewById(R.id.textViewImageDate);
		Date imageDate = ImageUtil.getExifDate(mFileName);
		if (imageDate == null) {
			textViewImageDate.setVisibility(View.GONE);
		}
		else {
			textViewImageDate.setText(fromHtml(getString(R.string.info_file_date, DateUtil.format(imageDate))));
		}

		TextView textViewNumberOfImages = (TextView) findViewById(R.id.textViewNumberOfImages);
		if (file.isDirectory()) {
			int imageCount = ImageUtil.getImagesInFolder(mFileName).size();
			double probability = -1;
			if (mListName != null && mListName.equals(ImageRegistry.getCurrentListName())) {
				probability = ImageRegistry.getCurrentImageList(false).getProbability(mFileName);
			}
			String probabilityString = "";
			if (probability > 0) {
				probabilityString = " (" + DisplayListInfoActivity.getPercentageString(probability) + "%)";
			}
			textViewNumberOfImages.setText(fromHtml(getString(R.string.info_number_of_images, imageCount + probabilityString)));
		}
		else {
			textViewNumberOfImages.setVisibility(View.GONE);
		}
	}

	/**
	 * Convert a html String into a text.
	 *
	 * @param html The html
	 * @return the text
	 */
	@SuppressWarnings("deprecation")
	private static Spanned fromHtml(final String html) {
		if (VERSION.SDK_INT >= VERSION_CODES.N) {
			return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
		}
		else {
			//noinspection deprecation
			return Html.fromHtml(html);
		}
	}
}
