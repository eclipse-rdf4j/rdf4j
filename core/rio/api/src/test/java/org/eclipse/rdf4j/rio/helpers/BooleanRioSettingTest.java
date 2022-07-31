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

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.rio.RioSetting;
import org.junit.Ignore;
import org.junit.Test;

public class BooleanRioSettingTest extends RioSettingTest<Boolean> {

	@Test
	@Override
	@Ignore
	public void testConvertIllegal() throws Exception {
	}

	@Test
	public void testConvertLegalStringVariants() throws Exception {
		assertThat(subject.convert("True")).isTrue();
		assertThat(subject.convert("Foo")).isFalse();
		assertThat(subject.convert("false")).isFalse();
		assertThat(subject.convert("1")).isFalse();
	}

	@Override
	protected Boolean getDefaultValue() {
		return true;
	}

	@Override
	protected String getLegalStringValue() {
		return "TRUE";
	}

	@Override
	protected Boolean getConvertedStringValue() {
		return true;
	}

	@Override
	protected String getIllegalStringValue() {
		throw new UnsupportedOperationException("no illegal value exists for boolean-type conversion");
	}

	@Override
	protected RioSetting<Boolean> createRioSetting(String key, String description, Boolean defaultValue) {
		return new BooleanRioSetting(key, description, defaultValue);
	}

}
