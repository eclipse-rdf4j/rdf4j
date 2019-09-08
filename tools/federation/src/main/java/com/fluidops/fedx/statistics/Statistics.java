/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx.statistics;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

import com.fluidops.fedx.algebra.ExclusiveGroup;
import com.fluidops.fedx.algebra.StatementSource;

public interface Statistics {

	
	public double selectivity(StatementPattern stmt);
	
	public double selectivity(ExclusiveGroup group);
	
	public boolean hasResults(Statement stmt, StatementSource source);

	public int estimatedResults(Statement stmt, StatementSource source);
	
	
	
	
}
