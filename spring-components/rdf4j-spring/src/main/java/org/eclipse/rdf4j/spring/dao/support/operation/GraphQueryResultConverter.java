package org.eclipse.rdf4j.spring.dao.support.operation;

import static org.eclipse.rdf4j.spring.dao.exception.mapper.ExceptionMapper.mapException;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModelFactory;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphQueryResultConverter {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private GraphQueryResult graphQueryResult;

	public GraphQueryResultConverter(GraphQueryResult graphQueryResult) {
		this.graphQueryResult = graphQueryResult;
	}

	public Model toModel(GraphQueryResult result) {
		try {
			Model resultModel = new TreeModelFactory().createEmptyModel();
			result.forEach(resultModel::add);
			return resultModel;
		} catch (Exception e) {
			logger.debug("Error converting graph query result to model", e);
			throw mapException("Error converting graph query result to model", e);
		} finally {
			result.close();
		}
	}
}
