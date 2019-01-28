package de.jeisfeld.randomimage.widgets;

import android.content.Intent;
import android.preference.PreferenceFragment;

import de.jeisfeld.randomimage.SelectDirectoryActivity;
import de.jeisfeld.randomimage.view.ImageSelectionPreference.ChosenImageListener;
import de.jeisfeld.randomimage.view.ImageSelectionPreference.ChosenImageListenerActivity;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class MiniWidgetConfigurationActivity extends GenericWidgetConfigurationActivity implements ChosenImageListenerActivity {
	/**
	 * A listener called when an image has been selected via ImageSelectionPreference.
	 */
	private ChosenImageListener mChosenImageListener = null;

	@Override
	protected final void configure(final int appWidgetId, final String listName) {
		MiniWidget.configure(appWidgetId, listName);
	}

	@Override
	protected final PreferenceFragment createFragment() {
		return new MiniWidgetConfigurationFragment();
	}

	@Override
	public final void setChosenImageListener(final ChosenImageListener listener) {
		mChosenImageListener = listener;
	}

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case SelectDirectoryActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				String selectedImage = SelectDirectoryActivity.getSelectedImage(resultCode, data);
				if (mChosenImageListener != null) {
					mChosenImageListener.onChosenImage(selectedImage);
				}
			}
			break;
		default:
			break;
		}
	}
}
