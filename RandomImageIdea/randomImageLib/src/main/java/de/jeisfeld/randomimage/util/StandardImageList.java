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
	private volatile Map<String, ArrayList<String>> mImageFilesByNestedList = null;

	/**
	 * The current weights of the nested lists, indicating how frequently the images of this list are selected.
	 */
	private volatile Map<String, Double> mNestedListWeights = new HashMap<>();

	/**
	 * The nested weights that have been set customly.
	 */
	private volatile Map<String, Double> mCustomNestedListWeights = new HashMap<>();

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
	 * @param configFile          the configuration file of this list.
	 * @param toastIfFilesMissing Flag indicating if a toast should be shown if files are missing.
	 */
	protected StandardImageList(final File configFile, final boolean toastIfFilesMissing) {
		super(configFile, toastIfFilesMissing);
	}

	/**
	 * Create a new image list.
	 *
	 * @param configFile the configuration file of this list.
	 * @param listName   the name of the list.
	 * @param cloneFile  If existing, then the new list will be cloned from this file.
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
		mImageFilesByNestedList = null;

		mNestedListWeights = new HashMap<>();
		mCustomNestedListWeights = new HashMap<>();
		mRandom = new Random();

		mAsyncLoader = new AsyncLoader(getAsyncRunnable(toastIfFilesMissing));
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
			Log.w(Application.TAG, "Did not get a random nested list.");
			return getRandomFileNameFromAllFiles();
		}

		ArrayList<String> nestedImageFiles = mImageFilesByNestedList.get(nestedList);

		if (nestedImageFiles == null || nestedImageFiles.size() == 0) {
			Log.w(Application.TAG, "Got an empty nested list.");
			return getRandomFileNameFromAllFiles();
		}

		return nestedImageFiles.get(mRandom.nextInt(nestedImageFiles.size()));
	}

	/**
	 * Get a random file name from all files in the list.
	 *
	 * @return A random file name.
	 */
	public String getRandomFileNameFromAllFiles() {
		ArrayList<String> allImageFiles = getAllImageFiles();
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

	@Override
	public boolean isReady() {
		return mAsyncLoader.isReady();
	}

	@Override
	public void executeWhenReady(final Runnable whileLoading, final Runnable afterLoading, final Runnable ifError) {
		mAsyncLoader.executeWhenReady(whileLoading, afterLoading, ifError);
	}

	@Override
	public ArrayList<String> getAllImageFiles() {
		mAsyncLoader.waitUntilReady();
		Set<String> allImageFiles = new HashSet<>();
		for (ArrayList<String> nestedListImages : mImageFilesByNestedList.values()) {
			allImageFiles.addAll(nestedListImages);
		}
		return new ArrayList<>(allImageFiles);
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
		if (mImageFilesByNestedList == null) {
			return 0;
		}
		ArrayList<String> images = mImageFilesByNestedList.get(nestedListName);
		return images == null ? 0 : images.size();
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
		if (mImageFilesByNestedList == null) {
			return 0.0;
		}
		int totalImageCount = 0;
		for (String otherListName : mImageFilesByNestedList.keySet()) {
			totalImageCount += mImageFilesByNestedList.get(otherListName).size();
		}
		if (totalImageCount == 0 || mImageFilesByNestedList.get(nestedListName) == null) {
			return 0.0;
		}
		else {
			return ((double) mImageFilesByNestedList.get(nestedListName).size()) / totalImageCount;
		}
	}

	/**
	 * Set the weight of a nested list.
	 *
	 * @param nestedListName The name of the nested list.
	 * @param weight         The custom weight that should be set.
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
		if (mImageFilesByNestedList == null || mImageFilesByNestedList.keySet().size() == 0) {
			return;
		}

		Set<String> listsWithCustomWeight = mCustomNestedListWeights.keySet();

		// next calculate the weights of all components
		double remainingWeight = 1;
		for (String otherNestedList : mCustomNestedListWeights.keySet()) {
			remainingWeight -= mCustomNestedListWeights.get(otherNestedList);
		}

		int remainingPictures = 0;
		for (String otherNestedList : mImageFilesByNestedList.keySet()) {
			if (!listsWithCustomWeight.contains(otherNestedList)) {
				remainingPictures += mImageFilesByNestedList.get(otherNestedList).size();
			}
		}

		for (String otherNestedList : mImageFilesByNestedList.keySet()) {
			if (listsWithCustomWeight.contains(otherNestedList)) {
				mNestedListWeights.put(otherNestedList, mCustomNestedListWeights.get(otherNestedList));
			}
			else {
				if (remainingPictures > 0) {
					mNestedListWeights.put(otherNestedList,
							remainingWeight * mImageFilesByNestedList.get(otherNestedList).size() / remainingPictures);
				}
			}
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
				final ArrayList<String> folderNames = getFolderNames();
				final ArrayList<String> fileNames = getFileNames();
				final ArrayList<String> nestedListNames = getNestedListNames();

				Map<String, ArrayList<String>> imageFilesByNestedListNew = new HashMap<>();

				Set<String> allImageFileSet = new HashSet<>();

				for (String folderName : folderNames) {
					allImageFileSet.addAll(getImageFilesInFolder(folderName));
				}
				for (String fileName : fileNames) {
					allImageFileSet.add(fileName);
				}

				imageFilesByNestedListNew.put(DEFAULT_NESTED_LIST, new ArrayList<>(allImageFileSet));

				for (String nestedListName : nestedListNames) {
					ImageList nestedImageList = ImageRegistry.getImageListByName(nestedListName, toastIfFilesMissing);
					if (nestedImageList != null) {
						ArrayList<String> nestedListFiles = nestedImageList.getAllImageFiles();
						imageFilesByNestedListNew.put(nestedListName, nestedListFiles);
					}
					String customWeightString = getNestedListProperty(nestedListName, PARAM_WEIGHT);
					if (customWeightString != null) {
						double customWeight = Double.parseDouble(customWeightString);
						mCustomNestedListWeights.put(nestedListName, customWeight);
					}
				}

				// Set it only here so that it is only visible when completed, and has always a complete state.
				mImageFilesByNestedList = imageFilesByNestedListNew;

				calculateWeights();
			}
		};
	}
}
