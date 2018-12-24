/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.rio.RioSetting;
import org.junit.After;
import org.junit.Test;

public class StringRioSettingTest {
	
	private static final String TEST_KEY = "org.eclipse.rio.string_rio_setting_test";

	private static final String TEST_DESCRIPTION = "test rio setting";
	
	@After
	public void resetEnvVar() {
		System.clearProperty(TEST_KEY);
	}
	
	@Test
	public void testDefaultValueNoSystemProp()
		throws Exception
	{
		RioSetting<String> subject = new StringRioSetting(TEST_KEY, TEST_DESCRIPTION, "foo bar");
		assertThat(subject.getDefaultValue()).isEqualTo("foo bar");
	}
	
	@Test
	public void testOverridingSystemProp()
		throws Exception
	{
		System.setProperty(TEST_KEY, "something else");
		RioSetting<String> subject = new StringRioSetting(TEST_KEY, TEST_DESCRIPTION, "foo bar");
		assertThat(subject.getDefaultValue()).isEqualTo("something else");
	}

}
