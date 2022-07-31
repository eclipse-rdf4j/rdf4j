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

public class LongRioSettingTest extends RioSettingTest<Long> {

	@Override
	protected Long getDefaultValue() {
		return 1234L;
	}

	@Override
	protected String getLegalStringValue() {
		return "5678";
	}

	@Override
	protected Long getConvertedStringValue() {
		return 5678L;
	}

	@Override
	protected String getIllegalStringValue() {
		return "not a long";
	}

	@Override
	protected RioSetting<Long> createRioSetting(String key, String description, Long defaultValue) {
		return new LongRioSetting(key, description, defaultValue);
	}

}
