package de.jeisfeld.randomimage.util;

/**
 * A class that may provide random files from some list.
 */
public abstract class RandomFileProvider {
	/**
	 * Get a random file name.
	 *
	 * @return A random file name.
	 */
	public abstract String getRandomFileName();

	/**
	 * Get information if files can be provided.
	 *
	 * @return true if files can be provided.
	 */
	// OVERRIDABLE
	public boolean isReady() {
		return true;
	}

	/**
	 * Execute actions when provider is ready.
	 *
	 * @param whileLoading
	 *            Actions to be done if not yet ready to inform the user about the loading.
	 * @param afterLoading
	 *            Actions to be done when ready.
	 * @param ifError
	 *            Actions to be done in case of loading error.
	 */
	// OVERRIDABLE
	public void executeWhenReady(final Runnable whileLoading, final Runnable afterLoading, final Runnable ifError) {
		afterLoading.run();
	}
}
