/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * Expressions implementing this interface can apply some {@link FilterValueExpr} during evaluation.
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
	 * @return true if this expression has a filter to apply
	 */
	public boolean hasFilter();

	/**
	 * register a new filter expression. If the expr has already a filter registered, the new expression is added to a
	 * {@link ConjunctiveFilterExpr}.
	 * 
	 * @param expr
	 */
	public void addFilterExpr(FilterExpr expr);

	/**
	 * register a filter that can be directly expressed as a binding, e.g.
	 * 
	 * SELECT * WHERE { ?s p o . FILTER (?s = X) }
	 * 
	 * is equivalent to
	 * 
	 * SELECT * WHERE { X p o . }
	 * 
	 * @param varName
	 * @param value
	 */
	public void addBoundFilter(String varName, Value value);

	/**
	 * 
	 * @return the currently registered filter expressions, usually of type {@link FilterExpr} or
	 *         {@link ConjunctiveFilterExpr}
	 */
	public FilterValueExpr getFilterExpr();

	/**
	 * @return the free variables of this expression
	 */
	public List<String> getFreeVars();

	/**
	 * Returns bound filter bindings, that need to be added as additional bindings to the final result
	 * 
	 * @return the bound filters, or <code>null</code>
	 */
	public BindingSet getBoundFilters();
}
