package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MemoryNamespaceStore implements NamespaceStoreInterface {

	private final Map<String, SimpleNamespace> namespacesMap = new HashMap<>();

	@Override
	public String getNamespace(String prefix) {
		String result = null;
		SimpleNamespace namespace = namespacesMap.get(prefix);
		if (namespace != null) {
			result = namespace.getName();
		}
		return result;
	}

	@Override
	public void setNamespace(String prefix, String name) {
		namespacesMap.put(prefix, new SimpleNamespace(prefix, name));
	}

	@Override
	public void removeNamespace(String prefix) {
		namespacesMap.remove(prefix);
	}

	@Override
	public Iterator<SimpleNamespace> iterator() {
		return namespacesMap.values().iterator();
	}

	@Override
	public void clear() {
		namespacesMap.clear();
	}

	@Override
	public void init() {

	}
}
