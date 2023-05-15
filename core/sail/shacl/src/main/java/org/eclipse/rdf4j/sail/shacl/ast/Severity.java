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
package org.eclipse.rdf4j.sail.shacl.ast;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Severity {

	Info(SHACL.INFO),
	Warning(SHACL.WARNING),
	Violation(SHACL.VIOLATION);

	private static final Logger logger = LoggerFactory.getLogger(Severity.class);

	private final Value iri;

	Severity(Value iri) {
		this.iri = iri;
	}

	public static Severity fromIri(IRI iri) {
		if (iri == null) {
			return null;
		} else if (iri == SHACL.VIOLATION) {
			return Severity.Violation;
		} else if (iri == SHACL.WARNING) {
			return Severity.Warning;
		} else if (iri == SHACL.INFO) {
			return Severity.Info;
		} else if (Severity.Info.iri.equals(iri)) {
			return Severity.Info;
		} else if (Severity.Warning.iri.equals(iri)) {
			return Severity.Warning;
		} else if (Severity.Violation.iri.equals(iri)) {
			return Severity.Violation;
		}

		logger.warn("Unknown sh:severity: <{}>", iri);

		return null;
	}

	public static Severity orDefault(Severity severity) {
		if (severity == null) {
			return Severity.Violation;
		}
		return severity;
	}

	public Value getIri() {
		return iri;
	}

	static {
		// If we add more severity levels then we need to remember to update all the places where we assume that the
		// severity can only be Info, Warning or Violation.
		assert Severity.values().length == 3;
	}
}
