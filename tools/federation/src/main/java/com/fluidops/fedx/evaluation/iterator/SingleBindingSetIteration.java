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

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Represents an iteration that contains only a single binding set.
 * 
 * @author Andreas Schwarte
 *
 */
public class SingleBindingSetIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException>
{

	protected final BindingSet res;
	protected boolean hasNext = true;
	
	
	public SingleBindingSetIteration(BindingSet res) {
		super();
		this.res = res;
	}

	
	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public BindingSet next() {
		hasNext = false;
		return res;
	}

	@Override
	public void remove() {
		// no-op		
	}
	
	@Override
	protected void handleClose() throws QueryEvaluationException {
		hasNext=false;
	}
}
