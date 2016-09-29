package de.jeisfeld.randomimage;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import de.jeisfeld.randomimage.DisplayImageListAdapter.ItemType;
import de.jeisfeld.randomimage.DisplayImageListAdapter.SelectionMode;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimage.view.ThumbImageView;
import de.jeisfeld.randomimage.view.ThumbImageView.MarkingType;
import de.jeisfeld.randomimagelib.R;

/**
 * Activity to display a list of images from a folder.
 */
public abstract class DisplayImageListActivity extends StartActivity {
	/**
	 * Temporary storage for selected files.
	 */
	private ArrayList<String> mSelectedFiles;
	/**
	 * Temporary storage for selected folders.
	 */
	private ArrayList<String> mSelectedFolders;
	/**
	 * Temporary storage for selected lists.
	 */
	private ArrayList<String> mSelectedLists;

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
			mSelectedFiles = savedInstanceState.getStringArrayList("selectedFiles");
			mSelectedFolders = savedInstanceState.getStringArrayList("selectedFolders");
			mSelectedLists = savedInstanceState.getStringArrayList("selectedLists");
		}
	}

	@Override
	protected final void onResume() {
		super.onResume();
		TrackingUtil.sendScreen(this);
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
	 * @param listNames   The list names.
	 * @param folderNames The list of folders.
	 * @param fileNames   The list of image files.
	 * @param fixedThumbs Flag indicating if fixed thumbnail images should be used (for performance reasons)
	 */
	protected final void setAdapter(final List<String> listNames, final List<String> folderNames,
									final List<String> fileNames, final boolean fixedThumbs) {
		setAdapter(listNames, folderNames, fileNames, fixedThumbs, mSelectedLists, mSelectedFolders, mSelectedFiles);
		mSelectedFiles = null;
		mSelectedFolders = null;
		mSelectedLists = null;
	}

	/**
	 * Initialize the adapter.
	 *
	 * @param listNames       The list names.
	 * @param folderNames     The list of folders.
	 * @param fileNames       The list of image files.
	 * @param fixedThumbs     Flag indicating if fixed thumbnail images should be used (for performance reasons)
	 * @param selectedLists   The selected lists.
	 * @param selectedFolders The selected folders.
	 * @param selectedFiles   The selected files.
	 */
	protected final void setAdapter(final List<String> listNames, final List<String> folderNames,
									final List<String> fileNames, final boolean fixedThumbs,
									final List<String> selectedLists, final List<String> selectedFolders, final List<String> selectedFiles) {
		DisplayImageListAdapter adapter = new DisplayImageListAdapter(this, listNames, folderNames, fileNames, fixedThumbs);
		if (selectedFiles != null || selectedFolders != null || selectedLists != null) {
			adapter.setSelectedFiles(selectedFiles);
			adapter.setSelectedFolders(selectedFolders);
			adapter.setSelectedLists(selectedLists);
		}
		mAdapter = adapter;
		mGridView.setAdapter(mAdapter);
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
				imageView.invalidate();
				imageView.forceLayout();
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
			outState.putStringArrayList("selectedFiles", getAdapter().getSelectedFiles());
			outState.putStringArrayList("selectedFolders", getAdapter().getSelectedFolders());
			outState.putStringArrayList("selectedLists", getAdapter().getSelectedLists());
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
