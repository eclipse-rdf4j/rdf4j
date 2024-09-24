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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.AFN;
import org.eclipse.rdf4j.model.vocabulary.APF;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.DOAP;
import org.eclipse.rdf4j.model.vocabulary.EARL;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.LIST;
import org.eclipse.rdf4j.model.vocabulary.LOCN;
import org.eclipse.rdf4j.model.vocabulary.ODRL2;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.ROV;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SD;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.SESAMEQNAME;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.model.vocabulary.SPINX;
import org.eclipse.rdf4j.model.vocabulary.SPL;
import org.eclipse.rdf4j.model.vocabulary.TIME;
import org.eclipse.rdf4j.model.vocabulary.VANN;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.WGS84;
import org.eclipse.rdf4j.model.vocabulary.XSD;

public class Formatter {

	public static String prefix(Set<? extends Value> set) {
		if (set.size() == 1) {
			return prefix(set.iterator().next());
		}

		return Arrays.toString(set.stream().map(Formatter::prefix).toArray());
	}

	public static String prefix(List<? extends Value> list) {
		if (list.size() == 1) {
			return prefix(list.iterator().next());
		}

		return Arrays.toString(list.stream().map(Formatter::prefix).toArray());
	}

	public static String prefix(Value in) {

		if (in == null) {
			return "null";
		}

		if (in instanceof IRI) {

			String namespace = ((IRI) in).getNamespace();

			List<Namespace> namespaces = List.of(
					AFN.NS,
					APF.NS,
					CONFIG.NS,
					DASH.NS,
					DC.NS,
					DCAT.NS,
					DCTERMS.NS,
					DOAP.NS,
					EARL.NS,
					FN.NS,
					FOAF.NS,
					GEO.NS,
					GEOF.NS,
					HYDRA.NS,
					LDP.NS,
					LIST.NS,
					LOCN.NS,
					ODRL2.NS,
					ORG.NS,
					OWL.NS,
					PROV.NS,
					RDF.NS,
					RDF4J.NS,
					RDFS.NS,
					ROV.NS,
					RSX.NS,
					SD.NS,
					SESAME.NS,
					SESAMEQNAME.NS,
					SHACL.NS,
					SKOS.NS,
					SKOSXL.NS,
					SP.NS,
					SPIF.NS,
					SPIN.NS,
					SPINX.NS,
					SPL.NS,
					TIME.NS,
					VANN.NS,
					VCARD4.NS,
					VOID.NS,
					WGS84.NS,
					XSD.NS,
					new SimpleNamespace("http://example.com/ns#", "ex"),
					new SimpleNamespace("http://example.com/ns/", "ex")
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
