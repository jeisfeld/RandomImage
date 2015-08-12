package de.jeisfeld.randomimage.util;

/**
 * A class that may provide random files from some list.
 */
public interface RandomFileProvider {
	/**
	 * Get a random file name.
	 *
	 * @return A random file name.
	 */
	String getRandomFileName();
}
