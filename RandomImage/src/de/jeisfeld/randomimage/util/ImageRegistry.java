package de.jeisfeld.randomimage.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.os.Environment;
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
	 * The folder where backup files are stored.
	 */
	private static final File BACKUP_FILE_FOLDER = new File(Environment.getExternalStorageDirectory(), "RandomImage");

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
		parseConfigFiles();
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
			switchToImageList(currentListName, CreationStyle.NONE);
		}

		if (currentImageList == null && configFileMap.size() > 0) {
			String firstName = getImageListNames().get(0);
			switchToImageList(firstName, CreationStyle.NONE);
		}

		if (currentImageList == null) {
			String newName = getNewListName();
			switchToImageList(newName, CreationStyle.CREATE_EMPTY);
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
	 * Get the names of all available image lists in the backup.
	 *
	 * @return The names of all available image lists in the backup.
	 */
	public static ArrayList<String> getBackupImageListNames() {
		Map<String, File> backupConfigFileMap = parseConfigFiles(BACKUP_FILE_FOLDER);
		ArrayList<String> nameList = new ArrayList<String>(backupConfigFileMap.keySet());
		Collections.sort(nameList);
		return nameList;
	}

	/**
	 * Switch to the image list with the given name.
	 *
	 * @param name
	 *            The name of the target image list.
	 * @param creationStyle
	 *            Flag indicating if the list should be created if non-existing.
	 *
	 * @return true if successful.
	 */
	public static boolean switchToImageList(final String name, final CreationStyle creationStyle) {
		if (name == null) {
			return false;
		}
		else if (currentImageList != null && name.equals(currentImageList.getListName())) {
			currentImageList.load();
			return true;
		}

		File configFile = getConfigFile(name);

		if (configFile == null) {
			if (creationStyle == CreationStyle.NONE) {
				Log.w(Application.TAG, "Could not switch to non-existing file list " + name);
				return false;
			}
			else {
				File newFile = getFileForListName(name);
				configFileMap.put(name, newFile);
				currentImageList =
						new ImageList(newFile, name, creationStyle == CreationStyle.CREATE_EMPTY ? null
								: configFileMap.get(getCurrentListName()));
				PreferenceUtil.setSharedPreferenceString(R.string.key_current_list_name, name);
				return true;
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
			parseConfigFiles();
			return success;
		}
	}

	/**
	 * Backup the image list of the given name.
	 *
	 * @param name
	 *            The name of the list
	 * @return the backup file path if successful.
	 */
	public static String backupImageList(final String name) {
		File configFile = configFileMap.get(name);
		File oldBackupFile = parseConfigFiles(BACKUP_FILE_FOLDER).get(name);

		if (configFile == null) {
			Log.e(Application.TAG, "Could not find config file of " + name + " for backup.");
			return null;
		}

		if (!BACKUP_FILE_FOLDER.exists()) {
			boolean success = BACKUP_FILE_FOLDER.mkdir();
			if (!success) {
				Log.e(Application.TAG, "Could not create backup dir " + BACKUP_FILE_FOLDER.getAbsolutePath());
				return null;
			}
		}

		File backupFile = new File(BACKUP_FILE_FOLDER, configFile.getName());
		if (oldBackupFile != null && !oldBackupFile.equals(backupFile)) {
			oldBackupFile.delete();
		}

		return FileUtil.copyFile(configFile, backupFile) ? backupFile.getAbsolutePath() : null;
	}

	/**
	 * Restore the image list of the given name.
	 *
	 * @param name
	 *            The name of the list
	 * @return true if successful.
	 */
	public static boolean restoreImageList(final String name) {
		File oldConfigFile = configFileMap.get(name);
		File backupFile = parseConfigFiles(BACKUP_FILE_FOLDER).get(name);

		if (backupFile == null) {
			Log.e(Application.TAG, "Could not find backup file of " + name + " for restore.");
			return false;
		}

		File tempBackupFile = null;
		if (oldConfigFile != null) {
			tempBackupFile = new File(CONFIG_FILE_FOLDER, oldConfigFile.getName() + ".bak");
			oldConfigFile.renameTo(tempBackupFile);
		}

		File newConfigFile = getFileForListName(name);
		boolean success = FileUtil.copyFile(backupFile, newConfigFile);
		if (success) {
			if (tempBackupFile != null) {
				tempBackupFile.delete();
			}
			parseConfigFiles();
			switchToImageList(name, CreationStyle.NONE);
		}
		return success;
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
	 * Get the ImageList for a certain name.
	 *
	 * @param name
	 *            The name.
	 * @return The image list for this name, if existing. Otherwise null.
	 */
	public static ImageList getImageListByName(final String name) {
		if (name == null) {
			return null;
		}
		if (getCurrentListName().equals(name)) {
			return getCurrentImageList();
		}

		File configFile = getConfigFile(name);
		if (configFile == null) {
			return null;
		}
		else {
			return new ImageList(configFile);
		}
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
			parseConfigFiles();
			configFile = configFileMap.get(name);
		}

		return configFile;
	}

	/**
	 * Get the list of available config files.
	 */
	private static void parseConfigFiles() {
		configFileMap = parseConfigFiles(CONFIG_FILE_FOLDER);
	}

	/**
	 * Get the image lists from the config file folder.
	 *
	 * @param configFileFolder
	 *            The config file folder.
	 * @return The map from list names to image list files.
	 */
	private static Map<String, File> parseConfigFiles(final File configFileFolder) {
		File[] configFiles = configFileFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return file.isFile() && file.getName().endsWith(CONFIG_FILE_SUFFIX);
			}
		});

		if (configFiles == null) {
			configFiles = new File[0];
		}

		Map<String, File> fileMap = new HashMap<String, File>();

		for (File configFile : configFiles) {
			String name = new ImageList(configFile).getListName();

			if (fileMap.containsKey(name)) {
				Log.e(Application.TAG, "Duplicate config list name " + name);
			}
			else {
				fileMap.put(name, configFile);
			}
		}
		return fileMap;
	}

	/**
	 * Get a new list name.
	 *
	 * @return A new list name.
	 */
	private static String getNewListName() {
		parseConfigFiles();

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

	/**
	 * Enumeration indicating what should happen when switching to a non-existing list.
	 */
	public enum CreationStyle {
		/**
		 * Do not switch.
		 */
		NONE,
		/**
		 * Create empty list.
		 */
		CREATE_EMPTY,
		/**
		 * Clone the current list.
		 */
		CLONE_CURRENT
	}

}
