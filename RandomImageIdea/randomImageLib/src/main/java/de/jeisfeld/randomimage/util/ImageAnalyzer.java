package de.jeisfeld.randomimage.util;

import java.util.Arrays;
import java.util.Comparator;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Utility class for analyzing the pixels of an image.
 */
public final class ImageAnalyzer {
	/**
	 * The thickness of the patches at the border of the image from which the colors are taken.
	 */
	private static final double BOUNDARY_THICKNESS = 0.1;
	/**
	 * The start distance of the patches.
	 */
	private static final double SLICE_START = 0.2;
	/**
	 * The end distance of the patches.
	 */
	private static final double SLICE_END = 0.3;

	/**
	 * Hide default constructor.
	 */
	private ImageAnalyzer() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get a color from an image.
	 *
	 * @param imageBitmap The image bitmap.
	 * @return The color from this image.
	 */
	public static int getColorFromImage(final Bitmap imageBitmap) {
		// Take 8 regions around the boundary.
		ColorStatistics[] statistics = new ColorStatistics[] {
				getColorStatistics(getSubPixels(imageBitmap, SLICE_START, SLICE_END, 0, BOUNDARY_THICKNESS)),
				getColorStatistics(getSubPixels(imageBitmap, 1 - SLICE_END, 1 - SLICE_START, 0, BOUNDARY_THICKNESS)),
				getColorStatistics(getSubPixels(imageBitmap, 1 - BOUNDARY_THICKNESS, 1, SLICE_START, SLICE_END)),
				getColorStatistics(getSubPixels(imageBitmap, 1 - BOUNDARY_THICKNESS, 1, 1 - SLICE_END, 1 - SLICE_START)),
				getColorStatistics(getSubPixels(imageBitmap, 1 - SLICE_END, 1 - SLICE_START, 1 - BOUNDARY_THICKNESS, 1)),
				getColorStatistics(getSubPixels(imageBitmap, SLICE_START, SLICE_END, 1 - BOUNDARY_THICKNESS, 1)),
				getColorStatistics(getSubPixels(imageBitmap, 0, BOUNDARY_THICKNESS, 1 - SLICE_END, 1 - SLICE_START)),
				getColorStatistics(getSubPixels(imageBitmap, 0, BOUNDARY_THICKNESS, SLICE_START, SLICE_END))};

		// Sort by variance.
		Arrays.sort(statistics, new Comparator<ColorStatistics>() {
			@Override
			public int compare(final ColorStatistics lhs, final ColorStatistics rhs) {
				return Long.valueOf(lhs.getVariance()).compareTo(rhs.getVariance());
			}
		});

		int[] colors = new int[] {statistics[0].getAverageColor(), statistics[1].getAverageColor(), statistics[2].getAverageColor()};
		return getColorClosestToAverage(colors);
	}

	/**
	 * Get an array of pixels from a rectangle region of a bitmap.
	 *
	 * @param imageBitmap The bitmap.
	 * @param xFrom       The x percentage of the left side of the rectangle.
	 * @param xTo         The x percentage of the right side of the rectangle.
	 * @param yFrom       The y percentage of the top of the rectangle.
	 * @param yTo         The y percentage of the bottom of the rectangle.
	 * @return The array of pixels.
	 */
	private static int[] getSubPixels(final Bitmap imageBitmap, final double xFrom, final double xTo, final double yFrom, final double yTo) {
		int xFromAbsolute = Math.max((int) (imageBitmap.getWidth() * xFrom), 0);
		int xToAbsolute = Math.min((int) (imageBitmap.getWidth() * xTo), imageBitmap.getWidth()) - 1;
		int yFromAbsolute = Math.max((int) (imageBitmap.getHeight() * yFrom), 0);
		int yToAbsolute = Math.min((int) (imageBitmap.getHeight() * yTo), imageBitmap.getHeight()) - 1;

		int[] result = new int[(xToAbsolute - xFromAbsolute + 1) * (yToAbsolute - yFromAbsolute + 1)];
		int arrayCounter = 0;

		for (int x = xFromAbsolute; x <= xToAbsolute; x++) {
			for (int y = yFromAbsolute; y <= yToAbsolute; y++) {
				result[arrayCounter++] = imageBitmap.getPixel(x, y);
			}
		}
		return result;
	}

	/**
	 * Calculate average and variance of the colors from an array.
	 *
	 * @param colorArray The array of colors.
	 * @return The information about average and variance.
	 */
	private static ColorStatistics getColorStatistics(final int[] colorArray) {
		int sampleSize = colorArray.length;
		long redSum = 0;
		long redSquareSum = 0;
		long greenSum = 0;
		long greenSquareSum = 0;
		long blueSum = 0;
		long blueSquareSum = 0;

		for (int color : colorArray) {
			int red = Color.red(color);
			redSum += red;
			redSquareSum += red * red;

			int green = Color.green(color);
			greenSum += green;
			greenSquareSum += green * green;

			int blue = Color.blue(color);
			blueSum += blue;
			blueSquareSum += blue * blue;
		}

		int averageColor = Color.rgb((int) (redSum / sampleSize), (int) (greenSum / sampleSize), (int) (blueSum / sampleSize));

		long variance = ((redSquareSum + greenSquareSum + blueSquareSum)
				- (redSum * redSum + greenSum * greenSum + blueSum * blueSum) / sampleSize) / sampleSize;

		return new ColorStatistics(averageColor, variance);
	}

	/**
	 * From an array of colors, get the color closest to the average color.
	 *
	 * @param colorArray The array of colors.
	 * @return The color closest to the average.
	 */
	private static int getColorClosestToAverage(final int[] colorArray) {
		int averageColor = getColorStatistics(colorArray).getAverageColor();

		long minSquareDistanceFromAverage = Long.MAX_VALUE;
		int bestColor = 0;

		for (int color : colorArray) {
			long squareDistanceFromAverage = (Color.red(color) - Color.red(averageColor)) * (Color.red(color) - Color.red(averageColor))
					+ (Color.green(color) - Color.green(averageColor)) * (Color.green(color) - Color.green(averageColor))
					+ (Color.blue(color) - Color.blue(averageColor)) * (Color.blue(color) - Color.blue(averageColor));

			if (squareDistanceFromAverage < minSquareDistanceFromAverage) {
				minSquareDistanceFromAverage = squareDistanceFromAverage;
				bestColor = color;
			}
		}

		return bestColor;
	}

	/**
	 * A holder for average and variance of an array of colors.
	 */
	private static final class ColorStatistics {
		/**
		 * The average color.
		 */
		private int mAverageColor;

		private int getAverageColor() {
			return mAverageColor;
		}

		/**
		 * The variance of the array.
		 */
		private long mVariance;

		private long getVariance() {
			return mVariance;
		}

		/**
		 * Constructor for the statistics.
		 *
		 * @param averageColor The average color.
		 * @param variance     The variance.
		 */
		private ColorStatistics(final int averageColor, final long variance) {
			mAverageColor = averageColor;
			mVariance = variance;
		}
	}
}
