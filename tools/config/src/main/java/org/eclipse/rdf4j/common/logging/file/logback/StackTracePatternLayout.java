/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.logging.file.logback;

import java.util.regex.Pattern;

import ch.qos.logback.classic.PatternLayout;

/**
 * PatternLayout that also prints stack traces.
 *
 * @author Herko ter Horst
 */
public class StackTracePatternLayout extends PatternLayout {

	static final String DEFAULT_CONVERSION_PATTERN = "[%-5p] %d{ISO8601} [%t] %m%n%ex";

	public static final Pattern DEFAULT_PARSER_PATTERN = Pattern
			.compile("\\[([^\\]]*)\\] ([^\\[]*)\\[([^\\]]*)\\] (.*)");

	/**
	 * Construct a StacktracePatternLayout with the default conversion pattern.
	 */
	public StackTracePatternLayout() {
		this(DEFAULT_CONVERSION_PATTERN);
	}

	/**
	 * Construct a StacktracePatternLayout with the specified conversion pattern.
	 *
	 * @param conversionPattern the conversion pattern to use
	 * @see <a href="https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html"> Information on
	 *      Log4J conversion patterns.</a>
	 */
	public StackTracePatternLayout(String conversionPattern) {
		super();
		this.setPattern(conversionPattern);
	}
}
