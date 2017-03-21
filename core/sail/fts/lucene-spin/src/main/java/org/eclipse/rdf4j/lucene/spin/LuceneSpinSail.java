/**
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.lucene.spin;

import java.io.File;
import java.util.Properties;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchIndex;
import org.eclipse.rdf4j.sail.lucene.SearchIndexQueryContextInitializer;
import org.eclipse.rdf4j.sail.lucene.util.SearchIndexUtils;
import org.eclipse.rdf4j.sail.spin.SpinSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activates support of {@link SearchIndex} feature inside {@link SpinSail} and manages the index during
 * statements adding/removing. Technically this sail binds {@link SearchIndex} using
 * .addQueryContextInitializer and wraps connection from baseSail by wrapped which modify SearchIndex.
 *
 * @author jacek grzebyta
 */
public class LuceneSpinSail extends NotifyingSailWrapper {

	private final Logger log = LoggerFactory.getLogger(LuceneSpinSail.class);

	private SearchIndex si;

	private Properties parameters = new Properties();

	public LuceneSpinSail() {
	}

	public LuceneSpinSail(NotifyingSail baseSail) {
		super(baseSail);
	}

	public Properties getParameters() {
		return parameters;
	}

	public void setParameters(Properties parameters) {
		this.parameters = parameters;
	}

	/**
	 * @throws SailException
	 */
	@Override
	public void initialize()
		throws SailException
	{
		((SpinSail)getBaseSail()).setEvaluationMode(TupleFunctionEvaluationMode.TRIPLE_SOURCE);
		parameters.setProperty(LuceneSail.INDEX_CLASS_KEY,
				getParameters().getProperty(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS));
		log.debug("index location: {}", parameters.getProperty(LuceneSail.LUCENE_DIR_KEY));
		try {
			si = SearchIndexUtils.createSearchIndex(parameters);
			// bind index to SpinSail
			((SpinSail)getBaseSail()).addQueryContextInitializer(new SearchIndexQueryContextInitializer(si));
		}
		catch (Exception ex) {
			log.warn("error occured during set up of the search index. It might affect functionality.");
			throw new SailException(ex);
		}

		super.initialize();
	}

	@Override
	public void setDataDir(File dataDir) {
		parameters.setProperty(LuceneSail.LUCENE_DIR_KEY, dataDir.getAbsolutePath() + "/index");
		getBaseSail().setDataDir(dataDir);
	}

	/**
	 * @return @throws SailException
	 */
	@Override
	public NotifyingSailConnection getConnection()
		throws SailException
	{

		NotifyingSailConnection connection = super.getConnection();
		if (si == null) {
			throw new SailException("Index is not created");
		}
		return new LuceneSpinSailConnection(connection, si);

	}

}
