package de.jeisfeld.randomimage;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * The main activity of the app.
 */
public class MainActivity extends Activity {
	/**
	 * The name of the displayed file.
	 */
	private String displayFileName;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			displayFileName = savedInstanceState.getString("displayFileName");

			if (displayFileName != null) {
				displayImage(displayFileName);
				return;
			}
		}

		ImageRegistry imageRegistry = ImageRegistry.getInstance();
		imageRegistry.load();
		Intent intent = getIntent();
		if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
			// Application was started from other application by passing one image
			Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
			imageRegistry.add(imageUri);
			imageRegistry.save();
		}
		else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) && intent.getType() != null
				&& intent.hasExtra(Intent.EXTRA_STREAM)) {
			// Application was started from other application by passing a list of images
			ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (imageUris != null) {
				for (int i = 0; i < imageUris.size(); i++) {
					imageRegistry.add(imageUris.get(i));
				}
				ImageRegistry.getInstance().save();
			}
		}
		else if (Intent.ACTION_MAIN.equals(intent.getAction()) && savedInstanceState == null) {
			// Application was started from launcher
			Log.d(Application.TAG, "Launched application");
		}

		displayImage(imageRegistry.getRandomFileName());
	}

	/**
	 * Display an Image on this view.
	 *
	 * @param fileName
	 *            The image file.
	 */
	private void displayImage(final String fileName) {
		displayFileName = fileName;

		PinchImageView imageView = new PinchImageView(this);
		setContentView(imageView);
		imageView.setImage(displayFileName, this, 1);
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (displayFileName != null) {
			outState.putString("displayFileName", displayFileName);
		}
	}

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		DisplayAllImagesActivity.startActivity(this);
		return true;
	}

}
