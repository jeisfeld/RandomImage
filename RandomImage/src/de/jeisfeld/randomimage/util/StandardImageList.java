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
	 * All image files represented by this image list, grouped by nested list. All non-grouped items are linked to
	 * virtual list "".
	 */
	private volatile Map<String, ArrayList<String>> imageFilesByNestedList = null;

	/**
	 * All image files represented by this image list, grouped by nested list. All non-grouped items are linked to
	 * virtual list "".
	 */
	private volatile Map<String, Double> nestedListWeights = null;

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
				Map<String, Double> nestedListWeightsNew = new HashMap<String, Double>();

				Set<String> allImageFileSet = new HashSet<String>();

				for (String folderName : folderNames) {
					allImageFileSet.addAll(getImageFilesInFolder(folderName));
				}
				for (String fileName : fileNames) {
					allImageFileSet.add(fileName);
				}
				int numberOfImages = allImageFileSet.size();

				imageFilesByNestedListNew.put(DEFAULT_NESTED_LIST, new ArrayList<String>(allImageFileSet));

				for (String nestedListName : nestedListNames) {
					ImageList nestedImageList = ImageRegistry.getImageListByName(nestedListName);
					if (nestedImageList != null) {
						ArrayList<String> nestedListFiles = nestedImageList.getAllImageFiles();
						imageFilesByNestedListNew.put(nestedListName, nestedListFiles);
						numberOfImages += nestedListFiles.size();
					}
				}

				for (String nestedListName : imageFilesByNestedListNew.keySet()) {
					if (numberOfImages == 0) {
						nestedListWeightsNew.put(nestedListName, 0.0);
					}
					else {
						nestedListWeightsNew.put(nestedListName,
								(double) imageFilesByNestedListNew.get(nestedListName).size() / numberOfImages);
					}
				}

				// Set it only here so that it is only visible when completed, and has always a complete state.
				imageFilesByNestedList = imageFilesByNestedListNew;
				nestedListWeights = nestedListWeightsNew;

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
		imageFilesByNestedList = null;
		nestedListWeights = null;
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
}
