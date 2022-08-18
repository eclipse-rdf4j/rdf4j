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
package org.elasticsearch.bootstrap;

import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

/**
 * test of elasticsearch pass. Thus as a workaround we deactivate this test. see
 * https://stackoverflow.com/questions/38712251/java-jar-hell-runtime-exception
 */
public class JarHell {

	private JarHell() {
	}

	public static void checkJarHell() throws Exception {
	}

	public static void checkJarHell(URL urls[]) throws Exception {
	}

	public static void checkVersionFormat(String targetVersion) {
	}

	public static void checkJavaVersion(String resource, String targetVersion) {
	}

	public static Set<URL> parseClassPath() {
		return Collections.emptySet();
	}

	public static void checkJarHell(Consumer o) {
	}
}
