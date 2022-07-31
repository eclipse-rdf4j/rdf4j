/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;
import org.junit.Ignore;
import org.junit.Test;

public class StringRioSettingTest extends RioSettingTest<String> {

	@Test
	@Override
	@Ignore
	public void testConvertIllegal() throws Exception {
	}

	@Override
	protected String getDefaultValue() {
		return "default value";
	}

	@Override
	protected String getLegalStringValue() {
		return "foo";
	}

	@Override
	protected String getConvertedStringValue() {
		return "foo";
	}

	@Override
	protected String getIllegalStringValue() {
		throw new UnsupportedOperationException("no illegal value exists for string-type conversion");
	}

	@Override
	protected RioSetting<String> createRioSetting(String key, String description, String defaultValue) {
		return new StringRioSetting(key, description, defaultValue);
	}

}
