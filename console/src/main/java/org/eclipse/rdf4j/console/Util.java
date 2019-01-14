/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

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
	 * @param ctxID context as string
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
	 * @param tokens command as series of tokens
	 * @param pos position to start from
	 * @param repository repository
	 * @return array of contexts or null for default context
	 * @throws IllegalArgumentException
	 */
	public static Resource[] getContexts(String[] tokens, int pos, Repository repository)
									throws IllegalArgumentException {	
		Resource[] contexts = new Resource[]{};

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
	 * Get string representation for a value.
	 * If the value is an IRI and is part of a known namespace, the prefix will be used to shorten it.
	 * 
	 * @param value value
	 * @param namespaces mapping (uri,prefix)
	 * @return string representation
	 */
	public static String getPrefixedValue(Value value, Map<String,String> namespaces) {
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
		return NTriplesUtil.toNTriplesString(value);
	}
	
	/**
	 * Join an array of values + separator, starting new line(s) when the joined values exceed the width. 
	 * Primarily used for displaying formatted help (e.g namespaces, config files) to the console.
	 * 
	 * @param width max column width
	 * @param padLen number of leading spaces on each new line 
	 * @param padFirst also pad the first line
	 * @param values array of values
	 * @param sep value separator
	 * @return list of values as a formatted string, or empty
	 */
	public static String joinFormatted(int width, int padLen, boolean padFirst, String[] values, String sep) {
		if (values.length == 0) {
			return "";
		}
	
		char[] spaces = new char[padLen];
		Arrays.fill(spaces, ' ');
		String padding = new String(spaces);
		
		StringBuilder buf = new StringBuilder();
		if (padFirst) {
			buf.append(padding);
		}
		
		int pos = buf.length();
		int sepLen = sep.length();
		
		for(String value: values) {
			int valLen = value.length();

			// too large, start new padded line
			if (pos + valLen > width) {
				buf.append("\n").append(padding);
				pos = padLen;
			}
			buf.append(value);
			pos += valLen;
			
			// don't add a separator if that would exceed the width
			if (pos + sepLen <= width) {
				buf.append(sep);
			}
			pos += sepLen;
		}
		
		String s = buf.toString();
		return s.endsWith(sep) ? s.substring(0, s.length() - sepLen) : s;
	}
}
