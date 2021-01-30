package de.jeisfeld.randomimage.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.RequiresApi;
import de.jeisfeld.randomimage.Application;

/**
 * Utility class for handling the media store.
 */
public final class MediaStoreUtil {
	/**
	 * The size of a mini thumbnail.
	 */
	public static final int MINI_THUMB_SIZE = 512;

	/**
	 * Hide default constructor.
	 */
	private MediaStoreUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get a real file path from the URI of the media store.
	 *
	 * @param contentUri Thr URI of the media store
	 * @return the file path.
	 */
	public static String getRealPathFromUri(final Uri contentUri) {
		Cursor cursor = null;
		try {
			String[] proj = {MediaStore.MediaColumns.DATA};
			cursor = Application.getAppContext().getContentResolver().query(contentUri, proj, null, null, null);
			if (cursor == null) {
				return null;
			}
			int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
			cursor.moveToFirst();
			return cursor.getString(columnIndex);
		}
		catch (Exception e) {
			return null;
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Retrieve a the image id of an image in the Mediastore from the path.
	 *
	 * @param path The path of the image
	 * @return the image id.
	 * @throws ImageNotFoundException thrown if the image is not found in the media store.
	 */
	private static int getImageId(final String path) throws ImageNotFoundException {
		ContentResolver resolver = Application.getAppContext().getContentResolver();

		try {
			Cursor imagecursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					new String[]{BaseColumns._ID}, MediaStore.MediaColumns.DATA + " = ?",
					new String[]{path}, MediaStore.MediaColumns.DATE_ADDED + " desc");
			if (imagecursor == null) {
				throw new ImageNotFoundException();
			}
			imagecursor.moveToFirst();

			if (!imagecursor.isAfterLast()) {
				int imageId = imagecursor.getInt(imagecursor.getColumnIndex(BaseColumns._ID));
				imagecursor.close();
				return imageId;
			}
			else {
				imagecursor.close();
				throw new ImageNotFoundException();
			}
		}
		catch (Exception e) {
			throw new ImageNotFoundException(e);
		}
	}

	/**
	 * Get an Uri from an file path.
	 *
	 * @param path The file path.
	 * @return The Uri. May be null.
	 */
	public static Uri getUriFromFile(final String path) {
		ContentResolver resolver = Application.getAppContext().getContentResolver();

		Cursor filecursor = resolver.query(MediaStore.Images.Media.getContentUri("external"),
				new String[]{BaseColumns._ID}, MediaColumns.DATA + " = ?",
				new String[]{path}, MediaColumns.DATE_ADDED + " desc");
		if (filecursor == null) {
			return null;
		}
		filecursor.moveToFirst();

		if (filecursor.isAfterLast()) {
			filecursor.close();
			triggerMediaScan(null, null, path);
			return null;
		}
		else {
			long imageId = filecursor.getLong(filecursor.getColumnIndex(BaseColumns._ID));
			Uri uri = MediaStore.Images.Media.getContentUri("external").buildUpon().appendPath(
					Long.toString(imageId)).build();
			filecursor.close();
			return uri;
		}
	}

	/**
	 * Get the list of all image paths.
	 *
	 * @return The list of all image paths.
	 */
	@RequiresApi(api = VERSION_CODES.KITKAT)
	public static List<String> getAllImagePaths() {
		ContentResolver resolver = Application.getAppContext().getContentResolver();
		String[] projection = {
				MediaColumns.DATA
		};
		List<String> result;
		try (Cursor cursor = resolver.query(MediaStore.Images.Media.getContentUri("external"), projection, null, null, null)) {
			result = new ArrayList<>();
			if (cursor != null) {
				while (cursor.moveToNext()) {
					result.add(cursor.getString(0));
				}
			}
		}
		return result;
	}

	/**
	 * Retrieve a thumbnail of a bitmap from the mediastore.
	 *
	 * @param path The path of the image
	 * @return the thumbnail.
	 */
	public static Bitmap getThumbnailFromPath(final String path) {
		ContentResolver resolver = Application.getAppContext().getContentResolver();

		try {
			int imageId = getImageId(path);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inDither = true;
			options.inSampleSize = 1;
			return MediaStore.Images.Thumbnails.getThumbnail(resolver, imageId, MediaStore.Images.Thumbnails.MINI_KIND, options);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Trigger the media scanner for all files.
	 *
	 * @param context   the context.
	 * @param basePaths the base paths for scanning.
	 * @param listener  A listener called when the scan is completed.
	 */
	public static void triggerMediaScan(final Context context, final OnScanCompletedListener listener, final String... basePaths) {
		List<String> paths;
		if (basePaths == null || basePaths.length == 0) {
			paths = FileUtil.getExtSdCardPaths();
			paths.add(0, FileUtil.SD_CARD_PATH);
		}
		else {
			paths = Arrays.asList(basePaths);
		}
		MediaScannerConnection.scanFile(context == null ? Application.getAppContext() : context, paths.toArray(new String[0]), null, listener);
	}

	/**
	 * Delete the thumbnail of a bitmap.
	 *
	 * @param path The path of the image
	 */
	public static void deleteThumbnail(final String path) {
		ContentResolver resolver = Application.getAppContext().getContentResolver();

		try {
			int imageId = getImageId(path);
			resolver.delete(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
					MediaStore.Images.Thumbnails.IMAGE_ID + " = ?", new String[]{"" + imageId});
		}
		catch (ImageNotFoundException e) {
			// ignore
		}
	}

	/**
	 * Utility exception to be thrown if an image cannot be found.
	 */
	private static final class ImageNotFoundException extends Exception {
		/**
		 * The default serial version id.
		 */
		private static final long serialVersionUID = 1L;

		private ImageNotFoundException() {
		}

		private ImageNotFoundException(final Throwable e) {
			super(e);
		}
	}

}
