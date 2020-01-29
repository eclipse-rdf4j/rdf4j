/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.URIUtil;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.ODRL2;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.ROV;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.TIME;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

/**
 * Namespace prefix setting
 * 
 * @author Bart Hanssens
 */
public class Prefixes extends ConsoleSetting<Set<Namespace>> {
	public final static String NAME = "prefixes";

	public final static Set<Namespace> DEFAULT = new HashSet<>();
	static {
		DEFAULT.add(DCAT.NS);
		DEFAULT.add(DCTERMS.NS);
		DEFAULT.add(FOAF.NS);
		DEFAULT.add(ODRL2.NS);
		DEFAULT.add(ORG.NS);
		DEFAULT.add(OWL.NS);
		DEFAULT.add(PROV.NS);
		DEFAULT.add(RDF.NS);
		DEFAULT.add(RDFS.NS);
		DEFAULT.add(ROV.NS);
		DEFAULT.add(SKOS.NS);
		DEFAULT.add(TIME.NS);
		DEFAULT.add(VCARD4.NS);
		DEFAULT.add(VOID.NS);
		DEFAULT.add(XMLSchema.NS);
	}

	@Override
	public String getHelpLong() {
		return "set prefixes=<default>         Set the prefixes to a default list of prefixes\n"
				+ "    prefixes=<none>            Remove all namespace prefixes\n"
				+ "	prefixes=prefix ns-url     Set prefix for namespace\n"
				+ "	prefixes=prefix <none>     Remove namespace prefix\n";
	}

	/**
	 * Constructor
	 * 
	 * Default set of namespaces are well-known ones
	 */
	public Prefixes() {
		super(new HashSet<>(DEFAULT));
	}

	/**
	 * Constructor
	 * 
	 * @param initValue
	 */
	public Prefixes(Set<Namespace> initValue) {
		super(initValue);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void clear() {
		get().clear();
	}

	/**
	 * Remove the namespace with specified prefix
	 * 
	 * @param prefix
	 */
	private void clearNamespace(String prefix) {
		get().removeIf(ns -> ns.getPrefix().equals(prefix));
	}

	@Override
	public String getAsString() {
		return get().stream().map(ns -> {
			return ns.getPrefix() + " " + ns.getName();
		}).sorted().collect(Collectors.joining(","));
	}

	/**
	 * Set a namespace from a string, using one whitespace to separate prefix and namespace URI E.g. 'dcterms
	 * http://purl.org/dc/terms/'
	 * 
	 * @param namespace
	 */
	private void setNamespace(String namespace) {
		String[] parts = namespace.split(" ");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Error parsing namespace: " + namespace);
		}
		if (parts[1].equals("<none>")) {
			clearNamespace(parts[0]);
			return;
		}
		if (!URIUtil.isValidURIReference(parts[1])) {
			throw new IllegalArgumentException("Error parsing namespace URI: " + parts[1]);
		}
		get().add(new SimpleNamespace(parts[0], parts[1]));
	}

	@Override
	public void setFromString(String value) throws IllegalArgumentException {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("Empty or null namespace value");
		}
		if (value.equals("<none>")) {
			clear();
			return;
		}
		if (value.equals("<default>")) {
			set(new HashSet<>(DEFAULT));
			return;
		}

		String[] namespaces = value.split(",");
		for (String namespace : namespaces) {
			setNamespace(namespace);
		}
	}
}
