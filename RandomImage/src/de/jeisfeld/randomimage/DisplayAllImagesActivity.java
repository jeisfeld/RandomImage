package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import de.jeisfeld.randomimage.DisplayAllImagesArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * The main activity of the app.
 */
public class DisplayAllImagesActivity extends Activity {
	/**
	 * Request code for getting images from gallery.
	 */
	private static final int REQUEST_CODE_GET_IMAGES = 1;

	/**
	 * The names of the files to be displayed.
	 */
	private String[] fileNames;

	/**
	 * The view showing the photos.
	 */
	private GridView gridView;

	/**
	 * The adapter handling the list of images.
	 */
	private DisplayAllImagesArrayAdapter adapter;

	/**
	 * The current action within this activity.
	 */
	private CurrentAction currentAction = CurrentAction.DISPLAY;

	/**
	 * Flag indicating if the list of pictures needs to be refreshed.
	 */
	private boolean needsRefresh = false;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 */
	public static final void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, DisplayAllImagesActivity.class);
		activity.startActivity(intent);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_delete_images);

		gridView = (GridView) findViewById(R.id.gridViewDeleteimages);

		fillListOfImages();

		if (savedInstanceState != null) {
			String[] selectedFiles = savedInstanceState.getStringArray("selectedFiles");
			adapter.setSelectedFiles(selectedFiles);
			currentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
			if (currentAction == CurrentAction.DELETE) {
				adapter.setSelectionMode(SelectionMode.MULTIPLE);
			}
		}
	}

	/**
	 * Fill the view with the current list of images.
	 */
	private void fillListOfImages() {
		fileNames = ImageRegistry.getInstance().getFileNames();
		adapter = new DisplayAllImagesArrayAdapter(this, fileNames);
		gridView.setAdapter(adapter);
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		return onPrepareOptionsMenu(menu);
	}

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		menu.clear();
		switch (currentAction) {
		case DISPLAY:
			getMenuInflater().inflate(R.menu.display_images, menu);
			return true;
		case DELETE:
			getMenuInflater().inflate(R.menu.delete_images, menu);
			return true;
		default:
			return false;
		}
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		int id = item.getItemId();

		switch (currentAction) {
		case DISPLAY:
			if (id == R.id.action_select_images_for_removal) {
				currentAction = CurrentAction.DELETE;
				adapter.setSelectionMode(SelectionMode.MULTIPLE);
			}
			if (id == R.id.action_add_images) {
				needsRefresh = false;
				DialogUtil.displayInfo(this, new MessageDialogListener() {
					/**
					 * The serial version uid.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void onDialogFinished() {
						triggerAddImage();
					}
				}, R.string.dialog_info_add_images, R.string.key_info_add_images);
			}
			break;
		case DELETE:
			if (id == R.id.action_remove_images) {
				ImageRegistry imageRegistry = ImageRegistry.getInstance();
				int removedFileCount = 0;
				for (String fileName : adapter.getSelectedFiles()) {
					boolean isRemoved = imageRegistry.remove(fileName);
					if (isRemoved) {
						removedFileCount++;
					}
				}
				if (removedFileCount > 0) {
					DialogUtil.displayToast(this, R.string.toast_removed_picture_count, removedFileCount);
					imageRegistry.save();
				}
				fillListOfImages();
				currentAction = CurrentAction.DISPLAY;
				adapter.setSelectionMode(SelectionMode.NONE);
			}
			else if (id == R.id.action_cancel) {
				unselectAllImages();
				currentAction = CurrentAction.DISPLAY;
				adapter.setSelectionMode(SelectionMode.NONE);
			}
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Trigger an intent for getting an image for addition.
	 */
	private void triggerAddImage() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, REQUEST_CODE_GET_IMAGES);
	}

	/**
	 * Unselect all images.
	 */
	private void unselectAllImages() {
		adapter.setSelectedFiles(null);
		for (int i = 0; i < gridView.getChildCount(); i++) {
			View imageView = gridView.getChildAt(i);
			if (imageView instanceof ThumbImageView) {
				((ThumbImageView) imageView).setMarked(false);
			}
		}
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("currentAction", currentAction);
		if (adapter != null) {
			outState.putStringArray("selectedFiles", adapter.getSelectedFiles());
		}
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == REQUEST_CODE_GET_IMAGES) {
			if (resultCode == RESULT_OK) {
				Uri selectedImageUri = data.getData();
				String addedFileName = ImageRegistry.getInstance().add(selectedImageUri);
				if (addedFileName != null) {
					DialogUtil.displayToast(this, R.string.toast_added_picture, addedFileName);
					needsRefresh = true;
				}
				triggerAddImage();
			}
			else {
				// Finally, refresh list of images
				if (needsRefresh) {
					ImageRegistry.getInstance().save();
					fillListOfImages();
				}
			}
		}
	}

	/**
	 * The current action in this activity.
	 */
	public enum CurrentAction {
		/**
		 * Just display photos.
		 */
		DISPLAY,
		/**
		 * Delete photos.
		 */
		DELETE
	}

}
