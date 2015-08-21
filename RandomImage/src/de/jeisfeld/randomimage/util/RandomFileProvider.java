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
	 * Wait until files can be provided.
	 */
	// OVERRIDABLE
	public void waitUntilReady() {
		// do nothing
	}
}
