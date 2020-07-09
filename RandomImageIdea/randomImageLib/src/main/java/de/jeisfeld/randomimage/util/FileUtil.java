package de.jeisfeld.randomimage.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import de.jeisfeld.randomimage.Application;

/**
 * Utility class for helping parsing file systems.
 */
public final class FileUtil {
	/**
	 * The name of the primary volume (LOLLIPOP).
	 */
	private static final String PRIMARY_VOLUME_NAME = "primary";

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
	 * The SD card path.
	 */
	public static final String SD_CARD_PATH = getSdCardPath();


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
		FileOutputStream outStream = null;
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		try {
			inStream = new FileInputStream(source);

			// First try the normal way
			if (isWritable(target)) {
				outStream = new FileOutputStream(target);
				inChannel = inStream.getChannel();
				outChannel = outStream.getChannel();
				inChannel.transferTo(0, inChannel.size(), outChannel);
			}
			else {
				return false;
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
				if (outChannel != null) {
					outChannel.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * Copy a file. The target file may even be on external SD card for Kitkat.
	 *
	 * @param source The source file
	 * @param target The target file
	 * @return true if the copying was successful.
	 */
	public static boolean copyFile(final File source, final DocumentFile target) {
		FileInputStream inStream = null;
		OutputStream outStream = null;
		try {
			inStream = new FileInputStream(source);
			outStream = Application.getAppContext().getContentResolver().openOutputStream(target.getUri());
			if (outStream != null) {
				byte[] buffer = new byte[4096]; // MAGIC_NUMBER
				int bytesRead;
				while ((bytesRead = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}
			}
		}
		catch (Exception e) {
			Log.e(Application.TAG,
					"Error when copying file from " + source.getAbsolutePath() + " to SAF file " + target.getName(), e);
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
		}
		return true;
	}

	/**
	 * Copy a file. The target file may even be on external SD card for Kitkat.
	 *
	 * @param source The source file
	 * @param target The target file
	 * @return true if the copying was successful.
	 */
	public static boolean copyFile(final DocumentFile source, final File target) {
		InputStream inStream = null;
		FileOutputStream outStream = null;
		try {
			inStream = Application.getAppContext().getContentResolver().openInputStream(source.getUri());
			outStream = new FileOutputStream(target);
			if (inStream != null) {
				byte[] buffer = new byte[4096]; // MAGIC_NUMBER
				int bytesRead;
				while ((bytesRead = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}
			}
		}
		catch (Exception e) {
			Log.e(Application.TAG,
					"Error when copying file from SAF file " + source.getName() + " to file " + target.getAbsolutePath(), e);
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
		if (file.delete()) {
			return true;
		}
		return !file.exists();
	}

	/**
	 * Delete a file. May be even on external SD card.
	 *
	 * @param file the file to be deleted.
	 * @return True if successfully deleted.
	 */
	public static boolean deleteFile(final DocumentFile file) {
		// First try the normal deletion.
		if (file.delete()) {
			return true;
		}
		return !file.exists();
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
	 * Check if a folder has subfolders.
	 *
	 * @param filePath the path of the input folder
	 * @return true if the input folder has subfolders.
	 */
	public static boolean hasSubfolders(final String filePath) {
		File file = new File(filePath);
		if (!file.exists() || !file.isDirectory()) {
			return false;
		}
		File[] subfolders = file.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File pathname) {
				return pathname.isDirectory();
			}
		});
		return subfolders != null && subfolders.length > 0;
	}

	/**
	 * Get a list of external SD card paths. (Kitkat or higher.)
	 *
	 * @return A list of external SD card paths.
	 */
	@RequiresApi(Build.VERSION_CODES.KITKAT)
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
		return paths.toArray(new String[0]);
	}

	/**
	 * Get a list of external SD card paths.
	 *
	 * @return A list of external SD card paths.
	 */
	public static String[] getExtSdCardPaths() {
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

			return paths.toArray(new String[0]);
		}
	}

	/**
	 * Check if the file represents the root folder of an external or internal SD card.
	 *
	 * @param file The file to be checked.
	 * @return true if root folder of an SD card.
	 */
	@RequiresApi(Build.VERSION_CODES.KITKAT)
	public static boolean isSdCardPath(final File file) {
		String filePath;
		try {
			filePath = file.getCanonicalPath();
		}
		catch (IOException e) {
			filePath = file.getAbsolutePath();
		}

		if (filePath.equals(getSdCardPath())) {
			return true;
		}

		for (String path : getExtSdCardPaths()) {
			if (filePath.equals(path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine the main folder of the external SD card containing the given file.
	 *
	 * @param file the file.
	 * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
	 * null is returned.
	 */

	public static String getExtSdCardFolderUnmounted(final File file) {
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
	 * Get the full path of a document from its tree URI.
	 *
	 * @param treeUri        The tree URI.
	 * @param volumeBasePath the base path of the volume.
	 * @return The path (without trailing file separator).
	 */
	@RequiresApi(api = VERSION_CODES.LOLLIPOP)
	@Nullable
	private static String getFullPathFromTreeUri(@Nullable final Uri treeUri, final String volumeBasePath) {
		if (treeUri == null) {
			return null;
		}
		if (volumeBasePath == null) {
			return File.separator;
		}
		String volumePath = volumeBasePath;
		if (volumePath.endsWith(File.separator)) {
			volumePath = volumePath.substring(0, volumePath.length() - 1);
		}

		String documentPath = FileUtil.getDocumentPathFromTreeUri(treeUri);
		if (documentPath.endsWith(File.separator)) {
			documentPath = documentPath.substring(0, documentPath.length() - 1);
		}

		if (documentPath.length() > 0) {
			if (documentPath.startsWith(File.separator)) {
				return volumePath + documentPath;
			}
			else {
				return volumePath + File.separator + documentPath;
			}
		}
		else {
			return volumePath;
		}
	}

	/**
	 * Get the full path of a document from its tree URI.
	 *
	 * @param treeUri The tree URI.
	 * @return The path (without trailing file separator).
	 */
	@RequiresApi(api = VERSION_CODES.LOLLIPOP)
	public static String getFullPathFromTreeUri(final Uri treeUri) {
		return getFullPathFromTreeUri(treeUri, getVolumePath(FileUtil.getVolumeIdFromTreeUri(treeUri)));
	}

	/**
	 * Get the path of a certain volume.
	 *
	 * @param volumeId The volume id.
	 * @return The path.
	 */
	private static String getVolumePath(final String volumeId) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return null;
		}

		try {
			StorageManager storageManager = (StorageManager) Application.getAppContext().getSystemService(Context.STORAGE_SERVICE);

			Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

			Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
			Method getUuid = storageVolumeClazz.getMethod("getUuid");
			Method getPath = storageVolumeClazz.getMethod("getPath");
			Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
			Object result = getVolumeList.invoke(storageManager);

			final int length = result == null ? 0 : Array.getLength(result);
			for (int i = 0; i < length; i++) {
				Object storageVolumeElement = Array.get(result, i);
				String uuid = (String) getUuid.invoke(storageVolumeElement);
				Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

				// primary volume?
				if (primary != null && primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
					return (String) getPath.invoke(storageVolumeElement);
				}

				// other volumes?
				if (uuid != null) {
					if (uuid.equals(volumeId)) {
						return (String) getPath.invoke(storageVolumeElement);
					}
				}
			}

			// not found.
			return null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Get the volume ID from the tree URI.
	 *
	 * @param treeUri The tree URI.
	 * @return The volume ID.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private static String getVolumeIdFromTreeUri(final Uri treeUri) {
		final String docId = DocumentsContract.getTreeDocumentId(treeUri);
		final String[] split = docId.split(":");

		if (split.length > 0) {
			return split[0];
		}
		else {
			return null;
		}
	}

	/**
	 * Get the document path (relative to volume name) for a tree URI (LOLLIPOP).
	 *
	 * @param treeUri The tree URI.
	 * @return the document path.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private static String getDocumentPathFromTreeUri(final Uri treeUri) {
		final String docId = DocumentsContract.getTreeDocumentId(treeUri);
		final String[] split = docId.split(":");
		if ((split.length >= 2) && (split[1] != null)) {
			return split[1];
		}
		else {
			return File.separator;
		}
	}

	/**
	 * Find out if a file is on an unmounted SD card path (for Lollipop).
	 *
	 * @param file The file
	 * @return the unmounted SD card path, if existing. Otherwise null.
	 */
	@RequiresApi(VERSION_CODES.LOLLIPOP)
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

		String path = getExtSdCardFolderUnmounted(file);
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
