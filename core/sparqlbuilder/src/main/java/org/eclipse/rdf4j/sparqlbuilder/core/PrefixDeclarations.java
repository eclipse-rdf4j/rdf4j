/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

/**
 * A collection of SPARQL Prefix declarations
 * 
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#prefNames"> SPARQL Prefix</a>
 */
public class PrefixDeclarations extends StandardQueryElementCollection<Prefix> {
	/**
	 * Add prefix declarations to this collection
	 * 
	 * @param prefixes
	 * @return this
	 */
	public PrefixDeclarations addPrefix(Prefix... prefixes) {
		addElements(prefixes);

		return this;
	}
}