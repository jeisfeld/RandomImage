package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.view.ThumbImageView;
import de.jeisfeld.randomimage.view.ThumbImageView.MarkingType;
import de.jeisfeld.randomimage.view.ThumbImageView.ThumbStyle;

/**
 * Array adapter class to display an eye photo pair in a list.
 */
public class DisplayImageListArrayAdapter extends ArrayAdapter<String> {
	/**
	 * Number of views to be preloaded.
	 */
	private static final int PRELOAD_SIZE;

	/**
	 * The cache where views of the displays are stored for smoother scrolling.
	 */
	private ViewCache viewCache;

	/**
	 * The activity holding this adapter.
	 */
	private final DisplayImageListActivity activity;

	/**
	 * The names of the nested lists.
	 */
	private ArrayList<String> nestedListNames;

	/**
	 * The names of the folders.
	 */
	private ArrayList<String> folderNames;

	/**
	 * The names of the files.
	 */
	private ArrayList<String> fileNames;

	/**
	 * The set of nested list names selected for deletion.
	 */
	private Set<String> selectedNestedListNames = new HashSet<String>();

	/**
	 * The set of foldernames selected for deletion.
	 */
	private Set<String> selectedFolderNames = new HashSet<String>();

	/**
	 * The set of filenames selected for deletion.
	 */
	private Set<String> selectedFileNames = new HashSet<String>();

	/**
	 * Flag indicating how selection is handled.
	 */
	private volatile SelectionMode selectionMode = SelectionMode.ONE;

	/**
	 * The way in which the thumbs are marked.
	 */
	private MarkingType markingType = MarkingType.NONE;

	static {
		// Set cache size in dependence of device memory.
		int memoryClass = SystemUtil.getLargeMemoryClass();

		if (memoryClass >= 512) { // MAGIC_NUMBER
			PRELOAD_SIZE = 50; // MAGIC_NUMBER
		}
		else if (memoryClass >= 256) { // MAGIC_NUMBER
			PRELOAD_SIZE = 20; // MAGIC_NUMBER
		}
		else {
			PRELOAD_SIZE = 5; // MAGIC_NUMBER
		}
	}

	/**
	 * Set the selection mode.
	 *
	 * @param selectionMode
	 *            The selection mode.
	 */
	public final void setSelectionMode(final SelectionMode selectionMode) {
		this.selectionMode = selectionMode;

		if (selectionMode == SelectionMode.ONE) {
			selectedFolderNames.clear();
			selectedFileNames.clear();
		}

		markingType = getMarkingTypeFromSelectionMode(selectionMode);
		setMarkabilityStatus(markingType);
	}

	/**
	 * Map the selectionMode of the adapter to the markingType of the thumb.
	 *
	 * @param selectionMode
	 *            The selectionMode
	 * @return the markingType
	 */
	public static MarkingType getMarkingTypeFromSelectionMode(final SelectionMode selectionMode) {
		switch (selectionMode) {
		case ONE:
			return MarkingType.NONE;
		case MULTIPLE_ADD:
			return MarkingType.HOOK;
		case MULTIPLE_REMOVE:
			return MarkingType.CROSS;
		default:
			return null;
		}
	}

	/**
	 * Constructor for the adapter.
	 *
	 * @param activity
	 *            The activity using the adapter.
	 * @param nestedListNames
	 *            The names of nested lists to be displayed.
	 * @param folderNames
	 *            The names of folders to be displayed.
	 * @param fileNames
	 *            The names of files to be displayed.
	 */
	public DisplayImageListArrayAdapter(final DisplayImageListActivity activity,
			final ArrayList<String> nestedListNames, final ArrayList<String> folderNames,
			final ArrayList<String> fileNames) {
		super(activity, R.layout.text_view_initializing);
		this.activity = activity;

		if (nestedListNames == null) {
			this.nestedListNames = new ArrayList<String>();
		}
		else {
			this.nestedListNames = nestedListNames;
			addAll(nestedListNames);
		}
		if (folderNames == null) {
			this.folderNames = new ArrayList<String>();
		}
		else {
			this.folderNames = folderNames;
			addAll(folderNames);
		}

		addAll(fileNames);
		this.fileNames = fileNames;

		this.viewCache = new ViewCache(fileNames.size() - 1, PRELOAD_SIZE);
	}

	/**
	 * Default adapter to be used by the framework.
	 *
	 * @param context
	 *            The Context the view is running in.
	 */
	public DisplayImageListArrayAdapter(final Context context) {
		super(context, R.layout.text_view_initializing);
		this.activity = (DisplayImageListActivity) context;
	}

	@Override
	public final View getView(final int position, final View convertView, final ViewGroup parent) {
		return viewCache.get(position, parent);
	}

