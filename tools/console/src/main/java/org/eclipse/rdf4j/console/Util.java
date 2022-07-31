/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

/**
 * Helper class
 *
 * @author Bart Hanssens
 */
public class Util {
	/**
	 * Get context IRI from string representation
	 *
	 * @param repository repository
	 * @param ctxID      context as string
	 * @return context IRI
	 */
	public static Resource getContext(Repository repository, String ctxID) {
		if (ctxID.equalsIgnoreCase("null")) {
			return null;
		}
		if (ctxID.startsWith("_:")) {
			return repository.getValueFactory().createBNode(ctxID.substring(2));
		}
		return repository.getValueFactory().createIRI(ctxID);
	}

	/**
	 * Get context IRIs from a series of tokens, starting from (zero-based) position within the series.
	 *
	 * @param tokens     command as series of tokens
	 * @param pos        position to start from
	 * @param repository repository
	 * @return array of contexts or null for default context
	 * @throws IllegalArgumentException
	 */
	public static Resource[] getContexts(String[] tokens, int pos, Repository repository)
			throws IllegalArgumentException {
		Resource[] contexts = new Resource[] {};

		if (tokens.length > pos) {
			contexts = new Resource[tokens.length - pos];
			for (int i = pos; i < tokens.length; i++) {
				contexts[i - pos] = getContext(repository, tokens[i]);
			}
		}
		return contexts;
	}

	/**
	 * Get path from file or URI
	 *
	 * @param file file name
	 * @return path or null
	 */
	@Deprecated
	public static Path getPath(String file) {
		Path path = null;
		try {
			path = Paths.get(file);
		} catch (InvalidPathException ipe) {
			try {
				path = Paths.get(new URI(file));
			} catch (URISyntaxException ex) {
				//
			}
		}
		return path;
	}

	/**
	 * Check if a string looks like a HTTP, HTTPS or file URI.
	 *
	 * @param str string
	 * @return true if
	 */
	public static boolean isHttpOrFile(String str) {
		String lower = str.toLowerCase();
		return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://");
	}

	/**
	 * Get path from file string if it's absolute, or from working directory if the file is relative.
	 *
	 * @param workDir working dir
	 * @param file    file name
	 * @return path normalized path
	 */
	public static Path getNormalizedPath(Path workDir, String file) {
		Path path = Paths.get(file);
		if (!path.isAbsolute() && (workDir != null)) {
			path = workDir.resolve(file);
		}
		return path.normalize();
	}

	/**
	 * Get string representation for a value. If the value is an IRI and is part of a known namespace, the prefix will
	 * be used to shorten it.
	 *
	 * @param value      value
	 * @param namespaces mapping (uri,prefix)
	 * @return string representation
	 */
	public static String getPrefixedValue(Value value, Map<String, String> namespaces) {
		if (value == null) {
			return null;
		}
		if (namespaces.isEmpty()) {
			return NTriplesUtil.toNTriplesString(value);
		}
		if (value instanceof IRI) {
			IRI uri = (IRI) value;
			String prefix = namespaces.get(uri.getNamespace());
			if (prefix != null) {
				return prefix + ":" + uri.getLocalName();
			}
		}
		if (value instanceof Literal) {
			Literal lit = (Literal) value;
			IRI uri = lit.getDatatype();
			String prefix = namespaces.get(uri.getNamespace());
			if (prefix != null) {
				return "\"" + lit.getLabel() + "\"^^" + prefix + ":" + uri.getLocalName();
			}
		}
		if (value instanceof Triple) {
			return "<<" + getPrefixedValue(((Triple) value).getSubject(), namespaces) + " "
					+ getPrefixedValue(((Triple) value).getPredicate(), namespaces) + " "
					+ getPrefixedValue(((Triple) value).getObject(), namespaces) + ">>";
		}
		return NTriplesUtil.toNTriplesString(value);
	}

	/**
	 * Format a string of values, starting new line(s) when the joined values exceed the width. Primarily used for
	 * displaying formatted help (e.g namespaces, config files) to the console. To be replaced by a commons text method
	 *
	 * @param width     maximum column width
	 * @param padding   left padding
	 * @param str       joined string
	 * @param separator value separator
	 * @return list of values as a formatted string, or empty
	 */
	@Deprecated
	public static String formatToWidth(int width, String padding, String str, String separator) {
		if (str.isEmpty()) {
			return "";
		}

		int padLen = padding.length();
		int strLen = str.length();
		int sepLen = separator.length();

		if (strLen + padLen <= width) {
			return padding + str;
		}

		String[] values = str.split(separator);
		StringBuilder builder = new StringBuilder(strLen + 4 * padLen + 8 * sepLen);

		int colpos = width; // force start on new line

		for (String value : values) {
			int len = value.length();
			if (colpos + sepLen + len <= width) {
				builder.append(separator);
			} else {
				builder.append("\n").append(padding);
				colpos = padLen;
			}
			builder.append(value);
			colpos += len;
		}
		// don't return initial newline
		return builder.substring(1);
	}
}
