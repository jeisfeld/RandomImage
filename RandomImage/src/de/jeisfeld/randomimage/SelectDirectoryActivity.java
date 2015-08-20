package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import de.jeisfeld.randomimage.DirectoryChooserDialogFragment.ChosenDirectoryListener;

/**
 * Add images sent to the app via intent.
 */
public class SelectDirectoryActivity extends Activity {
	/**
	 * The resource key for the flag telling if only folders should be queried.
	 */
	public static final String STRING_EXTRA_ONLY_FOLDER = "de.jeisfeld.randomimage.ONLY_FOLDER";
	/**
	 * The resource key for the returned folder.
	 */
	private static final String STRING_RESULT_FOLDER = "de.jeisfeld.randomimage.FOLDER";

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		boolean onlyFolder = getIntent().getBooleanExtra(STRING_EXTRA_ONLY_FOLDER, false);

		if (Intent.ACTION_GET_CONTENT.equals(getIntent().getAction()) && getIntent().getType() != null
				&& getIntent().getType().startsWith("image/") && onlyFolder) {
			DirectoryChooserDialogFragment.displayDirectoryChooserDialog(SelectDirectoryActivity.this,
					new ChosenDirectoryListener() {
						/**
						 * The serial version id.
						 */
						private static final long serialVersionUID = 1L;

						@Override
						public void onChosenDir(final String chosenDir) {
							Bundle resultData = new Bundle();
							resultData.putString(STRING_RESULT_FOLDER, chosenDir);
							Intent intent = new Intent();
							intent.putExtras(resultData);
							setResult(RESULT_OK, intent);
							finish();
						}

						@Override
						public void onCancelled() {
							finish();
						}
					}, null);
		}
		else {
			finish();
		}

	}

	/**
	 * Static helper method to extract the result folder.
	 *
	 * @param resultCode
	 *            The result code indicating if the response was successful.
	 * @param data
	 *            The activity response data.
	 * @return the result folder.
	 */
	public static final String getResultFolder(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			return data.getStringExtra(STRING_RESULT_FOLDER);
		}
		else {
			return null;
		}
	}

}
