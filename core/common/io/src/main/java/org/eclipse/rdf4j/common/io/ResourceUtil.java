/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * ResourceUtil is a utility class for retrieving resources (images, property-files, etc) from the classpath.
 */
public class ResourceUtil {

	/**
	 * The URL to the specified resource
	 *
	 * @param resourceName the name of the resource
	 * @return the URL to the specified resource, or null if the resource could not be found
	 */
	public static URL getURL(String resourceName) {
		// most likely to succeed
		URL result = Thread.currentThread().getContextClassLoader().getResource(resourceName);

		if (result == null) {
			// try the caller's class/classloader
			Class<?> caller = getCaller();
			result = caller.getResource(resourceName);

			if (result == null) {
				result = caller.getClassLoader().getResource(resourceName);

				if (result == null) {
					// last resort: the system classloader
					result = ClassLoader.getSystemResource(resourceName);
				}
			}
		}

		return result;
	}

	/**
	 * Get an inputstream on the specified resource.
	 *
	 * @param resourceName the name of the resource
	 * @return an inputstream on the specified resource, or null if the resource could not be found
	 */
	public static InputStream getInputStream(String resourceName) {
		// most likely to succeed
		InputStream result = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);

		if (result == null) {
			// try the caller's class/classloader
			Class<?> caller = getCaller();
			result = caller.getResourceAsStream(resourceName);
			if (result == null) {
				result = caller.getClassLoader().getResourceAsStream(resourceName);
				if (result == null) {
					// last resort: the system classloader
					result = ClassLoader.getSystemResourceAsStream(resourceName);
				}
			}
		}

		return result;
	}

	/**
	 * Retrieve the String contents of the specified resource, obtained by opening in inputstream on the resource and
	 * then interpreting the bytes contained in the inputstream as if they represented characters. This may not make
	 * sense on all resources. There is no "magic" in this method to read anything other than plain text.
	 *
	 * @param resourceName the name of the resource
	 * @return the String contents of the specified resource, or null if the specified resource could not be found
	 * @throws IOException when something goes wrong trying to read the resource
	 */
	public static String getString(String resourceName) throws IOException {
		try (InputStream in = ResourceUtil.getInputStream(resourceName)) {
			if (in == null) {
				return null;
			}
			return IOUtil.readString(in);
		}
	}

	/**
	 * Retrieve a properties resource.
	 *
	 * @param resourceName the name of the resource
	 * @return a Properties object representing the contents of the resource, or null if the specified resource could
	 *         not be found
	 * @throws IOException
	 */
	public static Properties getProperties(String resourceName) throws IOException {
		URL resourceURL = getURL(resourceName);

		if (resourceURL != null) {
			try (InputStream in = resourceURL.openStream()) {
				Properties result = new Properties();
				result.load(in);
				return result;
			}
		}

		return null;
	}

	/**
	 * Retrieve the calling class of a method in this class.
	 *
	 * @return the calling class of a method in this class, or this class if no other class could be determined.
	 */
	private static Class<?> getCaller() {
		Class<?> result = ResourceUtil.class;

		Class<?>[] callStack = CallerResolver.INSTANCE.getClassContext();
		if (callStack.length > 0) {
			int index = 0;
			// look for the first class on the stack that isn't this class or the
			// inner utility class (it's most likely stack[2], but we don't want to
			// count on that and find out it isn't)
			while (index < callStack.length && (result == ResourceUtil.class || result == CallerResolver.class)) {
				result = callStack[index];
				index = index + 1;
			}
		}

		return result;
	}

	/**
	 * A helper class to get the call context. It subclasses SecurityManager to make getClassContext() accessible. An
	 * instance of CallerResolver only needs to be created, not installed as an actual security manager. We use our own
	 * class instead of System.getSecurityManager(), because that may be set to null.
	 */
	private static final class CallerResolver extends SecurityManager {

		private static final CallerResolver INSTANCE;

		static {
			try {
				INSTANCE = new CallerResolver();
			} catch (SecurityException se) {
				throw new RuntimeException("Could not create CallerResolver: " + se);
			}
		}

		@Override
		protected Class<?>[] getClassContext() {
			return super.getClassContext();
		}
	}

}
