/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class Formatter {

	public static String prefix(Value in) {

		if (in == null) {
			return "null";
		}

		if (in instanceof IRI) {

			String namespace = ((IRI) in).getNamespace();

			if (namespace.equals(RDF.NAMESPACE)) {
				return in.toString().replace(RDF.NAMESPACE, RDF.PREFIX + ":");
			}
			if (namespace.equals(RDFS.NAMESPACE)) {
				return in.toString().replace(RDFS.NAMESPACE, RDFS.PREFIX + ":");
			}
			if (namespace.equals(SHACL.NAMESPACE)) {
				return in.toString().replace(SHACL.NAMESPACE, SHACL.PREFIX + ":");
			}
			if (namespace.equals("http://example.com/ns#")) {
				return in.toString().replace("http://example.com/ns#", "ex:");
			}
			if (namespace.equals("http://www.w3.org/2001/XMLSchema#")) {
				return in.toString().replace("http://www.w3.org/2001/XMLSchema#", "xsd:");
			}

		}

		return in.toString();

	}

}
