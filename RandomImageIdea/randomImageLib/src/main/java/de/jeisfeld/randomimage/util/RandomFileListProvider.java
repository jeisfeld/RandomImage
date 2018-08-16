package de.jeisfeld.randomimage.util;

import android.os.Parcelable;

/**
 * An interface that may provide random files as a list.
 */
public interface RandomFileListProvider extends RandomFileProvider, Parcelable {
	/**
	 * Get the current file name.
	 *
	 * @return The current file name of the list.
	 */
	String getCurrentFileName();

	/**
	 * Go one file forward.
	 */
	void goForward();

	/**
	 * Go one file backward.
	 */
	void goBackward();

	/**
	 * Remove the current file from the list.
	 *
	 * @return The current file name of the list, after removing the old one.
	 */
	String removeCurrentFileName();
}
