/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.structures;

import java.io.Serializable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

public class SubQuery implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8968907794785828994L;
	
	protected String subj = null;
	protected String pred = null;
	protected String obj = null;
	
	public SubQuery(String subj, String pred, String obj) {
		super();
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
	}
	
	public SubQuery(Resource subj, IRI pred, Value obj)
	{
		super();
		if (subj!=null)
			this.subj = subj.stringValue();
		if (pred!=null)
			this.pred = pred.stringValue();
		if (obj!=null)
			this.obj = obj.toString();	
		// we need to take toString() here since stringValue for literals does not contain the datatype
	}

	public SubQuery(StatementPattern stmt) {
		super();
		
		if (stmt.getSubjectVar().hasValue())
			subj = stmt.getSubjectVar().getValue().stringValue();
		if (stmt.getPredicateVar().hasValue())
			pred = stmt.getPredicateVar().getValue().stringValue();
		if (stmt.getObjectVar().hasValue())
			obj = stmt.getObjectVar().getValue().stringValue();
	}	
	
	@Override
	public int hashCode() {
		final int prime1 = 961;
		final int prime2 = 31;
		final int prime3 = 1;
		int result = 1;
		result += ((subj == null) ? 0 : subj.hashCode() * prime1);
		result += ((pred == null) ? 0 : pred.hashCode() * prime2);
		result += ((obj == null) ? 0 : obj.hashCode() * prime3);		
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubQuery other = (SubQuery) obj;
		if (this.obj == null) {
			if (other.obj != null)
				return false;
		} else if (!this.obj.equals(other.obj))
			return false;
		if (pred == null) {
			if (other.pred != null)
				return false;
		} else if (!pred.equals(other.pred))
			return false;
		if (subj == null) {
			if (other.subj != null)
				return false;
		} else if (!subj.equals(other.subj))
			return false;
		return true;
	}
	
	
}
