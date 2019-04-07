/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;

public class PropertyPaths {
	/**
	 * Construct property paths for use with the {@link SparqlBuilder}
	 * 
	 * <p>
	 * <b>Example:</b> {@code subject.has(path(zeroOrMore(property)), object)}.
	 * <p>
	 * 
	 * @param aElements the path elements
	 * @return a property path
	 */
	public static RdfPredicate path(RdfPredicate... aElements) {
		return () -> {
			StringBuilder sb = new StringBuilder();
			for (QueryElement element : aElements) {
				if (sb.length() > 0) {
					sb.append("/");
				}
				sb.append(element.getQueryString());
			}
			return sb.toString();
		};
	}

	public static RdfPredicate zeroOrMore(RdfPredicate aElement) {
		return () -> aElement.getQueryString() + "*";
	}

	public static RdfPredicate oneOrMore(RdfPredicate aElement) {
		return () -> aElement.getQueryString() + "+";
	}
}
