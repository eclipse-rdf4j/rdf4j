package org.eclipse.rdf4j.spring.dao.support;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.Value;

public interface UpdateCallback extends Consumer<Map<String, Value>> {
}
