package de.jeisfeld.randomimage.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.util.Log;

import de.jeisfeld.randomimage.Application;

/**
 * Utility class for helping parsing file systems.
 */
public final class FileUtil {
	/**
	 * Potential external SD paths for pre-Kitkat devices.
	 */
	@SuppressLint("SdCardPath")
	private static final String[] EXT_SD_PATHS = {
			"/storage/sdcard1", //!< Motorola Xoom
			"/storage/extSdCard",  //!< Samsung SGS3
			"/storage/sdcard0/external_sdcard",  // user request
			"/mnt/extSdCard",
			"/mnt/sdcard/external_sd",  //!< Samsung galaxy family
			"/mnt/external_sd",
			"/mnt/media_rw/sdcard1",   //!< 4.4.2 on CyanogenMod S3
			"/Removable/MicroSD",              //!< Asus transformer prime
			"/mnt/emmc",
			"/storage/external_sd",            //!< LG
			"/storage/ext_sd",                 //!< HTC One Max
			"/storage/removable/sdcard1",      //!< Sony Xperia Z1
			"/data/sdext",
			"/data/sdext2",
			"/data/sdext3",
			"/data/sdext4",
	};


	/**
	 * Hide default constructor.
	 */
	private FileUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Copy a file. The target file may even be on external SD card for Kitkat.
	 *
	 * @param source The source file
	 * @param target The target file
	 * @return true if the copying was successful.
	 */
	@SuppressWarnings("null")
	public static boolean copyFile(final File source, final File target) {
		FileInputStream inStream = null;
		OutputStream outStream = null;
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		try {
			inStream = new FileInputStream(source);

			// First try the normal way
			if (isWritable(target)) {
				// standard way
				outStream = new FileOutputStream(target);
				inChannel = inStream.getChannel();
				outChannel = ((FileOutputStream) outStream).getChannel();
				inChannel.transferTo(0, inChannel.size(), outChannel);
			}
			else {
				if (SystemUtil.isAndroid5()) {
					// TODO: Enable SAF
					return false;
				}
				else if (SystemUtil.isKitkat()) {
					// Workaround for Kitkat ext SD card
					Uri uri = MediaStoreUtil.getUriFromFile(target.getAbsolutePath());
					if (uri == null) {
						return false;
					}
					else {
						outStream = Application.getAppContext().getContentResolver().openOutputStream(uri);
					}
				}
				else {
					return false;
				}

				if (outStream != null) {
					// Both for SAF and for Kitkat, write to output stream.
					byte[] buffer = new byte[4096]; // MAGIC_NUMBER
					int bytesRead;
					while ((bytesRead = inStream.read(buffer)) != -1) {
						outStream.write(buffer, 0, bytesRead);
					}
				}

			}
		}
		catch (Exception e) {
			Log.e(Application.TAG,
					"Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
			return false;
		}
		finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (outStream != null) {
					outStream.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (inChannel != null) {
					inChannel.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			try {
				assert outChannel != null;
				outChannel.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * Delete a file. May be even on external SD card.
	 *
	 * @param file the file to be deleted.
	 * @return True if successfully deleted.
	 */
	public static boolean deleteFile(final File file) {
		// First try the normal deletion.
		if (file.delete()) {
			return true;
		}

		// Try with Storage Access Framework.
		if (SystemUtil.isAndroid5()) {
			// TODO: enable SAF
			return false;
		}

		// Try the Kitkat workaround.
		if (SystemUtil.isKitkat()) {
			ContentResolver resolver = Application.getAppContext().getContentResolver();

			try {
				Uri uri = MediaStoreUtil.getUriFromFile(file.getAbsolutePath());
				if (uri != null) {
					resolver.delete(uri, null, null);
				}
				return !file.exists();
			}
			catch (Exception e) {
				Log.e(Application.TAG, "Error when deleting file " + file.getAbsolutePath(), e);
				return false;
			}
		}

		return !file.exists();
	}

	/**
	 * Move a file. The target file may even be on external SD card.
	 *
	 * @param source The source file
	 * @param target The target file
	 * @return true if the copying was successful.
	 */
	public static boolean moveFile(final File source, final File target) {
		// First try the normal rename.
		if (source.renameTo(target)) {
			return true;
		}

		boolean success = copyFile(source, target);
		if (success) {
			success = deleteFile(source);
		}
		return success;
	}

	/**
	 * Check is a file is writable. Detects write issues on external SD card.
	 *
	 * @param file The file
	 * @return true if the file is writable.
	 */
	public static boolean isWritable(final File file) {
		boolean isExisting = file.exists();

		try {
			FileOutputStream output = new FileOutputStream(file, true);
			try {
				output.close();
			}
			catch (IOException e) {
				// do nothing.
			}
		}
		catch (FileNotFoundException e) {
			return false;
		}
		boolean result = file.canWrite();

		// Ensure that file is not created during this process.
		if (!isExisting) {
			//noinspection ResultOfMethodCallIgnored
			file.delete();
		}

		return result;
	}

	/**
	 * Get a list of external SD card paths. (Kitkat or higher.)
	 *
	 * @return A list of external SD card paths.
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static String[] getExtSdCardPathsForKitkat() {
		List<String> paths = new ArrayList<>();
		for (File file : Application.getAppContext().getExternalFilesDirs("external")) {
			if (file != null && !file.equals(Application.getAppContext().getExternalFilesDir("external"))) {
				int index = file.getAbsolutePath().lastIndexOf("/Android/data");
				if (index < 0) {
					Log.w(Application.TAG, "Unexpected external file dir: " + file.getAbsolutePath());
				}
				else {
					String path = file.getAbsolutePath().substring(0, index);
					try {
						path = new File(path).getCanonicalPath();
					}
					catch (IOException e) {
						// Keep non-canonical path.
					}
					paths.add(path);
				}
			}
		}
		return paths.toArray(new String[paths.size()]);
	}

	/**
	 * Get a list of external SD card paths.
	 *
	 * @return A list of external SD card paths.
	 */
	protected static String[] getExtSdCardPaths() {
		if (SystemUtil.isAtLeastVersion(VERSION_CODES.KITKAT)) {
			return getExtSdCardPathsForKitkat();
		}
		else {
			List<String> paths = new ArrayList<>();
			for (String path : EXT_SD_PATHS) {
				if (new File(path).isDirectory()) {
					paths.add(path);
				}
			}

			// remove duplicate Samsung path
			if (paths.contains("/mnt/extSdCard") && paths.contains("/storage/extSdCard")) {
				paths.remove("/mnt/extSdCard");
			}

			return paths.toArray(new String[paths.size()]);
		}
	}

	/**
	 * Determine the main folder of the external SD card containing the given file.
	 *
	 * @param file the file.
	 * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
	 * null is returned.
	 */

	public static String getExtSdCardFolder(final File file) {
		// Do not use Kitkat API, as it is unreliable for unmounted paths.
		for (String path : EXT_SD_PATHS) {
			if (file.getAbsolutePath().toLowerCase(Locale.getDefault()).startsWith(path.toLowerCase(Locale.getDefault()))) {
				return file.getAbsolutePath().substring(0, path.length());
			}
		}
		return null;
	}

	/**
	 * Get the SD card directory.
	 *
	 * @return The SD card directory.
	 */
	public static String getSdCardPath() {
		String sdCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

		try {
			sdCardDirectory = new File(sdCardDirectory).getCanonicalPath();
		}
		catch (IOException ioe) {
			Log.e(Application.TAG, "Could not get SD directory", ioe);
		}
		return sdCardDirectory;
	}

	/**
	 * Find out if a file is on an unmounted SD card path (for Lollipop).
	 *
	 * @param file The file
	 * @return the unmounted SD card path, if existing. Otherwise null.
	 */
	@TargetApi(VERSION_CODES.LOLLIPOP)
	public static String getUnmountedSdCardPathLollipop(final File file) {
		File currentFile = file;
		String mountStatus = Environment.getExternalStorageState(file);

		if (Environment.MEDIA_MOUNTED.equals(mountStatus)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(mountStatus)
				|| Environment.MEDIA_UNKNOWN.equals(mountStatus)) {
			return null;
		}

		File parentFile = currentFile.getParentFile();
		String parentStatus = parentFile == null ? null : Environment.getExternalStorageState(parentFile);
		while (parentStatus != null && !Environment.MEDIA_UNKNOWN.equals(parentStatus)) {
			currentFile = parentFile;
			parentFile = currentFile.getParentFile();
			parentStatus = parentFile == null ? null : Environment.getExternalStorageState(parentFile);
		}
		return currentFile.getAbsolutePath();
	}

	/**
	 * Find out if a file is on an unmounted SD card path.
	 *
	 * @param file The file
	 * @return the unmounted SD card path, if existing. Otherwise null.
	 */
	public static String getUnmountedSdCardPath(final File file) {
		if (SystemUtil.isAndroid5()) {
			return getUnmountedSdCardPathLollipop(file);
		}

		String path = getExtSdCardFolder(file);
		if (path == null) {
			return null;
		}
		else if (new File(path).isDirectory()) {
			String[] contents = new File(path).list();
			if (contents != null && contents.length > 0) {
				return null;
			}
			else {
				return path;
			}
		}
		else {
			return path;
		}
	}


	/**
	 * Determine the camera folder. There seems to be no Android API to work for real devices, so this is a best guess.
	 *
	 * @return the default camera folder.
	 */
	public static String getDefaultCameraFolder() {
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		if (path.exists()) {
			File test1 = new File(path, "Camera/");
			if (test1.exists()) {
				path = test1;
			}
			else {
				File test2 = new File(path, "100ANDRO/");
				if (test2.exists()) {
					path = test2;
				}
				else {
					path = new File(path, "100MEDIA/");
				}
			}
		}
		else {
			path = new File(path, "Camera/");
		}
		return path.getAbsolutePath();
	}

}
