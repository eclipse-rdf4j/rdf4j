/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc.config;

import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;

/**
 * {@link SailImplConfig} for the {@link SchemaCachingRDFSInferencer}
 *
 * @author Jeen Broekstra
 */
public class SchemaCachingRDFSInferencerConfig extends AbstractDelegatingSailImplConfig {

	public SchemaCachingRDFSInferencerConfig() {
		super(SchemaCachingRDFSInferencerFactory.SAIL_TYPE);
	}

	public SchemaCachingRDFSInferencerConfig(SailImplConfig delegate) {
		super(SchemaCachingRDFSInferencerFactory.SAIL_TYPE, delegate);
	}
}
