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

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;

class ComplexQueryFragment {
	private final String targetVarPrefix;
	private final StatementMatcher.Variable value;
	private final String query;
	private final StatementMatcher.Variable targetVar;

	public ComplexQueryFragment(String query, String targetVarPrefix, StatementMatcher.Variable targetVar,
			StatementMatcher.Variable value) {
		this.query = query;
		this.targetVarPrefix = targetVarPrefix;
		this.targetVar = targetVar;
		this.value = value;
	}

	public String getTargetVarPrefix() {
		return targetVarPrefix;
	}

	public StatementMatcher.Variable getValue() {
		return value;
	}

	public String getQuery() {
		return query;
	}

	public StatementMatcher.Variable getTargetVar() {
		return targetVar;
	}
}
