package de.jeisfeld.randomimage.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileFilter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.exifinterface.media.ExifInterface;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.util.TrackingUtil.Category;
import de.jeisfeld.randomimagelib.R;

/**
 * Utility class for operations with images.
 */
public final class ImageUtil {
	// JAVADOC:OFF
	// Rotation angles
	private static final int ROTATION_90 = 90;
	private static final int ROTATION_180 = 180;
	private static final int ROTATION_270 = 270;

	// JAVADOC:ON

	/**
	 * The maximum resolution handled by this app.
	 */
	public static final int MAX_BITMAP_SIZE = 2048;

	/**
	 * The number of milliseconds below which parsing of image folders is considered as "quick".
	 */
	private static final long QUICK_PARSING_MILLIS = 500;

	/**
	 * The number of milliseconds after which the app again parses image folders.
	 */
	private static final long REPARSING_INTERVAL = TimeUnit.DAYS.toMillis(7);

	/**
	 * The file endings considered as image files.
	 */
	private static final List<String> IMAGE_SUFFIXES = Arrays.asList(
			"JPG", "JPEG", "PNG", "BMP", "GIF");

	/**
	 * Suffix used to indicate recursive folders.
	 */
	public static final String RECURSIVE_SUFFIX = File.separator + "*";

	/**
	 * Hide default constructor.
	 */
	private ImageUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the date field with the EXIF date from the file If not existing, use the last modified date.
	 *
	 * @param path The file path of the image
	 * @return the date stored in the EXIF data.
	 */
	public static Date getExifDate(final String path) {
		Date retrievedDate = null;
		try {
			ExifInterface exif = new ExifInterface(path);
			String dateString = exif.getAttribute(ExifInterface.TAG_DATETIME);

			retrievedDate = DateUtil.parse(dateString, "yyyy:MM:dd HH:mm:ss");
		}
		catch (Exception e) {
			Log.w(Application.TAG, e.toString() + " - Cannot retrieve EXIF date for " + path);
		}
		if (retrievedDate == null) {
			File f = new File(path);
			retrievedDate = new Date(f.lastModified());
		}
		return retrievedDate;
	}

