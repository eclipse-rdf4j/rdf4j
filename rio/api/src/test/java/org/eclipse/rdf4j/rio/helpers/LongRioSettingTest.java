/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.rio.RioSetting;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LongRioSettingTest {

	private static final String TEST_KEY = "org.eclipse.rio.long_rio_setting_test";

	private static final String TEST_DESCRIPTION = "test rio setting";

	@After
	public void resetEnvVar() {
		System.clearProperty(TEST_KEY);
	}

	@Test
	public void testDefaultValueNoSystemProp()
		throws Exception
	{
		RioSetting<Long> subject = new LongRioSetting(TEST_KEY, TEST_DESCRIPTION, 123456L);
		assertThat(subject.getDefaultValue()).isEqualTo(123456L);
	}

	@Test
	public void testOverridingSystemProp()
		throws Exception
	{
		System.setProperty(TEST_KEY, "5678");
		RioSetting<Long> subject = new LongRioSetting(TEST_KEY, TEST_DESCRIPTION, 123456L);
		assertThat(subject.getDefaultValue()).isEqualTo(5678);
	}

	@Test
	public void testOverridingSystemPropIllegal()
		throws Exception
	{
		System.setProperty(TEST_KEY, "not-a-long");
		try {
			RioSetting<Long> subject = new LongRioSetting(TEST_KEY, TEST_DESCRIPTION, 123456L);
			fail("expected exception for illegal sytem property value");
		}
		catch (NumberFormatException e) {
			// expected
		}
	}

}
