/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.Namespace;

/**
 * Provides a map of namespace URI to prefix for all well-known vocabulary classes in
 * {@code org.eclipse.rdf4j.model.vocabulary}.
 */
public final class VocabularyNamespaceMap {

	/**
	 * Map of namespace URI to prefix for all vocabulary classes. For example,
	 * {@code "http://www.w3.org/1999/02/22-rdf-syntax-ns#"} maps to {@code "rdf"}.
	 */
	public static final Map<String, String> NAMESPACE_TO_PREFIX;

	static {
		Map<String, String> map = new LinkedHashMap<>();
		add(map, RDF.NS);
		add(map, RDFS.NS);
		add(map, OWL.NS);
		add(map, XSD.NS);
		add(map, FOAF.NS);
		add(map, SKOS.NS);
		add(map, SKOSXL.NS);
		add(map, DC.NS);
		add(map, DCTERMS.NS);
		add(map, DCAT.NS);
		add(map, DOAP.NS);
		add(map, EARL.NS);
		add(map, FN.NS);
		add(map, GEO.NS);
		add(map, GEOF.NS);
		add(map, HYDRA.NS);
		add(map, LDP.NS);
		add(map, LIST.NS);
		add(map, LOCN.NS);
		add(map, ODRL2.NS);
		add(map, ORG.NS);
		add(map, PROV.NS);
		add(map, RDF4J.NS);
		add(map, ROV.NS);
		add(map, RSX.NS);
		add(map, SD.NS);
		add(map, SESAME.NS);
		add(map, SHACL.NS);
		add(map, SP.NS);
		add(map, SPIF.NS);
		add(map, SPIN.NS);
		add(map, SPINX.NS);
		add(map, SPL.NS);
		add(map, APF.NS);
		add(map, AFN.NS);
		add(map, TIME.NS);
		add(map, VANN.NS);
		add(map, VCARD4.NS);
		add(map, VOID.NS);
		add(map, WGS84.NS);
		add(map, DASH.NS);
		add(map, CONFIG.NS);
		NAMESPACE_TO_PREFIX = Collections.unmodifiableMap(map);
	}

	private static void add(Map<String, String> map, Namespace ns) {
		map.put(ns.getName(), ns.getPrefix());
	}

	private VocabularyNamespaceMap() {
	}
}
