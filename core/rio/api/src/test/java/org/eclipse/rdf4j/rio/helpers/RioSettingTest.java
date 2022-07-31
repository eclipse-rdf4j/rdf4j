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
package org.eclipse.rdf4j.rio.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.rdf4j.rio.RioSetting;
import org.junit.Before;
import org.junit.Test;

public abstract class RioSettingTest<T> {

	protected static final String TEST_KEY = "org.eclipse.rio.long_rio_setting_test";

	protected static final String TEST_DESCRIPTION = "test rio setting";

	/**
	 * The test subject
	 */
	protected RioSetting<T> subject;

	@Before
	public void setUp() throws Exception {
		subject = createRioSetting(TEST_KEY, TEST_DESCRIPTION, getDefaultValue());
	}

	@Test
	public void testDefaultValue() throws Exception {
		assertThat(subject.getDefaultValue()).isEqualTo(getDefaultValue());
	}

	@Test
	public void testConvert() throws Exception {
		assertThat(subject.convert(getLegalStringValue())).isEqualTo(getConvertedStringValue());
	}

	@Test
	public void testConvertIllegal() throws Exception {
		assertThatThrownBy(() -> subject.convert(getIllegalStringValue()))
				.isInstanceOf(RioConfigurationException.class);
	}

	/**
	 * a (legal) default value for the type T
	 *
	 * @return a single legal default value.
	 */
	protected abstract T getDefaultValue();

	/**
	 * a legal string-represention of a setting value
	 *
	 * @return a legal string-representation of a setting value.
	 */
	protected abstract String getLegalStringValue();

	/**
	 * the value of type T that corresponds to the value returned by {@link #getLegalStringValue()}. NB implementors
	 * should return a hardcoded value, not doing on-the-fly conversion.
	 *
	 * @return a value of type T corresponding to the the value returned by {@link #getLegalStringValue()}
	 */
	protected abstract T getConvertedStringValue();

	/**
	 * an illegal string-representation of a setting value.
	 *
	 * @return an illegal string value;
	 */
	protected abstract String getIllegalStringValue();

	/**
	 * Create a new {@link RioSetting} for use as the test subject.
	 *
	 * @param key          the setting key
	 * @param description  the setting description
	 * @param defaultValue the default value
	 * @return a new {@link RioSetting} object
	 */
	protected abstract RioSetting<T> createRioSetting(String key, String description, T defaultValue);

}
