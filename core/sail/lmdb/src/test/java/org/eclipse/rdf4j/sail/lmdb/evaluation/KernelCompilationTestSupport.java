/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.tools.ToolProvider;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.JaninoKernel;

/**
 * Strict compiler harness: the default backend is the actual Janino SimpleCompiler. The explicitly selected
 * {@code javac} backend supports offline structural/semantic tests and is never described as Janino validation. Neither
 * backend can execute the interpreter in place of generated code. Compilation failures retain the source.
 */
public final class KernelCompilationTestSupport {
	public static final String BACKEND_PROPERTY = "rdf4j.test.kernel.compiler";

	private record Key(String name, String source, String backend) {
	}

	private static final Map<Key, Constructor<? extends JaninoKernel>> CACHE = new HashMap<>();
	private static int compilations;

	private KernelCompilationTestSupport() {
	}

	public static String backend() {
		String value = System.getProperty(BACKEND_PROPERTY, "janino");
		if (!value.equals("janino") && !value.equals("javac")) {
			throw new IllegalArgumentException("unknown kernel test compiler: " + value);
		}
		return value;
	}

	public static void verifyBackend() throws Exception {
		if (backend().equals("janino")) {
			try {
				Class<?> compiler = Class.forName("org.codehaus.janino.SimpleCompiler");
				System.out.println("JANINO_VERSION=" + compiler.getPackage().getImplementationVersion());
			} catch (ClassNotFoundException missing) {
				throw new IllegalStateException("Janino is required. Supply janino and commons-compiler JARs via "
						+ "JANINO_CLASSPATH, or explicitly select --javac for a non-Janino offline check.", missing);
			}
		}
	}

	public static synchronized int compilationCount() {
		return compilations;
	}

	public static JaninoKernel compile(LmdbNativeKernelIr.Kernel ir) throws Exception {
		return compile(ir.className(), LmdbNativeKernelEmitter.emit(ir));
	}

	public static synchronized JaninoKernel compile(String name, String source) throws Exception {
		// Compile exactly what the emitter supplies, as the production SimpleCompiler service does.
		// Ordinary emission already applies the optimizer with its own telemetry mode; special terminals do not.
		Key key = new Key(name, source, backend());
		Constructor<? extends JaninoKernel> constructor = CACHE.get(key);
		if (constructor == null) {
			try {
				Class<?> generated;
				if (key.backend.equals("janino")) {
					Class<?> compilerClass = Class.forName("org.codehaus.janino.SimpleCompiler");
					Object compiler = compilerClass.getConstructor().newInstance();
					compilerClass.getMethod("setParentClassLoader", ClassLoader.class)
							.invoke(compiler, JaninoKernel.class.getClassLoader());
					compilerClass.getMethod("cook", String.class).invoke(compiler, source);
					ClassLoader loader = (ClassLoader) compilerClass.getMethod("getClassLoader").invoke(compiler);
					generated = loader.loadClass(name);
				} else {
					Path output = Files.createTempDirectory("kernel-parity-javac-");
					try {
						Path file = output.resolve(name.replace('.', '/') + ".java");
						Files.createDirectories(file.getParent());
						Files.writeString(file, source);
						var compiler = ToolProvider.getSystemJavaCompiler();
						if (compiler == null || compiler.run(null, null, null, "-classpath",
								System.getProperty("java.class.path"), "-d", output.toString(), file.toString()) != 0) {
							throw new AssertionError("javac rejected generated source");
						}
						Map<String, byte[]> classes = new HashMap<>();
						try (var files = Files.walk(output)) {
							for (Path filePath : files.filter(f -> f.toString().endsWith(".class")).toList()) {
								String binary = output.relativize(filePath)
										.toString()
										.replace(java.io.File.separatorChar, '.');
								classes.put(binary.substring(0, binary.length() - 6), Files.readAllBytes(filePath));
							}
						}
						ClassLoader loader = new ClassLoader(JaninoKernel.class.getClassLoader()) {
							@Override
							protected Class<?> findClass(String binary) throws ClassNotFoundException {
								byte[] bytes = classes.get(binary);
								if (bytes == null)
									throw new ClassNotFoundException(binary);
								return defineClass(binary, bytes, 0, bytes.length);
							}
						};
						generated = loader.loadClass(name);
					} finally {
						try (var files = Files.walk(output)) {
							for (Path p : files.sorted(Comparator.reverseOrder()).toList())
								Files.delete(p);
						}
					}
				}
				constructor = generated.asSubclass(JaninoKernel.class).getConstructor();
				// The production compiler probes close() before bind. Check this separately from evaluation.
				constructor.newInstance().close();
				CACHE.put(key, constructor);
				compilations++;
			} catch (Throwable failure) {
				Path root = Path.of(System.getProperty("rdf4j.test.kernel.failures", "target/kernel-parity-failures"));
				Path file = root.resolve(name.replace('.', '/') + ".java");
				Files.createDirectories(file.getParent());
				Files.writeString(file, source);
				Throwable cause = failure instanceof InvocationTargetException e ? e.getCause() : failure;
				throw new AssertionError(key.backend + " compilation/probe failed; source: " + file, cause);
			}
		}
		return constructor.newInstance();
	}
}
