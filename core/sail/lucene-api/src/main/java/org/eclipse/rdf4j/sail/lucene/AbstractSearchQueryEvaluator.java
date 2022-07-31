/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;

public abstract class AbstractSearchQueryEvaluator implements SearchQueryEvaluator {

	@Override
	public void replaceQueryPatternsWithResults(final BindingSetAssignment bsa) {
		final QueryModelNode placeholder = removeQueryPatterns();
		if (bsa != null && bsa.getBindingSets() != null && bsa.getBindingSets().iterator().hasNext()) {
			placeholder.replaceWith(bsa);
		} else {
			placeholder.replaceWith(new EmptySet());
		}
	}

}