	/**
	 * Retrieve the rotation angle from the Exif data of an image.
	 *
	 * @param path The file path of the image
	 * @return the rotation stored in the exif data, mapped into degrees.
	 */
	private static int getExifRotation(final String path) {
		int rotation = 0;
		try {
			ExifInterface exif = new ExifInterface(path);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

			// if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
			// // Use custom implementation, as the previous one is not always reliable
			// orientation = JpegMetadataUtil.getExifOrientation(new File(path));
			// }

			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotation = ROTATION_270;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotation = ROTATION_180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotation = ROTATION_90;
				break;
			default:
				break;
			}
		}
		catch (Exception e) {
			Log.w(Application.TAG, "Exception when getting EXIF rotation");
		}
		return rotation;
	}

	/**
	 * Get a bitmap from a bitmap resource.
	 *
	 * @param resourceId The resource id.
	 * @return the bitmap.
	 */
	public static Bitmap getBitmapFromResource(final int resourceId) {
		Drawable bitmapDrawable = Application.getAppContext().getResources().getDrawable(resourceId);

		if (bitmapDrawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) bitmapDrawable).getBitmap();
		}
		else {
			return null;
		}
	}

	/**
	 * Return a bitmap of this photo.
	 *
	 * @param path    The file path of the image.
	 * @param maxSize The maximum size of this bitmap. If bigger, it will be resized.
	 * @return the bitmap.
	 */
	public static Bitmap getImageBitmap(final String path, final int maxSize) {
		return getImageBitmap(path, maxSize, maxSize, false);
	}

	/**
	 * Return a bitmap of this photo.
	 *
	 * @param path           The file path of the image.
	 * @param maxWidth       The maximum width of this bitmap. If bigger, it will be resized.
	 * @param maxHeight      The maximum height of this bitmap. If bigger, it will be resized.
	 * @param growIfRequired Flag indicating if the image size should be increased if required.
	 * @return the bitmap.
	 */
	private static Bitmap getImageBitmap(final String path, final int maxWidth, final int maxHeight, final boolean growIfRequired) {
		Bitmap bitmap = null;
		boolean foundThumbInMediaStore = false;
		int rotation = getExifRotation(path);

		if (maxWidth <= 0 || maxHeight <= 0) {
			bitmap = BitmapFactory.decodeFile(path);
		}
		else {
			if ((maxWidth <= MediaStoreUtil.MINI_THUMB_SIZE || maxHeight <= MediaStoreUtil.MINI_THUMB_SIZE)
					&& !(path.toUpperCase().endsWith(".PNG") || path.toUpperCase().endsWith(".GIF"))) {
				bitmap = MediaStoreUtil.getThumbnailFromPath(path);
				if (bitmap != null) {
					foundThumbInMediaStore = true;
				}
			}

			if (bitmap == null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = getBitmapFactor(path, maxWidth, maxHeight, rotation == ROTATION_90 || rotation == ROTATION_270, false);
				if (path.toUpperCase().endsWith(".PNG") || path.toUpperCase().endsWith(".GIF")) {
					options.inPreferredConfig = Config.ARGB_8888;
				}

				// options.inPurgeable = true;
				bitmap = BitmapFactory.decodeFile(path, options);
				if (bitmap == null) {
					// cannot create bitmap - return dummy
					Log.w(Application.TAG, "Cannot create bitmap from path " + path + " - return dummy bitmap");
					return getDummyBitmap();
				}
				if (VERSION.SDK_INT >= VERSION_CODES.Q && rotation != 0) {
					bitmap = rotateBitmap(bitmap, rotation);
				}
			}
			if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
				return bitmap;
			}
			if (VERSION.SDK_INT < VERSION_CODES.Q && rotation != 0) {
				bitmap = rotateBitmap(bitmap, rotation);
			}

			if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight || foundThumbInMediaStore || growIfRequired) {
				// Only if bitmap is bigger than maxSize, then resize it - but don't trust the thumbs from media store.
				if ((long) bitmap.getWidth() * maxHeight > (long) bitmap.getHeight() * maxWidth) {
					// noinspection UnnecessaryLocalVariable
					int targetWidth = maxWidth;
					int targetHeight = bitmap.getHeight() * maxWidth / bitmap.getWidth();
					bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
				}
				else {
					int targetWidth = bitmap.getWidth() * maxHeight / bitmap.getHeight();
					// noinspection UnnecessaryLocalVariable
					int targetHeight = maxHeight;
					bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
				}
			}

		}

		return bitmap;
	}

	/**
	 * Return a bitmap of this photo having the given minimum size.
	 *
	 * @param path      The file path of the image.
	 * @param minWidth  The minimum width of this bitmap. If smaller, it will be resized.
	 * @param minHeight The minimum height of this bitmap. If smaller, it will be resized.
	 * @return the bitmap.
	 */
	private static Bitmap getImageBitmapOfMinimumSize(final String path, final int minWidth, final int minHeight) {
		Bitmap bitmap = null;
		int rotation = getExifRotation(path);

		if (minWidth <= 0 || minHeight <= 0) {
			bitmap = BitmapFactory.decodeFile(path);
		}
		else {
			if (minWidth <= MediaStoreUtil.MINI_THUMB_SIZE && minHeight <= MediaStoreUtil.MINI_THUMB_SIZE) {
				bitmap = MediaStoreUtil.getThumbnailFromPath(path);
			}

			if (bitmap == null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = getBitmapFactor(path, minWidth, minHeight, rotation == ROTATION_90 || rotation == ROTATION_270, true);
				// options.inPurgeable = true;
				bitmap = BitmapFactory.decodeFile(path, options);
				if (bitmap == null) {
					// cannot create bitmap - return dummy
					Log.w(Application.TAG, "Cannot create bitmap from path " + path + " - return dummy bitmap");
					return getDummyBitmap();
				}
				if (rotation != 0) {
					bitmap = rotateBitmap(bitmap, rotation);
				}
			}
			if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
				return bitmap;
			}

			if ((long) bitmap.getWidth() * minHeight > (long) bitmap.getHeight() * minWidth) {
				int targetWidth = bitmap.getWidth() * minHeight / bitmap.getHeight();
				// noinspection UnnecessaryLocalVariable
				int targetHeight = minHeight;
				bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
			}
			else {
				// noinspection UnnecessaryLocalVariable
				int targetWidth = minWidth;
				int targetHeight = bitmap.getHeight() * minWidth / bitmap.getWidth();
				bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
			}
		}

		return bitmap;
	}

	/**
	 * Surround the given bitmap by transparent space to match the given size.
	 *
	 * @param baseBitmap the original bitmap. Should be already limited to at most the target dimensions.
	 * @param width      The width of the target bitmap.
	 * @param height     The height of the target bitmap.
	 * @return the sized bitmap.
	 */
	private static Bitmap extendToBitmapOfSize(final Bitmap baseBitmap, final int width, final int height) {
		Bitmap targetBitmap = Bitmap.createBitmap(width, height, baseBitmap.getConfig());
		Paint paint = new Paint();
		Canvas canvas = new Canvas(targetBitmap);
		canvas.drawBitmap(baseBitmap, (width - baseBitmap.getWidth()) / 2f, (height - baseBitmap.getHeight()) / 2f, paint);
		return targetBitmap;
	}

	/**
	 * Return a bitmap of this photo, where the Bitmap object has the exact given size.
	 *
	 * @param path   The file path of the image.
	 * @param width  The width of the target bitmap.
	 * @param height The height of the target bitmap.
	 * @param border The size of the border around the image. If negative, the image will be stretched to fill the bitmap.
	 * @return the bitmap.
	 */
	public static Bitmap getBitmapOfExactSize(final String path, final int width, final int height, final int border) {
		if (border >= 0) {
			Bitmap baseBitmap = getImageBitmap(path, width - 2 * border, height - 2 * border, true);
			return extendToBitmapOfSize(baseBitmap, width, height);
		}
		else {
			Bitmap baseBitmap = getImageBitmapOfMinimumSize(path, width, height);
			Bitmap targetBitmap = Bitmap.createBitmap(width, height, baseBitmap.getConfig());
			Paint paint = new Paint();
			Canvas canvas = new Canvas(targetBitmap);
			if (baseBitmap.getWidth() * height >= baseBitmap.getHeight() * width) {
				int horizontalPadding = (baseBitmap.getWidth() - baseBitmap.getHeight() * width / height) / 2;
				Rect srcRect = new Rect(horizontalPadding, 0, horizontalPadding + baseBitmap.getHeight() * width / height, baseBitmap.getHeight());
				canvas.drawBitmap(baseBitmap, srcRect, new Rect(0, 0, width, height), paint);
			}
			else {
				int verticalPadding = (baseBitmap.getHeight() - baseBitmap.getWidth() * height / width) / 2;
				Rect srcRect = new Rect(0, verticalPadding, baseBitmap.getWidth(), verticalPadding + baseBitmap.getWidth() * height / width);
				canvas.drawBitmap(baseBitmap, srcRect, new Rect(0, 0, width, height), paint);
			}
			return targetBitmap;
		}
	}

	/**
	 * Return a bitmap of this photo, where the Bitmap object has the exact given width and the minimum given height.
	 *
	 * @param path      The file path of the image.
	 * @param width     The width of the target bitmap.
	 * @param minHeight The minimum height of the target bitmap.
	 * @return the bitmap.
	 */
	public static Bitmap getBitmapOfMinimumHeight(final String path, final int width, final int minHeight) {
		Bitmap baseBitmap = getImageBitmap(path, width, Integer.MAX_VALUE, true);
		if (baseBitmap.getHeight() >= minHeight) {
			return baseBitmap;
		}
		else {
			return extendToBitmapOfSize(baseBitmap, width, minHeight);
		}
	}

	/**
	 * Utility to retrieve the sample size for BitmapFactory.decodeFile.
	 *
	 * @param filepath     the path of the bitmap.
	 * @param targetWidth  the target width of the bitmap
	 * @param targetHeight the target height of the bitmap
	 * @param rotate       flag indicating if the bitmap should be 90 degrees rotated.
	 * @param minimum      flag indicating if the dimensions are minimum dimensions.
	 * @return the sample size to be used.
	 */
	private static int getBitmapFactor(final String filepath, final int targetWidth, final int targetHeight, final boolean rotate,
									   final boolean minimum) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filepath, options);

		if (rotate) {
			return minimum
					? Math.min(options.outHeight / targetWidth, options.outWidth / targetHeight)
					: Math.max(options.outHeight / targetWidth, options.outWidth / targetHeight);
		}
		else {
			return minimum
					? Math.min(options.outWidth / targetWidth, options.outHeight / targetHeight)
					: Math.max(options.outWidth / targetWidth, options.outHeight / targetHeight);
		}
	}

	/**
	 * Rotate a bitmap.
	 *
	 * @param source The original bitmap
	 * @param angle  The rotation angle
	 * @return the rotated bitmap.
	 */
	private static Bitmap rotateBitmap(final Bitmap source, final float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	/**
	 * Get Mime type from URI.
	 *
	 * @param uri The URI
	 * @return the mime type.
	 */
	public static String getMimeType(final Uri uri) {
		ContentResolver contentResolver = Application.getAppContext().getContentResolver();
		String mimeType = contentResolver.getType(uri);
		if (mimeType == null) {
			String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
			if (extension != null) {
				extension = extension.toLowerCase(Locale.getDefault());
			}
			mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if (mimeType == null) {
				mimeType = "unknown";
			}
		}
		return mimeType;
	}

	/**
	 * Check if a file is an image file.
	 *
	 * @param file   The file
	 * @param strict if true, then the file content will be checked, otherwise the suffix is sufficient.
	 * @return true if it is an image file.
	 */
	public static boolean isImage(final File file, final boolean strict) {
		if (file == null || !file.exists() || file.isDirectory()) {
			return false;
		}
		if (strict) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(file.getPath(), options);

			return options.outWidth > 0 && options.outHeight > 0;
		}
		else {
			String fileName = file.getName();
			int index = fileName.lastIndexOf('.');
			if (index >= 0) {
				String suffix = fileName.substring(index + 1);
				return IMAGE_SUFFIXES.contains(suffix.toUpperCase(Locale.getDefault()));
			}
			else {
				return false;
			}
		}

	}

	/**
	 * Get the list of image files in a folder.
	 *
	 * @param folderName The folder name.
	 * @return The list of image files in this folder.
	 */
	public static ArrayList<String> getImagesInFolder(final String folderName) {
		ArrayList<String> fileNames = new ArrayList<>();
		if (folderName == null) {
			return fileNames;
		}

		List<String> imageFolders = new ArrayList<>();
		File folder = new File(folderName);

		if (folderName.endsWith(ImageUtil.RECURSIVE_SUFFIX)) {
			imageFolders.addAll(ImageUtil.getImageSubfolders(folderName));
		}
		else if (folder.exists() && folder.isDirectory()) {
			imageFolders.add(folderName);
		}
		else {
			return fileNames;
		}

		for (String imageFolderName : imageFolders) {
			File imageFolder = new File(imageFolderName);
			if (imageFolder.exists() && imageFolder.isDirectory()) {
				File[] imageFiles = imageFolder.listFiles(new FileFilter() {
					@Override
					public boolean accept(final File file) {
						return isImage(file, false);
					}
				});
				if (imageFiles != null) {
					for (File file : imageFiles) {
						fileNames.add(file.getAbsolutePath());
					}
				}
			}
		}

		Collections.sort(fileNames, Collator.getInstance());
		return fileNames;
	}

	/**
	 * Get information if a path represents an image folder.
	 *
	 * @param folderName The path.
	 * @return True if this is an image folder.
	 */
	public static boolean isImageFolder(final String folderName) {
		return getImagesInFolder(folderName).size() > 0;
	}

	/**
	 * Get all image folders on the device in a separate thread.
	 *
	 * @param listener A listener handling the response via callback.
	 */
	public static void getAllImageFolders(final OnImageFoldersFoundListener listener) {
		final Handler handler = new Handler();
		new Thread() {
			@Override
			public void run() {
				long timestamp = System.currentTimeMillis();
				final ArrayList<String> imageFolders = new ArrayList<>(getAllImageSubfolders(new File(FileUtil.SD_CARD_PATH), handler, listener));

				for (String path : FileUtil.getExtSdCardPaths()) {
					imageFolders.addAll(getAllImageSubfolders(new File(path), handler, listener));
				}
				TrackingUtil.sendTiming(Category.TIME_BACKGROUND, "Parse_Image_Folders", null, System.currentTimeMillis() - timestamp);
				TrackingUtil.sendEvent(Category.COUNTER_IMAGES, "Image_folders", null, (long) imageFolders.size());

				PreferenceUtil.setSharedPreferenceStringList(R.string.key_all_image_folders, imageFolders);
				PreferenceUtil.setSharedPreferenceLong(R.string.key_last_parsing_time, System.currentTimeMillis());
				if (listener != null) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							listener.handleImageFolders(imageFolders);
						}
					});
				}
			}
		}.start();
	}

	/**
	 * Redo parsing of all image folders if the last parsing was more than one week ago.
	 */
	public static void refreshStoredImageFoldersIfApplicable() {
		if (System.currentTimeMillis() > PreferenceUtil.getSharedPreferenceLong(R.string.key_last_parsing_time, 0) + REPARSING_INTERVAL) {
			getAllImageFolders(null);
		}
	}

	/**
	 * Get all image folders below one parent folder.
	 *
	 * @param parentFolder the folder where to look for image sub folders
	 * @param handler      A handler running on the GUI thread.
	 * @param listener     A listener handling the response via callback.
	 * @return The array of image folders.
	 */
	private static ArrayList<String> getAllImageSubfolders(final File parentFolder, final Handler handler,
														   final OnImageFoldersFoundListener listener) {
		ArrayList<String> result = new ArrayList<>();
		if (parentFolder == null) {
			return result;
		}
		if (!parentFolder.exists() || !parentFolder.isDirectory()) {
			return result;
		}
		if (!PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_show_hidden_folders)) {
			if (parentFolder.getName().startsWith(".")) {
				// do not consider hidden paths
				return result;
			}
			if (parentFolder.getAbsolutePath().endsWith("/Android/data")) {
				// do not consider Android data paths.
				return result;
			}
			if (isNoMediaDirectory(parentFolder)) {
				// do not consider .nomedia folders
				return result;
			}
		}
		// do not consider paths excluded via regexp
		final String hiddenFoldersPattern = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_hidden_folders_pattern);
		final boolean useRegexp = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_use_regex_filter)
				&& hiddenFoldersPattern != null && hiddenFoldersPattern.length() > 0;
		if (useRegexp && parentFolder.getAbsolutePath().matches(hiddenFoldersPattern)) {
			return result;
		}

		int numberOfImageFolders = 0;
		List<String> imageFiles = getImagesInFolder(parentFolder.getAbsolutePath());

		if (imageFiles.size() > 0) {
			result.add(parentFolder.getAbsolutePath());
			numberOfImageFolders++;
			if (handler != null && listener != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						listener.handleImageFolder(parentFolder.getAbsolutePath());
					}
				});
			}
		}

		File[] children = parentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return file.isDirectory();
			}
		});
		if (children == null) {
			return result;
		}
		Arrays.sort(children, FileComparator.getInstance());

		for (File aChildren : children) {
			List<String> imageFolders = getAllImageSubfolders(aChildren, handler, listener);
			if (imageFolders.size() > 0) {
				result.addAll(imageFolders);
				numberOfImageFolders++;
			}
		}

		// Add the current folder as recursive folder if there is more than one image subfolder.
		if (numberOfImageFolders >= 2) {
			result.add(0, parentFolder.getAbsolutePath() + RECURSIVE_SUFFIX);
			if (handler != null && listener != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						listener.handleImageFolder(parentFolder.getAbsolutePath() + RECURSIVE_SUFFIX);
					}
				});
			}
		}

		return result;
	}

	/**
	 * Check if a directory is marked as containing no media files.
	 *
	 * @param folder The directory path.
	 * @return true if marked as no media.
	 */
	private static boolean isNoMediaDirectory(final File folder) {
		File nomediaFile = new File(folder, ".nomedia");
		return nomediaFile.exists() && nomediaFile.isFile();
	}

	/**
	 * Get the list of all image folders from previously retrieved list.
	 *
	 * @return The list of all image folders, filtered by regexp.
	 */
	public static ArrayList<String> getAllStoredImageFolders() {
		final List<String> allImageFolders = PreferenceUtil.getSharedPreferenceStringList(R.string.key_all_image_folders);
		final ArrayList<String> filteredImageFolders = new ArrayList<>();
		String hiddenFoldersPattern = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_hidden_folders_pattern);
		boolean useRegexp = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_use_regex_filter)
				&& hiddenFoldersPattern != null && hiddenFoldersPattern.length() > 0;

		for (String path : allImageFolders) {
			if (!useRegexp || !path.matches(hiddenFoldersPattern)) {
				filteredImageFolders.add(path);
			}
		}
		return filteredImageFolders;
	}

	/**
	 * Get the list of all image subfolders of a given folder, either from stored list or by parsing again, in dependence of the
	 * parsing speed.
	 *
	 * @param parentFolder The input folder
	 * @return The image subfolders of this folder.
	 */
	public static ArrayList<String> getImageSubfolders(final String parentFolder) {
		ArrayList<String> quickParsingFolders = PreferenceUtil.getSharedPreferenceStringList(R.string.key_quick_parsing_image_folders);
		String pathPrefix = parentFolder;
		if (parentFolder.endsWith(RECURSIVE_SUFFIX)) {
			pathPrefix = parentFolder.substring(0, parentFolder.length() - RECURSIVE_SUFFIX.length());
		}
		if (quickParsingFolders.contains(pathPrefix)) {
			return getAllImageSubfolders(new File(pathPrefix), null, null);
		}
		else {
			ArrayList<String> result = new ArrayList<>();
			for (String folder : getAllStoredImageFolders()) {
				if (folder.equals(pathPrefix) || (folder.startsWith(pathPrefix + "/") && !folder.endsWith(RECURSIVE_SUFFIX))) {
					result.add(folder);
				}
			}
			return result;
		}
	}

	/**
	 * Check if parsing of subfolders of a given parent folder goes quickly.
	 *
	 * @param parentFolder The parent folder.
	 */
	public static void checkForQuickParsing(final String parentFolder) {
		final String pathPrefix;
		if (parentFolder.endsWith(RECURSIVE_SUFFIX)) {
			pathPrefix = parentFolder.substring(0, parentFolder.length() - RECURSIVE_SUFFIX.length());
		}
		else {
			pathPrefix = parentFolder;
		}

		new Thread() {
			@Override
			public void run() {
				long startParsingTimestamp = System.currentTimeMillis();
				getAllImageSubfolders(new File(pathPrefix), null, null);
				long parsingDuration = System.currentTimeMillis() - startParsingTimestamp;
				ArrayList<String> quickParsingFolders = PreferenceUtil.getSharedPreferenceStringList(R.string.key_quick_parsing_image_folders);
				if (parsingDuration > QUICK_PARSING_MILLIS) {
					quickParsingFolders.remove(pathPrefix);
				}
				else {
					quickParsingFolders.add(pathPrefix);
				}
				PreferenceUtil.setSharedPreferenceStringList(R.string.key_quick_parsing_image_folders, quickParsingFolders);
			}
		}.start();
	}

	/**
	 * Get the short name of an image folder.
	 *
	 * @param folderName The path of the image folder.
	 * @return The short name.
	 */
	public static String getImageFolderShortName(final String folderName) {
		if (folderName.endsWith(RECURSIVE_SUFFIX)) {
			String shortName = new File(folderName).getParentFile().getName();
			if ((FileUtil.SD_CARD_PATH + RECURSIVE_SUFFIX).equals(folderName) && !shortName.toLowerCase().contains("sd")) {
				return "SD Card";
			}
			else {
				return shortName;
			}
		}
		else {
			return new File(folderName).getName();
		}
	}

	/**
	 * Retrieves a dummy bitmap (for the case that an image file is not readable).
	 *
	 * @return the dummy bitmap.
	 */
	public static Bitmap getDummyBitmap() {
		return BitmapFactory.decodeResource(Application.getAppContext().getResources(), R.drawable.cannot_read_image);
	}

	/**
	 * Show a file in the phone gallery.
	 *
	 * @param context  the context from which the gallery is opened.
	 * @param fileName The file name.
	 * @return true if successful
	 */
	public static boolean showFileInGallery(final Context context, final String fileName) {
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			Uri uri = MediaStoreUtil.getUriFromFile(fileName);
			intent.setData(uri);
			context.startActivity(intent);
			return true;
		}
		catch (Exception e) {
			Log.e(Application.TAG, "Could not open file " + fileName + " in gallery.", e);
			return false;
		}
	}

	/**
	 * Utility method to change the colours of a black/white bitmap.
	 *
	 * @param sourceBitmap The original bitmap
	 * @param colorBlack   The target color of the black parts
	 * @param colorWhite   The target color of the white parts
	 * @return the bitmap with the target color.
	 */
	public static Bitmap changeBitmapColor(final Bitmap sourceBitmap, final int colorBlack, final int colorWhite) {
		if (sourceBitmap == null) {
			return null;
		}
		Bitmap colouredBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), sourceBitmap.getConfig());
		float red = Color.red(colorBlack);
		float green = Color.green(colorBlack);
		float blue = Color.blue(colorBlack);
		float red2 = Color.red(colorWhite);
		float green2 = Color.green(colorWhite);
		float blue2 = Color.blue(colorWhite);
		ColorMatrix colorMatrix = new ColorMatrix(new float[]{ //
				(red2 - red) / 255, 0, 0, 0, red, // MAGIC_NUMBER
				0, (green2 - green) / 255, 0, 0, green, // MAGIC_NUMBER
				0, 0, (blue2 - blue) / 255, 0, blue, // MAGIC_NUMBER
				0, 0, 0, 1, 0});
		ColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

		Paint paint = new Paint();
		paint.setColorFilter(filter);
		Canvas canvas = new Canvas(colouredBitmap);
		canvas.drawBitmap(sourceBitmap, 0, 0, paint);
		return colouredBitmap;
	}

	/**
	 * Get a colorized bitmap from a bitmap resource.
	 *
	 * @param resourceId   The bitmap resource id.
	 * @param colorBlackId The resourceId of the target color of the black parts
	 * @param colorWhiteId The resourceId of the target color of the white parts
	 * @return the colorized image bitmap
	 */
	public static Bitmap getColorizedBitmap(final int resourceId, final int colorBlackId, final int colorWhiteId) {
		return changeBitmapColor(getBitmapFromResource(resourceId),
				Application.getAppContext().getResources().getColor(colorBlackId),
				Application.getAppContext().getResources().getColor(colorWhiteId));
	}

	/**
	 * Get a transparent icon from a resource id.
	 *
	 * @param resourceId The resource id.
	 * @return The transparent icon.
	 */
	public static Drawable getTransparentIcon(final int resourceId) {
		Resources resources = Application.getAppContext().getResources();
		Drawable icon = new BitmapDrawable(resources, BitmapFactory.decodeResource(resources, resourceId));
		icon.setAlpha(128); // MAGIC_NUMBER
		return icon;
	}

	/**
	 * A listener to be called after all image folders have been found.
	 */
	public interface OnImageFoldersFoundListener {
		/**
		 * Handler for actions done after retrieving the complete list of image folders.
		 *
		 * @param imageFolders The list of image folders.
		 */
		void handleImageFolders(ArrayList<String> imageFolders);

		/**
		 * Handler for actions done after finding one image folder.
		 *
		 * @param imageFolder The image folder found.
		 */
		void handleImageFolder(String imageFolder);
	}

	/**
	 * A comparator comparing files by localized name.
	 */
	private static class FileComparator implements Comparator<File> {
		/**
		 * A singleton instance.
		 */
		private static final FileComparator INSTANCE = new FileComparator();

		/**
		 * Get the singleton instance.
		 *
		 * @return The singleton instance.
		 */
		private static FileComparator getInstance() {
			return INSTANCE;
		}

		@Override
		public int compare(final File o1, final File o2) {
			return Collator.getInstance().compare(o1.getName(), o2.getName());
		}
	}

}
