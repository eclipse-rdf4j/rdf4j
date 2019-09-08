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
