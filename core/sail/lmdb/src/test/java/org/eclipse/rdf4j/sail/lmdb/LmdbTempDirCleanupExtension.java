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
package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.io.TempDir;

public final class LmdbTempDirCleanupExtension implements InvocationInterceptor, AfterEachCallback, AfterAllCallback {

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
			.create(LmdbTempDirCleanupExtension.class);
	private static final String TEMP_DIRS = "tempDirs";

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		recordAnnotatedArguments(invocationContext, extensionContext);
		invocation.proceed();
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		recordAnnotatedArguments(invocationContext, extensionContext);
		invocation.proceed();
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		recordAnnotatedArguments(invocationContext, extensionContext);
		invocation.proceed();
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		recordAnnotatedArguments(invocationContext, extensionContext);
		invocation.proceed();
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		recordAnnotatedArguments(invocationContext, extensionContext);
		invocation.proceed();
	}

	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		recordAnnotatedArguments(invocationContext, extensionContext);
		invocation.proceed();
	}

	@Override
	public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		recordAnnotatedArguments(invocationContext, extensionContext);
		return invocation.proceed();
	}

	@Override
	public void afterEach(ExtensionContext context) {
		context.getTestInstance()
				.ifPresent(instance -> recordAnnotatedFields(instance, instance.getClass(), false, context));
		deleteRecordedTempDirs(context);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		context.getTestClass()
				.ifPresent(testClass -> recordAnnotatedFields(null, testClass, true, context));
		deleteRecordedTempDirs(context);
	}

	private static void recordAnnotatedArguments(ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) {
		Parameter[] parameters = invocationContext.getExecutable().getParameters();
		List<Object> arguments = invocationContext.getArguments();
		for (int i = 0; i < parameters.length && i < arguments.size(); i++) {
			if (parameters[i].isAnnotationPresent(TempDir.class)) {
				recordTempDir(arguments.get(i), extensionContext);
			}
		}
	}

	private static void recordAnnotatedFields(Object instance, Class<?> testClass, boolean staticOnly,
			ExtensionContext context) {
		Class<?> current = testClass;
		while (current != null && current != Object.class) {
			for (Field field : current.getDeclaredFields()) {
				if (!field.isAnnotationPresent(TempDir.class)
						|| Modifier.isStatic(field.getModifiers()) != staticOnly) {
					continue;
				}
				try {
					field.setAccessible(true);
					recordTempDir(field.get(staticOnly ? null : instance), context);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Failed to inspect LMDB @TempDir field " + field, e);
				}
			}
			current = current.getSuperclass();
		}
	}

	private static void recordTempDir(Object tempDir, ExtensionContext context) {
		if (tempDir instanceof Path) {
			tempDirs(context).add(((Path) tempDir).toAbsolutePath().normalize());
		} else if (tempDir instanceof File) {
			tempDirs(context).add(((File) tempDir).toPath().toAbsolutePath().normalize());
		}
	}

	@SuppressWarnings("unchecked")
	private static Set<Path> tempDirs(ExtensionContext context) {
		ExtensionContext.Store store = context.getStore(NAMESPACE);
		Set<Path> tempDirs = (Set<Path>) store.get(TEMP_DIRS);
		if (tempDirs == null) {
			tempDirs = new LinkedHashSet<>();
			store.put(TEMP_DIRS, tempDirs);
		}
		return tempDirs;
	}

	private static void deleteRecordedTempDirs(ExtensionContext context) {
		Set<Path> recorded = tempDirs(context);
		List<Path> dirs = new ArrayList<>(recorded);
		recorded.clear();
		dirs.sort(Comparator.comparingInt(Path::getNameCount).reversed());

		RuntimeException failure = null;
		for (Path dir : dirs) {
			try {
				LmdbTestUtil.deleteDir(dir);
			} catch (RuntimeException e) {
				if (failure == null) {
					failure = e;
				} else {
					failure.addSuppressed(e);
				}
			}
		}
		if (failure != null) {
			throw failure;
		}
	}
}