	/**
	 * Create a new ThumbImageView for the file on a certain position.
	 *
	 * @param position
	 *            The position.
	 * @param parent
	 *            The parent view.
	 * @param sameThread
	 *            if true, then image load will be done on the same thread. Otherwise a separate thread will be spawned.
	 * @return The ThumbImageView.
	 */
	private ThumbImageView createThumbImageView(final int position, final ViewGroup parent, final boolean sameThread) {
		final boolean isNestedList = position < nestedListNames.size();
		final boolean isFolder = !isNestedList && position < nestedListNames.size() + folderNames.size();

		final ThumbImageView thumbImageView =
				(ThumbImageView) LayoutInflater.from(activity).inflate(R.layout.adapter_display_images,
						parent, false);

		final String entryName;
		final String displayFileName;

		if (isNestedList) {
			entryName = nestedListNames.get(position);
			ArrayList<String> imageFiles =
					new ArrayList<String>(ImageRegistry.getImageListByName(entryName).getAllImageFiles());
			if (imageFiles.size() > 0) {
				displayFileName = imageFiles.get(new Random().nextInt(imageFiles.size()));
			}
			else {
				displayFileName = null;
			}

			// TODO: Special display variant for nested lists.
			thumbImageView.setImage(activity, displayFileName, sameThread, ThumbStyle.IMAGE_LIST, new Runnable() {
				@Override
				public void run() {
					thumbImageView.setMarkable(markingType);
					thumbImageView.setMarked(selectedNestedListNames.contains(entryName));
					thumbImageView.setFolderName(entryName);
				}
			});
		}
		else if (isFolder) {
			entryName = folderNames.get(position - nestedListNames.size());

			ArrayList<String> imageFiles = new ArrayList<String>(ImageList.getImageFilesInFolder(entryName));
			if (imageFiles.size() > 0) {
				displayFileName = imageFiles.get(new Random().nextInt(imageFiles.size()));
			}
			else {
				displayFileName = null;
			}

			thumbImageView.setImage(activity, displayFileName, sameThread, ThumbStyle.FOLDER, new Runnable() {
				@Override
				public void run() {
					thumbImageView.setMarkable(markingType);
					thumbImageView.setMarked(selectedFolderNames.contains(entryName));
					thumbImageView.setFolderName(new File(entryName).getName());
				}
			});
		}
		else {
			entryName = fileNames.get(position - nestedListNames.size() - folderNames.size());
			displayFileName = entryName;

			thumbImageView.setImage(activity, displayFileName, sameThread, ThumbStyle.IMAGE, new Runnable() {
				@Override
				public void run() {
					thumbImageView.setMarkable(markingType);
					thumbImageView.setMarked(selectedFileNames.contains(entryName));
				}
			});
		}

		final String listName;
		if (activity instanceof DisplayAllImagesActivity) {
			listName = ((DisplayAllImagesActivity) activity).getListName();
		}
		else {
			listName = null;
		}

		final Set<String> selectionList;
		if (isNestedList) {
			selectionList = selectedNestedListNames;
		}
		else if (isFolder) {
			selectionList = selectedFolderNames;
		}
		else {
			selectionList = selectedFileNames;
		}

		thumbImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				ThumbImageView view = (ThumbImageView) v;

				switch (selectionMode) {
				case ONE:
					if (isNestedList) {
						if (activity instanceof DisplayAllImagesActivity) {
							((DisplayAllImagesActivity) activity).switchToImageList(entryName, CreationStyle.NONE);
						}
					}
					else if (isFolder) {
						DisplayImagesFromFolderActivity.startActivity(activity, entryName, false);
					}
					else {
						if (activity instanceof DisplayImagesFromFolderActivity) {
							DisplayRandomImageActivity.startActivityForFolder(activity,
									new File(entryName).getParent(),
									displayFileName);
						}
						else {
							activity.startActivityForResult(DisplayRandomImageActivity
									.createIntent(activity, null, displayFileName, true),
									DisplayRandomImageActivity.REQUEST_CODE);
						}
					}
					break;
				case MULTIPLE_ADD:
				case MULTIPLE_REMOVE:
					if (view.isMarked()) {
						view.setMarked(false);
						selectionList.remove(entryName);
					}
					else {
						view.setMarked(true);
						selectionList.add(entryName);
					}
					break;
				default:
					break;
				}
			}
		});

		thumbImageView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(final View v) {
				if (isNestedList) {
					return false;
				}
				else {
					DisplayImageDetailsActivity.startActivity(activity, entryName, listName, true);
					return true;
				}
			}
		});

		return thumbImageView;
	}

	/**
	 * Get the list of selected files.
	 *
	 * @return The selected files.
	 */
	public final ArrayList<String> getSelectedFiles() {
		return new ArrayList<String>(selectedFileNames);
	}

	/**
	 * Set the list of selected files.
	 *
	 * @param selectedFiles
	 *            The names of the files.
	 */
	public final void setSelectedFiles(final ArrayList<String> selectedFiles) {
		if (selectedFiles == null) {
			selectedFileNames.clear();
		}
		else {
			selectedFileNames = new HashSet<String>(selectedFiles);
		}
	}

	/**
	 * Get the list of selected folders.
	 *
	 * @return The selected folders.
	 */
	public final ArrayList<String> getSelectedFolders() {
		return new ArrayList<String>(selectedFolderNames);
	}

	/**
	 * Set the list of selected folders.
	 *
	 * @param selectedFolders
	 *            The names of the folders.
	 */
	public final void setSelectedFolders(final ArrayList<String> selectedFolders) {
		if (selectedFolders == null) {
			selectedFolderNames.clear();
		}
		else {
			selectedFolderNames = new HashSet<String>(selectedFolders);
		}
	}

	/**
	 * Get the list of selected nested lists.
	 *
	 * @return The selected nested lists.
	 */
	public final ArrayList<String> getSelectedNestedLists() {
		return new ArrayList<String>(selectedNestedListNames);
	}

	/**
	 * Set the list of selected nested lists.
	 *
	 * @param selectedNestedLists
	 *            The names of the nested lists.
	 */
	public final void setSelectedNestedLists(final ArrayList<String> selectedNestedLists) {
		if (selectedNestedLists == null) {
			selectedNestedListNames.clear();
		}
		else {
			selectedNestedListNames = new HashSet<String>(selectedNestedLists);
		}
	}

	/**
	 * Set the markability status for all images in the cache.
	 *
	 * @param newMarkingType
	 *            the new marking type.
	 */
	private void setMarkabilityStatus(final MarkingType newMarkingType) {
		for (ThumbImageView view : viewCache.getCachedImages()) {
			view.setMarkable(newMarkingType);
		}
	}

	/**
	 * If in selection mode, select all images. If all images are selected, then deselect all images.
	 *
	 * @return true if all have been selected, false if all have been deselected or if not in selection mode.
	 */
	public final boolean toggleSelectAll() {
		if (selectionMode != SelectionMode.ONE) {
			if (selectedFileNames.size() < fileNames.size() || selectedFolderNames.size() < folderNames.size()
					|| selectedNestedListNames.size() < nestedListNames.size()) {
				setSelectedFiles(fileNames);
				setSelectedFolders(folderNames);
				setSelectedNestedLists(nestedListNames);
				for (ThumbImageView view : viewCache.getCachedImages()) {
					view.setMarked(true);
				}
				return true;
			}
			else {
				setSelectedFiles(null);
				setSelectedFolders(null);
				for (ThumbImageView view : viewCache.getCachedImages()) {
					view.setMarked(false);
				}
				return false;
			}
		}
		return false;
	}

	/**
	 * Cleanup the adapter cache and stop further preloading.
	 */
	public final void cleanupCache() {
		viewCache.interrupt();
	}

	/**
	 * Mode defining how selection works.
	 */
	public enum SelectionMode {
		/**
		 * One file can be selected for display.
		 */
		ONE,
		/**
		 * Multiple files can be selected for adding.
		 */
		MULTIPLE_ADD,
		/**
		 * Multiple files can be selected for removal.
		 */
		MULTIPLE_REMOVE,
	}

	/**
	 * Cache to store views from the GridView.
	 */
	private final class ViewCache {
		/**
		 * The map from positions to views.
		 */
		private SparseArray<ThumbImageView> cache;

		/**
		 * The preload size.
		 */
		private int preloadSize;

		/**
		 * The planned size of the cache.
		 */
		private int plannedSize;

		/**
		 * The current number of views on the grid.
		 */
		private int currentViewSize = 1;

		/**
		 * The maximum position.
		 */
		private int maxPosition;

		/**
		 * Center position of the cache.
		 */
		private volatile int currentCenter;

		/**
		 * Flag indicating if a preload thread is running.
		 */
		private boolean isPreloadRunning = false;

		/**
		 * The parentView view h olding the cached views.
		 */
		private ViewGroup parentView = null;

		/**
		 * Waiting preload thread. Always contains one element - this is done so that the array can be used as
		 * synchronization object.
		 */
		private Thread[] waitingThreads = new Thread[1];

		/**
		 * Flad indicating if the preload thread is interrupted and the thread is in the process of deletion.
		 */
		private boolean isInterrupted = false;

		/**
		 * Constructor of the cache.
		 *
		 * @param maxPosition
		 *            The maximum position that can be stored.
		 * @param preloadSize
		 *            The number of views to be preloaded.
		 */
		private ViewCache(final int maxPosition, final int preloadSize) {
			this.preloadSize = preloadSize;
			this.maxPosition = maxPosition;
			plannedSize = 2 * preloadSize;
			cache = new SparseArray<ThumbImageView>(plannedSize);
		}

		/**
		 * Preload a range of views and clean the cache.
		 *
		 * @param position
		 *            The position from where to do the preload
		 * @param atEnd
		 *            Flag indicating if we are at the end of the view or of the start.
		 */
		private void doPreload(final int position, final boolean atEnd) {
			synchronized (cache) {
				isPreloadRunning = true;
				int startPosition;
				int endPosition;

				adjustPlannedSize();

				// Skipping 0, because this is frequently loaded.
				if (atEnd) {
					startPosition = Math.max(1, Math.min(position + preloadSize, maxPosition) - plannedSize + 1);
				}
				else {
					startPosition = Math.max(1, position - preloadSize);
				}
				endPosition = Math.min(startPosition + plannedSize - 1, maxPosition);

				currentCenter = atEnd ? position - currentViewSize / 2 : position + currentViewSize / 2;

				// clean up, ignoring the first index which carries position 0.
				int index = 1;
				while (index < cache.size()) {
					if (cache.keyAt(index) < startPosition || cache.keyAt(index) > endPosition) {
						cache.removeAt(index);
					}
					else {
						index++;
					}
				}

				// preload.
				for (int i = startPosition; i <= endPosition; i++) {
					if (cache.indexOfKey(i) < 0) {
						ThumbImageView view = createThumbImageView(i, parentView, true);
						if (isInterrupted) {
							isPreloadRunning = false;
							return;
						}
						cache.put(i, view);

						// Prevent wrong marking status in race conditions.
						view.setMarkable(markingType);
					}
				}

				synchronized (waitingThreads) {
					if (waitingThreads[0] != null) {
						waitingThreads[0].start();
						waitingThreads[0] = null;
					}
				}

				isPreloadRunning = false;
			}
		}

		/**
		 * Trigger a preload thread, ensuring that only one such thread is running at a time.
		 *
		 * @param position
		 *            The position from where to do the preload
		 * @param atEnd
		 *            Flag indicating if we are at the end of the view or of the start.
		 */
		private void triggerPreload(final int position, final boolean atEnd) {
			if (position == 0 || isInterrupted) {
				return;
			}

			Thread preloadThread = new Thread() {
				@Override
				public void run() {
					try {
						doPreload(position, atEnd);
					}
					catch (Exception e) {
						Log.e(Application.TAG, "Exception while preloading.", e);
					}
				}
			};

			synchronized (waitingThreads) {
				if (isPreloadRunning) {
					waitingThreads[0] = preloadThread;
				}
				else {
					waitingThreads[0] = null;
					preloadThread.start();
				}

			}
		}

		/**
		 * Interrupt the preloading thread and clean the cache.
		 */
		private void interrupt() {
			isInterrupted = true;
			cache.clear();
		}

		/**
		 * Adjust the planned size according to the size of the view.
		 */
		private void adjustPlannedSize() {
			currentViewSize = parentView.getChildCount();
			int wishSize = Math.max(currentViewSize * 2, currentViewSize + 2 * preloadSize);
			if (plannedSize < wishSize) {
				plannedSize = wishSize;
			}
		}

		/**
		 * Get a view from the cache.
		 *
		 * @param position
		 *            The position of the view.
		 * @param parent
		 *            the parentView view.
		 * @return The view from cache, if existing. Otherwise a new view.
		 */
		private ThumbImageView get(final int position, final ViewGroup parent) {
			if (parentView == null) {
				parentView = parent;
			}
			else if (parentView != parent) {
				parentView = parent;
				cache.clear();
			}

			ThumbImageView thumbImageView;
			if (cache.indexOfKey(position) >= 0) {
				thumbImageView = cache.get(position);
			}
			else {
				thumbImageView = createThumbImageView(position, parentView, false);
				cache.put(position, thumbImageView);
			}
			triggerPreload(position, position > currentCenter);

			return thumbImageView;
		}

		/**
		 * Get all cached images.
		 *
		 * @return the array of cached images.
		 */
		private ThumbImageView[] getCachedImages() {
			ThumbImageView[] result = new ThumbImageView[cache.size()];
			for (int i = 0; i < cache.size(); i++) {
				result[i] = cache.valueAt(i);
			}
			return result;
		}

	}

}
