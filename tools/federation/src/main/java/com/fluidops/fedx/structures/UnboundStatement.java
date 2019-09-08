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
package com.fluidops.fedx.structures;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public class UnboundStatement implements Statement {

	private static final long serialVersionUID = 2612189412333330052L;

	
	protected final Resource subj;
	protected final IRI pred;
	protected final Value obj;
	
		
	public UnboundStatement(Resource subj, IRI pred, Value obj)
	{
		super();
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
	}

	@Override
	public Resource getContext() {
		return null;
	}

	@Override
	public Value getObject() {
		return obj;
	}

	@Override
	public IRI getPredicate()
	{
		return pred;
	}

	@Override
	public Resource getSubject() {
		return subj;
	}

}
