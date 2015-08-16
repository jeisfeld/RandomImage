package de.jeisfeld.randomimage;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.RandomFileProvider;
import de.jeisfeld.randomimage.view.PinchImageView;

/**
 * Display a random image.
 */
public class DisplayRandomImageActivity extends Activity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 2;
	/**
	 * The resource key for the image list.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";
	/**
	 * The resource key for the image folder.
	 */
	private static final String STRING_EXTRA_FOLDERNAME = "de.jeisfeld.randomimage.FOLDERNAME";
	/**
	 * The resource key for the file name.
	 */
	public static final String STRING_EXTRA_FILENAME = "de.jeisfeld.randomimage.FILENAME";
	/**
	 * The resource key for the flat indicating if it should be prevented to trigger the DisplayAllImagesActivity.
	 */
	private static final String STRING_EXTRA_PREVENT_DISPLAY_ALL = "de.jeisfeld.randomimage.PREVENT_DISPLAY_ALL";
	/**
	 * The resource key for the flag if the parent activity should be refreshed.
	 */
	private static final String STRING_RESULT_REFRESH_PARENT = "de.jeisfeld.randomimage.REFRESH_PARENT";

	/**
	 * The name of the used image list.
	 */
	private String listName;

	/**
	 * The name of the displayed file.
	 */
	private String currentFileName;

	/**
	 * The name of the previously displayed file.
	 */
	private String lastFileName = null;

	/**
	 * The gesture detector used by this activity.
	 */
	private GestureDetector gestureDetector;

	/**
	 * The direction of the last fling movement.
	 */
	private FlingDirection lastflingDirection = null;

	/**
	 * flag indicating if the activity should prevent to trigger DisplayAllImagesActivity.
	 */
	private boolean preventDisplayAll;

	/**
	 * The imageList used by the activity.
	 */
	private RandomFileProvider randomFileProvider;

	/**
	 * Static helper method to create an intent for this activity.
	 *
	 * @param context
	 *            The context in which this activity is started.
	 * @param listName
	 *            the image list which should be taken.
	 * @param fileName
	 *            the image file name which should be displayed first.
	 * @param preventDisplayAll
	 *            flag indicating if the activity should prevent to trigger DisplayAllImagesActivity.
	 * @return the intent.
	 */
	public static final Intent createIntent(final Context context, final String listName, final String fileName,
			final boolean preventDisplayAll) {
		Intent intent = new Intent(context, DisplayRandomImageActivity.class);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		if (fileName != null) {
			intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		}
		intent.putExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, preventDisplayAll);

		if (preventDisplayAll) {
			intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		}
		else {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		}
		return intent;
	}

	/**
	 * Static helper method to start the activity for the contents of an image folder.
	 *
	 * @param context
	 *            The context starting this activity.
	 * @param folderName
	 *            the name of the folder whose images should be displayed.
	 * @param fileName
	 *            the name of the file that should be displayed first
	 *
	 */
	public static final void startActivityForFolder(final Context context, final String folderName,
			final String fileName) {
		Intent intent = new Intent(context, DisplayRandomImageActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		intent.putExtra(STRING_EXTRA_FOLDERNAME, folderName);
		intent.putExtra(STRING_EXTRA_FILENAME, fileName);
		intent.putExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, true);
		context.startActivity(intent);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		createGestureDetector();

		if (savedInstanceState != null) {
			listName = savedInstanceState.getString("listName");
			currentFileName = savedInstanceState.getString("currentFileName");
			preventDisplayAll = savedInstanceState.getBoolean("preventDisplayAll");
		}

		if (listName == null) {
			listName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		}
		if (currentFileName == null) {
			currentFileName = getIntent().getStringExtra(STRING_EXTRA_FILENAME);
		}

		String folderName = getIntent().getStringExtra(STRING_EXTRA_FOLDERNAME);

		preventDisplayAll = getIntent().getBooleanExtra(STRING_EXTRA_PREVENT_DISPLAY_ALL, false);

		if (folderName != null) {
			// If folderName is provided, then use the list of images in this folder.
			randomFileProvider = new FolderRandomFileProvider(folderName, currentFileName);
		}
		else {
			// Otherwise, use the imageList.
			ImageList imageList;
			if (listName == null) {
				listName = ImageRegistry.getCurrentListName();
				imageList = ImageRegistry.getCurrentImageList();
				if (currentFileName == null) {
					// Reload the file when starting the app.
					imageList.load();
				}
			}
			else {
				imageList = ImageRegistry.getImageListByName(listName);
				if (imageList == null) {
					Log.e(Application.TAG, "Could not load image list");
					DialogUtil.displayToast(this, R.string.toast_error_while_loading, listName);
					return;
				}
			}
			randomFileProvider = imageList;
		}

		if (currentFileName == null) {
			boolean success = displayRandomImage();
			if (success) {
				DialogUtil.displayInfo(this, null, R.string.key_info_display_image, R.string.dialog_info_display_image);
			}
		}
		else {
			displayCurrentImage();
		}

		PreferenceUtil.incrementCounter(R.string.key_statistics_countdisplayrandom);

		test();
	}

	/**
	 * Display the current Image defined by currentFileName on this view.
	 */
	private void displayCurrentImage() {
		PinchImageView imageView = new PinchImageView(this);
		imageView.setGestureDetector(gestureDetector);
		setContentView(imageView);
		imageView.setImage(currentFileName, this, 1);
	}

	/**
	 * Display a random image.
	 *
	 * @return false if there was no image to be displayed.
	 */
	private boolean displayRandomImage() {
		lastFileName = currentFileName;
		currentFileName = randomFileProvider.getRandomFileName();
		if (currentFileName == null) {
			DisplayAllImagesActivity.startActivity(this, listName);
			finish();
			return false;
		}
		else {
			displayCurrentImage();
			return true;
		}
	}

	/**
	 * Create the gesture detector handling flinging and double tapping.
	 */
	private void createGestureDetector() {
		gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			/**
			 * The speed which is accepted as fling.
			 */
			private static final int FLING_SPEED = 3000;

			@Override
			public boolean onDoubleTap(final MotionEvent e) {
				if (preventDisplayAll) {
					if (listName != null) {
						finish();
					}
				}
				else {
					DisplayAllImagesActivity.startActivity(DisplayRandomImageActivity.this, listName);
				}
				return true;
			}

			@Override
			public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX,
					final float velocityY) {
				if (Math.abs(velocityX) + Math.abs(velocityY) > FLING_SPEED) {
					FlingDirection newFlingDirection = new FlingDirection(velocityX, velocityY);
					if (newFlingDirection.isOpposite(lastflingDirection) && lastFileName != null) {
						String tempFileName = currentFileName;
						currentFileName = lastFileName;
						lastFileName = tempFileName;
						displayCurrentImage();
					}
					else {
						displayRandomImage();
					}
					lastflingDirection = newFlingDirection;
					PreferenceUtil.incrementCounter(R.string.key_statistics_countfling);
					return true;
				}
				else {
					return false;
				}
			}

			@Override
			public void onLongPress(final MotionEvent e) {
				DisplayImageDetailsActivity.startActivity(DisplayRandomImageActivity.this, currentFileName, listName,
						preventDisplayAll);
			}

		});
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (currentFileName != null) {
			outState.putString("currentFileName", currentFileName);
			outState.putString("listName", listName);
			outState.putBoolean("preventDisplayAll", preventDisplayAll);
		}
	}

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		DisplayImageDetailsActivity.startActivity(this, currentFileName, listName, preventDisplayAll);
		return true;
	}

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case DisplayImageDetailsActivity.REQUEST_CODE:
			boolean finishParent = DisplayImageDetailsActivity.getResultFinishParent(resultCode, data);
			boolean fileRemoved = DisplayImageDetailsActivity.getResultFileRemoved(resultCode, data);
			if (finishParent) {
				returnResult(fileRemoved);
			}
			else if (fileRemoved) {
				displayRandomImage();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Static helper method to extract the result flag.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the flag if the parent activity should be refreshed.
	 */
	public static final boolean getResult(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res.getBoolean(STRING_RESULT_REFRESH_PARENT);
		}
		else {
			return false;
		}
	}

	/**
	 * Helper method: Return the flag if the parent activity should be finished.
	 *
	 * @param refreshParent
	 *            The flag if the parent activity should be refreshed.
	 */
	private void returnResult(final boolean refreshParent) {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_REFRESH_PARENT, refreshParent);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Method for temporary tests.
	 */
	private void test() {
	}

	/**
	 * A class allowing to select a random file name out of an image folder.
	 */
	private final class FolderRandomFileProvider implements RandomFileProvider {
		/**
		 * The list of files in the folder, being the base of the provider.
		 */
		private ArrayList<String> fileNames;

		/**
		 * The file name returned if there is no image file in the folder.
		 */
		private String defaultFileName;

		/**
		 * Constructor initializing with the folder name.
		 *
		 * @param folderName
		 *            The folder name.
		 * @param defaultFileName
		 *            The file name returned if there is no image file in the folder.
		 */
		private FolderRandomFileProvider(final String folderName, final String defaultFileName) {
			fileNames = ImageUtil.getImagesInFolder(folderName);
			this.defaultFileName = defaultFileName;
		}

		@Override
		public String getRandomFileName() {
			if (fileNames.size() > 0) {
				return fileNames.get(new Random().nextInt(fileNames.size()));
			}
			else {
				return defaultFileName;
			}
		}

	}

	/**
	 * A class holding the direction of a fling movement.
	 */
	private final class FlingDirection {
		/**
		 * The x velocity of the movement.
		 */
		private float velocityX;
		/**
		 * The y velocity of the movement.
		 */
		private float velocityY;

		/**
		 * Constructor.
		 *
		 * @param velocityX
		 *            The x velocity of the movement.
		 * @param velocityY
		 *            The y velocity of the movement.
		 */
		private FlingDirection(final float velocityX, final float velocityY) {
			this.velocityX = velocityX;
			this.velocityY = velocityY;
		}

		/**
		 * Get the direction of the fling as angle.
		 *
		 * @return the angle of the movement.
		 */
		private double getAngle() {
			double angle;
			if (velocityX > 0) {
				angle = Math.atan(velocityY / velocityX);
			}
			else if (velocityX < 0) {
				angle = Math.PI + Math.atan(velocityY / velocityX);
			}
			else if (velocityY > 0) {
				angle = Math.PI / 2;
			}
			else {
				angle = -Math.PI / 2;
			}
			if (angle < 0) {
				angle = angle + 2 * Math.PI;
			}
			return angle;
		}

		/**
		 * Find out if two flings go into opposite direction.
		 *
		 * @param otherDirection
		 *            The fling to be compared with.
		 * @return true if both go into opposite direction.
		 */
		private boolean isOpposite(final FlingDirection otherDirection) {
			if (otherDirection == null) {
				return false;
			}
			double angleDiff = Math.abs(getAngle() - otherDirection.getAngle());
			return angleDiff > Math.PI / 2 && angleDiff < 3 * Math.PI / 2; // MAGIC_NUMBER
		}
	}

}
