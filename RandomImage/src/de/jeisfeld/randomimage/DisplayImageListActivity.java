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

/**
 * Activity to display a list of images from a folder.
 */
public abstract class DisplayImageListActivity extends Activity {
	/**
	 * Temporary storage for selected files.
	 */
	private String[] selectedFiles;
	/**
	 * Temporary storage for selected folders.
	 */
	private String[] selectedFolders;

	/**
	 * The view showing the photos.
	 */
	private GridView gridView;

	protected final GridView getGridView() {
		return gridView;
	}

	/**
	 * The view showing the name of the list or folder.
	 */
	private TextView textViewTitle;

	/**
	 * The adapter handling the list of images.
	 */
	private DisplayImageListArrayAdapter adapter;

	protected final DisplayImageListArrayAdapter getAdapter() {
		return adapter;
	}

	// OVERRIDABLE
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_images);
		gridView = (GridView) findViewById(R.id.gridViewDisplayImages);
		textViewTitle = (TextView) findViewById(R.id.textViewTitle);

		if (savedInstanceState != null) {
			selectedFiles = savedInstanceState.getStringArray("selectedFiles");
			selectedFolders = savedInstanceState.getStringArray("selectedFolders");
		}
	}

	/**
	 * Initialize the adapter.
	 *
	 * @param folderNames
	 *            The list of folders.
	 * @param fileNames
	 *            The list of image files.
	 */
	protected final void setAdapter(final ArrayList<String> folderNames, final ArrayList<String> fileNames) {
		adapter = new DisplayImageListArrayAdapter(this, folderNames, fileNames);
		getGridView().setAdapter(adapter);
		if (selectedFiles != null) {
			adapter.setSelectedFiles(new ArrayList<String>(Arrays.asList(selectedFiles)));
			adapter.setSelectedFolders(new ArrayList<String>(Arrays.asList(selectedFolders)));
			selectedFiles = null;
			selectedFolders = null;
		}
	}

	/**
	 * Initialize the adapter.
	 *
	 * @param title
	 *            The title displayed on top of the display.
	 */
	protected final void setTitle(final String title) {
		textViewTitle.setText(title);
	}

	// OVERRIDABLE
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (adapter != null) {
			adapter.cleanupCache();
		}
	}

	/**
	 * Set the markability status of all views in the grid.
	 *
	 * @param selectionMode
	 *            The markability status.
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
		}
	}

}
