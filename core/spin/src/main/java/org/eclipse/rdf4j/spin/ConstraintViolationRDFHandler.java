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
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

public class ConstraintViolationRDFHandler extends AbstractRDFHandler {

	private boolean hasStatements;

	private String label;

	private String root;

	private String path;

	private String value;

	private ConstraintViolationLevel level;

	private ConstraintViolation violation;

	public ConstraintViolation getConstraintViolation() {
		return violation;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		hasStatements = false;
		label = null;
		root = null;
		path = null;
		value = null;
		level = ConstraintViolationLevel.ERROR;
		violation = null;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		hasStatements = true;
		IRI pred = st.getPredicate();
		if (RDFS.LABEL.equals(pred)) {
			Value labelValue = st.getObject();
			label = (labelValue instanceof Literal) ? labelValue.stringValue() : null;
		} else if (SPIN.VIOLATION_ROOT_PROPERTY.equals(pred)) {
			Value rootValue = st.getObject();
			root = (rootValue instanceof Resource) ? rootValue.stringValue() : null;
		} else if (SPIN.VIOLATION_PATH_PROPERTY.equals(pred)) {
			Value pathValue = st.getObject();
			path = (pathValue != null) ? pathValue.stringValue() : null;
		} else if (SPIN.VIOLATION_VALUE_PROPERTY.equals(pred)) {
			Value valueValue = st.getObject();
			value = (valueValue != null) ? valueValue.stringValue() : null;
		} else if (SPIN.VIOLATION_LEVEL_PROPERTY.equals(pred)) {
			Value levelValue = st.getObject();
			if (levelValue instanceof IRI) {
				level = ConstraintViolationLevel.valueOf((IRI) levelValue);
			}
			if (level == null) {
				throw new RDFHandlerException("Invalid value " + levelValue + " for " + SPIN.VIOLATION_LEVEL_PROPERTY
						+ ": " + st.getSubject());
			}
		}
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		if (hasStatements) {
			violation = new ConstraintViolation(label, root, path, value, level);
		}
	}
}
