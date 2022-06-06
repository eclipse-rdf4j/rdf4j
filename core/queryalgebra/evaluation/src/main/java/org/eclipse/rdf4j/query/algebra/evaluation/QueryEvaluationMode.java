/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.common.transaction.TransactionSetting;

/**
 * @author Jeen Broekstra
 */
public enum QueryEvaluationMode implements TransactionSetting {
	/**
	 * Strict minimally-compliant mode with respect to SPARQL 1.1 recommendation.
	 */
	MINIMAL_COMPLIANT_11,

	/**
	 * Standard mode extends minimal compliance with various practical operator behavioral extensions.
	 */
	STANDARD

}
