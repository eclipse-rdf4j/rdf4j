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

import org.eclipse.rdf4j.model.Value;

/**
 * Expressions implementing this interface can apply some {@link FilterValueExpr}
 * during evaluation.
 * 
 * @author Andreas Schwarte
 * 
 * @see StatementSourcePattern
 * @see ExclusiveStatement
 * @see ExclusiveGroup
 * 
 */
public interface FilterTuple {

	/**
	 * @return
	 * 			true if this expression has a filter to apply
	 */
	public boolean hasFilter();
	
	/**
	 * register a new filter expression. If the expr has already a filter registered, the
	 * new expression is added to a {@link ConjunctiveFilterExpr}.
	 * 
	 * @param expr
	 */
	public void addFilterExpr(FilterExpr expr);
	
	
	/**
	 * register a filter that can be directly expressed as a binding, e.g.
	 * 
	 * SELECT * WHERE {
	 *  ?s p o .
	 *  FILTER (?s = X)
	 * }
	 * 
	 * is equivalent to 
	 * 
	 * SELECT * WHERE {
	 * 	X p o .
	 * }
	 * 
	 * @param varName
	 * @param value
	 */
	public void addBoundFilter(String varName, Value value);
	
	
	/**
	 * 
	 * @return
	 * 		the currently registered filter expressions, usually of type {@link FilterExpr}
	 * 		or {@link ConjunctiveFilterExpr}
	 */
	public FilterValueExpr getFilterExpr();
	
	/**
	 * @return
	 * 			the free variables of this expression
	 */
	public List<String> getFreeVars();
}
