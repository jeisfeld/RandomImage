package de.jeisfeld.randomimage.util;

import android.os.Handler;

/**
 * A utility class for asynchronous loading of resources.
 */
public final class AsyncLoader {
	/**
	 * The thread currently loading the allImageFiles list.
	 */
	private volatile Thread mLoaderThread;

	/**
	 * Flag indicating that the active load should be followed by one reload.
	 */
	private boolean mReloadRequested;

	/**
	 * The runnable executed in this loader.
	 */
	private volatile Runnable mRunnable;

	/**
	 * Flag indicating if the loading has once been done.
	 */
	private volatile boolean mIsReady = false;

	/**
	 * Initialize the loader with a runnable.
	 *
	 * @param runnable The runnable to be executed by the loader.
	 */
	protected AsyncLoader(final Runnable runnable) {
		this.mRunnable = runnable;
	}

	/**
	 * Perform the loading.
	 */
	public synchronized void load() {
		mIsReady = false;
		if (mLoaderThread != null) {
			// Coalesce any number of reload requests into one additional pass. This avoids
			// creating a large number of threads when several views request the same list.
			mReloadRequested = true;
			return;
		}

		mLoaderThread = new Thread(() -> {
			while (true) {
				try {
					mRunnable.run();
				}
				finally {
					synchronized (AsyncLoader.this) {
						if (mReloadRequested) {
							mReloadRequested = false;
							continue;
						}
						mIsReady = true;
						mLoaderThread = null;
						AsyncLoader.this.notifyAll();
					}
				}
				return;
			}
		});
		mLoaderThread.start();
	}

	/**
	 * Check if loading has once been done.
	 *
	 * @return true if loading has once been done.
	 */
	public boolean isReady() {
		return mIsReady;
	}

	/**
	 * Wait until loading has once been done - should not be called from the main thread.
	 */
	public synchronized void waitUntilReady() {
		while (!mIsReady && mLoaderThread != null) {
			try {
				wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/**
	 * Execute actions when loading is done.
	 *
	 * @param whileLoading Actions to be done while loading to inform the user about the loading.
	 * @param afterLoading Actions to be done after loading.
	 * @param ifError      Actions to be done in case of error.
	 */
	public void executeWhenReady(final Runnable whileLoading, final Runnable afterLoading, final Runnable ifError) {
		// Put the loading thread into a safe environment, as activity may have been closed when loading is finished.
		final Runnable safeAfterLoading = new Runnable() {
			@Override
			public void run() {
				try {
					afterLoading.run();
				}
				catch (Exception e) {
					if (ifError != null) {
						ifError.run();
					}
				}
			}
		};

		if (mLoaderThread == null && !isReady()) {
			// Loading not yet started
			load();
		}

		if (isReady()) {
			safeAfterLoading.run();
			return;
		}

		if (whileLoading != null) {
			whileLoading.run();
		}

		final Handler handler = new Handler();

		new Thread() {
			@Override
			public void run() {
				waitUntilReady();
				if (isReady()) {
					handler.post(safeAfterLoading);
				}
				else if (ifError != null) {
					ifError.run();
				}
			}
		}.start();
	}
}
