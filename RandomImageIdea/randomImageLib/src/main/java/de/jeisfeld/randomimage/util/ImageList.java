package de.jeisfeld.randomimage.util;

import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.notifications.NotificationSettingsActivity;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.notifications.NotificationUtil.NotificationType;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
import de.jeisfeld.randomimage.widgets.WidgetSettingsActivity;
import de.jeisfeld.randomimagelib.R;

import static de.jeisfeld.randomimage.util.ListElement.Type.FILE;
import static de.jeisfeld.randomimage.util.ListElement.Type.FOLDER;
import static de.jeisfeld.randomimage.util.ListElement.Type.MISSING_PATH;
import static de.jeisfeld.randomimage.util.ListElement.Type.NESTED_LIST;

/**
 * Utility class for storing and persisting a list of image file names plus additional display information.
 */
public abstract class ImageList implements RandomFileProvider {
	/**
	 * Separator for properties in the file.
	 */
	private static final String PROPERTY_SEPARATOR = "=";

	/**
	 * Name of the property for the list name.
	 */
	private static final String PROP_LIST_NAME = "listName";

	/**
	 * The list of elements in the list.
	 */
	private final ArrayList<ListElement> mElements = new ArrayList<>();

	/**
	 * The config file where the list of files is stored.
	 */
	private File mConfigFile;

	/**
	 * Configuration properties of the file list.
	 */
	private Properties mProperties = new Properties();

	/**
	 * Create an image list and load it from its file, if existing.
	 *
	 * @param configFile the configuration file of this list.
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 */
	protected ImageList(final File configFile, final boolean toastIfFilesMissing) {
		init(toastIfFilesMissing); // OVERRIDABLE
		this.mConfigFile = configFile;
		load(toastIfFilesMissing); // OVERRIDABLE
	}

	/**
	 * Create a new image list.
	 *
	 * @param configFile the configuration file of this list.
	 * @param listName the name of the list.
	 * @param cloneFile If existing, then the new list will be cloned from this file.
	 */
	protected ImageList(final File configFile, final String listName, final File cloneFile) {
		init(false); // OVERRIDABLE
		if (configFile.exists()) {
			Log.e(Application.TAG, "Tried to overwrite existing image list file " + configFile.getAbsolutePath());
			this.mConfigFile = configFile;
			load(false); // OVERRIDABLE
		}
		else {
			if (cloneFile != null) {
				this.mConfigFile = cloneFile;
				load(false); // OVERRIDABLE
			}
			this.mConfigFile = configFile;
			setListName(listName);
			update(false); // OVERRIDABLE
		}
	}

	/**
	 * Get the list elements of a particular type.
	 *
	 * @param type The type.
	 * @return The elements of this type.
	 */
	public ArrayList<ListElement> getElements(final ListElement.Type type) {
		ArrayList<ListElement> result = new ArrayList<>();
		synchronized (mElements) {
			for (ListElement element : mElements) {
				if (element.getType() == type) {
					result.add(element);
				}
			}
		}
		return result;
	}

	/**
	 * Get the list elements of a particular type.
	 *
	 * @param type The type.
	 * @return The names of elements of this type.
	 */
	public ArrayList<String> getElementNames(final ListElement.Type type) {
		ArrayList<String> result = new ArrayList<>();
		synchronized (mElements) {
			for (ListElement element : mElements) {
				if (element.getType() == type) {
					result.add(element.getName());
				}
			}
		}
		return result;
	}

	/**
	 * Get an element from the list, if existing.
	 *
	 * @param type The element type.
	 * @param name The element name.
	 * @return The element instance.
	 */
	protected ListElement getElement(final ListElement.Type type, final String name) {
		if (name == null) {
			return null;
		}
		synchronized (mElements) {
			for (ListElement element : mElements) {
				if (type == element.getType() && name.equals(element.getName())) {
					return element;
				}
			}
		}
		return null;
	}

	/**
	 * Get the instance of the element which is currently in the list.
	 *
	 * @param element The element to be found
	 * @return The instance of this element in the list, if existing. Otherwise the given element.
	 */
	protected ListElement getElement(final ListElement element) {
		ListElement foundElement = getElement(element.getType(), element.getName());
		if (foundElement == null) {
			return element;
		}
		else {
			return foundElement;
		}
	}

