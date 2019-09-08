/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointFactory;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.federated.util.Version;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.eclipse.rdf4j.repository.Repository;


/**
 * The command line interface for federated query processing with FedX.
 * 
 * <pre>
 * Usage:
 * > FedX [Configuration] [Federation Setup] [Output] [Queries]
 * > FedX -{help|version}
 * 
 * WHERE
 * [Configuration] (optional)
 * Optionally specify the configuration to be used
 *      -c path/to/fedxConfig
 *      -p path/to/prefixConfig
 *      -planOnly
 *      
 * [Federation Setup] (optional)
 * Specify one or more federation members
 *      -s urlToSparqlEndpoint
 *      -l path/to/NativeStore
 *      -d path/to/dataconfig.ttl
 *      
 * [Output] (optional)
 * Specify the output options, default stdout. Files are created per query to results/%outputFolder%/q_%id%.{json|xml},
 * where the outputFolder is the current timestamp, if not specified otherwise.
 *      -f {STDOUT,JSON,XML}
 *      -folder outputFolder
 *      
 * [Queries]
 * Specify one or more queries, in file: separation of queries by empty line
 *      -q sparqlquery
 *      {@literal @}q path/to/queryfile
 *      
 * Notes:
 * The federation members can be specified explicitly (-s,-l,-d) or implicitly as 'dataConfig' 
 * via the fedx configuration  (-f)
 * 
 * If no PREFIX declarations are specified in the configurations, the CLI provides
 * some common PREFIXES, currently rdf, rdfs and foaf. 
 * </pre>
 * 
 * 
 * @author Andreas Schwarte
 *
 */
public class CLI {

	protected enum OutputFormat { STDOUT, JSON, XML; }
	
