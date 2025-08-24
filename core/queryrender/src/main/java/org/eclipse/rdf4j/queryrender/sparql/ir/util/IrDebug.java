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
package org.eclipse.rdf4j.queryrender.sparql.ir.util;

import java.lang.reflect.Type;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/** Lightweight IR debug printer using Gson pretty printing. */
public final class IrDebug {
	private IrDebug() {
	}

	private final static Set<String> ignore = Set.of("parent", "costEstimate", "totalTimeNanosActual", "cardinality",
			"cachedHashCode", "isVariableScopeChange", "resultSizeEstimate", "resultSizeActual");

	static class VarSerializer implements JsonSerializer<Var> {
		@Override
		public JsonElement serialize(Var src, Type typeOfSrc, JsonSerializationContext context) {
			// Turn Var into a JSON string using its toString()
			String string = src.toString();
			return new JsonPrimitive(src.toString().replace("=", ": "));
		}
	}

	public static String dump(IrNode node) {
		Gson gson = new GsonBuilder().setPrettyPrinting()
				.registerTypeAdapter(Var.class, new VarSerializer())
				.setExclusionStrategies(new ExclusionStrategy() {
					@Override
					public boolean shouldSkipField(FieldAttributes f) {
						// Exclude any field literally named "parent"

						return ignore.contains(f.getName());

					}

					@Override
					public boolean shouldSkipClass(Class<?> clazz) {
						// We don't want to skip entire classes, so return false
						return false;
					}
				})
				.create();
		return gson.toJson(node);
	}
}
