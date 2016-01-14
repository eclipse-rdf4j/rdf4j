/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
 
package org.eclipse.rdf4j.common.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.StringTokenizer;


/**
 * Utility methods for operations on Files.
 */
public class FileUtil {

	private static final String ILLEGAL_FILE_NAME_CHARS = "\\/:*?\"<>|";

	/**
	 * Gets the relative representations of a file compared to another.
	 * 
	 * @param subj
	 *        The File to find the relative form for.
	 * @param relativeTo
	 *        The File 'subj' should be made relative to.
	 * @return The relative representation of subj.
	 */
	public static String getRelativePath(File subj, File relativeTo) {
		// Get the absolute path to both files.
		String subjPath = subj.getAbsolutePath();
		String relativeToPath = relativeTo.getAbsolutePath();

		// Remove the filenames
		if (!subj.isDirectory()) {
			int idx = subjPath.lastIndexOf(File.separator);
			if (idx != -1) {
				subjPath = subjPath.substring(0, idx);
			}
		}
		if (!relativeTo.isDirectory()) {
			int idx = relativeToPath.lastIndexOf(File.separator);
			if (idx != -1) {
				relativeToPath = relativeToPath.substring(0, idx);
			}
		}

		// Check all common directories, starting with the root.
		StringTokenizer subjPathTok = new StringTokenizer(subjPath, File.separator);
		StringTokenizer relativeToPathTok = new StringTokenizer(relativeToPath, File.separator);

		String subjTok = null;
		String relativeToTok = null;
		while (subjPathTok.hasMoreTokens() && relativeToPathTok.hasMoreTokens()) {
			subjTok = subjPathTok.nextToken();
			relativeToTok = relativeToPathTok.nextToken();
			if (!subjTok.equals(relativeToTok)) {
				break;
			}
		}

		// If there are any tokens left, than these should be made relative.
		// The number of tokens left in 'relativeToTok' is the number of '..'.
		StringBuilder relPath = new StringBuilder();

		if (relativeToTok != null && !relativeToTok.equals(subjTok)) {
			// That's one dir
			relPath.append("..");
			relPath.append(File.separator);
		}
		while (relativeToPathTok.hasMoreTokens()) {
			relativeToPathTok.nextToken();
			relPath.append("..");
			relPath.append(File.separator);
		}

		// Now add the path to 'subj'
		if (subjTok != null && !subjTok.equals(relativeToTok)) {
			relPath.append(subjTok);
			relPath.append(File.separator);
		}
		while (subjPathTok.hasMoreTokens()) {
			subjTok = subjPathTok.nextToken();
			relPath.append(subjTok);
			relPath.append(File.separator);
		}

		// Last but not least, add the filename of 'subj'
		relPath.append(subj.getName());

		return relPath.toString();
	}

	/**
	 * Gets the relative representations of a file compared to another.
	 * 
	 * @param subj
	 *        The File to find the relative form for.
	 * @param relativeTo
	 *        The File 'subj' should be made relative to.
	 * @return The relative representation of subj.
	 */
	public static File getRelativeFile(File subj, File relativeTo) {
		return new File(getRelativePath(subj, relativeTo));
	}

	/**
	 * Gets the extension of the specified file name.
	 * 
	 * @param fileName
	 *        A file name.
	 * @return The file name extension (e.g. "exe" or "txt"), or <tt>null</tt>
	 *         if the file name does not have a (valid) extension.
	 */
	public static String getFileExtension(String fileName) {
		int lastDotIdx = fileName.lastIndexOf('.');

		if (lastDotIdx > 0 && lastDotIdx < fileName.length() - 1) {
			String extension = fileName.substring(lastDotIdx + 1).trim();

			if (isLegalFileName(extension)) {
				return extension;
			}
		}

		return null;
	}

	/**
	 * Checks whether the specified file name is a legal (DOS/Windows-) file
	 * name.
	 */
	public static boolean isLegalFileName(String fileName) {
		for (int i = 0; i < fileName.length(); i++) {
			char c = fileName.charAt(i);
			if (!isLegalFileNameChar(c)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks whether the specified character is a legal (DOS/Windows-) file
	 * name character.
	 */
	public static boolean isLegalFileNameChar(char c) {
		return ILLEGAL_FILE_NAME_CHARS.indexOf(c) == -1;
	}

	/**
	 * Copies the contents of file <tt>source</tt> to file
	 * <tt>destination</tt>.
	 */
	public static void copyFile(File source, File destination) throws IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(source);
			IOUtil.writeStream(in, destination);
		}
		finally {
			if (in != null)
				in.close();
		}
	}

	/**
	 * Creates a directory if it doesn't exist yet.
	 * 
	 * @param dir
	 *        The directory to create.
	 * @exception IOException
	 *            If the creation of the directory failed.
	 */
	public static void createDirIfNotExists(File dir) throws IOException {
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Unable to create directory: " + dir.toString());
		}
	}

