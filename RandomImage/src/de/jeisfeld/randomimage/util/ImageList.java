package de.jeisfeld.randomimage.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import android.net.Uri;
import android.util.Log;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.widgets.GenericWidget;

/**
 * Utility class for storing and persisting a list of image file names plus additional display information.
 */
public abstract class ImageList implements RandomFileProvider {
	/**
	 * Name of the property for the list name.
	 */
	private static final String PROP_LIST_NAME = "listName";

	/**
	 * The list of image files.
	 */
	private ArrayList<String> fileNames = new ArrayList<String>();

	/**
	 * The list of image folders.
	 */
	private ArrayList<String> folderNames = new ArrayList<String>();

	/**
	 * The config file where the list of files is stored.
	 */
	private File configFile = null;

	protected final File getConfigFile() {
		return configFile;
	}

	protected final void setConfigFile(final File configFile) {
		this.configFile = configFile;
	}

	/**
	 * Configuration properties of the file list.
	 */
	private Properties properties = new Properties();

	protected final Properties getProperties() {
		return properties;
	}

	protected final void setProperties(final Properties properties) {
		this.properties = properties;
	}

	/**
	 * Create an image list and load it from its file, if existing.
	 *
	 * @param configFile
	 *            the configuration file of this list.
	 *
	 */
	protected ImageList(final File configFile) {
		init(); // OVERRIDABLE
		setConfigFile(configFile);
		load(); // OVERRIDABLE
	}

	/**
	 * Create a new image list.
	 *
	 * @param configFile
	 *            the configuration file of this list.
	 * @param listName
	 *            the name of the list.
	 * @param cloneFile
	 *            If existing, then the new list will be cloned from this file.
	 *
	 */
	protected ImageList(final File configFile, final String listName, final File cloneFile) {
		init(); // OVERRIDABLE
		if (configFile.exists()) {
			Log.e(Application.TAG, "Tried to overwrite existing image list file " + configFile.getAbsolutePath());
			setConfigFile(configFile);
			load(); // OVERRIDABLE
		}
		else {
			if (cloneFile != null) {
				setConfigFile(cloneFile);
				load(); // OVERRIDABLE
			}
			setConfigFile(configFile);
			setListName(listName);
			save();
		}
	}

	/**
	 * Do initialization steps of the subclass of ImageList.
	 */
	protected abstract void init();

	/**
	 * Save and reload the list.
	 *
	 * @return true if both actions were successful.
	 */
	public final synchronized boolean update() {
		if (save()) {
			load();
			return true;
		}
		else {
			Log.e(Application.TAG, "Error while saving the image list.");
			DialogUtil.displayToast(Application.getAppContext(), R.string.toast_error_while_saving, getListName());
			return false;
		}
	}

	/**
	 * Check if the file or folder is contained in the list.
	 *
	 * @param fileName
	 *            The file name to be checked.
	 * @return true if contained in the list.
	 */
	public final boolean contains(final String fileName) {
		return fileName != null && (getFileNames().contains(fileName) || getFolderNames().contains(fileName));
	}

	/**
	 * Get an ImageList out of a config file.
	 *
	 * @param configFile
	 *            the config file.
	 * @return The image list.
	 */
	protected static ImageList parseConfigFile(final File configFile) {
		// TODO: enhance for other types.
		return new StandardImageList(configFile);
	}

	/**
	 * Load the list if image file names from the config file.
	 */
	// OVERRIDABLE
	public synchronized void load() {
		fileNames.clear();
		folderNames.clear();
		properties.clear();
		int notFoundFiles = 0;

		try {
			if (getConfigFile().exists()) {
				Scanner scanner = new Scanner(getConfigFile());
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String line = scanner.next();

					// ignore comment lines
					if (line == null || line.length() == 0 || line.startsWith("#")) {
						continue;
					}

					// handle file names
					if (line.startsWith(File.separator)) {
						File file = new File(line);
						if (file.exists()) {
							if (file.isDirectory()) {
								folderNames.add(line);
							}
							else {
								if (ImageUtil.isImage(file, false)) {
									fileNames.add(line);
								}
							}
						}
						else {
							Log.w(Application.TAG, "Cannot find file " + line);
							notFoundFiles++;
						}
						continue;
					}

					// handle properties
					if (line.contains("=")) {
						int index = line.indexOf('=');
						String name = line.substring(0, index);
						String value = line.substring(index + 1);
						getProperties().setProperty(name, value);
					}
				}
				scanner.close();
			}
		}
		catch (FileNotFoundException e) {
			Log.e(Application.TAG, "Could not find configuration file", e);
		}

