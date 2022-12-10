/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.transaction;

/**
 * The Query Evaluation Mode determines the behaviour of the SPARQL query engine on RDF4J repositories. It currently
 * supports two modes, {@link #STRICT} and and {@link #STANDARD}.
 *
 * @author Jeen Broekstra
 */
public enum QueryEvaluationMode implements TransactionSetting {
	/**
	 * Strict minimally-compliant mode for SPARQL 1.1. In this mode, no operators have been extended (as described in
	 * <a href="https://www.w3.org/TR/sparql11-query/#operatorExtensibility">section 17.3.1 of the SPARQL 1.1 Query
	 * Recommendation</a>).
	 */
	STRICT,

	/**
	 * Standard mode extends minimal compliance with various practical operator behavioral extensions, in a way that is
	 * still compliant with the SPARQL 1.1 specification (as described in
	 * <a href="https://www.w3.org/TR/sparql11-query/#operatorExtensibility">section 17.3.1 of the SPARQL 1.1 Query
	 * Recommendation</a>).
	 */
	STANDARD

}
