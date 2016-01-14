package de.jeisfeld.randomimage;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import de.jeisfeld.randomimage.DisplayImageListAdapter.ItemType;
import de.jeisfeld.randomimage.DisplayImageListAdapter.SelectionMode;
import de.jeisfeld.randomimage.view.ThumbImageView;
import de.jeisfeld.randomimage.view.ThumbImageView.MarkingType;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display a list of images from a folder.
 */
public abstract class DisplayImageListActivity extends Activity {
	/**
	 * Temporary storage for selected files.
	 */
	private String[] mSelectedFiles;
	/**
	 * Temporary storage for selected folders.
	 */
	private String[] mSelectedFolders;
	/**
	 * Temporary storage for selected lists.
	 */
	private String[] mSelectedLists;

	/**
	 * The view showing the photos.
	 */
	private GridView mGridView;

	protected final GridView getGridView() {
		return mGridView;
	}

	/**
	 * The adapter handling the list of images.
	 */
	private DisplayImageListAdapter mAdapter;

	protected final DisplayImageListAdapter getAdapter() {
		return mAdapter;
	}

	// OVERRIDABLE
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutId());
		mGridView = (GridView) findViewById(R.id.gridViewDisplayImages);

		if (savedInstanceState != null) {
			mSelectedFiles = savedInstanceState.getStringArray("selectedFiles");
			mSelectedFolders = savedInstanceState.getStringArray("selectedFolders");
			mSelectedLists = savedInstanceState.getStringArray("selectedLists");
		}
	}

	/**
	 * Get the layout used for the activity.
	 *
	 * @return The layout id.
	 */
	protected abstract int getLayoutId();

	/**
	 * Initialize the adapter.
	 *
	 * @param nestedListNames The nested list names.
	 * @param folderNames     The list of folders.
	 * @param fileNames       The list of image files.
	 * @param fixedThumbs     Flag indicating if fixed thumbnail images should be used (for performance reasons)
	 */
	protected final void setAdapter(final List<String> nestedListNames, final List<String> folderNames,
									final List<String> fileNames, final boolean fixedThumbs) {
		DisplayImageListAdapter adapter = new DisplayImageListAdapter(this, nestedListNames, folderNames, fileNames, fixedThumbs);
		if (mSelectedFiles != null || mSelectedFolders != null || mSelectedLists != null) {
			adapter.setSelectedFiles(mSelectedFiles == null ? null : Arrays.asList(mSelectedFiles));
			adapter.setSelectedFolders(mSelectedFolders == null ? null : Arrays.asList(mSelectedFolders));
			adapter.setSelectedNestedLists(mSelectedLists == null ? null : Arrays.asList(mSelectedLists));
			mSelectedFiles = null;
			mSelectedFolders = null;
			mSelectedLists = null;
		}
		mAdapter = adapter;

		getGridView().setAdapter(mAdapter);
	}

	// OVERRIDABLE
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			mAdapter.cleanupCache();
		}
	}

	/**
	 * Set the markability status of all views in the grid.
	 *
	 * @param selectionMode The markability status.
	 */
	protected final void setSelectionMode(final SelectionMode selectionMode) {
		getAdapter().setSelectionMode(selectionMode);
		MarkingType markingType = DisplayImageListAdapter.getMarkingTypeFromSelectionMode(selectionMode);

		for (int i = 0; i < getGridView().getChildCount(); i++) {
			View imageView = getGridView().getChildAt(i);
			if (imageView instanceof ThumbImageView) {
				((ThumbImageView) imageView).setMarkable(markingType);
			}
		}
	}

	/**
	 * If in selection mode, select all images. If all images are selected, then deselect all images.
	 */
	protected final void toggleSelectAll() {
		boolean markingStatus = getAdapter().toggleSelectAll();
		for (int i = 0; i < getGridView().getChildCount(); i++) {
			View imageView = getGridView().getChildAt(i);
			if (imageView instanceof ThumbImageView) {
				((ThumbImageView) imageView).setMarked(markingStatus);
			}
		}
	}

	// OVERRIDABLE
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (getAdapter() != null) {
			String[] emptyArray = {};
			outState.putStringArray("selectedFiles", getAdapter().getSelectedFiles().toArray(emptyArray));
			outState.putStringArray("selectedFolders", getAdapter().getSelectedFolders().toArray(emptyArray));
			outState.putStringArray("selectedLists", getAdapter().getSelectedNestedLists().toArray(emptyArray));
		}
	}

	/**
	 * This method determines what should happen when clicking on an item in the displayed list if in Selection mode ONE.
	 *
	 * @param itemType The item type (list, folder or file)
	 * @param name     The name of the item.
	 */
	public abstract void onItemClick(ItemType itemType, String name);

	/**
	 * This method determines what should happen when clicking on an item in the displayed list if in Selection mode ONE.
	 *
	 * @param itemType The item type (list, folder or file)
	 * @param name     The name of the item.
	 */
	public abstract void onItemLongClick(ItemType itemType, String name);

}
