package de.jeisfeld.randomimage.util;

import java.io.File;
import java.util.Properties;

import android.util.Log;
import de.jeisfeld.randomimage.Application;

/**
 * Utility class for storing and persisting a random image provider.
 */
public abstract class ImageList implements RandomFileProvider {
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
			setListName(listName); // OVERRIDABLE
			save(); // OVERRIDABLE
		}
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
	 * Handle the initialization of the class (such as initialization of lists).
	 */
	protected abstract void init();

	/**
	 * Get the name of the list.
	 *
	 * @return The name of the list.
	 */
	public abstract String getListName();

	/**
	 * Load the list if image file names from the config file.
	 */
	public abstract void load();

	/**
	 * Save the list of image file names to the config file.
	 *
	 * @return true if successful.
	 */
	protected abstract boolean save();

	/**
	 * Set the name of the list without changing the file name.
	 *
	 * @param listName
	 *            The new name of the list.
	 */
	protected abstract void setListName(final String listName);
}
