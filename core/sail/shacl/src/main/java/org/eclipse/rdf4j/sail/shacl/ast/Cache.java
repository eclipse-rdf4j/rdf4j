package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.rdf4j.model.Resource;

public class Cache {

	Map<Resource, Shape> cache = new HashMap<>();

	public Shape computeIfAbsent(Resource id, Function<Resource, Shape> mappingFunction) {
		return cache.computeIfAbsent(id, mappingFunction);
	}

	public Shape get(Resource id) {
		return cache.get(id);
	}

	public void put(Resource id, Shape shape) {
		cache.put(id, shape);
	}
}