	/**
	 * Deletes the given file and everything under it.
	 * 
	 * @return Whether all files were deleted succesfully.
	 */
	public static boolean deltree(File directory) {
		if (directory == null || !directory.exists()) {
			return true;
		}

		boolean result = true;
		if (directory.isFile()) {
			result = directory.delete();
		}
		else {
			File[] list = directory.listFiles();
			for (int i = list.length; i-- > 0;) {
				if (!deltree(list[i])) {
					result = false;
				}
			}
			if (!directory.delete()) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Deletes all files and directories in the specified directory. Nothing
	 * happens when the specified File is not a directory.
	 * 
	 * @return true when all files in the specified directory were successfully
	 *         deleted, when there where no files or when the specified file was
	 *         not a directory.
	 */
	public static boolean deleteFiles(File directory) {
		boolean result = true;
		
		if (directory.isDirectory()) {
			File[] list = directory.listFiles();
			for (int i = list.length; i-- > 0;) {
				File file = list[i];
				if (file.isFile()) {
					result = result && file.delete();
				}
			}
		}
		
		return result;
	}

	/**
	 * Deletes all files and directories in the specified directory. Nothing
	 * happens when the specified File is not a directory.
	 * 
	 * @return true when all children were successfully deleted, when there were
	 *         no children or when the file was not a directory.
	 */
	public static boolean deleteChildren(File directory) {
		boolean result = true;

		if (directory.isDirectory()) {
			File[] list = directory.listFiles();
			for (int i = list.length; i-- > 0;) {
				result = result && deltree(list[i]);
			}
		}

		return result;
	}

	/**
	 * Moves the given file and all files under it (if it's a directory) to the
	 * given location, excluding the given collection of File objects!
	 * 
	 * @param from
	 *        File or directory to be moved
	 * @param to
	 *        The file or directory to rename to
	 * @param excludes
	 *        The File objects to be excluded; if a directory is excluded, all
	 *        files under it are excluded as well!
	 * @return Whether moving was succesfull
	 */
	public static boolean moveRecursive(File from, File to, Collection<File> excludes) {
		if (from == null || !from.exists()) {
			return false;
		}

		boolean result = true;
		if (from.isFile()) {
			if (excludes == null || !excludes.contains(from)) {
				to.getParentFile().mkdirs();
				result = from.renameTo(to);
			}
		}
		else {
			boolean excludedFileFound = false;

			File[] list = from.listFiles();
			for (int i = list.length; i-- > 0;) {
				File listItem = list[i];
				if (excludes != null && excludes.contains(listItem)) {
					excludedFileFound = true;
				}
				else {
					if (!moveRecursive(listItem, new File(to, listItem.getName()), excludes)) {
						result = false;
					}
				}
			}

			// finally, move directory itself...
			if (!excludedFileFound) {
				if (!from.delete()) {
					result = false;
				}
			}
		}
		return result;
	}

	/**
	 * Creates a new and empty directory in the default temp directory using the
	 * given prefix. This methods uses {@link File#createTempFile} to create a
	 * new tmp file, deletes it and creates a directory for it instead.
	 * 
	 * @param prefix The prefix string to be used in generating the diretory's
	 * name; must be at least three characters long.
	 * @return A newly-created empty directory.
	 * @throws IOException If no directory could be created.
	 */
	public static File createTempDir(String prefix)
		throws IOException
	{
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr == null) {
			throw new IOException(
				"System property 'java.io.tmpdir' does not specify a tmp dir");
		}
		
		File tmpDir = new File(tmpDirStr);
		if (!tmpDir.exists()) {
			boolean created = tmpDir.mkdirs();
			if (!created) {
				throw new IOException("Unable to create tmp dir " + tmpDir);
			}
		}
		
		File resultDir = null;
		int suffix = (int)System.currentTimeMillis();
		int failureCount = 0;
		do {
			resultDir = new File(tmpDir, prefix + suffix % 10000);
			suffix++;
			failureCount++;
		}
		while (resultDir.exists() && failureCount < 50);
		
		if (resultDir.exists()) {
			throw new IOException(failureCount + 
				" attempts to generate a non-existent directory name failed, giving up");
		}
		boolean created = resultDir.mkdir();
		if (!created) {
			throw new IOException("Failed to create tmp directory");
		}
		
		return resultDir;
	}

	/**
	 * Deletes the specified diretory and any files and directories in it
	 * recursively.
	 * 
	 * @param dir The directory to remove.
	 * @throws IOException If the directory could not be removed.
	 */
	public static void deleteDir(File dir)
		throws IOException
	{
		if (!dir.isDirectory()) {
			throw new IOException("Not a directory " + dir);
		}
		
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			
			if (file.isDirectory()) {
				deleteDir(file);
			}
			else {
				boolean deleted = file.delete();
				if (!deleted) {
					throw new IOException("Unable to delete file" + file);
				}
			}
		}
		
		dir.delete();
	}
}
