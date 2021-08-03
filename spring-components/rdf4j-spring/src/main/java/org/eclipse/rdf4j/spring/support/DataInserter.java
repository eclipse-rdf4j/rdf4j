package org.eclipse.rdf4j.spring.support;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInserter {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	RepositoryConnectionFactory connectionFactory;

	@Transactional(propagation = Propagation.REQUIRED)
	public void insertData(Resource dataFile) {
		Objects.requireNonNull(dataFile);
		logger.debug("Loading data from {}...", dataFile);
		try {
			RepositoryConnection con = connectionFactory.getConnection();
			con.add(dataFile.getInputStream(), "", RDFFormat.TURTLE);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load test data", e);
		}
		logger.debug("\tdone loading data");
	}
}
