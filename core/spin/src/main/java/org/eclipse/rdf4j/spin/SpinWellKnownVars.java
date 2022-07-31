/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.spin;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIN;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

final class SpinWellKnownVars {

	private static final ValueFactory valueFactory = SimpleValueFactory.getInstance();

	static final SpinWellKnownVars INSTANCE = new SpinWellKnownVars();

	private final BiMap<String, IRI> stringToUri = HashBiMap.create();

	private final BiMap<IRI, String> uriToString = stringToUri.inverse();

	public SpinWellKnownVars() {
		stringToUri.put("this", SPIN.THIS_CONTEXT_INSTANCE);
		stringToUri.put("arg1", SPIN.ARG1_INSTANCE);
		stringToUri.put("arg2", SPIN.ARG2_INSTANCE);
		stringToUri.put("arg3", SPIN.ARG3_INSTANCE);
		stringToUri.put("arg4", SPIN.ARG4_INSTANCE);
		stringToUri.put("arg5", SPIN.ARG5_INSTANCE);
	}

	public IRI getURI(String name) {
		IRI IRI = stringToUri.get(name);
		if (IRI == null && name.startsWith("arg")) {
			try {
				Integer.parseInt(name.substring("arg".length()));
				IRI = valueFactory.createIRI(SPIN.NAMESPACE, "_" + name);
			} catch (NumberFormatException nfe) {
				// ignore - not a well-known argN variable
			}
		}
		return IRI;
	}

	public String getName(IRI IRI) {
		String name = uriToString.get(IRI);
		if (name == null && SPIN.NAMESPACE.equals(IRI.getNamespace()) && IRI.getLocalName().startsWith("_arg")) {
			String lname = IRI.getLocalName();
			try {
				Integer.parseInt(lname.substring("_arg".length()));
				name = lname.substring(1);
			} catch (NumberFormatException nfe) {
				// ignore - not a well-known argN variable
			}
		}
		return name;
	}
}
