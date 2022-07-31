/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

/**
 * Interface to support evaluation statistics that keep their own internal estimates and need to be notified of added or
 * removed statements.
 */
@Experimental
public interface DynamicStatistics {

	void add(ExtensibleStatement statement);

	void remove(ExtensibleStatement statement);

	void removeByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource... contexts);

	/**
	 *
	 * @return 1 if stale, 0 if not stale, 0.5 if 50% stale. Seen as, given a random statement (that has either been
	 *         added, or removed), what is the probability that the statistics will return an incorrect result?
	 * @param expectedSize
	 */
	double staleness(long expectedSize);

}
