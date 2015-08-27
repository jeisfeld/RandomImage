package de.jeisfeld.randomimage.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.util.Log;
import de.jeisfeld.randomimage.Application;

/**
 * Subclass of ImageList, giving each file the same weight.
 */
public final class StandardImageList extends ImageList {
	/**
	 * The name of the default nested list (containing the non-nested items).
	 */
	private static final String DEFAULT_NESTED_LIST = "";

	/**
	 * Name of the parameter so store the weight.
	 */
	private static final String PARAM_WEIGHT = "weight";

	/**
	 * All image files represented by this image list, grouped by nested list. All non-grouped items are linked to
	 * virtual list "".
	 */
	private volatile Map<String, ArrayList<String>> imageFilesByNestedList = null;

	/**
	 * The current weights of the nested lists, indicating how frequently the images of this list are selected.
	 */
	private volatile Map<String, Double> nestedListWeights = new HashMap<String, Double>();

	/**
	 * The nested weights that have been set customly.
	 */
	private volatile Map<String, Double> customNestedListWeights = new HashMap<String, Double>();

	/**
	 * The thread currently loading the allImageFiles list.
	 */
	private volatile Thread loaderThread;

	/**
	 * The thread waiting to load the allImageFile list.
	 */
	private volatile Thread waitingLoaderThread;

	/**
	 * The random number generator.
	 */
	private Random random;