	/**
	 * Find out if there is an element of a given type.
	 *
	 * @param type The type.
	 * @return True if the list contains an element of this type.
	 */
	public boolean hasElements(final ListElement.Type type) {
		synchronized (mElements) {
			for (ListElement element : mElements) {
				if (element.getType() == type) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Do initialization steps of the subclass of ImageList.
	 *
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 */
	protected abstract void init(boolean toastIfFilesMissing);

	/**
	 * Save and reload the list.
	 *
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 * @return true if both actions were successful.
	 */
	// OVERRIDABLE
	public synchronized boolean update(final boolean toastIfFilesMissing) {
		if (save()) {
			load(toastIfFilesMissing);
			return true;
		}
		else {
			Log.e(Application.TAG, "Error while saving the image list.");
			DialogUtil.displayToast(Application.getAppContext(), R.string.toast_error_while_saving, getListName());
			NotificationUtil.displayNotification(Application.getAppContext(), getListName(), NotificationType.ERROR_SAVING_LIST,
					R.string.title_notification_failed_saving, R.string.toast_failed_to_save_list, getListName());
			return false;
		}
	}

	/**
	 * Remove missing files from the list.
	 */
	public final void cleanupMissingFiles() {
		ArrayList<ListElement> newElements = new ArrayList<>();
		synchronized (mElements) {
			for (ListElement element : mElements) {
				if (element.getType() != MISSING_PATH) {
					newElements.add(element);
				}
			}
			mElements.clear();
			mElements.addAll(newElements);
		}
		update(true);
	}

	/**
	 * Check if the element is contained in the list.
	 *
	 * @param listElement The element to be checked.
	 * @return true if contained in the list.
	 */
	public final boolean contains(final ListElement listElement) {
		synchronized (mElements) {
			return listElement != null && mElements.contains(listElement);
		}
	}

	/**
	 * Check if the path is contained in the list.
	 *
	 * @param path The path to be checked.
	 * @return true if contained in the list.
	 */
	public final boolean contains(final String path) {
		if (path == null) {
			return false;
		}
		synchronized (mElements) {
			for (ListElement element : mElements) {
				if (path.equals(element.getName()) && element.getType() != NESTED_LIST) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if the list contains the other list.
	 *
	 * @param listName The list to be checked.
	 * @param includeDeepNestings flag indicating if recursive nestings should be considered.
	 * @return true if it is contained in some way.
	 */
	public final boolean containsNestedList(final String listName, final boolean includeDeepNestings) {
		if (getElementNames(NESTED_LIST).contains(listName)) {
			return true;
		}
		else if (!includeDeepNestings) {
			return false;
		}

		for (String nestedListName : getElementNames(NESTED_LIST)) {
			ImageList nestedList = ImageRegistry.getImageListByName(nestedListName, true);
			if (nestedList != null && nestedList.containsNestedList(listName, true)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get an ImageList out of a config file.
	 *
	 * @param configFile the config file.
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 * @return The image list.
	 */
	protected static ImageList getListFromConfigFile(final File configFile, final boolean toastIfFilesMissing) {
		// TODO: enhance for other classes.
		return new StandardImageList(configFile, toastIfFilesMissing);
	}

	/**
	 * Retrieve the info for an ImageList from the config file.
	 *
	 * @param configFile the config file.
	 * @return The name of the image list.
	 */
	protected static ImageListInfo getInfoFromConfigFile(final File configFile) {
		if (!configFile.exists() || configFile.isDirectory()) {
			return null;
		}

		String listName = null;

		try {
			Scanner scanner = new Scanner(configFile);
			scanner.useDelimiter("\n");
			while (scanner.hasNext()) {
				String line = scanner.next();

				// ignore comment lines
				if (line == null || line.length() == 0 || line.startsWith("#")) {
					continue;
				}

				// read properties
				if (line.contains(PROPERTY_SEPARATOR) && !line.startsWith(File.separator)) {
					int index = line.indexOf(PROPERTY_SEPARATOR);
					String name = line.substring(0, index);
					String value = line.substring(index + 1);

					if (PROP_LIST_NAME.equals(name)) {
						listName = value;
					}
				}
			}
			scanner.close();
		}
		catch (FileNotFoundException e) {
			Log.e(Application.TAG, "Could not find configuration file", e);
			return null;
		}
		if (listName == null) {
			listName = ImageRegistry.getListNameFromFileName(configFile);
		}
		if (listName == null) {
			return null;
		}
		else {
			// TODO: enhance for other classes.
			return new ImageListInfo(listName, configFile, StandardImageList.class);
		}
	}

	/**
	 * Load the list if image file names from the config file.
	 *
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 */
	// OVERRIDABLE
	public void load(final boolean toastIfFilesMissing) {
		if (!mConfigFile.exists()) {
			return;
		}
		final long timestamp = System.currentTimeMillis();

		synchronized (mElements) {
			mElements.clear();
			mProperties.clear();

			HashMap<ListElement.Type, SparseArray<Properties>> nestedPropertiesMap = new HashMap<>();
			for (ListElement.Type type : ListElement.Type.values()) {
				nestedPropertiesMap.put(type, new SparseArray<Properties>());
			}

			HashMap<ListElement, Integer> indexMap = new HashMap<>();
			ArrayList<String> missingPaths = new ArrayList<>();

			try {
				Scanner scanner = new Scanner(mConfigFile);
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
						if (file.getName().equals("*")) {
							if (file.getParentFile().isDirectory()) {
								mElements.add(new ListElement(FOLDER, line));
							}
							else {
								Log.w(Application.TAG, "Cannot find folder " + line);
								mElements.add(new ListElement(MISSING_PATH, line));
								missingPaths.add(line);
							}
						}
						else if (file.exists()) {
							if (file.isDirectory()) {
								mElements.add(new ListElement(FOLDER, line));
							}
							else {
								if (ImageUtil.isImage(file, false)) {
									mElements.add(new ListElement(FILE, line));
								}
								else {
									Log.w(Application.TAG, "File " + line + " is not an image file");
								}
							}
						}
						else {
							Log.w(Application.TAG, "Cannot find file " + line);
							mElements.add(new ListElement(MISSING_PATH, line));
							missingPaths.add(line);
						}
						continue;
					}

					// handle properties
					if (line.contains(PROPERTY_SEPARATOR)) {
						int index = line.indexOf(PROPERTY_SEPARATOR);
						String name = line.substring(0, index);
						String value = line.substring(index + 1);

						boolean found = false;
						for (ListElement.Type type : ListElement.Type.values()) {
							if (type.hasPrefix()) {
								if (name.startsWith(type.getPrefix() + "[")) {
									Matcher matcher = type.getElementPattern().matcher(name);
									if (matcher.find()) {
										int propertyIndex = Integer.parseInt(matcher.group(1));
										ListElement element = new ListElement(type, value);
										mElements.add(element);
										indexMap.put(element, propertyIndex);
									}
									found = true;
								}
								else if (name.startsWith(type.getPrefix() + ".")) {
									Matcher matcher = type.getPropertyPattern().matcher(name);
									if (matcher.find()) {
										SparseArray<Properties> nestedPropertiesArray = nestedPropertiesMap.get(type);
										String propertyName = matcher.group(1);
										int propertyIndex = Integer.parseInt(matcher.group(2));
										if (nestedPropertiesArray != null) { // SUPPRESS_CHECKSTYLE
											if (nestedPropertiesArray.get(propertyIndex) == null) { // SUPPRESS_CHECKSTYLE
												nestedPropertiesArray.put(propertyIndex, new Properties());
											}
											nestedPropertiesArray.get(propertyIndex).setProperty(propertyName, value);
										}
									}
									found = true;
								}
							}
						}

						if (!found) {
							mProperties.setProperty(name, value);
						}
					}
				}
				scanner.close();
			}
			catch (FileNotFoundException e) {
				Log.e(Application.TAG, "Could not find configuration file", e);
			}

			if (missingPaths.size() > 1) {
				if (toastIfFilesMissing) {
					DialogUtil.displayToast(Application.getAppContext(), R.string.toast_failed_to_load_files, getListName(),
							missingPaths.size());
				}
			}
			else if (missingPaths.size() == 1) {
				if (toastIfFilesMissing) {
					DialogUtil.displayToast(Application.getAppContext(), R.string.toast_failed_to_load_files_single, getListName());
				}
			}
			NotificationUtil.notifyNotFoundFiles(Application.getAppContext(), getListName(), missingPaths);

			// Nested properties are filled now.

			for (ListElement.Type type : ListElement.Type.values()) {
				if (type.hasPrefix()) {
					for (ListElement element : getElements(type)) {
						Integer nestedElementIndex = indexMap.get(element);
						if (nestedElementIndex != null && nestedPropertiesMap.get(type) != null) {
							Properties nestedProperties = nestedPropertiesMap.get(type).get(nestedElementIndex);
							if (nestedProperties == null) {
								element.setProperties(new Properties());
							}
							else {
								element.setProperties(nestedProperties);
							}
						}
					}
				}
			}
		}

		TrackingUtil.sendTiming(Category.TIME_BACKGROUND, "Load image list", "config file", System.currentTimeMillis() - timestamp);
	}

	/**
	 * Save the list of image file names to the config file.
	 *
	 * @return true if successful.
	 */
	private synchronized boolean save() {
		File backupFile = new File(mConfigFile.getParentFile(), mConfigFile.getName() + ".bak");

		if (mConfigFile.exists()) {
			boolean success = mConfigFile.renameTo(backupFile);
			if (!success) {
				Log.e(Application.TAG, "Could not backup config file to " + backupFile.getAbsolutePath());
				DialogUtil.displayToast(Application.getAppContext(), R.string.toast_failed_to_save_list, getListName());
				NotificationUtil.displayNotification(Application.getAppContext(), getListName(), NotificationType.ERROR_SAVING_LIST,
						R.string.title_notification_failed_saving, R.string.toast_failed_to_save_list, getListName());
				return false;
			}
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(mConfigFile));
			writer.println("# Properties");
			for (String key : mProperties.stringPropertyNames()) {
				writer.println(key + PROPERTY_SEPARATOR + mProperties.getProperty(key));
			}

			for (ListElement.Type type : ListElement.Type.values()) {
				if (type.hasPrefix()) {
					ArrayList<ListElement> elements = getElements(type);
					if (elements.size() > 0) {
						writer.println();
						writer.println("# " + type.getDescription() + " names");
						for (int i = 0; i < elements.size(); i++) {
							ListElement element = elements.get(i);
							writer.println(type.getPrefix() + "[" + i + "]" + PROPERTY_SEPARATOR + element.getName());
							Properties nestedProperties = element.getProperties();
							if (nestedProperties != null) {
								for (String nestedKey : nestedProperties.stringPropertyNames()) {
									writer.println(type.getPrefix() + "." + nestedKey + "[" + i + "]" + PROPERTY_SEPARATOR
											+ nestedProperties.getProperty(nestedKey));
								}
							}
						}
					}
				}
				else {
					if (hasElements(type)) {
						writer.println();
						writer.println("# " + type.getDescription() + " names");
						for (String name : getElementNames(type)) {
							writer.println(name);
						}
					}
				}
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
			Log.e(Application.TAG, "Could not store configuration to file " + mConfigFile.getAbsolutePath(), e);
			DialogUtil.displayToast(Application.getAppContext(), R.string.toast_failed_to_save_list, getListName());
			NotificationUtil.displayNotification(Application.getAppContext(), getListName(), NotificationType.ERROR_SAVING_LIST,
					R.string.title_notification_failed_saving, R.string.toast_failed_to_save_list, getListName());
			return false;
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
		NotificationUtil.cancelNotification(Application.getAppContext(), getListName(), NotificationType.ERROR_SAVING_LIST);
		return true;
	}

	/**
	 * Get the name of the list.
	 *
	 * @return The name of the list.
	 */
	public final String getListName() {
		String listName = mProperties.getProperty(PROP_LIST_NAME);

		if (listName == null) {
			listName = ImageRegistry.getListNameFromFileName(mConfigFile);
		}

		if (listName == null) {
			listName = Application.getResourceString(R.string.default_list_name);
		}

		return listName;
	}

	/**
	 * Set the name of the list without changing the file name.
	 *
	 * @param listName The new name of the list.
	 */
	private void setListName(final String listName) {
		if (listName == null) {
			mProperties.remove(PROP_LIST_NAME);
		}
		else {
			mProperties.setProperty(PROP_LIST_NAME, listName);
		}
		save();
	}

	/**
	 * Change the list name, also renaming the config file accordingly.
	 *
	 * @param listName The new name of the list.
	 * @param newConfigFile The new config file.
	 * @return true if successful.
	 */
	public final boolean changeListName(final String listName, final File newConfigFile) {
		File oldConfigFile = mConfigFile;
		String oldListName = getListName();
		mConfigFile = newConfigFile;
		setListName(listName);
		boolean success = save();

		if (success) {
			success = oldConfigFile.delete();
			if (!success) {
				Log.e(Application.TAG, "Could not delete old config file " + oldConfigFile.getAbsolutePath());
			}
			WidgetSettingsActivity.updateListName(oldListName, listName);
			NotificationSettingsActivity.updateListName(oldListName, listName);
			return success;
		}
		else {
			Log.e(Application.TAG, "Could not save to new config file " + mConfigFile.getAbsolutePath());
			mConfigFile = oldConfigFile;
			return false;
		}
	}

	/**
	 * Check if the list has no elements yet.
	 *
	 * @return True if there are no elements in the list.
	 */
	public final boolean isEmpty() {
		synchronized (mElements) {
			return mElements.size() == 0;
		}
	}

	/**
	 * Add a file name. This does not yet update the list of all images!
	 *
	 * @param fileName The file name to be added.
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
		ListElement element = new ListElement(FILE, fileName);

		synchronized (mElements) {
			if (mElements.contains(element)) {
				return false;
			}
			else {
				mElements.add(element);
				return true;
			}
		}
	}

	/**
	 * Add a file from an Uri. The lists are kept up to date, but are not saved!
	 *
	 * @param imageUri The uri of the file to be added.
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
	 * @param folderName The folder name to be added.
	 * @return true if the folder was not in the list before and hence has been added.
	 */
	public final boolean addFolder(final String folderName) {
		if (folderName == null) {
			return false;
		}

		File file = new File(folderName);
		boolean isRecursive = file.getName().equals("*");
		if (isRecursive) {
			file = file.getParentFile();
		}
		if (!file.exists() || !file.isDirectory()) {
			return false;
		}
		ListElement element = new ListElement(FOLDER, folderName);

		synchronized (mElements) {
			if (mElements.contains(element)) {
				return false;
			}
			else {
				mElements.add(element);
				if (isRecursive) {
					ImageUtil.checkForQuickParsing(folderName);
				}
				return true;
			}
		}
	}

	/**
	 * Add a list name. This does not yet update the list of all images!
	 *
	 * @param nestedListName The list name to be added.
	 * @return true if the given list was not included in the current list before and hence has been added.
	 */
	public final boolean addNestedList(final String nestedListName) {
		ListElement nestedList = new ListElement(NESTED_LIST, nestedListName);

		synchronized (mElements) {
			if (nestedListName == null || nestedListName.equals(getListName()) || mElements.contains(nestedList)) {
				return false;
			}

			if (!ImageRegistry.getImageListNames(ListFiltering.ALL_LISTS).contains(nestedListName)) {
				return false;
			}

			ImageList otherImageList = ImageRegistry.getImageListByName(nestedListName, true);
			if (otherImageList == null || otherImageList.containsNestedList(getListName(), true)) {
				DialogUtil.displayToast(Application.getAppContext(), R.string.toast_cyclic_nesting, nestedListName);
				return false;
			}

			mElements.add(nestedList);
		}
		return true;
	}

	/**
	 * Remove a single element. This does not yet update the list of all images!
	 *
	 * @param type The type of the element.
	 * @param name The name of the element to be removed.
	 * @return true if the element was removed.
	 */
	// OVERRIDABLE
	public boolean remove(final ListElement.Type type, final String name) {
		synchronized (mElements) {
			return mElements.remove(new ListElement(type, name));
		}
	}

	/**
	 * Add all SD root folders to the list.
	 */
	public final void addAllSdRoots() {
		addFolder(FileUtil.SD_CARD_PATH + ImageUtil.RECURSIVE_SUFFIX);

		for (String path : FileUtil.getExtSdCardPaths()) {
			addFolder(path + ImageUtil.RECURSIVE_SUFFIX);
		}
		save();
		init(false);
		load(false);
	}

	/**
	 * Get the probability that the given list element is selected as random image.
	 *
	 * @param type The type of the list element.
	 * @param name The name of the list element.
	 * @return The probability.
	 */
	public abstract double getProbability(ListElement.Type type, String name);

	/**
	 * Get the percentage of all images contained in a nested list or folder.
	 *
	 * @param type The type of the list element.
	 * @param name The name of the list element.
	 * @return The percentage of all images contained in this list element.
	 */
	public abstract double getPercentage(ListElement.Type type, String name);

	/**
	 * Class holding information on an image file.
	 */
	protected static final class ImageListInfo {
		/**
		 * The name of the image list.
		 */
		private String mName;

		protected String getName() {
			return mName;
		}

		/**
		 * The image list configuration file.
		 */
		private File mConfigFile;

		protected File getConfigFile() {
			return mConfigFile;
		}

		/**
		 * The class handling the image list.
		 */
		private Class<? extends ImageList> mListClass;

		protected Class<? extends ImageList> getListClass() { // SUPPRESS_CHECKSTYLE
			return mListClass;
		}

		/**
		 * Constructor for the class.
		 *
		 * @param name The name of the image list.
		 * @param configFile The image list configuration file.
		 * @param listClass The class handling the image list.
		 */
		protected ImageListInfo(final String name, final File configFile, final Class<? extends ImageList> listClass) {
			this.mName = name;
			this.mConfigFile = configFile;
			this.mListClass = listClass;
		}

	}

}
