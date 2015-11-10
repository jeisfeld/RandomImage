package de.jeisfeld.randomimage;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import de.jeisfeld.randomimage.DisplayImageListArrayAdapter.SelectionMode;
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
	 * The view showing the name of the list or folder.
	 */
	private TextView mTextViewTitle;

	/**
	 * The adapter handling the list of images.
	 */
	private DisplayImageListArrayAdapter mAdapter;

	protected final DisplayImageListArrayAdapter getAdapter() {
		return mAdapter;
	}

	// OVERRIDABLE
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_images);
		mGridView = (GridView) findViewById(R.id.gridViewDisplayImages);
		mTextViewTitle = (TextView) findViewById(R.id.textViewTitle);

		if (savedInstanceState != null) {
			mSelectedFiles = savedInstanceState.getStringArray("selectedFiles");
			mSelectedFolders = savedInstanceState.getStringArray("selectedFolders");
			mSelectedLists = savedInstanceState.getStringArray("selectedLists");
		}
	}

	/**
	 * Initialize the adapter.
	 *
	 * @param nestedListNames The nested list names.
	 * @param folderNames     The list of folders.
	 * @param fileNames       The list of image files.
	 */
	protected final void setAdapter(final ArrayList<String> nestedListNames, final ArrayList<String> folderNames,
									final ArrayList<String> fileNames) {
		mAdapter = new DisplayImageListArrayAdapter(this, nestedListNames, folderNames, fileNames);
		getGridView().setAdapter(mAdapter);
		if (mSelectedFiles != null) {
			mAdapter.setSelectedFiles(new ArrayList<>(Arrays.asList(mSelectedFiles)));
			mAdapter.setSelectedFolders(new ArrayList<>(Arrays.asList(mSelectedFolders)));
			mAdapter.setSelectedNestedLists(new ArrayList<>(Arrays.asList(mSelectedLists)));
			mSelectedFiles = null;
			mSelectedFolders = null;
			mSelectedLists = null;
		}
	}

	/**
	 * Initialize the adapter.
	 *
	 * @param title The title displayed on top of the display.
	 */
	protected final void setTitle(final String title) {
		mTextViewTitle.setText(title);
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
		MarkingType markingType = DisplayImageListArrayAdapter.getMarkingTypeFromSelectionMode(selectionMode);

		for (int i = 0; i < getGridView().getChildCount(); i++) {
			View imageView = getGridView().getChildAt(i);
			if (imageView instanceof ThumbImageView) {
				((ThumbImageView) imageView).setMarkable(markingType);
			}
		}
	}

	// OVERRIDABLE
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (getAdapter() != null) {
			outState.putStringArray("selectedFiles", getAdapter().getSelectedFiles().toArray(new String[0]));
			outState.putStringArray("selectedFolders", getAdapter().getSelectedFolders().toArray(new String[0]));
			outState.putStringArray("selectedLists", getAdapter().getSelectedNestedLists().toArray(new String[0]));
		}
	}

}
