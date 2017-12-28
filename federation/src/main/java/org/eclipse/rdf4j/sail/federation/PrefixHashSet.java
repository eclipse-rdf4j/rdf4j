/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James Leigh
 */
public class PrefixHashSet {

	private int length = Integer.MAX_VALUE; // NOPMD

	private final Map<String, List<String>> index = new HashMap<String, List<String>>();

	public PrefixHashSet(Iterable<String> values) {
		for (String value : values) {
			if (value.length() < length) {
				length = value.length();
			}
		}
		for (String value : values) {
			String key = value.substring(0, length);
			List<String> entry = index.get(key);
			if (entry == null) {
				index.put(key, entry = new ArrayList<String>()); // NOPMD
			}
			entry.add(value.substring(length));
		}
	}

	public boolean match(String value) {
		boolean result = false;
		if (value.length() >= length) {
			String key = value.substring(0, length);
			List<String> entry = index.get(key);
			if (entry != null) {
				result = matchValueToEntry(value, entry);
			}
		}
		return result;
	}

	private boolean matchValueToEntry(String value, List<String> entry) {
		boolean result = false;
		String tail = value.substring(length);
		for (String prefix : entry) {
			if (tail.startsWith(prefix)) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return index.toString();
	}
}
