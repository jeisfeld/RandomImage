package de.jeisfeld.randomimage;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.TextView;

/**
 * Activity to display a list of images from a folder.
 */
public abstract class DisplayImageListActivity extends Activity {
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
	private TextView textViewListName;

	/**
	 * The adapter handling the list of images.
	 */
	private DisplayAllImagesArrayAdapter adapter;

	protected final DisplayAllImagesArrayAdapter getAdapter() {
		return adapter;
	}

	// OVERRIDABLE
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_images);
		gridView = (GridView) findViewById(R.id.gridViewDisplayImages);
		textViewListName = (TextView) findViewById(R.id.textViewListName);
	}

	/**
	 * Initialize the adapter.
	 *
	 * @param folderNames
	 *            The list of folders.
	 * @param fileNames
	 *            The list of image files.
	 * @param listName
	 *            The display name of the list.
	 */
	protected final void setAdapter(final ArrayList<String> folderNames, final ArrayList<String> fileNames,
			final String listName) {
		adapter = new DisplayAllImagesArrayAdapter(this, folderNames, fileNames);
		getGridView().setAdapter(adapter);
		textViewListName.setText(listName);
	}

	// OVERRIDABLE
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (adapter != null) {
			adapter.cleanupCache();
		}
	}

	// OVERRIDABLE
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
	}

}
