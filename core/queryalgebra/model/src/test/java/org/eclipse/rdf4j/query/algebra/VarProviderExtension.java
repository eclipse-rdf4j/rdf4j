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
package org.eclipse.rdf4j.query.algebra;

import java.util.Optional;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that installs a custom {@link Var.Provider} for the duration of each test and resets it afterwards.
 *
 * <p>
 * Activated via {@link WithVarProvider} on the test class or method. The method-level annotation takes precedence over
 * the class-level annotation. The provider is always reset after each test regardless of outcome.
 *
 * @see WithVarProvider
 */
class VarProviderExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Optional<WithVarProvider> annotation = findAnnotation(context);
		if (annotation.isPresent()) {
			Var.Provider provider = annotation.get().value().getDeclaredConstructor().newInstance();
			Var.setProvider(provider);
		}
	}

	@Override
	public void afterEach(ExtensionContext context) {
		Var.resetProvider();
	}

	/**
	 * Returns the {@link WithVarProvider} annotation, preferring the method level over the class level.
	 */
	private Optional<WithVarProvider> findAnnotation(ExtensionContext context) {
		return context.getTestMethod()
				.map(m -> m.getAnnotation(WithVarProvider.class))
				.or(() -> context.getTestClass().map(c -> c.getAnnotation(WithVarProvider.class)));
	}
}
