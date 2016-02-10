package de.jeisfeld.randomimage.util;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Utility class for analyzing the pixels of an image.
 */
public final class ImageAnalyzer {
	/**
	 * The size to which the bitmap is shrunk before analyzing it.
	 */
	private static final int ANALYZED_BITMAP_SIZE = 100;
	/**
	 * The thickness of the rectangles at the border of the image from which the colors are taken.
	 */
	private static final double BOUNDARY_THICKNESS = 0.1;
	/**
	 * The width of the rectangles at the border of the image from which the colors are taken.
	 */
	private static final double SLICE_WIDTH = 0.1;
	/**
	 * The starting points of the rectangles at the border of the image from which the colors are taken.
	 */
	private static final double[] SLICE_STARTS = {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8};

	/**
	 * Hide default constructor.
	 */
	private ImageAnalyzer() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get some kind of "average color" of an image.
	 *
	 * @param imageBitmap The image bitmap.
	 * @return The "average color" of this bitmap.
	 */
	public static int getAverageImageColor(final Bitmap imageBitmap) {
		Bitmap shrunkBitmap = Bitmap.createScaledBitmap(imageBitmap, ANALYZED_BITMAP_SIZE, ANALYZED_BITMAP_SIZE, true);
		float[] hsv = new float[3]; // MAGIC_NUMBER
		int pixelCount = 0;
		float hueXSum = 0;
		float hueYSum = 0;
		float saturationSum = 0;
		float valueSum = 0;

		for (int x = 0; x < shrunkBitmap.getWidth(); x++) {
			for (int y = 0; y < shrunkBitmap.getWidth(); y++) {
				int color = shrunkBitmap.getPixel(x, y);
				Color.colorToHSV(color, hsv);
				hueXSum += Math.cos(hsv[0] * Math.PI / 180); // MAGIC_NUMBER
				hueYSum += Math.sin(hsv[0] * Math.PI / 180); // MAGIC_NUMBER
				saturationSum += hsv[1];
				valueSum += hsv[2];
				pixelCount++;
			}
		}

		float avgHue;
		try {
			avgHue = (float) (Math.atan2(hueYSum, hueXSum) * 180 / Math.PI); // MAGIC_NUMBER
			if (avgHue < 0) {
				avgHue += 360; // MAGIC_NUMBER
			}
		}
		catch (Exception e) {
			avgHue = 0;
		}

		return Color.HSVToColor(new float[] {avgHue, saturationSum / pixelCount, valueSum / pixelCount});
	}

