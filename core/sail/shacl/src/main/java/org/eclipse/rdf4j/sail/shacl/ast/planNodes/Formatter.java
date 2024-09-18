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

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;

public class Formatter {

	public static String prefix(Value in) {

		if (in == null) {
			return "null";
		}

		if (in instanceof IRI) {

			String namespace = ((IRI) in).getNamespace();

			List<Namespace> namespaces = List.of(
					RDF.NS,
					SHACL.NS,
					FOAF.NS,
					DCAT.NS,
					new SimpleNamespace("http://example.com/ns#", "ex"),
					XSD.NS
			);

			for (Namespace ns : namespaces) {
				if (namespace.equals(ns.getName())) {
					return ns.getPrefix() + ":" + ((IRI) in).getLocalName();
				}
			}

		}

		return in.toString();

	}

	public static String formatSparqlQuery(String query) {
		StringBuilder stringBuilder = new StringBuilder();
		query = query.replace(" .", " .\n");
		query = query.replace("\n\n", "\n");
		String[] split = query.split("\n");
		int indent = 0;
		for (String s : split) {
			s = s.trim();
			if (s.startsWith("}")) {
				indent--;
			}
			for (int i = 0; i < indent; i++) {
				stringBuilder.append("\t");
			}
			stringBuilder.append(s).append("\n");
			if (s.endsWith("{")) {
				indent++;
			}
		}
		return stringBuilder.toString().trim();
	}

}
