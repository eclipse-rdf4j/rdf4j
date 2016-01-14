/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;

/**
 * A negated property set is a SPARQL construction of the form {?X !(uri|^uri)
 * ?Y}. This class is a temporary representation used by the parser. It is
 * converted by the TupleExprBuilder into a set of joins and filters on regular
 * statement patterns.
 * 
 * @author Jeen
 */
public class NegatedPropertySet {

	private Scope scope;

	private Var subjectVar;

	private List<ValueExpr> objectList;

	private Var contextVar;

	private List<PropertySetElem> propertySetElems = new ArrayList<PropertySetElem>();

	/**
	 * @param scope
	 *        The scope to set.
	 */
	public void setScope(Scope scope) {
		this.scope = scope;
	}

	/**
	 * @return Returns the scope.
	 */
	public Scope getScope() {
		return scope;
	}

	/**
	 * @param subjectVar
	 *        The subjectVar to set.
	 */
	public void setSubjectVar(Var subjectVar) {
		this.subjectVar = subjectVar;
	}

	/**
	 * @return Returns the subjectVar.
	 */
	public Var getSubjectVar() {
		return subjectVar;
	}

	/**
	 * @param objectList
	 *        The objectList to set.
	 */
	public void setObjectList(List<ValueExpr> objectList) {
		this.objectList = objectList;
	}

	/**
	 * @return Returns the objectList.
	 */
	public List<ValueExpr> getObjectList() {
		return objectList;
	}

	/**
	 * @param contextVar
	 *        The contextVar to set.
	 */
	public void setContextVar(Var contextVar) {
		this.contextVar = contextVar;
	}

	/**
	 * @return Returns the contextVar.
	 */
	public Var getContextVar() {
		return contextVar;
	}

	/**
	 * @param propertySetElems
	 *        The propertySetElems to set.
	 */
	public void setPropertySetElems(List<PropertySetElem> propertySetElems) {
		this.propertySetElems = propertySetElems;
	}

	/**
	 * @return Returns the propertySetElems.
	 */
	public List<PropertySetElem> getPropertySetElems() {
		return propertySetElems;
	}

	/**
	 * @param elem
	 */
	public void addPropertySetElem(PropertySetElem elem) {
		propertySetElems.add(elem);

	}

}
