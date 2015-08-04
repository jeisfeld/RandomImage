package de.jeisfeld.randomimage.util;

import java.io.File;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.R;

/**
 * Utility class for storing and persisting the list of image names.
 */
public final class ImageRegistry {
	/**
	 * The name of the current config file where the list of files is stored.
	 */
	private static String currentConfigFileName = null;

	/**
	 * The folder where config files are stored.
	 */
	private static final File CONFIG_FILE_FOLDER = Application.getAppContext().getExternalFilesDir(null);

	/**
	 * The singleton currentImageList of the imageRegistry.
	 */
	private static volatile ImageList currentImageList = null;

	/**
	 * Hidden constructor.
	 */
	private ImageRegistry() {
	}

	/**
	 * Get an ImageRegistry currentImageList.
	 *
	 * @return An currentImageList.
	 */
	public static ImageList getCurrentImageList() {
		if (currentImageList == null) {
			if (currentConfigFileName == null) {
				currentConfigFileName =
						PreferenceUtil.getSharedPreferenceString(R.string.key_current_image_list,
								R.string.pref_default_current_image_list);
			}

			currentImageList = new ImageList(new File(CONFIG_FILE_FOLDER, currentConfigFileName));
		}
		return currentImageList;
	}

}
