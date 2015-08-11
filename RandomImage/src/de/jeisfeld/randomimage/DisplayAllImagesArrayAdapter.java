package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.SystemUtil;

/**
 * Array adapter class to display an eye photo pair in a list.
 */
public class DisplayAllImagesArrayAdapter extends ArrayAdapter<String> {
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
	private final DisplayAllImagesActivity activity;

	/**
	 * The names of the folders.
	 */
	private ArrayList<String> folderNames;

	/**
	 * The names of the files.
	 */
	private ArrayList<String> fileNames;

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
	private volatile SelectionMode selectionMode = SelectionMode.NONE;

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

		if (selectionMode == SelectionMode.NONE) {
			selectedFolderNames.clear();
			selectedFileNames.clear();
		}
	}

	/**
	 * Constructor for the adapter.
	 *
	 * @param activity
	 *            The activity using the adapter.
	 * @param folderNames
	 *            The names of folders to be displayed.
	 * @param fileNames
	 *            The names of files to be displayed.
	 */
	public DisplayAllImagesArrayAdapter(final DisplayAllImagesActivity activity,
			final ArrayList<String> folderNames, final ArrayList<String> fileNames) {
		super(activity, R.layout.text_view_initializing);

		addAll(folderNames);
		addAll(fileNames);

		this.activity = activity;
		this.folderNames = folderNames;
		this.fileNames = fileNames;
		this.viewCache = new ViewCache(fileNames.size() - 1, PRELOAD_SIZE);
	}

	/**
	 * Default adapter to be used by the framework.
	 *
	 * @param context
	 *            The Context the view is running in.
	 */
	public DisplayAllImagesArrayAdapter(final Context context) {
		super(context, R.layout.text_view_initializing);
		this.activity = (DisplayAllImagesActivity) context;
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
		final boolean isFolder = position < folderNames.size();

		final ThumbImageView thumbImageView =
				(ThumbImageView) LayoutInflater.from(activity).inflate(R.layout.adapter_display_images,
						parent, false);

		final String fileName;

		if (isFolder) {
			fileName = folderNames.get(position);

			String displayFileName = null;

			Set<String> imageFiles = ImageList.getImageFilesInFolder(fileName);
			if (imageFiles.size() > 0) {
				displayFileName = imageFiles.iterator().next();
			}

			thumbImageView.setImage(activity, displayFileName, sameThread, true, new Runnable() {
				@Override
				public void run() {
					thumbImageView.setMarkable(selectionMode == SelectionMode.MULTIPLE);
					thumbImageView.setMarked(selectedFolderNames.contains(fileName));
					thumbImageView.setFolderName(new File(fileName).getName());
				}
			});
		}
		else {
			fileName = fileNames.get(position - folderNames.size());

			thumbImageView.setImage(activity, fileName, sameThread, false, new Runnable() {
				@Override
				public void run() {
					thumbImageView.setMarkable(selectionMode == SelectionMode.MULTIPLE);
					thumbImageView.setMarked(selectedFileNames.contains(fileName));
				}
			});
		}

		thumbImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				ThumbImageView view = (ThumbImageView) v;

				switch (selectionMode) {
				case NONE:
					break;
				case ONE:
					break;
				case MULTIPLE:
					if (view.isMarked()) {
						view.setMarked(false);
						(isFolder ? selectedFolderNames : selectedFileNames).remove(fileName);
					}
					else {
						view.setMarked(true);
						(isFolder ? selectedFolderNames : selectedFileNames).add(fileName);
					}
					break;
				default:
					break;
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
	 * Set the markability status for all images in the cache.
	 *
	 * @param markable
	 *            the new markability status.
	 */
	public final void setMarkabilityStatus(final boolean markable) {
		for (ThumbImageView view : viewCache.getCachedImages()) {
			view.setMarkable(markable);
		}
	}

	/**
	 * If in selection mode, select all images. If all images are selected, then deselect all images.
	 *
	 * @return true if all have been selected, false if all have been deselected or if not in selection mode.
	 */
	public final boolean toggleSelectAll() {
		if (selectionMode == SelectionMode.MULTIPLE) {
			if (selectedFileNames.size() < fileNames.size() || selectedFolderNames.size() < folderNames.size()) {
				setSelectedFiles(fileNames);
				setSelectedFolders(folderNames);
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
		 * No selection possible.
		 */
		NONE,
		/**
		 * One file can be selected.
		 */
		ONE,
		/**
		 * Multiple files can be selected.
		 */
		MULTIPLE
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
						view.setMarkable(selectionMode == SelectionMode.MULTIPLE);
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
					doPreload(position, atEnd);
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
