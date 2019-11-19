package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.extensiblestore.NamespaceStoreInterface;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class NamespaceStore implements NamespaceStoreInterface {

	private final Map<String, SimpleNamespace> namespacesMap = new LinkedHashMap<>(16);

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
