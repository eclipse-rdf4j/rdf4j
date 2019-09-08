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

public class StatisticsImpl implements Statistics {

	@Override
	public int estimatedResults(Statement stmt, StatementSource source) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasResults(Statement stmt, StatementSource source) {
		return false;
	}

	@Override
	public double selectivity(StatementPattern stmt) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double selectivity(ExclusiveGroup group) {
		// TODO Auto-generated method stub
		return 0;
	}

}
