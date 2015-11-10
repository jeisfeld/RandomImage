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
	 * The thread waiting to load the allImageFile list.
	 */
	private volatile Thread mWaitingLoaderThread;

	/**
	 * The runnable executed in this loader.
	 */
	private volatile Runnable mRunnable;

	/**
	 * Flag indicating if the loading has once been done.
	 */
	private boolean mIsReady = false;

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

		mWaitingLoaderThread = new Thread() {
			@Override
			public void run() {
				mRunnable.run();

				synchronized (this) {
					mIsReady = true;
					if (mWaitingLoaderThread != null) {
						mLoaderThread = mWaitingLoaderThread;
						mWaitingLoaderThread = null;
						mLoaderThread.start();
					}
					else {
						mLoaderThread = null;
					}
				}
			}
		};

		if (mLoaderThread == null) {
			mLoaderThread = mWaitingLoaderThread;
			mWaitingLoaderThread = null;
			mLoaderThread.start();
		}
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
	public void waitUntilReady() {
		if (isReady()) {
			return;
		}

		// prevent exception if loaderThread gets deleted after check.
		Thread localLoaderThread = mLoaderThread;
		if (localLoaderThread != null) {
			try {
				localLoaderThread.join();
			}
			catch (InterruptedException e) {
				// do nothing
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

		final Thread localLoaderThread = mLoaderThread;
		if (isReady()) {
			afterLoading.run();
			return;
		}

		if (whileLoading != null) {
			whileLoading.run();
		}

		final Handler handler = new Handler();

		new Thread() {
			@Override
			public void run() {
				try {
					localLoaderThread.join();
					handler.post(safeAfterLoading);
				}
				catch (InterruptedException e) {
					if (ifError != null) {
						ifError.run();
					}
				}
			}
		}.start();
	}
}
