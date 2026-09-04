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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Installs a custom {@link Var.Provider} for the duration of the annotated test class or test method.
 *
 * <p>
 * The specified provider is instantiated via its no-argument constructor, installed before each test, and removed after
 * each test via {@link Var#resetProvider()}. When placed on both a class and a method, the method-level annotation
 * takes precedence.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * &#64;WithVarProvider(MyVarProvider.class)
 * class MyTest {
 *
 * 	&#64;Test
 * 	void test() {
 * 		Var v = Var.of("x"); // produced by MyVarProvider
 * 	}
 * }
 * </pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(VarProviderExtension.class)
public @interface WithVarProvider {

	/**
	 * The {@link Var.Provider} implementation to install. Must have a public no-argument constructor.
	 */
	Class<? extends Var.Provider> value();
}
