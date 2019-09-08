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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Inserts original bindings into the result.
 * 
 * @author Andreas Schwarte
 */
public class IndependentJoingroupBindingsIteration extends LookAheadIteration<BindingSet, QueryEvaluationException>{

	protected final BindingSet bindings;
	protected final CloseableIteration<BindingSet, QueryEvaluationException> iter;
	protected ArrayList<BindingSet> result = null;
	protected int currentIdx = 0;
	
	public IndependentJoingroupBindingsIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter, BindingSet bindings) {
		this.bindings = bindings;
		this.iter = iter;
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		
		if (result==null) {
			result = computeResult();
		}
		
		if (currentIdx>=result.size())
			return null;
		
		return result.get(currentIdx++);
	}

	
	protected ArrayList<BindingSet> computeResult() throws QueryEvaluationException {
		
		List<Binding> a_res = new ArrayList<Binding>();
		List<Binding> b_res = new ArrayList<Binding>();
		
		// collect results XXX later asynchronously
		// assumes that bindingset of iteration has exactly one binding
		while (iter.hasNext()) {
			
			BindingSet bIn = iter.next();
			
			if (bIn.size()!=1)
				throw new RuntimeException("For this optimization a bindingset needs to have exactly one binding, it has " + bIn.size() + ": " + bIn);

			Binding b = bIn.getBinding( bIn.getBindingNames().iterator().next() );
			int bIndex = Integer.parseInt(b.getName().substring(b.getName().lastIndexOf("_")+1));
			
			if (bIndex==0)
				a_res.add(b);
			else if (bIndex==1)
				b_res.add(b);
			else
				throw new RuntimeException("Unexpected binding value.");
		}
		
		ArrayList<BindingSet> res = new ArrayList<BindingSet>(a_res.size() * b_res.size());
		
		for (Binding a : a_res) {
			for (Binding b : b_res) {
				QueryBindingSet newB = new QueryBindingSet(bindings.size() + 2);
				newB.addAll(bindings);
				newB.addBinding(a.getName().substring(0, a.getName().lastIndexOf("_")), a.getValue());
				newB.addBinding(b.getName().substring(0, b.getName().lastIndexOf("_")), b.getValue());
				res.add(newB);
			}
		}
		
		return res;
	}

}
