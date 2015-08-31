package de.jeisfeld.randomimage.util;

/**
 * A utility class for asynchronous loading of resources.
 */
public final class AsyncLoader {
	/**
	 * The thread currently loading the allImageFiles list.
	 */
	private volatile Thread loaderThread;

	/**
	 * The thread waiting to load the allImageFile list.
	 */
	private volatile Thread waitingLoaderThread;

	/**
	 * The runnable executed in this loader.
	 */
	private volatile Runnable runnable;

	/**
	 * Flag indicating if the loading has once been done.
	 */
	private boolean isReady = false;

	/**
	 * Initialize the loader with a runnable.
	 *
	 * @param runnable
	 *            The runnable to be executed by the loader.
	 */
	protected AsyncLoader(final Runnable runnable) {
		this.runnable = runnable;
	}

	/**
	 * Perform the loading.
	 */
	public synchronized void load() {

		waitingLoaderThread = new Thread() {
			@Override
			public void run() {
				runnable.run();

				synchronized (this) {
					isReady = true;
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
	 * Check if loading has once been done.
	 *
	 * @return true if loading has once been done.
	 */
	public boolean isReady() {
		return isReady;
	}

	/**
	 * Wait until loading has once been done.
	 */
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
}
