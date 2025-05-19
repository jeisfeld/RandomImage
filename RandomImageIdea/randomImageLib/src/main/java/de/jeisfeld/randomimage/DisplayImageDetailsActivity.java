package de.jeisfeld.randomimage;

import android.app.Activity;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import androidx.exifinterface.media.ExifInterface;
import de.jeisfeld.randomimage.notifications.NotificationConfigurationActivity;
import de.jeisfeld.randomimage.notifications.NotificationConfigurationFragment;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.util.DateUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.FormattingUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimage.widgets.GenericWidget.UpdateType;
import de.jeisfeld.randomimage.widgets.GenericWidgetConfigurationActivity;
import de.jeisfeld.randomimage.widgets.ImageWidget;
import de.jeisfeld.randomimage.widgets.ImageWidgetConfigurationActivity;
import de.jeisfeld.randomimage.widgets.MiniWidget;
import de.jeisfeld.randomimage.widgets.MiniWidgetConfigurationActivity;
import de.jeisfeld.randomimage.widgets.ShortcutDummyWidget;
import de.jeisfeld.randomimage.widgets.StackedImageWidgetConfigurationActivity;
import de.jeisfeld.randomimagelib.R;

import static de.jeisfeld.randomimage.util.ListElement.Type.FILE;
import static de.jeisfeld.randomimage.util.ListElement.Type.FOLDER;

/**
 * Activity to display the details of an image.
 */
