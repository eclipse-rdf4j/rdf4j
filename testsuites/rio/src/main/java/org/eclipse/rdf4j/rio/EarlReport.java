/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.DOAP;
import org.eclipse.rdf4j.model.vocabulary.EARL;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arjohn Kampman
 * @author Peter Ansell
 */
public class EarlReport {

	/**
	 * Helper variable for tests run by Peter Ansell
	 */
	public static final IRI ANSELL = SimpleValueFactory.getInstance().createIRI("https://github.com/ansell");

	/**
	 * Helper variable for tests run by Jeen Broekstra
	 */
	public static final IRI BROEKSTRA = SimpleValueFactory.getInstance().createIRI(
			"https://bitbucket.org/jeenbroekstra");

	protected Repository earlRepository;

	protected ValueFactory vf;

	protected RepositoryConnection con;

	protected Resource projectNode;

	protected Resource asserterNode;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public void generateReport(TestSuite nextTestSuite, IRI nextAsserterNode, IRI specURI)
		throws Exception
	{
		// IMPORTANT: Set this to whoever is running the tests
		asserterNode = nextAsserterNode;

		earlRepository = new SailRepository(new MemoryStore());
		earlRepository.initialize();
		vf = earlRepository.getValueFactory();
		con = earlRepository.getConnection();
		con.begin();

		con.setNamespace("rdf", RDF.NAMESPACE);
		con.setNamespace("xsd", XMLSchema.NAMESPACE);
		con.setNamespace("doap", DOAP.NAMESPACE);
		con.setNamespace("earl", EARL.NAMESPACE);
		con.setNamespace("dcterms", DCTERMS.NAMESPACE);

		projectNode = vf.createIRI("http://www.openrdf.org/#sesame");
		BNode releaseNode = vf.createBNode();
		con.add(projectNode, RDF.TYPE, DOAP.PROJECT);
		con.add(projectNode, RDF.TYPE, EARL.TEST_SUBJECT);
		con.add(projectNode, RDF.TYPE, EARL.SOFTWARE);
		con.add(projectNode, DOAP.NAME, vf.createLiteral("OpenRDF Sesame"));
		con.add(projectNode, DCTERMS.TITLE, vf.createLiteral("OpenRDF Sesame"));
		con.add(projectNode, DOAP.HOMEPAGE, vf.createIRI("http://www.openrdf.org/#sesame"));
		con.add(projectNode, DOAP.LICENSE,
				vf.createIRI("https://bitbucket.org/openrdf/sesame/src/master/core/LICENSE.txt"));
		con.add(
				projectNode,
				DOAP.DESCRIPTION,
				vf.createLiteral("Sesame is an extensible Java framework for storing, querying and inferencing for RDF."));
		// Release date of Sesame-1.0
		con.add(projectNode, DOAP.CREATED, vf.createLiteral("2004-03-25", XMLSchema.DATE));
		con.add(projectNode, DOAP.PROGRAMMING_LANGUAGE, vf.createLiteral("Java"));
		con.add(projectNode, DOAP.IMPLEMENTS, specURI);
		con.add(projectNode, DOAP.DOWNLOAD_PAGE, vf.createIRI("http://sourceforge.net/projects/sesame/files/"));
		con.add(projectNode, DOAP.MAILING_LIST,
				vf.createIRI("http://lists.sourceforge.net/lists/listinfo/sesame-general"));
		con.add(projectNode, DOAP.BUG_DATABASE, vf.createIRI("https://openrdf.atlassian.net/browse/SES"));
		con.add(projectNode, DOAP.BLOG, vf.createIRI("http://www.openrdf.org/news.jsp"));

		con.add(projectNode, DOAP.DEVELOPER, ANSELL);
		con.add(projectNode, DOAP.DEVELOPER, BROEKSTRA);

		con.add(ANSELL, RDF.TYPE, EARL.ASSERTOR);
		con.add(ANSELL, RDF.TYPE, FOAF.PERSON);
		con.add(ANSELL, FOAF.NAME, vf.createLiteral("Peter Ansell"));
		con.add(BROEKSTRA, RDF.TYPE, EARL.ASSERTOR);
		con.add(BROEKSTRA, RDF.TYPE, FOAF.PERSON);
		con.add(BROEKSTRA, FOAF.NAME, vf.createLiteral("Jeen Broekstra"));

		con.add(projectNode, DOAP.RELEASE, releaseNode);
		con.add(releaseNode, RDF.TYPE, DOAP.VERSION);
		con.add(releaseNode, DOAP.NAME, vf.createLiteral("Sesame 2.8.0"));
		SimpleDateFormat xsdDataFormat = new SimpleDateFormat("yyyy-MM-dd");
		String currentDate = xsdDataFormat.format(new Date());
		con.add(releaseNode, DOAP.CREATED, vf.createLiteral(currentDate, XMLSchema.DATE));

		TestResult testResult = new TestResult();
		EarlTestListener listener = new EarlTestListener();
		testResult.addListener(listener);

		logger.info("running EARL tests..");
		nextTestSuite.run(testResult);

		logger.info("tests complete, generating EARL report...");

		con.commit();

		RDFWriterFactory factory = RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).orElseThrow(
				Rio.unsupportedFormat(RDFFormat.TURTLE));
		File outFile = File.createTempFile("sesame-earl-compliance",
				"." + RDFFormat.TURTLE.getDefaultFileExtension());
		FileOutputStream out = new FileOutputStream(outFile);
		try {
			con.export(factory.getWriter(out));
		}
		finally {
			out.close();
		}

		con.close();
		earlRepository.shutDown();

		logger.info("EARL output written to " + outFile);
		System.out.println("EARL output written to " + outFile);
	}

	protected class EarlTestListener implements TestListener {

		private int errorCount;

		private int failureCount;

		public void startTest(Test test) {
			errorCount = failureCount = 0;
		}

		public void endTest(Test test) {
			IRI testURI = null;
			boolean didIgnoreFailure = false;
			if (test instanceof PositiveParserTest) {
				testURI = ((PositiveParserTest)test).testUri;
			}
			else if (test instanceof NegativeParserTest) {
				testURI = ((NegativeParserTest)test).testUri;
				didIgnoreFailure = ((NegativeParserTest)test).didIgnoreFailure;
			}
			else {
				throw new RuntimeException("Unexpected test type: " + test.getClass());
			}
			System.out.println("testURI: " + testURI.stringValue());
			try {
				BNode testNode = vf.createBNode();
				BNode resultNode = vf.createBNode();
				con.add(testNode, RDF.TYPE, EARL.ASSERTION);
				con.add(testNode, EARL.ASSERTEDBY, asserterNode);
				con.add(testNode, EARL.MODE, EARL.AUTOMATIC);
				con.add(testNode, EARL.SUBJECT, projectNode);
				con.add(testNode, EARL.TEST, testURI);
				con.add(testNode, EARL.RESULT, resultNode);
				con.add(resultNode, RDF.TYPE, EARL.TESTRESULT);

				if (didIgnoreFailure) {
					con.add(resultNode, EARL.OUTCOME, EARL.NOTTESTED);
				}
				else if (errorCount > 0 || failureCount > 0) {
					con.add(resultNode, EARL.OUTCOME, EARL.FAIL);
				}
				else {
					con.add(resultNode, EARL.OUTCOME, EARL.PASS);
				}
			}
			catch (RepositoryException e) {
				throw new RuntimeException(e);
			}
		}

		public void addError(Test test, Throwable t) {
			errorCount++;
		}

		public void addFailure(Test test, AssertionFailedError error) {
			failureCount++;
		}
	}
}
