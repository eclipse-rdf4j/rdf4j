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
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;

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
		if (value instanceof IRI) {
			IRI uri = (IRI) value;
			String prefix = namespaces.get(uri.getNamespace());
			if (prefix != null) {
				return prefix + ":" + uri.getLocalName();
			}
		}
		return NTriplesUtil.toNTriplesString(value);
	}
}
