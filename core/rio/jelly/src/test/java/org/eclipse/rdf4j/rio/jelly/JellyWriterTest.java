/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jelly;

import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.RioSetting;

import eu.neverblink.jelly.convert.rdf4j.rio.JellyParserFactory;
import eu.neverblink.jelly.convert.rdf4j.rio.JellyWriterFactory;
import eu.neverblink.jelly.convert.rdf4j.rio.JellyWriterSettings;

/**
 * @author Piotr Sowiński
 */
public class JellyWriterTest extends RDFWriterTest {

	public JellyWriterTest() {
		super(new JellyWriterFactory(), new JellyParserFactory());
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting[] {
				JellyWriterSettings.PROTO_VERSION,
				JellyWriterSettings.STREAM_NAME,
				JellyWriterSettings.PHYSICAL_TYPE,
				JellyWriterSettings.ALLOW_RDF_STAR,
				JellyWriterSettings.LOGICAL_TYPE,
				JellyWriterSettings.MAX_NAME_TABLE_SIZE,
				JellyWriterSettings.MAX_PREFIX_TABLE_SIZE,
				JellyWriterSettings.MAX_DATATYPE_TABLE_SIZE,
				JellyWriterSettings.FRAME_SIZE,
				JellyWriterSettings.ENABLE_NAMESPACE_DECLARATIONS,
				JellyWriterSettings.DELIMITED_OUTPUT,
				JellyWriterSettings.RDF_VERSION,
		};
	}
}
