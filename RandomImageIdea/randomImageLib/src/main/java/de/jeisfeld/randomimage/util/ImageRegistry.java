package de.jeisfeld.randomimage.util;

import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.util.ImageList.ImageListInfo;
import de.jeisfeld.randomimagelib.R;

/**
 * Utility class for storing and persisting the list of image names.
 */
public final class ImageRegistry {
	/**
	 * The folder where backup files are stored.
	 */
	public static final File BACKUP_FILE_FOLDER = new File(Environment.getExternalStorageDirectory(), "RandomImage");

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
	private static volatile ImageList mCurrentImageList = null;

	/**
	 * A map from image list name to corresponding config file.
	 */
	private static Map<String, ImageListInfo> mImageListInfoMap = new HashMap<>();

	static {
		parseConfigFiles();
	}

	/**
	 * Hidden constructor.
	 */
	private ImageRegistry() {
	}

	/**
	 * Get the currentImageList.
	 *
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 * @return The currentImageList.
	 */
	public static ImageList getCurrentImageList(final boolean toastIfFilesMissing) {
		String currentListName = getCurrentListName();

		if (mCurrentImageList == null && currentListName != null) {
			switchToImageList(currentListName, CreationStyle.NONE, toastIfFilesMissing);
		}

		if (mCurrentImageList == null && mImageListInfoMap.size() > 0) {
			String firstName = getImageListNames(ListFiltering.HIDE_BY_REGEXP).get(0);
			switchToImageList(firstName, CreationStyle.NONE, toastIfFilesMissing);
		}

		if (mCurrentImageList == null) {
			// Create default list
			String newName = getNewListName();
			switchToImageList(newName, CreationStyle.CREATE_EMPTY, toastIfFilesMissing);
		}
		return mCurrentImageList;
	}

	/**
	 * Get the currentImageList, ensuring that it is freshly loaded.
	 *
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 * @return The currentImageList.
	 */
	public static ImageList getCurrentImageListRefreshed(final boolean toastIfFilesMissing) {
		if (mCurrentImageList == null) {
			return getCurrentImageList(toastIfFilesMissing);
		}
		else {
			mCurrentImageList.load(toastIfFilesMissing);
			return mCurrentImageList;
		}
	}

	/**
	 * Get the name of the current list.
	 *
	 * @return The name of the current list.
	 */
	public static String getCurrentListName() {
		return PreferenceUtil.getSharedPreferenceString(R.string.key_current_list_name);
	}

	/**
	 * Get the names of all available image lists.
	 *
	 * @param listFiltering The way in which the list should be filtered.
	 * @return The names of all available image lists.
	 */
	public static ArrayList<String> getImageListNames(final ListFiltering listFiltering) {
		ArrayList<String> nameList = new ArrayList<>(mImageListInfoMap.keySet());
		return filterNameList(nameList, listFiltering);
	}

