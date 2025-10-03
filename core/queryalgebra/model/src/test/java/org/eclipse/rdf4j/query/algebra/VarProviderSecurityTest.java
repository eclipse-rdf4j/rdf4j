/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.PropertyPermission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

public class VarProviderSecurityTest {

	static class DenyPropertyReadsSecurityManager extends SecurityManager {
		@Override
		public void checkPermission(Permission perm) {
			if (perm instanceof PropertyPermission && perm.getActions().contains("read")) {
				throw new SecurityException("Denied property read: " + perm.getName());
			}
		}

		@Override
		public void checkPermission(Permission perm, Object context) {
			checkPermission(perm);
		}
	}

	@Test
	@EnabledForJreRange(max = JRE.JAVA_16)
	void providerLookupDoesNotFailWhenPropertyReadDenied() throws Exception {
		SecurityManager original = System.getSecurityManager();
		try {
			System.setSecurityManager(new DenyPropertyReadsSecurityManager());

			// Load Var class without initializing
			ClassLoader cl = this.getClass().getClassLoader();
			Class<?> varClass = Class.forName("org.eclipse.rdf4j.query.algebra.Var", false, cl);

			// Defer initialization until invocation of a factory method
			Method of = varClass.getMethod("of", String.class);

			assertThatCode(() -> of.invoke(null, "x")).doesNotThrowAnyException();
		} finally {
			System.setSecurityManager(original);
		}
	}

	@Test
	void providerLookupWorksNormallyWithoutSecurityManager() throws Exception {
		// This test exercises the same path without a SecurityManager present (JDK >= 17),
		// ensuring Var.of does not throw during provider initialization in the common case.
		Class<?> varClass = Class.forName("org.eclipse.rdf4j.query.algebra.Var", false,
				this.getClass().getClassLoader());
		Method of = varClass.getMethod("of", String.class);
		assertThatCode(() -> of.invoke(null, "y")).doesNotThrowAnyException();
	}
}
