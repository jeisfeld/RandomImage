package de.jeisfeld.randomimage.util;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;

/**
 * Subclass of ImageList, giving each file the same weight.
 */
public final class StandardImageList extends ImageList {
	/**
	 * Name of the parameter so store the weight.
	 */
	private static final String PARAM_WEIGHT = "weight";

	/**
	 * The image files contained directly in the list.
	 */
	private volatile ArrayList<String> mImageFilesInList = null;

	/**
	 * The image files contained in the list, including nested lists.
	 */
	private volatile ArrayList<String> mAllImageFilesInList = null;

	/**
	 * The current weights of the nested lists, indicating how frequently the images of this list are selected.
	 */
	private volatile Map<String, Double> mNestedListWeights = new HashMap<>();

	/**
	 * The nested weights that have been set customly. This map is also used for synchronization of updates.
	 */
	private final Map<String, Double> mCustomNestedListWeights = new HashMap<>();

	/**
	 * The nested lists contained in this list.
	 */
	private volatile Map<String, ImageList> mNestedLists = new HashMap<>();

	/**
	 * The random number generator.
	 */
	private Random mRandom;

	/**
	 * An asynchronous loader for the image lists.
	 */
	private volatile AsyncLoader mAsyncLoader;

	/**
	 * Create an image list and load it from its file, if existing.
	 *
	 * @param configFile the configuration file of this list.
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 */
	protected StandardImageList(final File configFile, final boolean toastIfFilesMissing) {
		super(configFile, toastIfFilesMissing);
	}

	/**
	 * Create a new image list.
	 *
	 * @param configFile the configuration file of this list.
	 * @param listName the name of the list.
	 * @param cloneFile If existing, then the new list will be cloned from this file.
	 */
	protected StandardImageList(final File configFile, final String listName, final File cloneFile) {
		super(configFile, listName, cloneFile);
	}

	@Override
	public synchronized void load(final boolean toastIfFilesMissing) {
		super.load(toastIfFilesMissing);
		mAsyncLoader.load();
	}

	@Override
	protected void init(final boolean toastIfFilesMissing) {
		// This needs to be null, as it is taken as criterion for successful loading.
		mImageFilesInList = null;

		mNestedListWeights = new HashMap<>();

		mRandom = new Random();
		mAsyncLoader = new AsyncLoader(getAsyncRunnable(toastIfFilesMissing));
	}

	/**
	 * Get the list of file names, including the files in the image list, in shuffled order.
	 *
	 * @return The list of file names.
	 */
	public ArrayList<String> getShuffledFileNames() {
		ArrayList<String> clonedList = new ArrayList<>(getAllImageFiles());
		Collections.shuffle(clonedList);
		return clonedList;
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
			Log.w(Application.TAG, "Tried to get random file before list was fully loaded.");
			return getRandomFileNameFromAllFiles();
		}