	/**
	 * Get a color from an image. The color is taken from a border area with not too high variance.
	 *
	 * @param imageBitmap The image bitmap.
	 * @return A color from this image.
	 */
	public static int getColorFromImage(final Bitmap imageBitmap) {
		Bitmap shrunkBitmap = Bitmap.createScaledBitmap(imageBitmap, ANALYZED_BITMAP_SIZE, ANALYZED_BITMAP_SIZE, true);

		// Take regions around the boundary.
		List<ColorStatistics> statistics = new ArrayList<>();

		for (double startValue : SLICE_STARTS) {
			statistics.add(getColorStatistics(getSubPixels(shrunkBitmap, startValue, startValue + SLICE_WIDTH, 0, BOUNDARY_THICKNESS)));
			statistics.add(getColorStatistics(getSubPixels(shrunkBitmap, 1 - BOUNDARY_THICKNESS, 1, startValue, startValue + SLICE_WIDTH)));
			statistics.add(getColorStatistics(getSubPixels(shrunkBitmap, 1 - startValue - SLICE_WIDTH, 1 - startValue, 1 - BOUNDARY_THICKNESS, 1)));
			statistics.add(getColorStatistics(getSubPixels(shrunkBitmap, 0, BOUNDARY_THICKNESS, 1 - startValue - SLICE_WIDTH, 1 - startValue)));
		}

		return ColorForest.getBestColor(statistics);
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


	/**
	 * Utility class building a tree from an array of color statistics in order to find a color with maximum representation.
	 */
	private static final class ColorForest {
		/**
		 * The root nodes of a forest of binary trees.
		 */
		private List<ColorNode> mNodes;

		/**
		 * Get the best color out of a list of color statistics.
		 *
		 * @param statistics The list of color statistics.
		 * @return The best color.
		 */
		private static int getBestColor(final List<ColorStatistics> statistics) {
			ColorForest colorForest = new ColorForest(statistics);

			while (colorForest.mNodes.size() > 1) {
				colorForest.joinBestPair();
			}

			return colorForest.mNodes.get(0).getBestLeaf().mAverage;
		}

		/**
		 * Initialize the forest with a list of color statistics. Each statistic will represent a one-node tree.
		 *
		 * @param statistics the list of color statistics.
		 */
		private ColorForest(final List<ColorStatistics> statistics) {
			mNodes = new ArrayList<>();

			for (ColorStatistics statistic : statistics) {
				mNodes.add(new ColorNode(statistic.getAverageColor(), statistic.getVariance()));
			}
		}

		/**
		 * Join the best pair of root nodes (i.e. the ones having the smallest variance when joined).
		 */
		private void joinBestPair() {
			ColorNode bestPair = new ColorNode(0, Long.MAX_VALUE);

			for (int i = 0; i < mNodes.size() - 1; i++) {
				for (int j = i + 1; j < mNodes.size(); j++) {
					ColorNode currentPair = new ColorNode(mNodes.get(i), mNodes.get(j));
					if (currentPair.mVariance < bestPair.mVariance) {
						bestPair = currentPair;
					}
				}
			}

			mNodes.remove(bestPair.mChild1);
			mNodes.remove(bestPair.mChild2);
			mNodes.add(bestPair);
		}

		/**
		 * A node of a binary color tree. Represents either a single color or the root of a tree.
		 */
		private static final class ColorNode {
			/**
			 * The first child, if existing.
			 */
			private ColorNode mChild1;
			/**
			 * The second child, if existing.
			 */
			private ColorNode mChild2;
			/**
			 * The number of mNodes in the subtree rooted by this node.
			 */
			private int mWeight;
			/**
			 * The average of all colors in the subtree rooted by this node.
			 */
			private int mAverage;
			/**
			 * The total variance of all color mNodes in the subtree rooted by this node.
			 */
			private long mVariance;
			/**
			 * The value of a node for later usage as representative color.
			 */
			private double mValue;

			/**
			 * Constructor of a leaf - a basic color entry.
			 *
			 * @param color    The color of the leaf.
			 * @param variance The original variance of this entry.
			 */
			private ColorNode(final int color, final long variance) {
				mChild1 = null;
				mChild2 = null;
				mWeight = 1;
				mAverage = color;
				mVariance = variance;
				mValue = calculateValue();
			}

			/**
			 * Constructor of a non-leaf - join two color entries.
			 *
			 * @param child1 The first child.
			 * @param child2 The second child.
			 */
			private ColorNode(final ColorNode child1, final ColorNode child2) {
				mChild1 = child1;
				mChild2 = child2;
				mWeight = child1.mWeight + child2.mWeight;
				mAverage = Color.rgb((child1.mWeight * Color.red(child1.mAverage) + child2.mWeight * Color.red(child2.mAverage)) / mWeight,
						(child1.mWeight * Color.green(child1.mAverage) + child2.mWeight * Color.green(child2.mAverage)) / mWeight,
						(child1.mWeight * Color.blue(child1.mAverage) + child2.mWeight * Color.blue(child2.mAverage)) / mWeight);
				mVariance = (child1.mWeight * child1.mVariance + child2.mWeight * child2.mVariance) / mWeight
						+ ((Color.red(child1.mAverage) - Color.red(child2.mAverage)) * (Color.red(child1.mAverage) - Color.red(child2.mAverage)))
						+ ((Color.green(child1.mAverage) - Color.green(child2.mAverage))
						* (Color.green(child1.mAverage) - Color.green(child2.mAverage)))
						+ ((Color.blue(child1.mAverage) - Color.blue(child2.mAverage)) * (Color.blue(child1.mAverage) - Color.blue(child2.mAverage)))
						* child1.mWeight * child2.mWeight / (mWeight * mWeight);
				mValue = Math.max(calculateValue(), Math.max(child1.mValue, child2.mValue));
			}

			/**
			 * Calculate the value from weight and variance - this should ensure that collections with low variance are preferred, provided
			 * they have enough weight. Give slight preference to saturated colors.
			 *
			 * @return The calculated value.
			 */
			private double calculateValue() {
				float[] hsv = new float[3]; // MAGIC_NUMBER
				Color.colorToHSV(mAverage, hsv);

				return mWeight / (50 + Math.sqrt(mVariance)) * (1 + hsv[1] * hsv[2]); // MAGIC_NUMBER
			}

			/**
			 * Check if it is a leaf.
			 *
			 * @return true for a leaf.
			 */
			private boolean isLeaf() {
				return mChild1 == null;
			}

			/**
			 * Get the child having the bigger weight. In case of equality, take the one with lower variance.
			 *
			 * @return The better child.
			 */
			private ColorNode getBestChild() {
				if (mChild1.mValue >= mChild2.mValue) {
					return mChild1;
				}
				else {
					return mChild2;
				}
			}


			/**
			 * Go down the tree via best children to get the best leaf node.
			 *
			 * @return The best leaf node.
			 */
			private ColorNode getBestLeaf() {
				if (isLeaf()) {
					return this;
				}
				else {
					return getBestChild().getBestLeaf();
				}
			}

			@Override
			public String toString() {
				return "(" + mWeight + "|" + Color.red(mAverage) + "," + Color.green(mAverage) + "," + Color.blue(mAverage) + "|" + mVariance
						+ "|" + mValue + ")";
			}
		}
	}


}
