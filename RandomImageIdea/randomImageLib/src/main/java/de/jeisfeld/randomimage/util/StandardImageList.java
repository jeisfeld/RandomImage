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

import static de.jeisfeld.randomimage.util.ListElement.DUMMY_NESTED_FOLDER;
import static de.jeisfeld.randomimage.util.ListElement.Type.FILE;
import static de.jeisfeld.randomimage.util.ListElement.Type.FOLDER;
import static de.jeisfeld.randomimage.util.ListElement.Type.NESTED_LIST;

/**
 * Subclass of ImageList, giving each file the same weight.
 */
public final class StandardImageList extends ImageList {
	/**
	 * Name of the parameter so store the weight.
	 */
	private static final String PARAM_WEIGHT = "weight";

	/**
	 * The image files contained in folders of the list, either weighted or dummy.
	 */
	private final HashMap<String, ArrayList<String>> mImageFilesInFolders = new HashMap<>();

	/**
	 * The image files contained in the list, including nested lists.
	 */
	private volatile ArrayList<String> mAllImageFilesInList = null;

	/**
	 * The current weights of the nested lists, indicating how frequently the images of this list are selected.
	 */
	private volatile Map<ListElement, Double> mNestedElementWeights = new HashMap<>();

	/**
	 * The nested weights that have been set customly. This map is also used for synchronization of updates.
	 */
	private final Map<ListElement, Double> mCustomWeights = new HashMap<>();

	/**
	 * The nested lists contained in this list.
	 */
	private volatile Map<ListElement, ImageList> mNestedLists = new HashMap<>();

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
		// This needs to be removed, as it is taken as criterion for successful loading.
		mAllImageFilesInList = null;

		mNestedElementWeights = new HashMap<>();

		mRandom = new Random(System.currentTimeMillis());
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
		ListElement weightedElement = getRandomWeightedElement();
		if (weightedElement == null) {
			Log.w(Application.TAG, "Tried to get random file before list was fully loaded, or sum of weights is below 100%");
			return getRandomFileNameFromAllFiles();
		}