		if ("".equals(nestedList)) {
			if (mImageFilesInList == null || mImageFilesInList.size() == 0) {
				Log.w(Application.TAG, "No files contained directly in list.");
				return getRandomFileNameFromAllFiles();
			}
			return mImageFilesInList.get(mRandom.nextInt(mImageFilesInList.size()));
		}
		else {
			return mNestedLists.get(nestedList).getRandomFileName();
		}
	}

	/**
	 * Get a set of distinct random file names.
	 *
	 * @param numberOfFiles The number of files to retrieve.
	 * @param forbiddenFiles A list of files that should not be retrieved.
	 * @return The result list.
	 */
	public ArrayList<String> getSetOfRandomFileNames(final int numberOfFiles, final List<String> forbiddenFiles) {
		ArrayList<String> allFiles = getShuffledFileNames();
		if (allFiles.size() == 0) {
			return new ArrayList<>();
		}
		allFiles.removeAll(forbiddenFiles);

		if (numberOfFiles > allFiles.size()) {
			// If list not sufficient, then allow duplicates and forbidden files for further addition.
			allFiles.addAll(getSetOfRandomFileNames(numberOfFiles - allFiles.size(), new ArrayList<String>()));
			return allFiles;
		}
		if (numberOfFiles == allFiles.size()) {
			return allFiles;
		}
		if (numberOfFiles >= allFiles.size() / 2) {
			// Do not consider weights if we have to get more than half of the images.
			return new ArrayList<>(allFiles.subList(0, numberOfFiles));
		}

		ArrayList<String> resultList = new ArrayList<>();

		// Try first to get the files in a random way.
		for (int counter = 0; counter < 3 * numberOfFiles + 50 && resultList.size() < numberOfFiles; counter++) { // MAGIC_NUMBER
			String fileName = getRandomFileName();
			if (!forbiddenFiles.contains(fileName) && !resultList.contains(fileName)) {
				resultList.add(fileName);
			}
		}

		if (resultList.size() < numberOfFiles) {
			allFiles.removeAll(resultList);
			resultList.addAll(allFiles.subList(0, numberOfFiles - resultList.size()));
		}

		return resultList;
	}

	/**
	 * Get a random file name from all files in the list.
	 *
	 * @return A random file name.
	 */
	private String getRandomFileNameFromAllFiles() {
		List<String> allImageFiles = getAllImageFiles();
		if (allImageFiles != null && allImageFiles.size() > 0) {
			return allImageFiles.get(mRandom.nextInt(allImageFiles.size()));
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
		synchronized (mCustomNestedListWeights) {
			double randomNumber = mRandom.nextDouble();
			double accumulatedWeights = 0;
			for (String nestedListName : mNestedListWeights.keySet()) {
				accumulatedWeights += mNestedListWeights.get(nestedListName);
				if (randomNumber < accumulatedWeights) {
					return nestedListName;
				}
			}
			return null;
		}
	}

	@Override
	public boolean isReady() {
		return mAsyncLoader.isReady();
	}

	@Override
	public void executeWhenReady(final Runnable whileLoading, final Runnable afterLoading, final Runnable ifError) {
		mAsyncLoader.executeWhenReady(whileLoading, afterLoading, ifError);
	}

	@Override
	public void waitUntilReady() {
		mAsyncLoader.waitUntilReady();
	}

	@Override
	public List<String> getAllImageFiles() {
		mAsyncLoader.waitUntilReady();
		return mAllImageFilesInList;
	}

	@Override
	public double getProbability(final String fileName) {
		File file = new File(fileName);
		double nonNestedWeight = getNestedListWeight("");
		if (nonNestedWeight == 0) {
			return 0;
		}
		int nonNestedSize = mImageFilesInList.size();

		if (file.getName().equals("*") || file.isDirectory()) {
			if (getFolderNames().contains(fileName)) {
				return nonNestedWeight * ImageUtil.getImagesInFolder(fileName).size() / nonNestedSize;
			}
			else {
				return 0;
			}
		}
		else if (file.isFile()) {
			if (getFileNames().contains(fileName)) {
				return nonNestedWeight / nonNestedSize;
			}
			else {
				return 0;
			}
		}
		else {
			return 0;
		}
	}

	@Override
	public synchronized boolean update(final boolean toastIfFilesMissing) {
		boolean success = super.update(toastIfFilesMissing);
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
			mCustomNestedListWeights.remove(nestedListName);
		}
		return success;
	}

	/**
	 * Get the number of images of a nested list.
	 *
	 * @param nestedListName The name of the nested list.
	 * @return The number of images of the nested list.
	 */
	public int getNestedListImageCount(final String nestedListName) {
		if (mNestedLists == null) {
			return 0;
		}
		synchronized (mCustomNestedListWeights) {
			ImageList nestedList = mNestedLists.get(nestedListName);
			return nestedList == null ? 0 : nestedList.getAllImageFiles().size();
		}
	}

	/**
	 * Get the weight of a nested list.
	 *
	 * @param nestedListName The name of the nested list.
	 * @return The weight of the nested list.
	 */
	public double getNestedListWeight(final String nestedListName) {
		if (mNestedListWeights == null) {
			return 0;
		}
		Double weight = mNestedListWeights.get(nestedListName);
		return weight == null ? 0 : weight;
	}

	/**
	 * Get the custom weight of a nested list.
	 *
	 * @param nestedListName The name of the nested list.
	 * @return The custom weight of the nested list, if existing. Otherwise null.
	 */
	public Double getCustomNestedListWeight(final String nestedListName) {
		if (mNestedListWeights == null) {
			return null;
		}
		return mCustomNestedListWeights.get(nestedListName);
	}

	/**
	 * Get the percentage of all images contained in a nested list.
	 *
	 * @param nestedListName The name of the nested list.
	 * @return The percentage of all images contained in this list.
	 */
	public Double getImagePercentage(final String nestedListName) {
		if (mNestedLists == null) {
			return 0.0;
		}
		int nestedListImageCount = getNestedListImageCount(nestedListName);
		if (nestedListImageCount == 0) {
			return 0.0;
		}

		int totalImageCount = 0;
		for (String otherListName : mNestedLists.keySet()) {
			totalImageCount += getNestedListImageCount(otherListName);
		}
		if (totalImageCount == 0) {
			return 0.0;
		}

		return (double) nestedListImageCount / totalImageCount;
	}

	/**
	 * Set the weight of a nested list.
	 *
	 * @param nestedListName The name of the nested list.
	 * @param weight The custom weight that should be set.
	 * @return True if the custom weight could be set.
	 */
	public boolean setCustomNestedListWeight(final String nestedListName, final Double weight) {
		if (mNestedListWeights == null) {
			return false;
		}

		if (weight == null) {
			mCustomNestedListWeights.remove(nestedListName);
			setNestedListProperty(nestedListName, PARAM_WEIGHT, null);
		}
		else if (weight < 0 || weight > 1) {
			return false;
		}
		else {
			mCustomNestedListWeights.put(nestedListName, weight);
			setNestedListProperty(nestedListName, PARAM_WEIGHT, weight.toString());
		}

		Set<String> listsWithCustomWeight = mCustomNestedListWeights.keySet();

		if (weight != null) {
			// If the total sum is bigger than 1, then reduce other weights proportionally.
			double remainingWeight = 1 - weight;
			double sumOfNestedWeights = 0;
			for (String otherNestedList : listsWithCustomWeight) {
				if (!otherNestedList.equals(nestedListName)) {
					sumOfNestedWeights += mCustomNestedListWeights.get(otherNestedList);
				}
			}
			if (sumOfNestedWeights > remainingWeight) {
				double changeFactor = remainingWeight / sumOfNestedWeights;
				for (String otherNestedList : listsWithCustomWeight) {
					if (!otherNestedList.equals(nestedListName)) {
						double newWeight = changeFactor * mCustomNestedListWeights.get(otherNestedList);
						mCustomNestedListWeights.put(otherNestedList, newWeight);
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
		if (mNestedLists == null || mCustomNestedListWeights == null || mImageFilesInList == null) {
			return;
		}

		synchronized (mCustomNestedListWeights) {
			Set<String> listsWithCustomWeight = mCustomNestedListWeights.keySet();

			// next calculate the weights of all components
			double remainingWeight = 1;
			for (String otherNestedList : mCustomNestedListWeights.keySet()) {
				remainingWeight -= mCustomNestedListWeights.get(otherNestedList);
			}

			int remainingPictures = 0;
			for (String otherNestedList : mNestedLists.keySet()) {
				if (!listsWithCustomWeight.contains(otherNestedList)) {
					remainingPictures += mNestedLists.get(otherNestedList).getAllImageFiles().size();
				}
			}
			remainingPictures += mImageFilesInList.size();

			for (String otherNestedList : mNestedLists.keySet()) {
				if (listsWithCustomWeight.contains(otherNestedList)) {
					mNestedListWeights.put(otherNestedList, mCustomNestedListWeights.get(otherNestedList));
				}
				else {
					if (remainingPictures > 0) {
						mNestedListWeights.put(otherNestedList,
								remainingWeight * mNestedLists.get(otherNestedList).getAllImageFiles().size() / remainingPictures);
					}
				}
			}
			mNestedListWeights.put("", remainingWeight * mImageFilesInList.size() / remainingPictures);
		}
	}

	/**
	 * Instantiate the runnable loading the image lists.
	 *
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 * @return The runnable loading the image lists.
	 */
	private Runnable getAsyncRunnable(final boolean toastIfFilesMissing) {
		return new Runnable() {
			@Override
			public void run() {
				final long timestamp = System.currentTimeMillis();
				final ArrayList<String> folderNames = getFolderNames();
				final ArrayList<String> fileNames = getFileNames();
				final ArrayList<String> nestedListNames = getNestedListNames();
				final Map<String, ImageList> nestedLists = new HashMap<>();

				Set<String> imageFileSet = new HashSet<>();
				Set<String> allImageFileSet = new HashSet<>();

				for (String folderName : folderNames) {
					imageFileSet.addAll(ImageUtil.getImagesInFolder(folderName));
				}
				for (String fileName : fileNames) {
					imageFileSet.add(fileName);
				}
				allImageFileSet.addAll(imageFileSet);

				for (String nestedListName : nestedListNames) {
					ImageList nestedImageList = ImageRegistry.getImageListByName(nestedListName, toastIfFilesMissing);
					if (nestedImageList != null) {
						nestedLists.put(nestedListName, nestedImageList);
						allImageFileSet.addAll(nestedImageList.getAllImageFiles());
					}
					String customWeightString = getNestedListProperty(nestedListName, PARAM_WEIGHT);
					if (customWeightString != null) {
						double customWeight = Double.parseDouble(customWeightString);
						mCustomNestedListWeights.put(nestedListName, customWeight);
					}
				}

				// Set it only here so that it is only visible when completed, and has always a complete state.
				mAllImageFilesInList = new ArrayList<>(allImageFileSet);
				mImageFilesInList = new ArrayList<>(imageFileSet);
				mNestedLists = nestedLists;

				calculateWeights();

				TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "All images in list", null, (long) mAllImageFilesInList.size());
				TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "Images in list", null, (long) fileNames.size());
				TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "Folders in list", null, (long) folderNames.size());
				TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "Nested lists in list", null, (long) nestedLists.size());
				TrackingUtil.sendTiming(Category.TIME_BACKGROUND, "Load image list", "all images", System.currentTimeMillis() - timestamp);
			}
		};
	}
}
