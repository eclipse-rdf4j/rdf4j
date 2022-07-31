/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Either supplies a statement matching the given pattern, or {@link Optional#empty()} otherwise.
 *
 * @author Peter Ansell
 */
@FunctionalInterface
public interface GetStatementOptional {

	/**
	 * Either supplies a statement matching the given pattern, or {@link Optional#empty()} otherwise.
	 *
	 * @param subject   A {@link Resource} to be used to match to statements.
	 * @param predicate An {@link IRI} to be used to match to statements.
	 * @param object    A {@link Value} to be used to match to statements.
	 * @param contexts  An array of context {@link Resource} objects, or left out (not null) to select from all
	 *                  contexts.
	 * @return An {@link Optional} either containing a single statement matching the pattern or {@link Optional#empty()}
	 *         otherwise.
	 */
	Optional<Statement> get(Resource subject, IRI predicate, Value object, Resource... contexts);

}
