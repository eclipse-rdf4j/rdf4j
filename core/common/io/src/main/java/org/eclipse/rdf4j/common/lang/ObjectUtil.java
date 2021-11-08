/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.lang;

/**
 * Generic utility methods related to objects.
 *
 * @author Arjohn Kampman
 */
public class ObjectUtil {

	/**
	 * Compares two objects or null references.
	 *
	 * @param o1 The first object.
	 * @param o2 The second object
	 * @return <var>true</var> if both objects are <var>null</var>, if the object references are identical, or if the
	 *         objects are equal according to the {@link Object#equals} method of the first object; <var>false</var> in
	 *         all other situations.
	 */
	public static boolean nullEquals(Object o1, Object o2) {
		return o1 == o2 || o1 != null && o2 != null && o1.equals(o2);
	}

	/**
	 * Returns the hash code of the supplied object, or <var>0</var> if a null reference is supplied.
	 *
	 * @param o An object or null reference.
	 * @return The object's hash code, or <var>0</var> if the parameter is <var>null</var>.
	 */
	public static int nullHashCode(Object o) {
		return o == null ? 0 : o.hashCode();
	}
}
