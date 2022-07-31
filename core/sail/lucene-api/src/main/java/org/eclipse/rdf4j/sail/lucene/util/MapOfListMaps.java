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
package org.eclipse.rdf4j.sail.lucene.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;

/**
 * @author andriy.nikolov
 *
 * @deprecated since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *             warning from one release to the next.
 */
@Deprecated
@InternalUseOnly
public class MapOfListMaps<Index1Type, Index2Type, DataType> {

	private final Map<Index1Type, Map<Index2Type, List<DataType>>> data;

	/**
	 *
	 */
	public MapOfListMaps() {
		data = new HashMap<>();
	}

	public List<DataType> get(Index1Type key1, Index2Type key2) {
		Map<Index2Type, List<DataType>> intermediateMap = data.get(key1);
		if (intermediateMap != null) {
			List<DataType> tmp = intermediateMap.get(key2);
			if (tmp != null) {
				return tmp;
			}
		}
		return Collections.emptyList();
	}

	public Map<Index2Type, List<DataType>> get(Index1Type key1) {
		Map<Index2Type, List<DataType>> intermediateMap = data.get(key1);
		if (intermediateMap != null) {
			return intermediateMap;
		} else {
			return Collections.emptyMap();
		}
	}

	public void add(Index1Type key1, Index2Type key2, DataType value) {
		Map<Index2Type, List<DataType>> intermediateMap = data.get(key1);
		List<DataType> tmpList;

		if (intermediateMap == null) {
			intermediateMap = new HashMap<>();
			data.put(key1, intermediateMap);
		}

		tmpList = intermediateMap.get(key2);

		if (tmpList == null) {
			tmpList = new ArrayList<>();
			intermediateMap.put(key2, tmpList);
		}

		tmpList.add(value);
	}

	@Override
	public String toString() {
		return data.toString();
	}
}
