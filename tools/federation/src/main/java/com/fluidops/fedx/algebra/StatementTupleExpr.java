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

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;


/**
 * Interface for any expression that can be evaluated
 * 
 * @author Andreas Schwarte
 *
 * @see StatementSourcePattern
 * @see ExclusiveStatement
 * @see ExclusiveGroup
 */
public interface StatementTupleExpr extends TupleExpr, QueryRef {

	/**
	 * @return
	 * 		the id of this expr
	 */
	public String getId();
	
	/**
	 * @return
	 * 		a list of free (i.e. unbound) variables in this expression
	 */
	public List<String> getFreeVars();
	
	/**
	 * @return
	 * 		the number of free (i.e. unbound) variables in this expression
	 */
	public int getFreeVarCount();
	
	/**
	 * @return
	 * 		a list of sources that are relevant for evaluation of this expression
	 */
	public List<StatementSource> getStatementSources();
	
	/**
	 * returns true iff this statement has free variables in the presence
	 * of the specified binding set
	 * 
	 * @param binding
	 * @return whether the statement has free vars
	 */
	public boolean hasFreeVarsFor(BindingSet binding);
	
	/**
	 * Evaluate this expression using the provided bindings
	 * 
	 * @param bindings
	 * @return
	 * 			the result iteration
	 * 
	 * @throws QueryEvaluationException
	 */
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) throws QueryEvaluationException; 

}
