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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Lightweight IR debug printer using Gson pretty printing.
 *
 * Produces objects of the form {"class": "<FQN>", "data": {...}} so it is easy to see the concrete IR node type in
 * dumps. Several noisy fields from RDF4J algebra nodes are excluded to keep output focused on relevant structure.
 */
public final class IrDebug {
	private final static Set<String> ignore = Set.of("parent", "costEstimate", "totalTimeNanosActual", "cardinality",
			"cachedHashCode", "isVariableScopeChange", "resultSizeEstimate", "resultSizeActual");

	private IrDebug() {
	}

	public static String dump(IrNode node) {

		Gson gson = new GsonBuilder().setPrettyPrinting()
				.registerTypeAdapter(Var.class, new VarSerializer())
//				.registerTypeAdapter(IrNode.class, new ClassNameAdapter<IrNode>())
				.registerTypeAdapterFactory(new OrderedAdapterFactory())
				.setExclusionStrategies(new ExclusionStrategy() {
					@Override
					public boolean shouldSkipField(FieldAttributes f) {
						// Exclude noisy fields that do not help understanding the IR shape
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

	static class VarSerializer implements JsonSerializer<Var> {
		@Override
		public JsonElement serialize(Var src, Type typeOfSrc, JsonSerializationContext context) {
			// Turn Var into a JSON string using its toString()
			String string = src.toString();
			return new JsonPrimitive(src.toString().replace("=", ": "));
		}
	}

//	static class ClassNameAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
//		@Override
//		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
//			JsonObject obj = new JsonObject();
//			obj.addProperty("class", src.getClass().getName());
//			obj.add("data", context.serialize(src));
//			return obj;
//		}
//
//		@Override
//		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
//				throws JsonParseException {
//			JsonObject obj = json.getAsJsonObject();
//			String className = obj.get("class").getAsString();
//			try {
//				Class<?> clazz = Class.forName(className);
//				return context.deserialize(obj.get("data"), clazz);
//			} catch (ClassNotFoundException e) {
//				throw new JsonParseException(e);
//			}
//		}
//	}

	static class OrderedAdapterFactory implements TypeAdapterFactory {
		@Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			Class<? super T> raw = type.getRawType();

			// Only wrap bean-like classes
			if (raw.isPrimitive()
					|| Number.class.isAssignableFrom(raw)
					|| CharSequence.class.isAssignableFrom(raw)
					|| Boolean.class.isAssignableFrom(raw)
					|| raw.isEnum()
					|| Collection.class.isAssignableFrom(raw)
					|| Map.class.isAssignableFrom(raw)) {
				return null;
			}

			final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

			return new TypeAdapter<T>() {
				@Override
				public void write(JsonWriter out, T value) throws IOException {
					if (value == null) {
						out.nullValue();
						return;
					}

					// Produce a detached tree
					JsonElement tree = delegate.toJsonTree(value);

					if (tree.isJsonObject()) {
						JsonObject obj = tree.getAsJsonObject();
						JsonObject reordered = new JsonObject();

						// primitives
						obj.entrySet()
								.stream()
								.filter(e -> e.getValue().isJsonPrimitive())
								.forEach(e -> reordered.add(e.getKey(), e.getValue()));

						// arrays
						obj.entrySet()
								.stream()
								.filter(e -> e.getValue().isJsonArray())
								.forEach(e -> reordered.add(e.getKey(), e.getValue()));

						// objects
						obj.entrySet()
								.stream()
								.filter(e -> e.getValue().isJsonObject())
								.forEach(e -> reordered.add(e.getKey(), e.getValue()));

						// Directly dump reordered element into the writer
						Streams.write(reordered, out);
					} else {
						// Non-object → just dump as is
						Streams.write(tree, out);
					}
				}

				@Override
				public T read(JsonReader in) throws IOException {
					return delegate.read(in);
				}
			};
		}
	}
}
