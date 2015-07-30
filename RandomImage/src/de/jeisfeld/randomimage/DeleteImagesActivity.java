package de.jeisfeld.randomimage;

import de.jeisfeld.randomimage.util.ImageRegistry;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;

/**
 * The main activity of the app.
 */
public class DeleteImagesActivity extends Activity {
	/**
	 * The resource key for the array of filenames.
	 */
	private static final String STRING_EXTRA_FILENAMES = "de.eisfeldj.augendiagnose.FILENAMES";

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
	private DeleteImagesArrayAdapter adapter;

	/**
	 * Static helper method to start the activity, passing the list of files.
	 *
	 * @param activity
	 *            The activity starting this activity.
	 * @param fileNames
	 *            The list of image files.
	 */
	public static final void startActivity(final Activity activity, final String[] fileNames) {
		Intent intent = new Intent(activity, DeleteImagesActivity.class);
		intent.putExtra(STRING_EXTRA_FILENAMES, fileNames);
		activity.startActivity(intent);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_delete_images);

		fileNames = getIntent().getStringArrayExtra(STRING_EXTRA_FILENAMES);
		adapter = new DeleteImagesArrayAdapter(this, fileNames);

		if (savedInstanceState != null) {
			String[] selectedFiles = savedInstanceState.getStringArray("selectedFiles");
			adapter.setSelectedFiles(selectedFiles);
		}

		// Prepare the view
		gridView = (GridView) findViewById(R.id.gridViewDeleteimages);
		gridView.setAdapter(adapter);
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.delete_images, menu);
		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_delete_images) {
			ImageRegistry imageRegistry = ImageRegistry.getInstance();
			for (String fileName : adapter.getSelectedFiles()) {
				imageRegistry.remove(fileName);
			}
			imageRegistry.save();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (adapter != null) {
			outState.putStringArray("selectedFiles", adapter.getSelectedFiles());
		}
	}
}
