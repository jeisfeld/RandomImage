package de.jeisfeld.randomimage.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.R;

/**
 * Utility class for storing and persisting the list of image names.
 */
public final class ImageRegistry {
	/**
	 * The folder where config files are stored.
	 */
	private static final File CONFIG_FILE_FOLDER = Application.getAppContext().getExternalFilesDir(null);

	/**
	 * The suffix for config files.
	 */
	private static final String CONFIG_FILE_SUFFIX = ".txt";

	/**
	 * The suffix for config files.
	 */
	private static final String CONFIG_FILE_PREFIX = "imageList ";

	/**
	 * Maximum allowed length of list names.
	 */
	private static final int MAX_NAME_LENGTH = 100;

	/**
	 * The singleton currentImageList of the imageRegistry.
	 */
	private static volatile ImageList currentImageList = null;

	/**
	 * A map from image list name to corresponding config file.
	 */
	private static Map<String, File> configFileMap = new HashMap<String, File>();

	static {
		checkConfigFiles();
	}

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
		String currentListName = PreferenceUtil.getSharedPreferenceString(R.string.key_current_list_name);

		if (currentImageList == null && currentListName != null) {
			switchToImageList(currentListName, false);
		}

		if (currentImageList == null && configFileMap.size() > 0) {
			String firstName = getImageListNames().get(0);
			switchToImageList(firstName, false);
		}

		if (currentImageList == null) {
			String newName = getNewListName();
			switchToImageList(newName, true);
		}
		return currentImageList;
	}

	/**
	 * Get the name of the current list.
	 *
	 * @return The name of the current list.
	 */
	public static String getCurrentListName() {
		return getCurrentImageList().getListName();
	}

	/**
	 * Get the names of all available image lists.
	 *
	 * @return The names of all available image lists.
	 */
	public static ArrayList<String> getImageListNames() {
		ArrayList<String> nameList = new ArrayList<String>(configFileMap.keySet());
		Collections.sort(nameList);
		return nameList;
	}

	/**
	 * Switch to the image list with the given name.
	 *
	 * @param name
	 *            The name of the target image list.
	 * @param create
	 *            Flag indicating if the list should be created if non-existing.
	 *
	 * @return true if successful.
	 */
	public static boolean switchToImageList(final String name, final boolean create) {
		if (name == null) {
			return false;
		}

		File configFile = getConfigFile(name);

		if (configFile == null) {
			if (create) {
				File newFile = getFileForListName(name);
				configFileMap.put(name, newFile);
				currentImageList = new ImageList(newFile);
				currentImageList.setListName(name);
				PreferenceUtil.setSharedPreferenceString(R.string.key_current_list_name, name);
				return true;
			}
			else {
				Log.w(Application.TAG, "Could not switch to non-existing file list " + name);
				return false;
			}
		}
		else {
			currentImageList = new ImageList(configFile);
			PreferenceUtil.setSharedPreferenceString(R.string.key_current_list_name, name);
			return true;
		}
	}

	/**
	 * Delete the image list of the given name.
	 *
	 * @param name
	 *            The name of the list to be deleted.
	 * @return true if successfully deleted.
	 */
	public static boolean deleteImageList(final String name) {
		File fileToBeDeleted = getConfigFile(name);

		if (fileToBeDeleted == null) {
			return false;
		}
		else {
			boolean success = fileToBeDeleted.delete();
			checkConfigFiles();
			return success;
		}
	}

	/**
	 * Rename the current list.
	 *
	 * @param newName
	 *            The new name.
	 * @return true if successful.
	 */
	public static boolean renameCurrentList(final String newName) {
		String currentName = getCurrentListName();
		File newConfigFile = getFileForListName(newName);
		boolean success = currentImageList.changeListName(newName, newConfigFile);
		if (success) {
			configFileMap.remove(currentName);
			configFileMap.put(newName, newConfigFile);
			PreferenceUtil.setSharedPreferenceString(R.string.key_current_list_name, newName);
		}
		return success;
	}

	/**
	 * Get the config file for a certain list name.
	 *
	 * @param name
	 *            The list name.
	 * @return The config file, if existing, otherwise null.
	 */
	private static File getConfigFile(final String name) {
		File configFile = configFileMap.get(name);

		if (configFile == null) {
			checkConfigFiles();
			configFile = configFileMap.get(name);
		}

		return configFile;
	}

	/**
	 * Get the list of available config files.
	 */
	private static void checkConfigFiles() {
		File[] configFiles = CONFIG_FILE_FOLDER.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return file.isFile() && file.getName().endsWith(CONFIG_FILE_SUFFIX);
			}
		});

		if (configFiles == null) {
			configFiles = new File[0];
		}
		configFileMap.clear();

		for (File configFile : configFiles) {
			String name = new ImageList(configFile).getListName();

			if (configFileMap.containsKey(name)) {
				Log.e(Application.TAG, "Duplicate config list name " + name);
			}
			else {
				configFileMap.put(name, configFile);
			}
		}
	}

	/**
	 * Get a new list name.
	 *
	 * @return A new list name.
	 */
	private static String getNewListName() {
		checkConfigFiles();

		String baseListName = Application.getResourceString(R.string.default_list_name);
		String listName = baseListName;

		int counter = 1;
		while (configFileMap.containsKey(listName)) {
			listName = baseListName + " (" + (++counter) + ")";
		}

		return listName;
	}

	/**
	 * Generate a file for a new list name.
	 *
	 * @param name
	 *            The list name.
	 * @return A file for this name.
	 */
	private static File getFileForListName(final String name) {
		String baseName =
				CONFIG_FILE_PREFIX
						+ name.replaceAll("[\\.]", ",")
								.replaceAll("[\\s]", " ")
								.replaceAll(
										"[^A-Za-z0-9äöüßÄÖÜáàéèíìóòúùÁÀÉÈÍÌÓÒÚÙ\\ \\-\\_\\%\\&\\?\\!\\$\\(\\)\\,\\;\\:]",
										"");
		if (baseName.length() > MAX_NAME_LENGTH) {
			baseName = baseName.substring(0, MAX_NAME_LENGTH);
		}
		baseName = baseName.trim();

		String fileName = baseName + CONFIG_FILE_SUFFIX;
		File resultFile = new File(CONFIG_FILE_FOLDER, fileName);

		int counter = 1;
		while (resultFile.exists()) {
			fileName = baseName + " (" + (++counter) + ")" + CONFIG_FILE_SUFFIX;
			resultFile = new File(CONFIG_FILE_FOLDER, fileName);
		}

		return resultFile;
	}

	/**
	 * Get the list name out of the file name (for the case that the list name is not stored there).
	 *
	 * @param file
	 *            the config file.
	 * @return the list name generated from the file name.
	 */
	protected static String getListNameFromFileName(final File file) {
		if (file == null) {
			return null;
		}

		String listName = file.getName();

		if (listName.endsWith(CONFIG_FILE_SUFFIX)) {
			listName = listName.substring(0, listName.length() - CONFIG_FILE_SUFFIX.length());
		}

		if (listName.startsWith(CONFIG_FILE_PREFIX)) {
			listName = listName.substring(CONFIG_FILE_PREFIX.length());
		}

		return listName;
	}

}
