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
package org.eclipse.rdf4j.sail.inferencer.fc.config;

import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

/**
 * @author Arjohn Kampman
 */
@Deprecated
public class ForwardChainingRDFSInferencerConfig extends AbstractDelegatingSailImplConfig {

	public ForwardChainingRDFSInferencerConfig() {
		super(ForwardChainingRDFSInferencerFactory.SAIL_TYPE);
	}

	public ForwardChainingRDFSInferencerConfig(SailImplConfig delegate) {
		super(ForwardChainingRDFSInferencerFactory.SAIL_TYPE, delegate);
	}
}
