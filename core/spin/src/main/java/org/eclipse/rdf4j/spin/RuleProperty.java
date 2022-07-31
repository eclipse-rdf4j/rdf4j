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

import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;

public class RuleProperty {

	private final IRI IRI;

	private List<IRI> nextRules = Collections.emptyList();

	private int maxIterCount = -1;

	public RuleProperty(IRI ruleUri) {
		this.IRI = ruleUri;
	}

	public IRI getUri() {
		return IRI;
	}

	public List<IRI> getNextRules() {
		return nextRules;
	}

	public void setNextRules(List<IRI> nextRules) {
		this.nextRules = nextRules;
	}

	public int getMaxIterationCount() {
		return maxIterCount;
	}

	public void setMaxIterationCount(int maxIterCount) {
		this.maxIterCount = maxIterCount;
	}
}
