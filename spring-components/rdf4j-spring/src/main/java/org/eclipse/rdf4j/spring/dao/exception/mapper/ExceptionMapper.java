package org.eclipse.rdf4j.spring.dao.exception.mapper;

import org.eclipse.rdf4j.spring.dao.exception.Rdf4JSpringException;

public class ExceptionMapper {
	public static Rdf4JSpringException mapException(String message, Exception e) {
		return new Rdf4JSpringException(message, e);
	}
}
