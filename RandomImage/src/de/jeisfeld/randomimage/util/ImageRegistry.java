package de.jeisfeld.randomimage.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import android.net.Uri;
import android.util.Log;
import de.jeisfeld.randomimage.Application;

/**
 * Utility class for storing and persisting the list of image names.
 */
public final class ImageRegistry {
	/**
	 * The name of the config file where the list of files is stored.
	 */
	private static final String IMAGES_FILE_NAME = "fileNames.txt";

	/**
	 * The config file where the list of files is stored.
	 */
	private static final File IMAGES_FILE = new File(Application.getAppContext().getExternalFilesDir(null),
			IMAGES_FILE_NAME);

	/**
	 * The singleton instance of the imageRegistry.
	 */
	private static volatile ImageRegistry instance = null;

	/**
	 * The list of image files.
	 */
	private List<String> fileNames = new ArrayList<String>();

	/**
	 * Hidden constructor.
	 */
	private ImageRegistry() {
		load();
	}

	/**
	 * Get an ImageRegistry instance.
	 *
	 * @return An instance.
	 */
	public static ImageRegistry getInstance() {
		if (instance == null) {
			instance = new ImageRegistry();
		}
		return instance;
	}

	/**
	 * Load the list if image file names from the config file.
	 */
	public synchronized void load() {
		fileNames.clear();
		try {
			if (IMAGES_FILE.exists()) {
				Scanner scanner = new Scanner(IMAGES_FILE);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String fileName = scanner.next();
					if (new File(fileName).exists()) {
						fileNames.add(fileName);
					}
					else {
						Log.w(Application.TAG, "Cannot find file " + fileName);
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
			writer = new PrintWriter(new FileWriter(IMAGES_FILE));
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

}