	/**
	 * Prepare a list of list names according to filtering.
	 *
	 * @param nameList      The full list.
	 * @param listFiltering The filtering to be applied.
	 * @return The filtered list.
	 */
	private static ArrayList<String> filterNameList(final ArrayList<String> nameList, final ListFiltering listFiltering) {
		Collections.sort(nameList, Collator.getInstance());
		switch (listFiltering) {
		case HIDE_BY_REGEXP:
			String hiddenListsPattern = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_hidden_lists_pattern);
			boolean useRegexp = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_use_regex_filter)
					&& hiddenListsPattern != null && hiddenListsPattern.length() > 0;
			ArrayList<String> filteredList = new ArrayList<>();
			for (String name : nameList) {
				if (!useRegexp || !name.matches(hiddenListsPattern)) {
					filteredList.add(name);
				}
			}
			return filteredList;
		case ALL_LISTS:
		default:
			return nameList;
		}
	}

	/**
	 * Get the names of all available standard image lists.
	 *
	 * @param listFiltering The way in which the list should be filtered.
	 * @return The names of all available standard image lists.
	 */
	public static ArrayList<String> getStandardImageListNames(final ListFiltering listFiltering) {
		ArrayList<String> nameList = new ArrayList<>();

		for (String name : mImageListInfoMap.keySet()) {
			if (mImageListInfoMap.get(name).getListClass().equals(StandardImageList.class)) {
				nameList.add(name);
			}
		}

		Collections.sort(nameList, Collator.getInstance());
		return filterNameList(nameList, listFiltering);
	}

	/**
	 * Get the names of all available image lists in the backup.
	 *
	 * @param listFiltering The filtering to be applied.
	 * @return The names of all available image lists in the backup.
	 */
	public static ArrayList<String> getBackupImageListNames(final ListFiltering listFiltering) {
		if (SystemUtil.isAtLeastVersion(VERSION_CODES.Q)) {
			if (getBackupDocumentFolder() == null) {
				return new ArrayList<>();
			}
			Map<String, ImageListInfo> backupConfigFileMap = parseConfigFiles(getBackupDocumentFolder());
			ArrayList<String> nameList = new ArrayList<>(backupConfigFileMap.keySet());
			return filterNameList(nameList, listFiltering);
		}
		else {
			Map<String, ImageListInfo> backupConfigFileMap = parseConfigFiles(BACKUP_FILE_FOLDER);
			ArrayList<String> nameList = new ArrayList<>(backupConfigFileMap.keySet());
			return filterNameList(nameList, listFiltering);
		}
	}

	/**
	 * Switch to the image list with the given name.
	 *
	 * @param name                The name of the target image list.
	 * @param creationStyle       Flag indicating if the list should be created if non-existing.
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 * @return true if successful.
	 */
	public static boolean switchToImageList(final String name, final CreationStyle creationStyle, final boolean toastIfFilesMissing) {
		if (name == null) {
			return false;
		}
		else if (mCurrentImageList != null && name.equals(mCurrentImageList.getListName())) {
			mCurrentImageList.load(toastIfFilesMissing);
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
				mImageListInfoMap.put(name, new ImageListInfo(name, newFile, StandardImageList.class));
				mCurrentImageList =
						new StandardImageList(newFile, name,
								creationStyle == CreationStyle.CLONE_CURRENT ? getConfigFile(getCurrentListName()) : null);
				PreferenceUtil.setSharedPreferenceString(R.string.key_current_list_name, name);
				return true;
			}
		}
		else {
			mCurrentImageList = ImageList.getListFromConfigFile(configFile, toastIfFilesMissing);
			PreferenceUtil.setSharedPreferenceString(R.string.key_current_list_name, name);
			return true;
		}
	}

	/**
	 * Delete the image list of the given name.
	 *
	 * @param name The name of the list to be deleted.
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
	 * @param name The name of the list
	 * @return the backup file path if successful.
	 */
	public static String backupImageList(final String name) {
		File configFile = getConfigFile(name);
		if (configFile == null) {
			Log.e(Application.TAG, "Could not find config file of " + name + " for backup.");
			return null;
		}

		if (SystemUtil.isAtLeastVersion(VERSION_CODES.Q)) {
			if (getBackupDocumentFolder() == null) {
				return null;
			}
			DocumentFile oldBackupFile = getBackupDocumentFolder().findFile(configFile.getName());
			if (oldBackupFile != null) {
				FileUtil.deleteFile(oldBackupFile);
			}
			ImageListInfo oldImageListInfo = parseConfigFiles(getBackupDocumentFolder()).get(name);
			oldBackupFile = oldImageListInfo == null ? null : oldImageListInfo.getConfigDocumentFile();
			if (oldBackupFile != null) {
				FileUtil.deleteFile(oldBackupFile);
			}
			DocumentFile backupFile = getBackupDocumentFolder().createFile("*", configFile.getName());
			if (backupFile == null) {
				return null;
			}
			else {
				return FileUtil.copyFile(configFile, backupFile) ? backupFile.getName() : null;
			}
		}
		else {
			if (!BACKUP_FILE_FOLDER.exists()) {
				boolean success = BACKUP_FILE_FOLDER.mkdir();
				if (!success) {
					Log.e(Application.TAG, "Could not create backup dir " + BACKUP_FILE_FOLDER.getAbsolutePath());
					return null;
				}
			}

			File backupFile = new File(BACKUP_FILE_FOLDER, configFile.getName());
			ImageListInfo oldImageListInfo = parseConfigFiles(BACKUP_FILE_FOLDER).get(name);
			File oldBackupFile = oldImageListInfo == null ? null : oldImageListInfo.getConfigFile();
			if (oldBackupFile != null && !oldBackupFile.equals(backupFile)) {
				FileUtil.deleteFile(oldBackupFile);
			}
			return FileUtil.copyFile(configFile, backupFile) ? backupFile.getAbsolutePath() : null;
		}
	}

	/**
	 * Restore the image list of the given name.
	 *
	 * @param name The name of the list
	 * @return true if successful.
	 */
	public static boolean restoreImageList(final String name) {
		if (SystemUtil.isAtLeastVersion(VERSION_CODES.Q)) {
			if (getBackupDocumentFolder() == null) {
				return false;
			}
			ImageListInfo backupFileInfo = parseConfigFiles(getBackupDocumentFolder()).get(name);
			DocumentFile backupFile = backupFileInfo == null ? null : backupFileInfo.getConfigDocumentFile();

			if (backupFile == null) {
				Log.e(Application.TAG, "Could not find backup file of " + name + " for restore.");
				return false;
			}

			File oldConfigFile = getConfigFile(name);
			File tempBackupFile = null;
			if (oldConfigFile != null) {
				tempBackupFile = new File(getConfigFileFolder(), oldConfigFile.getName() + ".bak");
				//noinspection ResultOfMethodCallIgnored
				oldConfigFile.renameTo(tempBackupFile);
			}

			File newConfigFile = getFileForListName(name);
			boolean success = FileUtil.copyFile(backupFile, newConfigFile);
			if (success) {
				if (tempBackupFile != null) {
					//noinspection ResultOfMethodCallIgnored
					tempBackupFile.delete();
				}
				parseConfigFiles();
				switchToImageList(name, CreationStyle.NONE, true);
			}
			return success;
		}
		else {
			ImageListInfo backupFileInfo = parseConfigFiles(BACKUP_FILE_FOLDER).get(name);
			File backupFile = backupFileInfo == null ? null : backupFileInfo.getConfigFile();

			if (backupFile == null) {
				Log.e(Application.TAG, "Could not find backup file of " + name + " for restore.");
				return false;
			}

			File oldConfigFile = getConfigFile(name);
			File tempBackupFile = null;
			if (oldConfigFile != null) {
				tempBackupFile = new File(getConfigFileFolder(), oldConfigFile.getName() + ".bak");
				//noinspection ResultOfMethodCallIgnored
				oldConfigFile.renameTo(tempBackupFile);
			}

			File newConfigFile = getFileForListName(name);
			boolean success = FileUtil.copyFile(backupFile, newConfigFile);
			if (success) {
				if (tempBackupFile != null) {
					//noinspection ResultOfMethodCallIgnored
					tempBackupFile.delete();
				}
				parseConfigFiles();
				switchToImageList(name, CreationStyle.NONE, true);
			}
			return success;
		}
	}

	/**
	 * Get the backup folder as DocumentFile.
	 *
	 * @return The backup folder as DocumentFile.
	 */
	@RequiresApi(api = VERSION_CODES.Q)
	public static DocumentFile getBackupDocumentFolder() {
		Uri treeUri = PreferenceUtil.getSharedPreferenceUri(R.string.key_backup_folder_uri);
		if (treeUri == null) {
			return null;
		}
		return DocumentFile.fromTreeUri(Application.getAppContext(), treeUri);
	}

	/**
	 * Get the config file folder on regular storage.
	 *
	 * @return The config file folder.
	 */
	public static File getConfigFileFolder() {
		return Application.getAppContext().getExternalFilesDir(null);
	}


	/**
	 * Rename an image list.
	 *
	 * @param oldName The name of the image list to be renamed.
	 * @param newName The new name.
	 * @return true if successful.
	 */
	public static boolean renameImageList(final String oldName, final String newName) {
		if (newName == null) {
			return false;
		}
		if (newName.equals(oldName)) {
			return true;
		}
		ImageList imageList = getImageListByName(oldName, false);
		if (imageList == null) {
			return false;
		}

		File newConfigFile = getFileForListName(newName);
		boolean success = imageList.changeListName(newName, newConfigFile);
		if (success) {
			mImageListInfoMap.put(newName, new ImageListInfo(newName, newConfigFile,
					mImageListInfoMap.get(oldName).getListClass()));
			mImageListInfoMap.remove(oldName);
			if (oldName.equals(mCurrentImageList.getListName())) {
				PreferenceUtil.setSharedPreferenceString(R.string.key_current_list_name, newName);
			}
		}
		return success;
	}

	/**
	 * Get the StandardImageList for a certain name.
	 *
	 * @param name                The name.
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 * @return The image list for this name, if existing. Otherwise null.
	 */
	public static ImageList getImageListByName(final String name, final boolean toastIfFilesMissing) {
		if (name == null) {
			return null;
		}
		if (getCurrentListName().equals(name)) {
			return getCurrentImageList(toastIfFilesMissing);
		}

		File configFile = getConfigFile(name);
		if (configFile == null) {
			return null;
		}
		else {
			return ImageList.getListFromConfigFile(configFile, toastIfFilesMissing);
		}
	}

	/**
	 * Get the StandardImageList for a certain name.
	 *
	 * @param name                The name.
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 * @return The image list for this name, if existing. Otherwise null.
	 */
	public static StandardImageList getStandardImageListByName(final String name, final boolean toastIfFilesMissing) {
		ImageList imageList = getImageListByName(name, toastIfFilesMissing);

		if (imageList instanceof StandardImageList) {
			return (StandardImageList) imageList;
		}
		else {
			return null;
		}
	}

	/**
	 * Get the config file for a certain list name.
	 *
	 * @param name The list name.
	 * @return The config file, if existing, otherwise null.
	 */
	private static File getConfigFile(final String name) {
		ImageListInfo imageListInfo = mImageListInfoMap.get(name);

		File configFile = imageListInfo == null ? null : imageListInfo.getConfigFile();

		if (configFile == null) {
			parseConfigFiles();
			imageListInfo = mImageListInfoMap.get(name);
			configFile = imageListInfo == null ? null : imageListInfo.getConfigFile();
		}

		return configFile;
	}

	/**
	 * Get the list of available config files.
	 */
	public static void parseConfigFiles() {
		if (getConfigFileFolder() != null) {
			mImageListInfoMap = parseConfigFiles(getConfigFileFolder());
		}
	}

	/**
	 * Get the image lists from the config file folder.
	 *
	 * @param configFileFolder The config file folder.
	 * @return The map from list names to image list files.
	 */
	private static Map<String, ImageListInfo> parseConfigFiles(final File configFileFolder) {
		File[] configFiles = configFileFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return file.isFile() && file.getName().endsWith(CONFIG_FILE_SUFFIX);
			}
		});

		if (configFiles == null) {
			configFiles = new File[0];
		}

		Map<String, ImageListInfo> fileMap = new HashMap<>();

		for (File configFile : configFiles) {
			ImageListInfo imageListInfo = ImageList.getInfoFromConfigFile(configFile);

			if (imageListInfo != null) {
				String name = imageListInfo.getName();

				if (name == null) {
					Log.e(Application.TAG, "List name is null");
				}
				else if (fileMap.containsKey(name)) {
					Log.e(Application.TAG, "Duplicate config list name " + name);
				}
				else {
					fileMap.put(name, new ImageListInfo(name, configFile, imageListInfo.getListClass()));
				}
			}
		}
		return fileMap;
	}

	/**
	 * Get the image lists from the config file folder.
	 *
	 * @param configFileFolder the config file folder.
	 * @return The map from list names to image list files.
	 */
	private static Map<String, ImageListInfo> parseConfigFiles(final DocumentFile configFileFolder) {
		DocumentFile[] allFiles = configFileFolder.listFiles();

		List<DocumentFile> configFiles = new ArrayList<>();
		for (DocumentFile file : allFiles) {
			if (file.isFile() && file.getName() != null && file.getName().endsWith(CONFIG_FILE_SUFFIX)) {
				configFiles.add(file);
			}
		}

		Map<String, ImageListInfo> fileMap = new HashMap<>();

		for (DocumentFile configFile : configFiles) {
			ImageListInfo imageListInfo = ImageList.getInfoFromConfigFile(configFile);

			if (imageListInfo != null) {
				String name = imageListInfo.getName();

				if (name == null) {
					Log.e(Application.TAG, "List name is null");
				}
				else if (fileMap.containsKey(name)) {
					Log.e(Application.TAG, "Duplicate config list name " + name);
				}
				else {
					fileMap.put(name, new ImageListInfo(name, configFile, imageListInfo.getListClass()));
				}
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
		while (mImageListInfoMap.containsKey(listName)) {
			listName = baseListName + " (" + (++counter) + ")";
		}

		return listName;
	}

	/**
	 * Generate a file for a new list name.
	 *
	 * @param name The list name.
	 * @return A file for this name.
	 */
	private static File getFileForListName(final String name) {
		String baseName = getConfigFileBaseNameForListName(name);
		String fileName = baseName + CONFIG_FILE_SUFFIX;
		File resultFile = new File(getConfigFileFolder(), fileName);

		int counter = 1;
		while (resultFile.exists()) {
			fileName = baseName + " (" + (++counter) + ")" + CONFIG_FILE_SUFFIX;
			resultFile = new File(getConfigFileFolder(), fileName);
		}

		return resultFile;
	}

	/**
	 * Get the config file base name for a list name.
	 *
	 * @param name The list name.
	 * @return The config file name.
	 */
	private static String getConfigFileBaseNameForListName(final String name) {
		String baseName =
				CONFIG_FILE_PREFIX
						+ name.replaceAll("[.]", ",")
						.replaceAll("[\\s]", " ")
						.replaceAll(
								"[^A-Za-z0-9äöüßÄÖÜáàéèíìóòúùÁÀÉÈÍÌÓÒÚÙ \\-_%&?!$(),;:]",
								"");
		if (baseName.length() > MAX_NAME_LENGTH) {
			baseName = baseName.substring(0, MAX_NAME_LENGTH);
		}
		return baseName.trim();
	}

	/**
	 * Get the list name out of the file name (for the case that the list name is not stored there).
	 *
	 * @param fileName the config file name.
	 * @return the list name generated from the file name.
	 */
	protected static String getListNameFromFileName(final String fileName) {
		String listName = fileName;

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

	/**
	 * Types of filtering the list of image lists.
	 */
	public enum ListFiltering {
		/**
		 * Show all lists.
		 */
		ALL_LISTS,
		/**
		 * Hide the lists as per the regular expression in the configuration.
		 */
		HIDE_BY_REGEXP
	}

}
