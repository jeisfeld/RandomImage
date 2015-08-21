package de.jeisfeld.randomimage.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Subclass of ImageList, giving each file the same weight.
 */
public final class StandardImageList extends ImageList {

	/**
	 * All image files represented by this image list - configures image files as well as all images in configured
	 * folders.
	 */
	private volatile ArrayList<String> allImageFiles;

	/**
	 * The thread currently loading the allImageFiles list.
	 */
	private volatile Thread loaderThread;

	/**
	 * The thread waiting to load the allImageFile list.
	 */
	private volatile Thread waitingLoaderThread;

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
				Set<String> allImageFileSet = new HashSet<String>();

				for (String nestedListName : nestedListNames) {
					ImageList nestedImageList = ImageRegistry.getImageListByName(nestedListName);
					if (nestedImageList != null) {
						allImageFileSet.addAll(nestedImageList.getAllImageFiles());
					}
				}
				for (String folderName : folderNames) {
					allImageFileSet.addAll(getImageFilesInFolder(folderName));
				}
				for (String fileName : fileNames) {
					allImageFileSet.add(fileName);
				}
				allImageFiles = new ArrayList<String>(allImageFileSet);

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
		allImageFiles = null;
	}

	/**
	 * Get the list of file names, including the files in the configured folders, in shuffled order.
	 *
	 * @return The list of file names.
	 */
	public String[] getShuffledFileNames() {
		ArrayList<String> clondedList = new ArrayList<String>(allImageFiles);
		Collections.shuffle(clondedList);
		return clondedList.toArray(new String[0]);
	}

	/**
	 * Get a random file name from the registry.
	 *
	 * @return A random file name.
	 */
	@Override
	public String getRandomFileName() {
		if (allImageFiles != null && allImageFiles.size() > 0) {
			return allImageFiles.get(new Random().nextInt(allImageFiles.size()));
		}
		else {
			return null;
		}
	}

	@Override
	public boolean isReady() {
		return allImageFiles != null;
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
		return new ArrayList<String>(allImageFiles);
	}
}
