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
	private ArrayList<String> allImageFiles;

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
		allImageFiles.clear();

		super.load();

		Set<String> allImageFileSet = new HashSet<String>();

		for (String folderName : getFolderNames()) {
			allImageFileSet.addAll(getImageFilesInFolder(folderName));
		}

		for (String fileName : getFileNames()) {
			allImageFileSet.add(fileName);
		}

		allImageFiles = new ArrayList<String>(allImageFileSet);
	}

	/**
	 * Do initialization steps of this subclass of ImageList.
	 */
	@Override
	protected void init() {
		allImageFiles = new ArrayList<String>();
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
		if (allImageFiles.size() > 0) {
			return allImageFiles.get(new Random().nextInt(allImageFiles.size()));
		}
		else {
			return null;
		}
	}

}
