package de.jeisfeld.randomimage.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import android.net.Uri;
import android.util.Log;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.R;

/**
 * Utility class for storing and persisting a list of image file names plus additional display information.
 */
public final class ImageList {
	/**
	 * Name of the property for the list name.
	 */
	private static final String PROP_LIST_NAME = "listName";

	/**
	 * The config file where the list of files is stored.
	 */
	private File configFile = null;

	/**
	 * The list of image files.
	 */
	private List<String> fileNames = new ArrayList<String>();

	/**
	 * Configuration properties of the file list.
	 */
	private Properties properties = new Properties();

	/**
	 * Create an image list and load it from its file, if existing.
	 *
	 * @param configFile
	 *            the configuration file of this list.
	 *
	 */
	protected ImageList(final File configFile) {
		this.configFile = configFile;
		load();
	}

	/**
	 * Load the list if image file names from the config file.
	 */
	public synchronized void load() {
		fileNames.clear();
		properties.clear();
		try {
			if (configFile.exists()) {
				Scanner scanner = new Scanner(configFile);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String line = scanner.next();

					// ignore comment lines
					if (line == null || line.length() == 0 || line.startsWith("#")) {
						continue;
					}

					// handle file names
					if (line.startsWith(File.separator)) {
						if (new File(line).exists()) {
							fileNames.add(line);
						}
						else {
							Log.w(Application.TAG, "Cannot find file " + line);
						}
						continue;
					}

					// handle properties
					if (line.contains("=")) {
						int index = line.indexOf('=');
						String name = line.substring(0, index);
						String value = line.substring(index + 1);
						properties.setProperty(name, value);
					}
				}
				scanner.close();
			}
		}
		catch (FileNotFoundException e) {
			Log.e(Application.TAG, "Could not find configuration file", e);
		}
	}

	/**
	 * Save the list of image file names to the config file.
	 */
	public synchronized void save() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(configFile));
			writer.println("# Properties");
			for (Object keyObject : properties.keySet()) {
				String key = (String) keyObject;
				writer.println(key + "=" + properties.getProperty(key));
			}
			writer.println();
			writer.println("# File names");
			for (String fileName : fileNames) {
				writer.println(fileName);
			}
		}
		catch (IOException e) {
			Log.e(Application.TAG, "Could not store configuration to file", e);
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * Get the list of file names as array.
	 *
	 * @return The list of file names.
	 */
	public String[] getFileNames() {
		return fileNames.toArray(new String[0]);
	}

	/**
	 * Add a file name.
	 *
	 * @param fileName
	 *            The file name to be added.
	 * @return true if the file was not in the list before and hence has been added.
	 */
	public boolean add(final String fileName) {
		if (fileName != null && !fileNames.contains(fileName)) {
			fileNames.add(fileName);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Add a file from an Uri.
	 *
	 * @param imageUri
	 *            The uri of the file to be added.
	 * @return the file name, if it was added.
	 */
	public String add(final Uri imageUri) {
		if (imageUri != null && ImageUtil.getMimeType(imageUri).startsWith("image/")) {
			String fileName = MediaStoreUtil.getRealPathFromUri(imageUri);
			boolean isAdded = add(fileName);
			return isAdded ? fileName : null;
		}
		else {
			return null;
		}
	}

	/**
	 * Remove a file name.
	 *
	 * @param fileName
	 *            The file name to be removed.
	 * @return true if a file was removed.
	 */
	public boolean remove(final String fileName) {
		return fileNames.remove(fileName);
	}

	/**
	 * Get information if a file name is contained in the registry.
	 *
	 * @param fileName
	 *            The file name.
	 * @return True if contained.
	 */
	public boolean contains(final String fileName) {
		return fileNames.contains(fileName);
	}

	/**
	 * Get a random file name from the registry.
	 *
	 * @return A random file name.
	 */
	public String getRandomFileName() {
		if (fileNames.size() > 0) {
			return fileNames.get(new Random().nextInt(fileNames.size()));
		}
		else {
			return null;
		}
	}

	/**
	 * Get the number of files in the registry.
	 *
	 * @return The number of files.
	 */
	public int size() {
		return fileNames.size();
	}

	/**
	 * Get the name of the list.
	 *
	 * @return The name of the list.
	 */
	public String getListName() {
		String listName = properties.getProperty(PROP_LIST_NAME);

		if (listName == null) {
			listName = Application.getResourceString(R.string.default_list_name);
		}

		return listName;
	}

	/**
	 * Set the name of the list.
	 *
	 * @param listName
	 *            The new name of the list.
	 */
	public void setListName(final String listName) {
		if (listName == null) {
			properties.remove(PROP_LIST_NAME);
		}
		else {
			properties.setProperty(PROP_LIST_NAME, listName);
		}
		save();
	}
}
