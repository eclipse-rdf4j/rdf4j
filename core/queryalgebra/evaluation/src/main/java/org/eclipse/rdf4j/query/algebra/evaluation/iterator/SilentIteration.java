/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Wrap an inner iteration and suppress exceptions silently
 * 
 * @author Andreas Schwarte
 */
public class SilentIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {
	
	protected CloseableIteration<BindingSet, QueryEvaluationException> iter;
	
	public SilentIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter) {
		super();
		this.iter = iter;
	}
	
	
	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		
		try {
			if (iter.hasNext())
				return iter.next();
		} catch (Exception e) {
			// suppress
		}
		
		return null;
	}

}
