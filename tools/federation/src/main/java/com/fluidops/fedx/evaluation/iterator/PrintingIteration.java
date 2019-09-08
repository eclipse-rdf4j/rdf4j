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

import java.util.LinkedList;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;


/**
 * Print the bindings of the inner iteration to stdout, however maintain a copy, which
 * is accessible through this iteration.
 * 
 * @author Andreas Schwarte
 *
 */
public class PrintingIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException>
{

	protected final CloseableIteration<BindingSet, QueryEvaluationException> inner;
	protected LinkedList<BindingSet> copyQueue = new LinkedList<BindingSet>();
	protected boolean done = false;
	
	public PrintingIteration(
			CloseableIteration<BindingSet, QueryEvaluationException> inner) {
		super();
		this.inner = inner;
	}


	public void print() throws QueryEvaluationException {
		int count = 0;
		while (inner.hasNext()) {
			BindingSet item = inner.next();
			System.out.println(item);
			count++;
			synchronized (copyQueue) {
				copyQueue.addLast(item);
			}
		}
		done = true;
		System.out.println("Done with inner queue. Processed " + count + " items.");
	}
	
	
	
	
	@Override	
	public boolean hasNext() throws QueryEvaluationException {
		return !done || copyQueue.size()>0;
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		synchronized (copyQueue) {
			return copyQueue.removeFirst();
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	protected void handleClose() throws QueryEvaluationException {
		inner.close();
		done=true;
		synchronized (copyQueue){
			copyQueue.clear();
		}
	}
}
