package de.jeisfeld.randomimage.util;

/**
 * An interface that may provide random files from some list.
 */
public interface RandomFileProvider {
	/**
	 * Get a random file name.
	 *
	 * @return A random file name.
	 */
	String getRandomFileName();

	/**
	 * Get information if files can be provided.
	 *
	 * @return true if files can be provided.
	 */
	boolean isReady();

	/**
	 * Wait until the provider is ready.
	 */
	void waitUntilReady();

	/**
	 * Execute actions when provider is ready.
	 *
	 * @param whileLoading Actions to be done if not yet ready to inform the user about the loading.
	 * @param afterLoading Actions to be done when ready.
	 * @param ifError      Actions to be done in case of loading error.
	 */
	void executeWhenReady(Runnable whileLoading, Runnable afterLoading, Runnable ifError);
}
