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
package org.eclipse.rdf4j.sail.shacl;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that prints a line to stdout when a test starts (and ends). This helps identify which test is
 * currently running if the suite hangs.
 */
public class TestStartLoggerExtension
		implements BeforeEachCallback, BeforeTestExecutionCallback, TestWatcher {

	private static final Logger logger = LoggerFactory.getLogger(TestStartLoggerExtension.class);

	private static void print(String phase, ExtensionContext context) {
		String cls = context.getTestClass().map(Class::getName).orElse("<unknown-class>");
		String method = context.getTestMethod().map(m -> m.getName()).orElse("<unknown-method>");
		String display = context.getDisplayName();
		System.out.println("[TEST] " + phase + ": " + cls + "#" + method + " (" + display + ")");
		System.out.flush();
//		context.publishReportEntry("[TEST] " + phase + ": " + cls + "#" + method + " (" + display + ")");
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		// Earliest per-test hook before any user @BeforeEach methods
		print("BeforeEach", context);
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) {
		// Immediately before the test method executes
		print("Start", context);
	}

	@Override
	public void testSuccessful(ExtensionContext context) {
		print("Success", context);
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		print("Failed: " + cause.getClass().getSimpleName() + ": " + cause.getMessage(), context);
	}

	@Override
	public void testAborted(ExtensionContext context, Throwable cause) {
		print("Aborted: " + (cause == null ? "" : cause.getClass().getSimpleName()), context);
	}
}
