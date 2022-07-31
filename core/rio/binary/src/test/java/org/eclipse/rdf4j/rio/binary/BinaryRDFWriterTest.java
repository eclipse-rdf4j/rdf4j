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
package org.eclipse.rdf4j.rio.binary;

import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BinaryRDFWriterSettings;

/**
 * @author Arjohn Kampman
 */
public class BinaryRDFWriterTest extends RDFWriterTest {

	public BinaryRDFWriterTest() {
		super(new BinaryRDFWriterFactory(), new BinaryRDFParserFactory());
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting[] {
				BinaryRDFWriterSettings.VERSION,
				BinaryRDFWriterSettings.BUFFER_SIZE,
				BinaryRDFWriterSettings.CHARSET,
				BinaryRDFWriterSettings.RECYCLE_IDS
		};
	}
}
