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
package org.eclipse.rdf4j.rio;

/**
 * A container object for easy setting and passing of {@link RDFWriter} configuration options.
 *
 * @author Jeen Broekstra
 * @author Peter Ansell
 */
public class WriterConfig extends RioConfig {

	/**
	 */
	private static final long serialVersionUID = 270L;

	/**
	 * Creates a ParserConfig object starting with default settings.
	 */
	public WriterConfig() {
		super();
	}

	@Override
	public WriterConfig useDefaults() {
		super.useDefaults();
		return this;
	}

	@Override
	public <T extends Object> WriterConfig set(RioSetting<T> setting, T value) {
		super.set(setting, value);
		return this;
	}
}
