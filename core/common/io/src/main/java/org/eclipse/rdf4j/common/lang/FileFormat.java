/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.lang;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Abstract representation of a file format. File formats are identified by a {@link #getName() name} and can have one
 * or more associated MIME types, zero or more associated file extensions and can specify a (default) character
 * encoding.
 *
 * @author Arjohn Kampman
 */
public class FileFormat {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The file format's name.
	 */
	private final String name;

	/**
	 * The file format's MIME types. The first item in the list is interpreted as the default MIME type for the format.
	 */
	private final List<String> mimeTypes;

	/**
	 * The file format's (default) charset.
	 */
	private final Charset charset;

	/**
	 * The file format's file extensions. The first item in the list is interpreted as the default file extension for
	 * the format.
	 */
	private final List<String> fileExtensions;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new FileFormat object.
	 *
	 * @param name          The name of the file format, e.g. "PLAIN TEXT".
	 * @param mimeType      The (default) MIME type of the file format, e.g. <var>text/plain</var> for plain text files.
	 * @param charset       The default character encoding of the file format. Specify <var>null</var> if not
	 *                      applicable.
	 * @param fileExtension The (default) file extension for the file format, e.g. <var>txt</var> for plain text files.
	 */
	public FileFormat(String name, String mimeType, Charset charset, String fileExtension) {
		this(name, List.of(mimeType), charset, List.of(fileExtension));
	}

	/**
	 * Creates a new FileFormat object.
	 *
	 * @param name           The name of the file format, e.g. "PLAIN TEXT".
	 * @param mimeType       The (default) MIME type of the file format, e.g. <var>text/plain</var> for plain text
	 *                       files.
	 * @param charset        The default character encoding of the file format. Specify <var>null</var> if not
	 *                       applicable.
	 * @param fileExtensions The file format's file extension(s), e.g. <var>txt</var> for plain text files. The first
	 *                       item in the list is interpreted as the default file extension for the format.
	 */
	public FileFormat(String name, String mimeType, Charset charset, Collection<String> fileExtensions) {
		this(name, List.of(mimeType), charset, fileExtensions);
	}

	/**
	 * Creates a new FileFormat object.
	 *
	 * @param name           The name of the file format, e.g. "PLAIN TEXT".
	 * @param mimeTypes      The MIME type(s) of the file format, e.g. <var>text/plain</var> for theplain text files.
	 *                       The first item in the list is interpreted as the default MIME type for the format. The
	 *                       supplied list should contain at least one MIME type.
	 * @param charset        The default character encoding of the file format. Specify <var>null</var> if not
	 *                       applicable.
	 * @param fileExtensions The file format's file extension(s), e.g. <var>txt</var> for plain text files. The first
	 *                       item in the list is interpreted as the default file extension for the format.
	 */
	public FileFormat(String name, Collection<String> mimeTypes, Charset charset, Collection<String> fileExtensions) {
		assert name != null : "name must not be null";
		assert mimeTypes != null : "mimeTypes must not be null";
		assert !mimeTypes.isEmpty() : "mimeTypes must not be empty";
		assert fileExtensions != null : "fileExtensions must not be null";

		this.name = name;
		this.mimeTypes = new ArrayList<>(mimeTypes);
		this.charset = charset;
		this.fileExtensions = new ArrayList<>(fileExtensions);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the name of this file format.
	 *
	 * @return A human-readable format name, e.g. "PLAIN TEXT".
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the default MIME type for this file format.
	 *
	 * @return A MIME type string, e.g. "text/plain".
	 */
	public String getDefaultMIMEType() {
		return mimeTypes.get(0);
	}

	/**
	 * Checks if the specified MIME type matches the FileFormat's default MIME type. The MIME types are compared
	 * ignoring upper/lower-case differences.
	 *
	 * @param mimeType The MIME type to compare to the FileFormat's default MIME type.
	 * @return <var>true</var> if the specified MIME type matches the FileFormat's default MIME type.
	 */
	public boolean hasDefaultMIMEType(String mimeType) {
		return getDefaultMIMEType().equalsIgnoreCase(mimeType);
	}

	/**
	 * Gets the file format's MIME types.
	 *
	 * @return An unmodifiable list of MIME type strings, e.g. "text/plain".
	 */
	public List<String> getMIMETypes() {
		return Collections.unmodifiableList(mimeTypes);
	}

	/**
	 * Checks if specified MIME type matches one of the FileFormat's MIME types. The MIME types are compared ignoring
	 * upper/lower-case differences.
	 *
	 * @param mimeType The MIME type to compare to the FileFormat's MIME types.
	 * @return <var>true</var> if the specified MIME type matches one of the FileFormat's MIME types.
	 */
	public boolean hasMIMEType(String mimeType) {
		if (mimeType == null) {
			return false;
		}
		String type = mimeType;
		if (mimeType.indexOf(';') > 0) {
			type = mimeType.substring(0, mimeType.indexOf(';'));
		}
		for (String mt : this.mimeTypes) {
			if (mt.equalsIgnoreCase(mimeType)) {
				return true;
			}
			if (mimeType != type && mt.equalsIgnoreCase(type)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets the default file name extension for this file format.
	 *
	 * @return A file name extension (excluding the dot), e.g. "txt", or <var>null</var> if there is no common file
	 *         extension for the format.
	 */
	public String getDefaultFileExtension() {
		if (fileExtensions.isEmpty()) {
			return null;
		} else {
			return fileExtensions.get(0);
		}
	}

	/**
	 * Checks if the specified file name extension matches the FileFormat's default file name extension. The file name
	 * extension MIME types are compared ignoring upper/lower-case differences.
	 *
	 * @param extension The file extension to compare to the FileFormat's file extension.
	 * @return <var>true</var> if the file format has a default file name extension and if it matches the specified
	 *         extension, <var>false</var> otherwise.
	 */
	public boolean hasDefaultFileExtension(String extension) {
		String ext = getDefaultFileExtension();
		return ext != null && ext.equalsIgnoreCase(extension);
	}

	/**
	 * Gets the file format's file extensions.
	 *
	 * @return An unmodifiable list of file extension strings, e.g. "txt".
	 */
	public List<String> getFileExtensions() {
		return Collections.unmodifiableList(fileExtensions);
	}

	/**
	 * Checks if the FileFormat's file extension is equal to the specified file extension. The file extensions are
	 * compared ignoring upper/lower-case differences.
	 *
	 * @param extension The file extension to compare to the FileFormat's file extension.
	 * @return <var>true</var> if the specified file extension is equal to the FileFormat's file extension.
	 */
	public boolean hasFileExtension(String extension) {
		for (String ext : fileExtensions) {
			if (ext.equalsIgnoreCase(extension)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get the (default) charset for this file format.
	 *
	 * @return the (default) charset for this file format, or null if this format does not have a default charset.
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Checks if the FileFormat has a (default) charset.
	 *
	 * @return <var>true</var> if the FileFormat has a (default) charset.
	 */
	public boolean hasCharset() {
		return charset != null;
	}

	/**
	 * Compares FileFormat objects based on their {@link #getName() name}, ignoring case.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof FileFormat) {
			return name.equalsIgnoreCase(((FileFormat) other).name);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return name.toLowerCase(Locale.ENGLISH).hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(name);

		sb.append(" (mimeTypes=");
		for (int i = 0; i < mimeTypes.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(mimeTypes.get(i));
		}

		sb.append("; ext=");
		for (int i = 0; i < fileExtensions.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(fileExtensions.get(i));
		}

		sb.append(")");

		return sb.toString();
	}

	/*----------------*
	 * Static methods *
	 *----------------*/

	/**
	 * Tries to match the specified MIME type with the MIME types of the supplied file formats.
	 *
	 * @param mimeType    A MIME type, e.g. "text/plain".
	 * @param fileFormats The file formats to match the MIME type against.
	 * @return A FileFormat object if the MIME type was recognized, or {@link Optional#empty()} otherwise.
	 */
	public static <FF extends FileFormat> Optional<FF> matchMIMEType(String mimeType, Iterable<FF> fileFormats) {
		// First try to match with the default MIME type
		for (FF fileFormat : fileFormats) {
			if (fileFormat.hasDefaultMIMEType(mimeType)) {
				return Optional.of(fileFormat);
			}
		}

		// Try alternative MIME types too
		for (FF fileFormat : fileFormats) {
			if (fileFormat.hasMIMEType(mimeType)) {
				return Optional.of(fileFormat);
			}
		}

		return Optional.empty();
	}

	/**
	 * Tries to match the specified file name with the file extensions of the supplied file formats.
	 *
	 * @param fileName    A file name.
	 * @param fileFormats The file formats to match the file name extension against.
	 * @return A FileFormat that matches the file name extension, or {@link Optional#empty()} otherwise.
	 */
	public static <FF extends FileFormat> Optional<FF> matchFileName(String fileName, Iterable<FF> fileFormats) {
		// Strip any directory info from the file name
		int lastPathSepIdx = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		if (lastPathSepIdx >= 0) {
			fileName = fileName.substring(lastPathSepIdx + 1);
		}

		int dotIdx;
		while ((dotIdx = fileName.lastIndexOf('.')) >= 0) {
			String ext = fileName.substring(dotIdx + 1);

			// First try to match with the default file extension of the formats
			for (FF fileFormat : fileFormats) {
				if (fileFormat.hasDefaultFileExtension(ext)) {
					return Optional.of(fileFormat);
				}
			}

			// Try alternative file extensions too
			for (FF fileFormat : fileFormats) {
				if (fileFormat.hasFileExtension(ext)) {
					return Optional.of(fileFormat);
				}
			}

			// No match, check if the file name has more "extensions" (e.g.
			// example.rdf.gz)
			fileName = fileName.substring(0, dotIdx);
		}

		return Optional.empty();
	}
}