		if (notFoundFiles > 1) {
			DialogUtil.displayToast(Application.getAppContext(), R.string.toast_failed_to_load_files,
					notFoundFiles, getListName());
		}
		else if (notFoundFiles == 1) {
			DialogUtil.displayToast(Application.getAppContext(), R.string.toast_failed_to_load_files_single,
					getListName());
		}
	}

	/**
	 * Save the list of image file names to the config file.
	 *
	 * @return true if successful.
	 */
	protected final synchronized boolean save() {
		File backupFile = new File(getConfigFile().getParentFile(), getConfigFile().getName() + ".bak");

		if (getConfigFile().exists()) {
			boolean success = getConfigFile().renameTo(backupFile);
			if (!success) {
				Log.e(Application.TAG, "Could not backup config file to " + backupFile.getAbsolutePath());
				DialogUtil.displayToast(Application.getAppContext(), R.string.toast_failed_to_save_list, getListName());
				return false;
			}
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(getConfigFile()));
			writer.println("# Properties");
			for (Object keyObject : getProperties().keySet()) {
				String key = (String) keyObject;
				writer.println(key + "=" + getProperties().getProperty(key));
			}
			writer.println();
			writer.println("# Folder names");
			for (String folderName : folderNames) {
				writer.println(folderName);
			}
			writer.println();
			writer.println("# File names");
			for (String fileName : fileNames) {
				writer.println(fileName);
			}

			writer.close();

			if (backupFile.exists()) {
				boolean success = backupFile.delete();
				if (!success) {
					Log.e(Application.TAG, "Could not delete backup file " + backupFile.getAbsolutePath());
					return false;
				}
			}
		}
		catch (IOException e) {
			Log.e(Application.TAG, "Could not store configuration to file " + getConfigFile().getAbsolutePath(), e);
			DialogUtil.displayToast(Application.getAppContext(), R.string.toast_failed_to_save_list, getListName());
			return false;
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
		return true;
	}

	/**
	 * Get all image files in the given folder.
	 *
	 * @param folderName
	 *            The image folder name
	 * @return The list of image files in this folder.
	 */
	public static Set<String> getImageFilesInFolder(final String folderName) {
		File folder = new File(folderName);
		Set<String> result = new HashSet<String>();
		if (!folder.exists() || !folder.isDirectory()) {
			return result;
		}

		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				if (ImageUtil.isImage(file, false)) {
					result.add(file.getAbsolutePath());
				}
			}
		}
		return result;
	}

	/**
	 * Get the name of the list.
	 *
	 * @return The name of the list.
	 */
	public final String getListName() {
		String listName = getProperties().getProperty(PROP_LIST_NAME);

		if (listName == null) {
			listName = ImageRegistry.getListNameFromFileName(getConfigFile());
		}

		if (listName == null) {
			listName = Application.getResourceString(R.string.default_list_name);
		}

		return listName;
	}

	/**
	 * Set the name of the list without changing the file name.
	 *
	 * @param listName
	 *            The new name of the list.
	 */
	private void setListName(final String listName) {
		if (listName == null) {
			getProperties().remove(PROP_LIST_NAME);
		}
		else {
			getProperties().setProperty(PROP_LIST_NAME, listName);
		}
		save();
	}

	/**
	 * Change the list name, also renaming the config file accordingly.
	 *
	 * @param listName
	 *            The new name of the list.
	 * @param newConfigFile
	 *            The new config file.
	 * @return true if successful.
	 */
	public final boolean changeListName(final String listName, final File newConfigFile) {
		File oldConfigFile = getConfigFile();
		String oldListName = getListName();
		setConfigFile(newConfigFile);
		setListName(listName);
		boolean success = save();

		if (success) {
			success = oldConfigFile.delete();
			if (!success) {
				Log.e(Application.TAG, "Could not delete old config file " + oldConfigFile.getAbsolutePath());
			}
			GenericWidget.updateListName(oldListName, listName);
			return success;
		}
		else {
			Log.e(Application.TAG, "Could not save to new config file " + getConfigFile().getAbsolutePath());
			setConfigFile(oldConfigFile);
			return false;
		}
	}

	/**
	 * Get the list of file names in the list as array.
	 *
	 * @return The list of file names.
	 */
	public final ArrayList<String> getFileNames() {
		return fileNames;
	}

	/**
	 * Get the list of folder names in the list as array.
	 *
	 * @return The list of file names.
	 */
	public final ArrayList<String> getFolderNames() {
		return folderNames;
	}

	/**
	 * Add a file name. This does not yet update the list of all images!
	 *
	 * @param fileName
	 *            The file name to be added.
	 * @return true if the file was not in the list before and hence has been added.
	 */
	public final boolean addFile(final String fileName) {
		if (fileName == null) {
			return false;
		}

		File file = new File(fileName);
		if (!file.exists() || file.isDirectory()) {
			return false;
		}

		if (fileNames.contains(fileName)) {
			return false;
		}
		else {
			fileNames.add(fileName);
			return true;
		}
	}

	/**
	 * Add a file from an Uri. The lists are kept up to date, but are not saved!
	 *
	 * @param imageUri
	 *            The uri of the file to be added.
	 * @return the file name, if it was added.
	 */
	public final String add(final Uri imageUri) {
		if (imageUri != null && ImageUtil.getMimeType(imageUri).startsWith("image/")) {
			String fileName = MediaStoreUtil.getRealPathFromUri(imageUri);
			boolean isAdded = addFile(fileName);
			return isAdded ? fileName : null;
		}
		else {
			return null;
		}
	}

	/**
	 * Add a folder name. This does not yet update the list of all images!
	 *
	 * @param folderName
	 *            The folder name to be added.
	 * @return true if the folder was not in the list before and hence has been added.
	 */
	public final boolean addFolder(final String folderName) {
		if (folderName == null) {
			return false;
		}

		File file = new File(folderName);
		if (!file.exists() || !file.isDirectory()) {
			return false;
		}

		if (folderNames.contains(folderName)) {
			return false;
		}
		else {
			folderNames.add(folderName);
			return true;
		}
	}

	/**
	 * Remove a single file name. This does not yet update the list of all images!
	 *
	 * @param fileName
	 *            The file name to be removed.
	 * @return true if the file was removed.
	 */
	public final boolean removeFile(final String fileName) {
		File file = new File(fileName);

		if (!file.exists() || file.isDirectory()) {
			return false;
		}
		return fileNames.remove(fileName);
	}

	/**
	 * Remove a single folder name. This does not yet update the list of all images!
	 *
	 * @param folderName
	 *            The folder name to be removed.
	 * @return true if the folder was removed.
	 */
	public final boolean removeFolder(final String folderName) {
		File file = new File(folderName);

		if (!file.exists() || !file.isDirectory()) {
			return false;
		}
		return folderNames.remove(folderName);
	}
}