public class DisplayImageDetailsActivity extends BaseActivity {
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
	 * The resource key for the widget from which the activity was started.
	 */
	private static final String STRING_EXTRA_NOTIFICATION_ID = "de.jeisfeld.randomimage.NOTIFICATION_ID";
	/**
	 * The resource key for the widget from which the activity was started.
	 */
	private static final String STRING_EXTRA_APP_WIDGET_ID = "de.jeisfeld.randomimage.APP_WIDGET_ID";
	/**
	 * The resource key for the flat indicating if it should be prevented to trigger the ConfigureImageListActivity.
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
	 * The size of the image in the heading.
	 */
	private static final int HEADING_IMAGE_SIZE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32,
			Application.getAppContext().getResources().getDisplayMetrics());

	/**
	 * The name of the file as stored in the image list.
	 */
	private String mFileNameInList;

	/**
	 * The name of the file whose details should be displayed.
	 */
	private String mFileName;

	/**
	 * The type of the file whose details should be displayed.
	 */
	private FileType mFileType;

	/**
	 * The name of the list from which this file is taken.
	 */
	private String mListName;

	/**
	 * The id of the widget from which the image was displayed.
	 */
	private int mAppWidgetId;

	/**
	 * The id of the notification from which the image was displayed.
	 */
	private int mNotificationId;

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
	 * @param notificationId    The notification from which the image was displayed.
	 * @param appWidgetId       The widget from which the image was displayed.
	 * @param preventDisplayAll flag indicating if the activity should prevent to trigger ConfigureImageListActivity.
	 */
	public static void startActivity(final Activity activity, final String fileName, final String listName, final Integer notificationId,
									 final Integer appWidgetId, final boolean preventDisplayAll) {
		Intent intent = new Intent(activity, DisplayImageDetailsActivity.class);
		intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		if (notificationId != null) {
			intent.putExtra(STRING_EXTRA_NOTIFICATION_ID, notificationId);
		}
		if (appWidgetId != null) {
			intent.putExtra(STRING_EXTRA_APP_WIDGET_ID, appWidgetId);
		}
		intent.putExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, preventDisplayAll);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_image_details);
		// Ensure that this activity can be shown in Landscape mode, even if the parent activity is forced to Portrait.
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

		mFileNameInList = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		mFileName = mFileNameInList;
		mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		mAppWidgetId = getIntent().getIntExtra(STRING_EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		mNotificationId = getIntent().getIntExtra(STRING_EXTRA_NOTIFICATION_ID, -1);
		mPreventDisplayAll = getIntent().getBooleanExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, false);

		File file = new File(mFileName);
		if (file.isFile()) {
			mFileType = FileType.IMAGE_FILE;
		}
		else if (mFileName.endsWith(ImageUtil.RECURSIVE_SUFFIX) && file.getParentFile().isDirectory()) {
			mFileType = FileType.FOLDER_RECURSIVE;
			mFileName = file.getParent();
		}
		else if (file.isDirectory()) {
			mFileType = FileType.FOLDER_SIMPLE;
		}
		else {
			mFileType = FileType.UNKNOWN;
		}

		// Enable icon
		final TextView title = findViewById(android.R.id.title);
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
		if (mFileType == FileType.IMAGE_FILE) {
			galleryFileName = mFileName;
		}
		else if (mFileType == FileType.FOLDER_SIMPLE) {
			setTitle(R.string.title_activity_display_folder_details);
			ArrayList<String> files = ImageUtil.getImagesInFolder(mFileName);
			if (!files.isEmpty()) {
				galleryFileName = files.get(0);
			}
			else {
				galleryFileName = null;
			}
		}
		else {
			setTitle(R.string.title_activity_display_folder_details);
			galleryFileName = null;
		}
		if (galleryFileName != null) {
			Button btnViewInGallery = findViewById(R.id.buttonViewInGallery);
			btnViewInGallery.setVisibility(View.VISIBLE);
			btnViewInGallery.setOnClickListener(v -> {
				ImageUtil.showFileInGallery(DisplayImageDetailsActivity.this, galleryFileName);
				returnResult(false, false);
			});
		}

		if (mFileType == FileType.IMAGE_FILE) {
			configureButtonsForImage();
		}

		if (mListName != null) {
			configureButtonsForImageList();
		}

		Button btnConfigure = findViewById(R.id.buttonConfigure);
		btnConfigure.setOnClickListener(v -> MainConfigurationActivity.startActivity(DisplayImageDetailsActivity.this));
	}

	@Override
	protected final void onResume() {
		super.onResume();
	}

	/**
	 * Configure the buttons related to a single image.
	 */
	private void configureButtonsForImage() {
		Button btnSendTo = findViewById(R.id.buttonSendTo);
		btnSendTo.setVisibility(View.VISIBLE);
		btnSendTo.setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_SEND);
			Uri uri = MediaStoreUtil.getUriFromFile(mFileName);
			if (uri == null) {
				return;
			}
			intent.putExtra(Intent.EXTRA_STREAM, uri);
			intent.setType("image/*");
			startActivity(intent);
			returnResult(false, false);
		});

		Button btnDisplayMap = findViewById(R.id.buttonShowOnMap);
		final Intent gpsIntent = getGpsIntent();
		if (gpsIntent != null) {
			btnDisplayMap.setVisibility(View.VISIBLE);
			btnDisplayMap.setOnClickListener(v -> {
				startActivity(gpsIntent);
				returnResult(false, false);
			});
		}


		if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			Button btnUseInWidget = findViewById(R.id.buttonUseInWidget);
			Button btnConfigureWidget = findViewById(R.id.buttonConfigureWidget);
			btnConfigureWidget.setVisibility(View.VISIBLE);
			final Intent settingsIntent;
			if (MiniWidget.hasWidgetOfId(mAppWidgetId)) {
				btnUseInWidget.setVisibility(View.VISIBLE);
				btnUseInWidget.setOnClickListener(v -> {
					PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_icon_image, mAppWidgetId, mFileName);
					MiniWidget.updateInstances(UpdateType.BUTTONS_BACKGROUND, mAppWidgetId);
					DialogUtil.displayToast(DisplayImageDetailsActivity.this, R.string.toast_widget_image_updated);
					returnResult(false, false);
				});
				settingsIntent = new Intent(this, MiniWidgetConfigurationActivity.class);
			}
			else if (ShortcutDummyWidget.hasWidgetOfId(mAppWidgetId)) {
				btnConfigureWidget.setText(R.string.menu_configure_shortcut);
				btnUseInWidget.setVisibility(View.VISIBLE);
				btnUseInWidget.setText(R.string.menu_display_in_shortcut);
				btnUseInWidget.setOnClickListener(v -> {
					PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_icon_image, mAppWidgetId, mFileName);
					DisplayListInfoActivity.updateShortcut(this, mAppWidgetId);
					DialogUtil.displayToast(DisplayImageDetailsActivity.this, R.string.toast_shortcut_image_updated);
					returnResult(false, false);
				});
				settingsIntent = new Intent(this, MiniWidgetConfigurationActivity.class);
			}
			else if (ImageWidget.hasWidgetOfId(mAppWidgetId)) {
				btnUseInWidget.setVisibility(View.VISIBLE);
				btnUseInWidget.setOnClickListener(v -> {
					PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_widget_current_file_name, mAppWidgetId, mFileName);
					ImageWidget.updateInstances(UpdateType.BUTTONS_BACKGROUND, mAppWidgetId);
					DialogUtil.displayToast(DisplayImageDetailsActivity.this, R.string.toast_widget_image_updated);
					returnResult(false, false);
				});
				settingsIntent = new Intent(this, ImageWidgetConfigurationActivity.class);
			}
			else {
				settingsIntent = new Intent(this, StackedImageWidgetConfigurationActivity.class);
			}
			btnConfigureWidget.setOnClickListener(v -> {
				settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				settingsIntent.putExtra(GenericWidgetConfigurationActivity.EXTRA_RECONFIGURE_WIDGET, true);
				returnResult(true, false);
				startActivity(settingsIntent);
				finish();
			});
		}

		if (mNotificationId >= 0) {
			Button btnConfigureNotification = findViewById(R.id.buttonConfigureNotification);
			btnConfigureNotification.setVisibility(View.VISIBLE);
			btnConfigureNotification.setOnClickListener(v -> {
				Intent settingsIntent = new Intent(this, NotificationConfigurationActivity.class);
				settingsIntent.putExtra(NotificationConfigurationFragment.STRING_NOTIFICATION_ID, mNotificationId);
				returnResult(true, false);
				startActivity(settingsIntent);
				finish();
			});
		}
	}

	/**
	 * Configure the buttons related to the image list.
	 */
	private void configureButtonsForImageList() {
		final StandardImageList imageList = ImageRegistry.getStandardImageListByName(mListName, false);
		if (imageList != null && imageList.contains(mFileNameInList)) {
			Button btnRemoveFromList = findViewById(R.id.buttonRemoveFromList);
			btnRemoveFromList.setVisibility(View.VISIBLE);
			btnRemoveFromList.setOnClickListener(v -> {
				final String filesString;
				if (mFileType == FileType.IMAGE_FILE) {
					filesString = DialogUtil.createFileFolderMessageString(null, null, Collections.singletonList(mFileName));
				}
				else {
					filesString = DialogUtil.createFileFolderMessageString(null, Collections.singletonList(mFileName), null);
				}

				DialogUtil.displayConfirmationMessage(DisplayImageDetailsActivity.this,
						new ConfirmDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog) {
								if (mFileType == FileType.FOLDER_SIMPLE) {
									imageList.remove(FOLDER, mFileNameInList);
									NotificationUtil.notifyUpdatedList(DisplayImageDetailsActivity.this, mListName, true,
											null, Collections.singletonList(mFileName), null);
								}
								else if (mFileType == FileType.FOLDER_RECURSIVE) {
									imageList.remove(FOLDER, mFileNameInList);
									NotificationUtil.notifyUpdatedList(DisplayImageDetailsActivity.this, mListName, true,
											null, Collections.singletonList(mFileName), null);
								}
								else {
									imageList.remove(FILE, mFileNameInList);
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
			});
		}

		if (mListName != null && !mPreventDisplayAll) {
			Button btnEditList = findViewById(R.id.buttonEditList);
			btnEditList.setVisibility(View.VISIBLE);
			btnEditList.setOnClickListener(v -> {
				ConfigureImageListActivity.startActivity(DisplayImageDetailsActivity.this, mListName, mAppWidgetId);
				returnResult(false, false);
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
	public static boolean getResultFinishParent(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res != null && res.getBoolean(STRING_RESULT_FINISH_PARENT);
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
	public static boolean getResultFileRemoved(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res != null && res.getBoolean(STRING_RESULT_FILE_REMOVED);
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
		final File file = new File(mFileName);

		TextView textViewFileName = findViewById(R.id.textViewFileName);
		textViewFileName.setText(file.getName());

		TextView textViewParentFolder = findViewById(R.id.textViewParentFolder);
		textViewParentFolder.setText(DialogUtil.fromHtml(getString(R.string.info_parent_folder, file.getParent())));

		TextView textViewImageDate = findViewById(R.id.textViewImageDate);
		Date imageDate = ImageUtil.getExifDate(mFileName);
		textViewImageDate.setText(DialogUtil.fromHtml(getString(R.string.info_file_date, DateUtil.format(imageDate))));

		final TextView textViewNumberOfImages = findViewById(R.id.textViewNumberOfImages);
		final View layoutConfigureViewFrequency = findViewById(R.id.layoutConfigureViewFrequency);
		final EditText editTextViewFrequency = findViewById(R.id.editTextViewFrequency);

		if (mFileType == FileType.FOLDER_SIMPLE || mFileType == FileType.FOLDER_RECURSIVE) {
			if (mListName != null && mListName.equals(ImageRegistry.getCurrentListName())) {
				new Thread() {
					@Override
					public void run() {
						final ImageList imageList = ImageRegistry.getCurrentImageList(false);
						final int imageCount = ImageUtil.getImagesInFolder(mFileNameInList).size();
						final double percentage = imageList.getPercentage(FOLDER, mFileNameInList);
						final double probability = imageList.getProbability(FOLDER, mFileNameInList);
						runOnUiThread(() -> {
							textViewNumberOfImages
									.setText(DialogUtil.fromHtml(getString(R.string.info_number_of_images_and_proportion,
											imageCount, FormattingUtil.getPercentageString(percentage))));
							Double customNestedListWeight = null;
							if (imageList instanceof StandardImageList) {
								customNestedListWeight = ((StandardImageList) imageList).getCustomNestedElementWeight(FOLDER, mFileNameInList);
							}
							if (customNestedListWeight == null) {
								editTextViewFrequency.setHint(FormattingUtil.getPercentageString(probability));
							}
							else {
								editTextViewFrequency.setText(FormattingUtil.getPercentageString(customNestedListWeight));
							}

							if (imageList instanceof StandardImageList) {
								ImageButton buttonSave = findViewById(R.id.button_save);
								buttonSave.setOnClickListener(v -> {
									try {
										((StandardImageList) imageList).setCustomWeight(
												FOLDER, mFileNameInList, FormattingUtil.getPercentageValue(editTextViewFrequency));
									}
									catch (NumberFormatException e) {
										// do not change weight
									}
									finish();
								});
							}
						});

					}
				}.start();
			}
			else {
				layoutConfigureViewFrequency.setVisibility(View.GONE);
				new Thread() {
					@Override
					public void run() {
						final int imageCount = ImageUtil.getImagesInFolder(mFileNameInList).size();
						runOnUiThread(() -> textViewNumberOfImages.setText(DialogUtil.fromHtml(getString(R.string.info_number_of_images, imageCount))));
					}
				}.start();
			}
		}
		else {
			textViewNumberOfImages.setVisibility(View.GONE);
			layoutConfigureViewFrequency.setVisibility(View.GONE);
		}
	}

	/**
	 * Get an intent for displaying the photo location on a map.
	 *
	 * @return The intent, if geo location is available, otherwise null.
	 */
	private Intent getGpsIntent() {
		try {
			ExifInterface exifInterface;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				Uri photoUri = MediaStoreUtil.getUriFromFile(mFileName);
				if (photoUri == null) {
					return null;
				}
				photoUri = MediaStore.setRequireOriginal(photoUri);
				InputStream stream = getContentResolver().openInputStream(photoUri);
				exifInterface = new ExifInterface(stream);
				stream.close();
			}
			else {
				exifInterface = new ExifInterface(mFileName);
			}

			double[] gpsCoordinates = exifInterface.getLatLong();

			if (gpsCoordinates == null || gpsCoordinates.length < 2 || (gpsCoordinates[0] == 0 && gpsCoordinates[1] == 0)) {
				return null;
			}

			String gpsString = String.format(Locale.ENGLISH, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
					gpsCoordinates[0], gpsCoordinates[1], gpsCoordinates[0], gpsCoordinates[1], getString(R.string.info_photo_location));
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(gpsString));

			if (intent.resolveActivity(getPackageManager()) != null) {
				return intent;
			}
			return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * The type of file for which details are displayed.
	 */
	private enum FileType {
		/**
		 * A file.
		 */
		IMAGE_FILE,
		/**
		 * A folder.
		 */
		FOLDER_SIMPLE,
		/**
		 * A recursive folder.
		 */
		FOLDER_RECURSIVE,
		/**
		 * Unknown file.
		 */
		UNKNOWN
	}

}
