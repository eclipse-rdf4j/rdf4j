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

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.sail.SailException;

public class EmptyNode implements PlanNode {

	private boolean printed = false;

	static final private EmptyNode instance = new EmptyNode();

	private EmptyNode() {
	}

	public static EmptyNode getInstance() {
		return instance;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new EmptyIteration<>();
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId())
				.append(" [label=\"")
				.append(StringEscapeUtils.escapeJava(this.toString()))
				.append("\"];")
				.append("\n");
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "Empty";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}
}
