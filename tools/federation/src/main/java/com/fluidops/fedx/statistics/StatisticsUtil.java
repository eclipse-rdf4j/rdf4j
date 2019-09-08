/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx.statistics;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;

import com.fluidops.fedx.structures.UnboundStatement;

public class StatisticsUtil {

	
	public static Statement toStatement(StatementPattern stmt, BindingSet bindings) {
		
		Value subj = toValue(stmt.getSubjectVar(), bindings);
		Value pred = toValue(stmt.getPredicateVar(), bindings);
		Value obj = toValue(stmt.getObjectVar(), bindings);
		
		return new UnboundStatement((Resource) subj, (IRI) pred, obj);
	}
	
	
	
	protected static Value toValue(Var var, BindingSet bindings) {
		if (var.hasValue())
			return var.getValue();
		
		if (bindings.hasBinding(var.getName()))	
			return bindings.getValue(var.getName());
		
		return null;			
	}
}
