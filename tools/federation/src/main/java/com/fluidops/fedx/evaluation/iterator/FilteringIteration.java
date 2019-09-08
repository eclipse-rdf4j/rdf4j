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
package com.fluidops.fedx.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.algebra.FilterValueExpr;
import com.fluidops.fedx.evaluation.FederationEvalStrategy;

/**
 * Filters iteration according to specified filterExpr.
 * 
 * @author Andreas Schwarte
 */
public class FilteringIteration extends FilterIteration<BindingSet, QueryEvaluationException> {
	
	private static final Logger log = LoggerFactory.getLogger(FilteringIteration.class);
	
	protected FilterValueExpr filterExpr;
	protected FederationEvalStrategy strategy;
	
	public FilteringIteration(FilterValueExpr filterExpr, CloseableIteration<BindingSet, QueryEvaluationException> iter) throws QueryEvaluationException {
		super(iter);
		this.filterExpr = filterExpr;
		this.strategy = FederationManager.getInstance().getStrategy();
	}	
	
	@Override
	protected boolean accept(BindingSet bindings) throws QueryEvaluationException {
		try {
			return strategy.isTrue(filterExpr, bindings);
		} catch (ValueExprEvaluationException e) {
			log.warn("Failed to evaluate filter expr: " + e.getMessage());
			// failed to evaluate condition
			return false;
		}
	}
}
