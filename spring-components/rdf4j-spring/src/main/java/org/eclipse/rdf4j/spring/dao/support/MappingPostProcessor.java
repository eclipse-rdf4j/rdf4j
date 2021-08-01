package org.eclipse.rdf4j.spring.dao.support;

import java.util.function.Function;

public interface MappingPostProcessor<I, O> extends Function<I, O> {
}
