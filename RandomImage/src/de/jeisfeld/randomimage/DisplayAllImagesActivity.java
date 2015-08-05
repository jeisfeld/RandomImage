package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import de.jeisfeld.randomimage.DisplayAllImagesArrayAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * Activity to display the list of images configured for this app.
 */
public class DisplayAllImagesActivity extends Activity {
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
		setContentView(R.layout.activity_display_images);

		gridView = (GridView) findViewById(R.id.gridViewDeleteimages);

		fillListOfImages();

		if (savedInstanceState != null) {
			String[] selectedFiles = savedInstanceState.getStringArray("selectedFiles");
			adapter.setSelectedFiles(selectedFiles);
			currentAction = (CurrentAction) savedInstanceState.getSerializable("currentAction");
			changeAction(currentAction);
		}
	}

	/**
	 * Fill the view with the current list of images.
	 */
	private void fillListOfImages() {
		fileNames = ImageRegistry.getCurrentImageList().getFileNames();
		adapter = new DisplayAllImagesArrayAdapter(this, fileNames);
		gridView.setAdapter(adapter);
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
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
			return onOptionsItemSelectedDisplay(id);
		case DELETE:
			return onOptionsItemSelectedDelete(id);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Handler for options selected while in display mode.
	 *
	 * @param menuId
	 *            The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemSelectedDisplay(final int menuId) {
		switch (menuId) {
		case R.id.action_select_images_for_removal:
			changeAction(CurrentAction.DELETE);
			DialogUtil.displayInfo(this, null, R.string.dialog_info_delete_images, R.string.key_info_delete_images);
			return true;
		case R.id.action_add_images:
			AddImagesFromGalleryActivity.startActivity(this);
			return true;
		case R.id.action_backup_list:
			// TODO
			return true;
		case R.id.action_restore_list:
			// TODO
			return true;
		case R.id.action_rename_list:
			// TODO
			return true;
		case R.id.action_clone_list:
			// TODO
			return true;
		case R.id.action_create_list:
			// TODO
			return true;
		case R.id.action_switch_list:
			// TODO
			return true;
		default:
			return false;
		}
	}

	/**
	 * Handler for options selected while in delete mode.
	 *
	 * @param menuId
	 *            The selected menu item.
	 * @return true if menu item was consumed.
	 */
	private boolean onOptionsItemSelectedDelete(final int menuId) {
		switch (menuId) {
		case R.id.action_remove_images:
			ImageList imageList = ImageRegistry.getCurrentImageList();
			int removedFileCount = 0;
			for (String fileName : adapter.getSelectedFiles()) {
				boolean isRemoved = imageList.remove(fileName);
				if (isRemoved) {
					removedFileCount++;
				}
			}
			if (removedFileCount > 0) {
				DialogUtil.displayToast(this, R.string.toast_removed_images_count, removedFileCount);
				imageList.save();
			}
			fillListOfImages();
			changeAction(CurrentAction.DISPLAY);
			return true;
		case R.id.action_cancel:
			changeAction(CurrentAction.DISPLAY);
			return true;
		default:
			return false;
		}
	}

	/**
	 * Change the action within this activity.
	 *
	 * @param action
	 *            the new action.
	 */
	private void changeAction(final CurrentAction action) {
		if (action != null) {
			currentAction = action;
			setMarkabilityStatus(action == CurrentAction.DELETE);
			invalidateOptionsMenu();
		}
	}

	/**
	 * Set the markability status of all views in the grid.
	 *
	 * @param markable
	 *            The markability status.
	 */
	private void setMarkabilityStatus(final boolean markable) {
		adapter.setSelectionMode(markable ? SelectionMode.MULTIPLE : SelectionMode.NONE);

		for (int i = 0; i < gridView.getChildCount(); i++) {
			View imageView = gridView.getChildAt(i);
			if (imageView instanceof ThumbImageView) {
				((ThumbImageView) imageView).setMarkable(markable);
			}
		}
		for (ThumbImageView view : adapter.getCachedImages()) {
			view.setMarkable(markable);
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
		if (requestCode == AddImagesFromGalleryActivity.REQUEST_CODE) {
			int addedImagesCount = AddImagesFromGalleryActivity.getResult(resultCode, data);
			if (addedImagesCount > 0) {
				DialogUtil.displayToast(this, R.string.toast_added_images_count, addedImagesCount);
				fillListOfImages();
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