	/**
	 * Create an image list and load it from its file, if existing.
	 *
	 * @param configFile
	 *            the configuration file of this list.
	 *
	 */
	protected StandardImageList(final File configFile) {
		super(configFile);
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
	protected StandardImageList(final File configFile, final String listName, final File cloneFile) {
		super(configFile, listName, cloneFile);
	}

	@Override
	public synchronized void load() {
		super.load();

		// Asynchronously parse the list of images, working on a copy of file and folder names to prevent concurrent
		// modification.
		final ArrayList<String> folderNames = getFolderNames();
		final ArrayList<String> fileNames = getFileNames();
		final ArrayList<String> nestedListNames = getNestedListNames();

		waitingLoaderThread = new Thread() {
			@Override
			public void run() {
				Map<String, ArrayList<String>> imageFilesByNestedListNew = new HashMap<String, ArrayList<String>>();

				Set<String> allImageFileSet = new HashSet<String>();

				for (String folderName : folderNames) {
					allImageFileSet.addAll(getImageFilesInFolder(folderName));
				}
				for (String fileName : fileNames) {
					allImageFileSet.add(fileName);
				}

				imageFilesByNestedListNew.put(DEFAULT_NESTED_LIST, new ArrayList<String>(allImageFileSet));

				for (String nestedListName : nestedListNames) {
					ImageList nestedImageList = ImageRegistry.getImageListByName(nestedListName);
					if (nestedImageList != null) {
						ArrayList<String> nestedListFiles = nestedImageList.getAllImageFiles();
						imageFilesByNestedListNew.put(nestedListName, nestedListFiles);
					}
					String customWeightString = getNestedListProperty(nestedListName, PARAM_WEIGHT);
					if (customWeightString != null) {
						double customWeight = Double.parseDouble(customWeightString);
						customNestedListWeights.put(nestedListName, customWeight);
					}
				}

				// Set it only here so that it is only visible when completed, and has always a complete state.
				imageFilesByNestedList = imageFilesByNestedListNew;

				calculateWeights();

				synchronized (this) {
					if (waitingLoaderThread != null) {
						loaderThread = waitingLoaderThread;
						waitingLoaderThread = null;
						loaderThread.start();
					}
					else {
						loaderThread = null;
					}
				}
			}
		};

		if (loaderThread == null) {
			loaderThread = waitingLoaderThread;
			waitingLoaderThread = null;
			loaderThread.start();
		}
	}

	/**
	 * Do initialization steps of this subclass of ImageList.
	 */
	@Override
	protected void init() {
		// This needs to be null, as it is taken as criterion for successful loading.
		imageFilesByNestedList = null;

		nestedListWeights = new HashMap<String, Double>();
		customNestedListWeights = new HashMap<String, Double>();
		random = new Random();
	}

	/**
	 * Get the list of file names, including the files in the configured folders, in shuffled order.
	 *
	 * @return The list of file names.
	 */
	public String[] getShuffledFileNames() {
		ArrayList<String> clonedList = getAllImageFiles();
		Collections.shuffle(clonedList);
		return clonedList.toArray(new String[0]);
	}

	/**
	 * Get a random file name from the registry.
	 *
	 * @return A random file name.
	 */
	@Override
	public String getRandomFileName() {
		String nestedList = getRandomNestedList();

		if (nestedList == null) {
			Log.e(Application.TAG, "Did not get a random nested list.");
			return getRandomFileNameFromAllFiles();
		}

		ArrayList<String> nestedImageFiles = imageFilesByNestedList.get(nestedList);

		if (nestedImageFiles == null || nestedImageFiles.size() == 0) {
			Log.e(Application.TAG, "Got an empty nested list.");
			return getRandomFileNameFromAllFiles();
		}

		return nestedImageFiles.get(random.nextInt(nestedImageFiles.size()));
	}

	/**
	 * Get a random file name from all files in the list.
	 *
	 * @return A random file name.
	 */
	public String getRandomFileNameFromAllFiles() {
		ArrayList<String> allImageFiles = getAllImageFiles();
		if (allImageFiles != null && allImageFiles.size() > 0) {
			return allImageFiles.get(random.nextInt(allImageFiles.size()));
		}
		else {
			return null;
		}
	}

	/**
	 * Get a nested list according to weight distribution.
	 *
	 * @return A random nested list.
	 */
	private String getRandomNestedList() {
		double randomNumber = random.nextDouble();

		double accumulatedWeights = 0;
		for (String nestedListName : nestedListWeights.keySet()) {
			accumulatedWeights += nestedListWeights.get(nestedListName);
			if (randomNumber < accumulatedWeights) {
				return nestedListName;
			}
		}
		return null;
	}

	@Override
	public boolean isReady() {
		return imageFilesByNestedList != null;
	}

	@Override
	public void waitUntilReady() {
		if (isReady()) {
			return;
		}

		// prevent exception if loaderThread gets deleted after check.
		Thread localLoaderThread = loaderThread;
		if (localLoaderThread != null) {
			try {
				localLoaderThread.join();
			}
			catch (InterruptedException e) {
				// do nothing
			}
		}
	}

	@Override
	public ArrayList<String> getAllImageFiles() {
		waitUntilReady();
		Set<String> allImageFiles = new HashSet<String>();
		for (ArrayList<String> nestedListImages : imageFilesByNestedList.values()) {
			allImageFiles.addAll(nestedListImages);
		}
		return new ArrayList<String>(allImageFiles);
	}

	@Override
	public synchronized boolean update() {
		boolean success = super.update();
		if (success) {
			calculateWeights();
		}
		return success;
	}

	@Override
	public boolean removeNestedList(final String nestedListName) {
		boolean success = super.removeNestedList(nestedListName);
		if (success) {
			setNestedListProperty(nestedListName, PARAM_WEIGHT, null);
			customNestedListWeights.remove(nestedListName);
		}
		return success;
	}

	/**
	 * Get the number of images of a nested list.
	 *
	 * @param nestedListName
	 *            The name of the nested list.
	 * @return The number of images of the nested list.
	 */
	public int getNestedListImageCount(final String nestedListName) {
		if (imageFilesByNestedList == null) {
			return 0;
		}
		ArrayList<String> images = imageFilesByNestedList.get(nestedListName);
		return images == null ? 0 : images.size();
	}

	/**
	 * Get the weight of a nested list.
	 *
	 * @param nestedListName
	 *            The name of the nested list.
	 * @return The weight of the nested list.
	 */
	public double getNestedListWeight(final String nestedListName) {
		if (nestedListWeights == null) {
			return 0;
		}
		Double weight = nestedListWeights.get(nestedListName);
		return weight == null ? 0 : weight;
	}

	/**
	 * Get the custom weight of a nested list.
	 *
	 * @param nestedListName
	 *            The name of the nested list.
	 * @return The custom weight of the nested list, if existing. Otherwise null.
	 */
	public Double getCustomNestedListWeight(final String nestedListName) {
		if (nestedListWeights == null) {
			return null;
		}
		Double weight = customNestedListWeights.get(nestedListName);
		return weight;
	}

	/**
	 * Get the percentage of all images contained in a nested list.
	 *
	 * @param nestedListName
	 *            The name of the nested list.
	 * @return The percentage of all images contained in this list.
	 */
	public Double getImagePercentage(final String nestedListName) {
		if (imageFilesByNestedList == null) {
			return 0.0;
		}
		int totalImageCount = 0;
		for (String otherListName : imageFilesByNestedList.keySet()) {
			totalImageCount += imageFilesByNestedList.get(otherListName).size();
		}
		if (totalImageCount == 0 || imageFilesByNestedList.get(nestedListName) == null) {
			return 0.0;
		}
		else {
			return ((double) imageFilesByNestedList.get(nestedListName).size()) / totalImageCount;
		}
	}

	/**
	 * Set the weight of a nested list.
	 *
	 * @param nestedListName
	 *            The name of the nested list.
	 * @param weight
	 *            The custom weight that should be set.
	 * @return True if the custom weight could be set.
	 */
	public boolean setCustomNestedListWeight(final String nestedListName, final Double weight) {
		if (nestedListWeights == null) {
			return false;
		}

		if (weight == null) {
			customNestedListWeights.remove(nestedListName);
			setNestedListProperty(nestedListName, PARAM_WEIGHT, null);
		}
		else if (weight < 0 || weight > 1) {
			return false;
		}
		else {
			customNestedListWeights.put(nestedListName, weight);
			setNestedListProperty(nestedListName, PARAM_WEIGHT, weight.toString());
		}

		Set<String> listsWithCustomWeight = customNestedListWeights.keySet();

		if (weight != null) {
			// If the total sum is bigger than 1, then reduce other weights proportionally.
			double remainingWeight = 1 - weight;
			double sumOfNestedWeights = 0;
			for (String otherNestedList : listsWithCustomWeight) {
				if (!otherNestedList.equals(nestedListName)) {
					sumOfNestedWeights += customNestedListWeights.get(otherNestedList);
				}
			}
			if (sumOfNestedWeights > remainingWeight) {
				double changeFactor = remainingWeight / sumOfNestedWeights;
				for (String otherNestedList : listsWithCustomWeight) {
					if (!otherNestedList.equals(nestedListName)) {
						double newWeight = changeFactor * customNestedListWeights.get(otherNestedList);
						customNestedListWeights.put(otherNestedList, newWeight);
						setNestedListProperty(otherNestedList, PARAM_WEIGHT, Double.toString(newWeight));
					}
				}
			}
		}

		calculateWeights();
		return true;
	}

	/**
	 * Calculate the weights of all nested lists.
	 */
	private void calculateWeights() {
		if (imageFilesByNestedList == null || imageFilesByNestedList.keySet().size() == 0) {
			return;
		}

		Set<String> listsWithCustomWeight = customNestedListWeights.keySet();

		// next calculate the weights of all components
		double remainingWeight = 1;
		for (String otherNestedList : customNestedListWeights.keySet()) {
			remainingWeight -= customNestedListWeights.get(otherNestedList);
		}

		int remainingPictures = 0;
		for (String otherNestedList : imageFilesByNestedList.keySet()) {
			if (!listsWithCustomWeight.contains(otherNestedList)) {
				remainingPictures += imageFilesByNestedList.get(otherNestedList).size();
			}
		}

		for (String otherNestedList : imageFilesByNestedList.keySet()) {
			if (listsWithCustomWeight.contains(otherNestedList)) {
				nestedListWeights.put(otherNestedList, customNestedListWeights.get(otherNestedList));
			}
			else {
				if (remainingPictures > 0) {
					nestedListWeights.put(otherNestedList, remainingWeight * imageFilesByNestedList.get(otherNestedList).size() / remainingPictures);
				}
			}
		}
	}
}
