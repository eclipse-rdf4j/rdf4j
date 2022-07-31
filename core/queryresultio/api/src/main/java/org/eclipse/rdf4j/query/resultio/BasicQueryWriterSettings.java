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
package org.eclipse.rdf4j.query.resultio;

import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.RioSettingImpl;

/**
 * {@link RioSetting} constants to use with {@link QueryResultWriter}s.
 *
 * @author Peter Ansell
 */
public class BasicQueryWriterSettings {

	/**
	 * Specifies whether the writer should add the proprietary "http://www.openrdf.org/schema/qname#qname" annotations
	 * to output.
	 * <p>
	 * Defaults to false.
	 */
	public final static RioSetting<Boolean> ADD_SESAME_QNAME = new RioSettingImpl<>(
			"org.eclipse.rdf4j.query.resultio.addsesameqname", "Add Sesame QName", false);

	/**
	 * Specifies a callback function name for wrapping JSON results to support the JSONP cross-origin request
	 * methodology.
	 * <p>
	 * Defaults to "sesamecallback".
	 */
	public static final RioSetting<String> JSONP_CALLBACK = new RioSettingImpl<>(
			"org.eclipse.rdf4j.query.resultio.jsonpcallback", "JSONP callback function", "sesamecallback");

	/**
	 * Private default constructor
	 */
	private BasicQueryWriterSettings() {
	}

}
