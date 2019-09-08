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

import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

import com.fluidops.fedx.evaluation.SparqlFederationEvalStrategyWithValues;
import com.fluidops.fedx.util.QueryStringUtil;

/**
 * Inserts original bindings into the result. This implementation is used for
 * bound joins with VALUES clauses, see {@link SparqlFederationEvalStrategyWithValues}.
 * 
 * It is assumed the the query results contain a binding for "?__index" which corresponds
 * to the index in the input mappings. See {@link QueryStringUtil} for details
 * 
 * @author Andreas Schwarte
 * @see SparqlFederationEvalStrategyWithValues
 * @since 3.0
 */
public class BoundJoinVALUESConversionIteration extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException>{

	/**
	 * The binding name for the index
	 */
	public static final String INDEX_BINDING_NAME = "__index";
	
	protected final List<BindingSet> bindings;
	
	public BoundJoinVALUESConversionIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter, List<BindingSet> bindings) {
		super(iter);
		this.bindings = bindings;
	}

	@Override
	protected BindingSet convert(BindingSet bIn) throws QueryEvaluationException {
		QueryBindingSet res = new QueryBindingSet();
		int bIndex = Integer.parseInt(bIn.getBinding(INDEX_BINDING_NAME).getValue().stringValue());
		Iterator<Binding> bIter = bIn.iterator();
		while (bIter.hasNext()) {
			Binding b = bIter.next();
			if (b.getName().equals(INDEX_BINDING_NAME))
				continue;
			res.addBinding(b);
		}
		for (Binding bs : bindings.get(bIndex))
			res.setBinding(bs);
		return res;
	}
}
