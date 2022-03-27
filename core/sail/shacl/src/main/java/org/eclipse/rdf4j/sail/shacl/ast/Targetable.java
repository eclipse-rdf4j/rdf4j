/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.stream.Stream;

import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public interface Targetable {

	Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner);

	String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider);

}
