package de.jeisfeld.randomimage.view;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.SelectDirectoryActivity;

/**
 * A variant of ListPreference that allows to choose from a list of given images (configured in the menu configuration)
 * or to select a custom image via the directory browser.
 */
public class ImageSelectionPreference extends ListPreference {

	/**
	 * List value to represent a custom image to be chosen.
	 */
	private static final String CUSTOM_IMAGE = "__custom__";

	/**
	 * The selected index in the list.
	 */
	private int mSelectedIndex = -1;

	/**
	 * The custom directory selected via directory browser. Value is null if no custom directory is selected.
	 */
	private String mSelectedCustomImage = null;

	/**
	 * The list index of the custom directory.
	 */
	private int mCustomIndex = -1;

	/**
	 * The constructor replaces placeholders for external storage and camera folder.
	 *
	 * @param context The Context this is associated with.
	 * @param attrs   (from Preference) The attributes of the XML tag that is inflating the preference.
	 */
	public ImageSelectionPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		CharSequence[] entryValues = getEntryValues();

		// Update special values for external storage and camera folder
		for (int i = 0; i < entryValues.length; i++) {
			String value = entryValues[i].toString();

			if (value.equals(CUSTOM_IMAGE)) {
				mCustomIndex = i;
			}
		}
	}

	/**
	 * Standard constructor.
	 *
	 * @param context The Context this is associated with.
	 */
	public ImageSelectionPreference(final Context context) {
		this(context, null);
	}

	/**
	 * Create the dialog and prepare the creation of the directory selection dialog.
	 *
	 * @param builder The DialogBuilder to be customized.
	 */
	@Override
	protected final void onPrepareDialogBuilder(final Builder builder) {
		super.onPrepareDialogBuilder(builder);

		final CharSequence[] entries = getEntries();

		int clickedDialogEntryIndex = findIndexOfValue(getValue());

		if (clickedDialogEntryIndex < 0) {
			clickedDialogEntryIndex = mCustomIndex;
		}

		builder.setSingleChoiceItems(entries, clickedDialogEntryIndex, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				mSelectedCustomImage = null;

				if (getEntryValues()[which].toString().equals(CUSTOM_IMAGE)) {
					// determine custom folder via dialog
					ChosenImageListener listener = new ChosenImageListener() {
						@Override
						public void onChosenImage(final String chosenImage) {
							if (chosenImage == null) {
								ImageSelectionPreference.this.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
							}
							else {
								mSelectedIndex = which;
								mSelectedCustomImage = chosenImage;
								ImageSelectionPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
							}
							dialog.dismiss();
						}
					};

					try {
						((ChosenImageListenerActivity) getContext()).setChosenImageListener(listener);
						SelectDirectoryActivity.startActivity((Activity) getContext());
					}
					catch (ClassCastException e) {
						Log.e(Application.TAG, "Could not open image chooser", e);
					}
				}
				else {
					mSelectedIndex = which;
					setValueIndex(which);
					ImageSelectionPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
					dialog.dismiss();
				}
			}
		});

	}

	/**
	 * Fill the value after closing the dialog. This is mostly the same as in ListPreference, but takes special care in
	 * the case of custom folder.
	 *
	 * @param positiveResult (from DialogPreference) positiveResult Whether the positive button was clicked (true), or the negative
	 *                       button was clicked or the dialog was canceled (false).
	 */
	@Override
	protected final void onDialogClosed(final boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult && mSelectedIndex >= 0 && getEntryValues() != null) {
			String value;
			if (mSelectedCustomImage != null) {
				value = mSelectedCustomImage;
			}
			else {
				value = getEntryValues()[mSelectedIndex].toString();
			}
			if (callChangeListener(value)) {
				setValue(value);
				if (mSelectedCustomImage != null) {
					setSummary(value);
				}
				else {
					setSummary(getEntries()[mSelectedIndex].toString());
				}
			}
		}
	}

	/**
	 * Callback interface for selected image.
	 */
	public interface ChosenImageListener {
		/**
		 * Called when an image is selected.
		 *
		 * @param chosenImage The selected Image.
		 */
		void onChosenImage(String chosenImage);
	}

	/**
	 * Activity supporting ChosenImageListener.
	 */
	public interface ChosenImageListenerActivity {
		/**
		 * Set the listener for image selection.
		 *
		 * @param listener The listener.
		 */
		void setChosenImageListener(ChosenImageListener listener);
	}
}