		if (weightedElement.getType() == NESTED_LIST) {
			ImageList imageList = mNestedLists.get(weightedElement);
			if (imageList != null) {
				return imageList.getRandomFileName();
			}
			else {
				Log.w(Application.TAG, "Tried to get random file while list was not available");
				return getRandomFileNameFromAllFiles();
			}
		}
		else {
			// Folder or dummy
			ArrayList<String> filesInFolder = mImageFilesInFolders.get(weightedElement.getName());
			if (filesInFolder == null || filesInFolder.isEmpty()) {
				Log.w(Application.TAG, "No files contained in selected folder.");
				return getRandomFileNameFromAllFiles();
			}
			return filesInFolder.get(mRandom.nextInt(filesInFolder.size()));
		}
	}

	/**
	 * Get a set of distinct random file names.
	 *
	 * @param numberOfFiles  The number of files to retrieve.
	 * @param forbiddenFiles A list of files that should not be retrieved.
	 * @return The result list.
	 */
	public ArrayList<String> getSetOfRandomFileNames(final int numberOfFiles, final List<String> forbiddenFiles) {
		ArrayList<String> allFiles = getShuffledFileNames();
		if (allFiles.isEmpty()) {
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
		if (allImageFiles != null && !allImageFiles.isEmpty()) {
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
	private ListElement getRandomWeightedElement() {
		synchronized (mCustomWeights) {
			double randomNumber = mRandom.nextDouble();
			double accumulatedWeights = 0;
			for (ListElement weightedElement : mNestedElementWeights.keySet()) {
				accumulatedWeights += mNestedElementWeights.get(weightedElement);
				if (randomNumber < accumulatedWeights) {
					return weightedElement;
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
	public double getProbability(final ListElement.Type type, final String name) {
		if (mNestedElementWeights != null) {
			Double weight = mNestedElementWeights.get(new ListElement(type, name));
			if (weight != null) {
				return weight;
			}
		}
		// Prevent infinite loop
		if (DUMMY_NESTED_FOLDER.equals(new ListElement(type, name))) {
			return 0;
		}

		double nonNestedWeight = getProbability(DUMMY_NESTED_FOLDER.getType(), DUMMY_NESTED_FOLDER.getName());
		if (nonNestedWeight == 0) {
			return 0;
		}
		int unweightedSize = mImageFilesInFolders.get(DUMMY_NESTED_FOLDER.getName()) == null ? 0
				: mImageFilesInFolders.get(DUMMY_NESTED_FOLDER.getName()).size();

		File file = new File(name);
		if (file.getName().equals("*") || file.isDirectory()) {
			if (getElementNames(FOLDER).contains(name)) {
				return nonNestedWeight * ImageUtil.getImagesInFolder(name).size() / unweightedSize;
			}
			else {
				return 0;
			}
		}
		else if (file.isFile()) {
			if (getElementNames(FILE).contains(name)) {
				return nonNestedWeight / unweightedSize;
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
	public boolean remove(final ListElement.Type type, final String name) {
		boolean success = super.remove(type, name);
		if (success && type != FILE) {
			mCustomWeights.remove(new ListElement(type, name));
			mNestedElementWeights.remove(new ListElement(type, name));
			calculateWeights();
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
		synchronized (mCustomWeights) {
			ImageList nestedList = mNestedLists.get(new ListElement(NESTED_LIST, nestedListName));
			return nestedList == null ? 0 : nestedList.getAllImageFiles().size();
		}
	}

	/**
	 * Get the custom weight of a nested element.
	 *
	 * @param type the type of the nested element.
	 * @param name The name of the nested element.
	 * @return The custom weight of the nested element, if existing. Otherwise null.
	 */
	public Double getCustomNestedElementWeight(final ListElement.Type type, final String name) {
		if (mNestedElementWeights == null) {
			return null;
		}
		return mCustomWeights.get(new ListElement(type, name));
	}

	@Override
	public double getPercentage(final ListElement.Type type, final String name) {
		int allImageCount = getAllImageFiles().size();
		if (allImageCount == 0) {
			return 0.0;
		}

		int elementImageCount;
		switch (type) {
		case FILE:
			elementImageCount = 1;
			break;
		case FOLDER:
			elementImageCount = ImageUtil.getImagesInFolder(name).size();
			break;
		case NESTED_LIST:
			elementImageCount = getNestedListImageCount(name);
			break;
		default:
			elementImageCount = 0;
		}
		return (double) elementImageCount / allImageCount;
	}

	/**
	 * Set the weight of a nested element.
	 *
	 * @param type   The type of the nested element.
	 * @param name   The name of the nested element.
	 * @param weight The custom weight that should be set.
	 * @return True if the custom weight could be set.
	 */
	public boolean setCustomWeight(final ListElement.Type type, final String name, final Double weight) {
		ListElement element = getElement(type, name);
		if (element == null) {
			return false;
		}
		if (mNestedElementWeights == null) {
			return false;
		}

		synchronized (mCustomWeights) {
			if (weight == null) {
				mCustomWeights.remove(element);
				mNestedElementWeights.remove(element);
				element.getProperties().remove(PARAM_WEIGHT);
			}
			else if (weight < 0 || weight > 1) {
				return false;
			}
			else {
				mCustomWeights.put(element, weight);
				element.getProperties().put(PARAM_WEIGHT, weight.toString());
			}

			Set<ListElement> elementsWithCustomWeight = mCustomWeights.keySet();

			if (weight != null) {
				// If the total sum is bigger than 1, then reduce other weights proportionally.
				double remainingWeight = 1 - weight;
				double sumOfNestedWeights = 0;
				for (ListElement elementWithCustomWeight : elementsWithCustomWeight) {
					if (!elementWithCustomWeight.equals(element)) {
						sumOfNestedWeights += mCustomWeights.get(elementWithCustomWeight);
					}
				}

				boolean hasUnweightedElements = mImageFilesInFolders.containsKey(DUMMY_NESTED_FOLDER.getName())
						&& !mImageFilesInFolders.get(DUMMY_NESTED_FOLDER.getName()).isEmpty();
				if (!hasUnweightedElements) {
					for (ListElement nestedList : mNestedLists.keySet()) {
						if (!mCustomWeights.containsKey(nestedList)) {
							hasUnweightedElements = true;
							break;
						}
					}
				}

				if (sumOfNestedWeights > remainingWeight // BOOLEAN_EXPRESSION_COMPLEXITY
						|| sumOfNestedWeights < remainingWeight && sumOfNestedWeights > 0 && !hasUnweightedElements) {
					double changeFactor = remainingWeight / sumOfNestedWeights;
					for (ListElement otherNestedList : elementsWithCustomWeight) {
						if (!otherNestedList.equals(element)) {
							double newWeight = changeFactor * mCustomWeights.get(otherNestedList);
							mCustomWeights.put(otherNestedList, newWeight);
							getElement(otherNestedList).getProperties().put(PARAM_WEIGHT, Double.toString(newWeight));
						}
					}
				}
			}
		}
		update(false);

		calculateWeights();
		return true;
	}

	/**
	 * Calculate the weights of all nested lists.
	 */
	private void calculateWeights() {
		if (mNestedLists == null || mAllImageFilesInList == null || !mImageFilesInFolders.containsKey(DUMMY_NESTED_FOLDER.getName())) {
			return;
		}

		synchronized (mCustomWeights) {
			Set<ListElement> elementsWithCustomWeight = mCustomWeights.keySet();

			// next calculate the weights of all components
			double remainingWeight = 1;
			for (ListElement nestedElementWithCustomWeight : elementsWithCustomWeight) {
				remainingWeight -= mCustomWeights.get(nestedElementWithCustomWeight);
				mNestedElementWeights.put(nestedElementWithCustomWeight, mCustomWeights.get(nestedElementWithCustomWeight));
			}

			int remainingPictures = 0;
			for (ListElement otherNestedList : mNestedLists.keySet()) {
				if (!elementsWithCustomWeight.contains(otherNestedList)) {
					List<String> filesInOtherNestedList = mNestedLists.get(otherNestedList) == null ? null
							: mNestedLists.get(otherNestedList).getAllImageFiles();
					remainingPictures += filesInOtherNestedList == null ? 0 : filesInOtherNestedList.size();
				}
			}
			int unweightedPictures = mImageFilesInFolders.get(DUMMY_NESTED_FOLDER.getName()) == null ? 0
					: mImageFilesInFolders.get(DUMMY_NESTED_FOLDER.getName()).size();
			remainingPictures += unweightedPictures;

			if (remainingPictures > 0) {
				for (ListElement otherNestedList : mNestedLists.keySet()) {
					if (!elementsWithCustomWeight.contains(otherNestedList)) {
						List<String> filesInOtherNestedList = mNestedLists.get(otherNestedList) == null ? null
								: mNestedLists.get(otherNestedList).getAllImageFiles();
						mNestedElementWeights.put(otherNestedList,
								filesInOtherNestedList == null ? 0 : remainingWeight * filesInOtherNestedList.size() / remainingPictures);
					}
				}
				mNestedElementWeights.put(DUMMY_NESTED_FOLDER, remainingWeight * unweightedPictures / remainingPictures);
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
		return () -> {
			final long timestamp = System.currentTimeMillis();
			final ArrayList<ListElement> folders = getElements(FOLDER);
			final ArrayList<String> fileNames = getElementNames(FILE);
			final ArrayList<ListElement> nestedLists = getElements(NESTED_LIST);
			final Map<ListElement, ImageList> nestedListMap = new HashMap<>();

			Set<String> imageFileSet = new HashSet<>(fileNames);
			Set<String> allImageFileSet = new HashSet<>(fileNames);
			for (ListElement folder : folders) {
				allImageFileSet.addAll(ImageUtil.getImagesInFolder(folder.getName()));
				String customWeightString = folder.getProperties().getProperty(PARAM_WEIGHT);
				if (customWeightString == null) {
					imageFileSet.addAll(ImageUtil.getImagesInFolder(folder.getName()));
				}
				else {
					mImageFilesInFolders.put(folder.getName(), ImageUtil.getImagesInFolder(folder.getName()));
					if (mCustomWeights != null) {
						mCustomWeights.put(folder, Double.parseDouble(customWeightString));
					}
				}
			}

			for (ListElement nestedList : nestedLists) {
				ImageList nestedImageList = ImageRegistry.getImageListByName(nestedList.getName(), toastIfFilesMissing);
				if (nestedImageList != null && nestedImageList.getAllImageFiles() != null) {
					nestedListMap.put(nestedList, nestedImageList);
					allImageFileSet.addAll(nestedImageList.getAllImageFiles());
				}
				String customWeightString = nestedList.getProperties().getProperty(PARAM_WEIGHT);
				if (customWeightString != null) {
					double customWeight = Double.parseDouble(customWeightString);
					mCustomWeights.put(nestedList, customWeight);
				}
			}

			// Set it only here so that it is only visible when completed, and has always a complete state.
			mAllImageFilesInList = new ArrayList<>(allImageFileSet);
			mImageFilesInFolders.put(DUMMY_NESTED_FOLDER.getName(), new ArrayList<>(imageFileSet));
			mNestedLists = nestedListMap;

			calculateWeights();

			TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "All_images_in_list", null, (long) mAllImageFilesInList.size());
			TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "Images_in_list", null, (long) fileNames.size());
			TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "Folders_in_list", null, (long) folders.size());
			TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "Nested_lists_in_list", null, (long) nestedListMap.size());
			TrackingUtil.sendTiming(Category.TIME_BACKGROUND, "Load_image_list", "all images", System.currentTimeMillis() - timestamp);
		};
	}
}
