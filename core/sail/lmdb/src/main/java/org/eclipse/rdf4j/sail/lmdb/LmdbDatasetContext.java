/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.util.Optional;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;

/**
 * Exposes LMDB-specific resources from a
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext}.
 */
@InternalUseOnly
public interface LmdbDatasetContext {

	Optional<LmdbEvaluationDataset> getLmdbDataset();

	Optional<ValueStore> getValueStore();
}