	protected String fedxConfig=null;
	protected boolean planOnly = false;
	protected String prefixDeclarations = null;
	protected List<Endpoint> endpoints = new ArrayList<Endpoint>();
	protected OutputFormat outputFormat = OutputFormat.STDOUT;
	protected List<String> queries = new ArrayList<String>();
	protected Repository repo = null;
	protected String outFolder = null;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new CLI().run(args);
		} catch (Exception e) {
			System.out.println("Error while using the FedX CLI. System will exit. \nDetails: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	
	public void run(String[] args) {

		// activate logging to stdout if verbose is set
		configureLogging();
		
		System.out.println("FedX Cli " + Version.getVersionInfo().getLongVersion());
		
		// parse the arguments and construct config
		parse(args);

		if (Config.getConfig().getDataConfig()!=null) {
			// currently there is no duplicate detection, so the following is a hint for the user
			// can cause problems if members are explicitly specified (-s,-l,-d) and via the fedx configuration
			if (endpoints.size()>0)
				System.out.println("WARN: Mixture of implicitely and explicitely specified federation members, dataConfig used: " + Config.getConfig().getDataConfig());
			try {
				List<Endpoint> additionalEndpoints = EndpointFactory.loadFederationMembers(new File(Config.getConfig().getDataConfig()));
				endpoints.addAll(additionalEndpoints);
			} catch (FedXException e) {
				error("Failed to load implicitly specified data sources from fedx configuration. Data config is: " + Config.getConfig().getDataConfig() + ". Details: " + e.getMessage(), false);
			}
		}
		
		// generic checks
		if (endpoints.size()==0)
			error("No federation members specified. At least one data source is required.", true);
		
		if (queries.size()==0)
			error("No queries specified", true);

		
		// setup the federation
		try {
			repo = FedXFactory.initializeFederation(endpoints);

			// initialize default prefix declarations (if the user did not specify anything)
			if (Config.getConfig().getPrefixDeclarations() == null) {
				initDefaultPrefixDeclarations();
			}

			int count = 1;
			for (String queryString : queries) {

				try {
					if (planOnly) {
						System.out.println(QueryManager.getQueryPlan(queryString));
					} else {
						System.out.println("Running Query " + count);
						runQuery(queryString, count);
					}
				} catch (Exception e) {
					error("Query " + count + " could not be evaluated: \n" + e.getMessage(), false);
				}
				count++;
			}
		} catch (FedXException e) {
			error("Problem occured while setting up the federation: " + e.getMessage(), false);
		} finally {
			repo.shutDown();
		}

		System.out.println("Done.");
		System.exit(0);
 	}

	
	
	
	protected void parse(String[] _args) {
		if (_args.length==0) {
			printUsage(true);
		}
		
		if (_args.length==1 && _args[0].equals("-help"))  {
			printUsage(true);		
		}
				
		List<String> args = new LinkedList<String>(Arrays.asList(_args));
		
		parseConfiguaration(args, false);
		
		// initialize config
		try {
			Config.initialize(fedxConfig); // fedxConfig may be null (default values)
			if (prefixDeclarations!=null) {
				Config.getConfig().set("prefixDeclarations", prefixDeclarations);	// override in config
			}
		} catch (FedXException e) {
			error("Problem occured while setting up the federation: " + e.getMessage(), false);
		}		
		
		parseEndpoints(args, false);
		parseOutput(args);
		parseQueries(args);
		
		if (outFolder==null) {
			outFolder = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());
		}

	}
	
	/**
	 * Parse the FedX Configuration,
	 *  1) -c path/to/fedxconfig.prop
	 *  2) -p path/to/prefixDeclaration.prop
	 *  3) -planOnly	 
	 *  
	 * @param args
	 */
	protected void parseConfiguaration(List<String> args, boolean printError) {
		String arg = args.get(0);
		
		// fedx config
		if (arg.equals("-c")) {
			readArg(args);											// remove -c
			fedxConfig = readArg(args, "path/to/fedxConfig.ttl");	// remove path
		}
		
		// prefixConfiguration
		else if (arg.equals("-p")) {
			readArg(args);
			prefixDeclarations = readArg(args, "path/to/prefixDeclarations.prop");
		}
		
		else if (arg.equals("-planOnly")) {
			readArg(args);
			planOnly = true;
		}
		
		else {
			if (printError)
				error("Unxpected Configuration Option: " + arg, false);
			else
				return;
		}
		
		parseConfiguaration(args, false);
	}
	
	
	/**
	 * Parse the endpoints, i.e. federation members
	 *  1) SPARQL Endpoint: -s url
	 *  2) Local NativeStore: -l path/to/NativeStore
	 *  3) Dataconfig: -d path/to/dataconfig.ttl
	 *  
	 * @param args
	 */
	protected void parseEndpoints(List<String> args, boolean printError) {
		String arg = args.get(0);
		
		if (arg.equals("-s")) {
			readArg(args);		// remove -s
			String url = readArg(args,"urlToSparqlEndpoint");
			try {
				Endpoint endpoint = EndpointFactory.loadSPARQLEndpoint(url);
				endpoints.add(endpoint);
			} catch (FedXException e) {
				error("SPARQL endpoint " + url + " could not be loaded: " + e.getMessage(), false);
			}			
		}
		
		else if (arg.equals("-l")) {
			readArg(args);		// remove -l
			String path = readArg(args,"path/to/NativeStore");
			try {
				Endpoint endpoint = EndpointFactory.loadNativeEndpoint(path);
				endpoints.add(endpoint);
			} catch (FedXException e) {
				error("NativeStore " + path + " could not be loaded: " + e.getMessage(), false);
			}
		}
		
		else if (arg.equals("-d")) {
			readArg(args);		// remove -d
			String dataConfig = readArg(args,"path/to/dataconfig.ttl");
			try {
				List<Endpoint> ep = EndpointFactory.loadFederationMembers(new File(dataConfig));
				endpoints.addAll(ep);
			} catch (FedXException e) {
				error("Data config '" + dataConfig + "' could not be loaded: " + e.getMessage(), false);
			}
		}
		
		else {			
			if (printError)
				error("Expected at least one federation member (-s, -l, -d), was: " + arg, false);
			else
				return;			
		}
		
		parseEndpoints(args, false);
	}
	
	
	/**
	 * Parse output options
	 *  1) Format: -f {STDOUT,XML,JSON}
	 *  2) OutputFolder: -folder outputFolder 
	 * 
	 * @param args
	 */
	protected void parseOutput(List<String> args) {
		
		String arg = args.get(0);
		
		if (arg.equals("-f")) {
			readArg(args);		// remove -s
			
			String format = readArg(args, "output format {STDOUT, XML, JSON}");
			
			if (format.equals("STDOUT"))
				outputFormat = OutputFormat.STDOUT;
			else if (format.equals("JSON"))
				outputFormat = OutputFormat.JSON;
			else if (format.equals("XML")) {
				outputFormat = OutputFormat.XML;
			}
			else {
				error("Unexpected output format: " + format + ". Available options: STDOUT,XML,JSON", false);
			}
		}
		
		else if (arg.equals("-folder")) {
			readArg(args);		// remove -folder
			
			outFolder = readArg(args, "outputFolder");
		}
		
		else {
			return;
		}
		
		parseOutput(args);
	}
	
	
	/**
	 * Parse query input
	 *  1) Querystring: -q SparqlQuery
	 *  2) File: @q path/to/QueryFile
	 *  
	 * @param args
	 */
	protected void parseQueries(List<String> args) {
		String arg = args.get(0);
		
		if (arg.equals("-q")) {
			readArg(args);	// remove -q
			String query = readArg(args, "SparqlQuery");
			queries.add(query);
		}
		
		else if (arg.equals("@q")) {
			readArg(args);	// remove @q
			String queryFile = readArg(args, "path/to/queryFile");
			try {	
				List<String> q = QueryStringUtil.loadQueries(queryFile);
				queries.addAll(q);
			} catch (IOException e) {
				error("Error loading query file '" + queryFile + "': " + e.getMessage(), false);
			}
		}
		
		else {
			error("Unexpected query argument: " + arg, false);
		}
		
		if (args.size()>0)
			parseQueries(args);
	}
	
	protected String readArg(List<String> args, String... expected) {
		if (args.size()==0)
			error("Unexpected end of args, expected: " + Arrays.asList(expected), false);
		return args.remove(0);
	}
	
	/**
	 * initializes default prefix declarations from org.eclipse.rdf4j.federated.commonPrefixesCli.prop
	 */
	protected void initDefaultPrefixDeclarations() {
		
		QueryManager qm = FederationManager.getInstance().getQueryManager();
		Properties props = new Properties();
		try (InputStream in = CLI.class.getResourceAsStream("/org/eclipse/rdf4j/federated/commonPrefixesCli.prop")) {
			props.load(in);
		} catch (IOException e)	{
			throw new FedXRuntimeException("Error loading prefix properties: " + e.getMessage());
		}
		
		for (String ns : props.stringPropertyNames()) {
			qm.addPrefixDeclaration(ns, props.getProperty(ns));  	// register namespace/prefix pair
		}
	}
	
	protected void runQuery(String queryString, int queryId) throws QueryEvaluationException {
			
		TupleQuery query;
		try {
			query = QueryManager.prepareTupleQuery(queryString);
		} catch (MalformedQueryException e) {
			throw new QueryEvaluationException("Query is malformed: " + e.getMessage());
		} 
		int count = 0;
		long start = System.currentTimeMillis();
		
		try (TupleQueryResult res = query.evaluate()) {
			
			if (outputFormat == OutputFormat.STDOUT) {
				while (res.hasNext()) {
					System.out.println(res.next());
					count++;
				}
			}
			
			else if (outputFormat == OutputFormat.JSON) {
				
				File out = new File("results/"+ outFolder + "/q_" + queryId + ".json");
				if (!out.getParentFile().mkdirs()) {
					error("Failed to create output directories", false);
				}
				
				System.out.println("Results are being written to " + out.getPath());
				
				try (FileOutputStream fout = new FileOutputStream(out)) {
					SPARQLResultsJSONWriter w = new SPARQLResultsJSONWriter(fout);
					w.startQueryResult(res.getBindingNames());
					
					while (res.hasNext()) {
						w.handleSolution(res.next());
						count++;
					}
					
					w.endQueryResult();
				} catch (IOException e) {
					error("IO Error while writing results of query " + queryId + " to JSON file: " + e.getMessage(), false);
				} catch (TupleQueryResultHandlerException e) {
					error("Tuple result error while writing results of query " + queryId + " to JSON file: " + e.getMessage(), false);
				}
			}
			
			else if (outputFormat == OutputFormat.XML) {
				
				File out = new File("results/" + outFolder + "/q_" + queryId + ".xml");
				if (!out.getParentFile().mkdirs()) {
					error("Failed to create output directories", false);
				}
				
				System.out.println("Results are being written to " + out.getPath());
				
				try (FileOutputStream fout = new FileOutputStream(out)) {
					SPARQLResultsXMLWriter w = new SPARQLResultsXMLWriter(fout);
					w.startQueryResult(res.getBindingNames());
					
					while (res.hasNext()) {
						w.handleSolution(res.next());
						count++;
					}
					
					w.endQueryResult();
					
				} catch (IOException e) {
					error("IO Error while writing results of query " + queryId + " to XML file: " + e.getMessage(), false);
				} catch (TupleQueryResultHandlerException e) {
					error("Tuple result error while writing results of query " + queryId + " to JSON file: " + e.getMessage(), false);
				}
			}
		}
		
		long duration = System.currentTimeMillis() - start;		// the duration in ms
		
		System.out.println("Done query " + queryId + ": duration=" + duration + "ms, results=" + count);
	}
	
	
	
	/**
	 * Print an error and exit
	 * 
	 * @param errorMsg
	 */
	protected void error(String errorMsg, boolean printHelp) {
		System.out.println("ERROR: " + errorMsg);
		if (printHelp) {
			System.out.println("");
			printUsage();
		}
		System.exit(1);
	}
	
	
	/**
	 * Print the documentation
	 */
	protected void printUsage(boolean... exit) {
		
		System.out.println("Usage:");
		System.out.println("> FedX [Configuration] [Federation Setup] [Output] [Queries]");
		System.out.println("> FedX -{help|version}");
		System.out.println("");
		System.out.println("WHERE");
		System.out.println("[Configuration] (optional)");
		System.out.println("Optionally specify the configuration to be used");
		System.out.println("\t-c path/to/fedxConfig");
		System.out.println("\t-p path/to/prefixDeclarations");
		System.out.println("\t-planOnly");
		System.out.println("");
		System.out.println("[Federation Setup] (optional)");
		System.out.println("Specify one or more federation members");
		System.out.println("\t-s urlToSparqlEndpoint");
		System.out.println("\t-l path/to/NativeStore");
		System.out.println("\t-d path/to/dataconfig.ttl");
		System.out.println("");
		System.out.println("[Output] (optional)");
		System.out.println("Specify the output options, default stdout. Files are created per query to results/%outputFolder%/q_%id%.{json|xml}, where the outputFolder is the current timestamp, if not specified otherwise.");
		System.out.println("\t-f {STDOUT,JSON,XML}");
		System.out.println("\t-folder outputFolder");
		System.out.println("");
		System.out.println("[Queries]");
		System.out.println("Specify one or more queries, in file: separation of queries by empty line");
		System.out.println("\t-q sparqlquery");
		System.out.println("\t@q path/to/queryfile");
		System.out.println("");
		System.out.println("Examples:");
		System.out.println("Please have a look at the examples attached to this package.");
		System.out.println("");
		System.out.println("Notes:");
		System.out.println("The federation members can be specified explicitely (-s,-l,-d) or implicitely as 'dataConfig' via the fedx configuration (-f)");
		System.out.println("If no PREFIX declarations are specified in the configurations, the CLI provides some common PREFIXES, currently rdf, rdfs and foaf. ");
		
		if (exit.length!=0 && exit[0])
			System.exit(0);
	}
	
	
	/**
	 * Activate logging if -verbose is enabled.
	 * 
	 * Verbose level: 0=off (default), 1=INFO, 2=DEBUG, 3=ALL
	 */
	protected void configureLogging() {
		
		if (System.getProperty("log4j.configurationFile") == null) {
			File logFile = new File("etc/log4j.properties");
			if (!logFile.exists()) {
				System.out.println(
						"WARN: Log4j configuration not found in 'etc/log4j.properties. Logging features may not fully work.");
			} else {
				System.setProperty("log4j.configurationFile", "file:etc/log4j.properties");
			}
		}
	}

}
