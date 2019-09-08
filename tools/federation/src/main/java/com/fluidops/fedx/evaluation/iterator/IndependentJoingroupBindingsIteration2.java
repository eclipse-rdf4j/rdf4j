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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Inserts original bindings into the result.
 * 
 * @author Andreas Schwarte
 */
public class IndependentJoingroupBindingsIteration2 extends LookAheadIteration<BindingSet, QueryEvaluationException>{

	// a pattern matcher for the binding resolver, pattern: myVar_%outerID%#bindingId, e.g. name_0#0
	protected static final Pattern pattern = Pattern.compile("(.*)_(.*)_(.*)");	
	
	protected final List<BindingSet> bindings;
	protected final CloseableIteration<BindingSet, QueryEvaluationException> iter;
	protected ArrayList<BindingSet> result = null;
	protected int currentIdx = 0;
	
	public IndependentJoingroupBindingsIteration2(CloseableIteration<BindingSet, QueryEvaluationException> iter, List<BindingSet> bindings) {
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
		
		List<BindingInfo> a_res = new ArrayList<BindingInfo>();
		List<BindingInfo> b_res = new ArrayList<BindingInfo>();
		
		// collect results XXX later asynchronously
		// assumes that bindingset of iteration has exactly one binding
		while (iter.hasNext()) {
			
			BindingSet bIn = iter.next();
			
			if (bIn.size()!=1)
				throw new RuntimeException("For this optimization a bindingset needs to have exactly one binding, it has " + bIn.size() + ": " + bIn);

			Binding b = bIn.getBinding( bIn.getBindingNames().iterator().next() );
			
			// name is something like myVar_%outerID%_bindingId, e.g. name_0_0
			Matcher m = pattern.matcher(b.getName());
			if (!m.find())
				throw new QueryEvaluationException("Unexpected pattern for binding name: " + b.getName());
			
			BindingInfo bInfo = new BindingInfo(m.group(1), Integer.parseInt(m.group(3)), b.getValue());
			int bIndex = Integer.parseInt(m.group(2));
			
//			int tmp = b.getName().indexOf("_");
//			String pattern = b.getName().substring(tmp+1);
//			String split[] = pattern.split("_");
//			
//			int bIndex = Integer.parseInt(split[0]);
//			int bindingsIdx = Integer.parseInt(split[1]);
//			BindingInfo bInfo = new BindingInfo(b.getName().substring(0, tmp), bindingsIdx, b.getValue());
			
			// add a new binding info to the correct result list
			if (bIndex==0) {
				a_res.add(bInfo);
			}
			else if (bIndex==1)
				b_res.add(bInfo);
			else
				throw new RuntimeException("Unexpected binding value.");
		}
		
		ArrayList<BindingSet> res = new ArrayList<BindingSet>(a_res.size() * b_res.size());
		
		for (BindingInfo a : a_res) {
			for (BindingInfo b : b_res) {
				if (a.bindingsIdx!=b.bindingsIdx)
					continue;
				QueryBindingSet newB = new QueryBindingSet(bindings.size() + 2);
				newB.addBinding(a.name, a.value);
				newB.addBinding(b.name, b.value);
				newB.addAll(bindings.get(a.bindingsIdx));
				res.add(newB);
			}
		}
		
		return res;
	}
	
	
	protected class BindingInfo {
		public final String name;
		public final int bindingsIdx;
		public final Value value;
		public BindingInfo(String name, int bindingsIdx, Value value) {
			super();
			this.name = name;
			this.bindingsIdx = bindingsIdx;
			this.value = value;
		}	
		public String toString() {
			return name + ":" + value.stringValue();
		}
	}

}
