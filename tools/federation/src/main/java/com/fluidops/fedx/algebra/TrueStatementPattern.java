/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.algebra;

import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * A TrueStatementPattern represents a stmt with no free variables which in addition is
 * available at one of the provided sources.
 * 
 * @author Andreas Schwarte
 *
 */
public class TrueStatementPattern extends StatementPattern implements BoundJoinTupleExpr {

	private static final long serialVersionUID = -5389629235610754092L;

	public TrueStatementPattern(StatementPattern node) {
		super(node.getSubjectVar(), node.getPredicateVar(), node.getObjectVar(), node.getContextVar());
	}
}
