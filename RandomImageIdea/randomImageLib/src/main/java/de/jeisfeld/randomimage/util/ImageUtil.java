package de.jeisfeld.randomimage.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.webkit.MimeTypeMap;
import de.jeisfeld.randomimage.Application;
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
	 * Number of milliseconds for retry of getting bitmap.
	 */
	private static final long BITMAP_RETRY = 50;

	/**
	 * The file endings considered as image files.
	 */
	private static final List<String> IMAGE_SUFFIXES = Arrays.asList(
			new String[] { "JPG", "JPEG", "PNG", "BMP", "TIF", "TIFF", "GIF" });

	/**
	 * Hide default constructor.
	 */
	private ImageUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the date field with the EXIF date from the file If not existing, use the last modified date.
	 *
	 * @param path
	 *            The file path of the image
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
	 * @param path
	 *            The file path of the image
	 * @return the rotation stored in the exif data, mapped into degrees.
	 */
	public static int getExifRotation(final String path) {
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
	 * Return a bitmap of this photo.
	 *
	 * @param path
	 *            The file path of the image.
	 * @param maxSize
	 *            The maximum size of this bitmap. If bigger, it will be resized.
	 * @return the bitmap.
	 */
	public static Bitmap getImageBitmap(final String path, final int maxSize) {
		Bitmap bitmap = null;

		if (maxSize <= 0) {
			bitmap = BitmapFactory.decodeFile(path);
		}
		else {

			if (maxSize <= MediaStoreUtil.MINI_THUMB_SIZE) {
				bitmap = MediaStoreUtil.getThumbnailFromPath(path, maxSize);
			}

			if (bitmap == null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = getBitmapFactor(path, maxSize);
				// options.inPurgeable = true;
				bitmap = BitmapFactory.decodeFile(path, options);
				if (bitmap == null) {
					// cannot create bitmap - try once more in case that the image was just in process of saving
					// metadata
					try {
						Thread.sleep(BITMAP_RETRY);
					}
					catch (InterruptedException e) {
						// ignore exception
					}
					bitmap = BitmapFactory.decodeFile(path, options);
					if (bitmap == null) {
						// cannot create bitmap - return dummy
						Log.w(Application.TAG, "Cannot create bitmap from path " + path + " - return dummy bitmap");
						return getDummyBitmap();
					}
				}
			}
			if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
				return bitmap;
			}

			if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize
					|| maxSize <= MediaStoreUtil.MINI_THUMB_SIZE) {
				// Only if bitmap is bigger than maxSize, then resize it - but don't trust the thumbs from media store.
				if (bitmap.getWidth() > bitmap.getHeight()) {
					int targetWidth = maxSize;
					int targetHeight = bitmap.getHeight() * maxSize / bitmap.getWidth();
					bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
				}
				else {
					int targetWidth = bitmap.getWidth() * maxSize / bitmap.getHeight();
					int targetHeight = maxSize;
					bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
				}
			}

		}

		int rotation = getExifRotation(path);
		if (rotation != 0) {
			bitmap = rotateBitmap(bitmap, rotation);
		}

		return bitmap;
	}

	/**
	 * Retrieve a part of a bitmap in full resolution.
	 *
	 * @param fullBitmap
	 *            The bitmap from which to get the part.
	 * @param minX
	 *            The minimum X position to retrieve.
	 * @param maxX
	 *            The maximum X position to retrieve.
	 * @param minY
	 *            The minimum Y position to retrieve.
	 * @param maxY
	 *            The maximum Y position to retrieve.
	 * @return The bitmap.
	 */
	public static Bitmap getPartialBitmap(final Bitmap fullBitmap, final float minX, final float maxX,
			final float minY,
			final float maxY) {
		Bitmap partialBitmap =
				Bitmap.createBitmap(fullBitmap, Math.round(minX * fullBitmap.getWidth()),
						Math.round(minY * fullBitmap.getHeight()),
						Math.round((maxX - minX) * fullBitmap.getWidth()),
						Math.round((maxY - minY) * fullBitmap.getHeight()));

		return partialBitmap;
	}

	/**
	 * Utility to retrieve the sample size for BitmapFactory.decodeFile.
	 *
	 * @param filepath
	 *            the path of the bitmap.
	 * @param targetSize
	 *            the target size of the bitmap
	 * @return the sample size to be used.
	 */
	private static int getBitmapFactor(final String filepath, final int targetSize) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filepath, options);
		int size = Math.max(options.outWidth, options.outWidth);
		return size / targetSize;
	}

	/**
	 * Rotate a bitmap.
	 *
	 * @param source
	 *            The original bitmap
	 * @param angle
	 *            The rotation angle
	 * @return the rotated bitmap.
	 */
	public static Bitmap rotateBitmap(final Bitmap source, final float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	/**
	 * Get Mime type from URI.
	 *
	 * @param uri
	 *            The URI
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
	 * @param file
	 *            The file
	 * @param strict
	 *            if true, then the file content will be checked, otherwise the suffix is sufficient.
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
	 * @param folderName
	 *            The folder name.
	 * @return The list of image files in this folder.
	 */
	public static ArrayList<String> getImagesInFolder(final String folderName) {
		ArrayList<String> fileNames = new ArrayList<String>();
		if (folderName == null) {
			return fileNames;
		}
		File folder = new File(folderName);
		if (!folder.exists() || !folder.isDirectory()) {
			return fileNames;
		}
		File[] imageFiles = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return isImage(file, false);
			}
		});
		if (imageFiles == null) {
			return fileNames;
		}

		for (File file : imageFiles) {
			fileNames.add(file.getAbsolutePath());
		}
		Collections.sort(fileNames);
		return fileNames;
	}

	/**
	 * Get information if a path represents an image folder.
	 *
	 * @param folderName
	 *            The path.
	 * @return True if this is an image folder.
	 */
	public static boolean isImageFolder(final String folderName) {
		return getImagesInFolder(folderName).size() > 0;
	}

	/**
	 * Get all image folders on the device in a separate thread.
	 *
	 * @param listener
	 *            A listener handling the response via callback.
	 */
	public static void getAllImageFolders(final OnImageFoldersFoundListener listener) {
		final Handler handler = new Handler();

		new Thread() {
			@Override
			public void run() {
				final ArrayList<String> imageFolders = new ArrayList<String>();

				if (SystemUtil.isAtLeastVersion(Build.VERSION_CODES.KITKAT)) {
					imageFolders.addAll(getAllImageSubfolders(new File(FileUtil.getSdCardPath()), handler, listener));

					for (String path : FileUtil.getExtSdCardPaths()) {
						imageFolders.addAll(getAllImageSubfolders(new File(path), handler, listener));
					}
				}
				else {
					imageFolders.addAll(getAllImageSubfolders(new File("/mnt"), handler, listener));
					imageFolders.addAll(getAllImageSubfolders(new File("/Removable"), handler, listener));
				}

				if (listener != null) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							listener.handleImageFolders(imageFolders);
						}
					});
				}

				saveAllImageFolders(imageFolders);
			}
		}.start();
	}

	/**
	 * Get all image folders below one parent folder.
	 *
	 * @param parentFolder
	 *            the folder where to look for image sub folders
	 * @param handler
	 *            A handler running on the GUI thread.
	 * @param listener
	 *            A listener handling the response via callback.
	 * @return The array of image folders.
	 */
	private static ArrayList<String> getAllImageSubfolders(final File parentFolder, final Handler handler,
			final OnImageFoldersFoundListener listener) {
		ArrayList<String> result = new ArrayList<String>();
		if (parentFolder == null) {
			return result;
		}
		if (!parentFolder.exists() || !parentFolder.isDirectory()) {
			return result;
		}
		if (parentFolder.getName().startsWith(".")) {
			// do not consider hidden paths
			return result;
		}
		if (parentFolder.getAbsolutePath().endsWith("Android/data")) {
			// do not consider Android data paths.
			return result;
		}

		if (isImageFolder(parentFolder.getAbsolutePath())) {
			result.add(parentFolder.getAbsolutePath());
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

		for (int i = 0; i < children.length; i++) {
			result.addAll(getAllImageSubfolders(children[i], handler, listener));
		}

		return result;
	}

	/**
	 * Save the list of image folders in a shared preference.
	 *
	 * @param imageFolders
	 *            The list of image folders.
	 */
	private static void saveAllImageFolders(final ArrayList<String> imageFolders) {
		if (imageFolders == null || imageFolders.size() == 0) {
			PreferenceUtil.removeSharedPreference(R.string.key_all_image_folders);
			return;
		}

		StringBuffer saveStringBuffer = new StringBuffer();

		for (String imageFolder : imageFolders) {
			saveStringBuffer.append("\n");
			saveStringBuffer.append(imageFolder);
		}

		String saveString = saveStringBuffer.substring("\n".length());

		PreferenceUtil.setSharedPreferenceString(R.string.key_all_image_folders, saveString);
	}

	/**
	 * Restore the saved list of image folders from shared preference.
	 *
	 * @return The retrieved list of image folders.
	 */
	public static ArrayList<String> getAllImageFoldersFromStorage() {
		String restoreString = PreferenceUtil.getSharedPreferenceString(R.string.key_all_image_folders);
		if (restoreString == null || restoreString.length() == 0) {
			return new ArrayList<String>();
		}

		String[] folderArray = restoreString.split("\\r?\\n");
		return new ArrayList<String>(Arrays.asList(folderArray));
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
	 * @param context
	 *            the context from which the gallery is opened.
	 * @param fileName
	 *            The file name.
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
	 * File filter class to identify image files.
	 */
	public static class ImageFileFilter implements FileFilter {
		@Override
		public final boolean accept(final File file) {
			Uri uri = Uri.fromFile(file);
			return file.exists() && file.isFile() && ImageUtil.getMimeType(uri).startsWith("image/");
		}
	}

	/**
	 * A listener to be called after all image folders have been found.
	 */
	public interface OnImageFoldersFoundListener {
		/**
		 * Handler for actions done after retrieving the complete list of image folders.
		 *
		 * @param imageFolders
		 *            The list of image folders.
		 */
		void handleImageFolders(ArrayList<String> imageFolders);

		/**
		 * Handler for actions done after finding one image folder.
		 *
		 * @param imageFolder
		 *            The image folder found.
		 */
		void handleImageFolder(String imageFolder);
	}

}
