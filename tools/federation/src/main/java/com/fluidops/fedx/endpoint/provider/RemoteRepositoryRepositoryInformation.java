/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.endpoint.provider;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

import com.fluidops.fedx.endpoint.EndpointType;
import com.fluidops.fedx.util.Vocabulary;


/**
 * Class holding information for RDF4J {@link HTTPRepository} initialization.
 * 
 * <p>
 * Format:
 * </p>
 * 
 * <pre>
 * <%name%> a sd:Service ;
 *  	fedx:store "RemoteRepository" ;
 *  	fedx:repositoryServer "%location%" ;
 *  	fedx:repositoryName "%name%" .
 * 
 * <http://dbpedia> a sd:Service ;
 *  	fedx:store "RemoteRepository";
 *  	fedx:repositoryServer "http://<host>/openrdf-sesame" ;
 *  	fedx:repositoryName "dbpedia" .
 * </pre>
 * 
 * <p>
 * Note: the id is constructed from the name: http://dbpedia.org/ =>
 * remote_dbpedia.org
 * </p>
 * 
 * 
 * @author Andreas Schwarte
 *
 */
public class RemoteRepositoryRepositoryInformation extends RepositoryInformation {

	public RemoteRepositoryRepositoryInformation(Model graph, Resource repNode) {
		super(EndpointType.RemoteRepository);
		initialize(graph, repNode);
	}

	public RemoteRepositoryRepositoryInformation(String repositoryServer, String repositoryName) {
		super("remote_" + repositoryName, "http://"+repositoryName, repositoryServer + "/" + repositoryName, EndpointType.RemoteRepository);
		setProperty("repositoryServer", repositoryServer);
		setProperty("repositoryName", repositoryName);		
	}
	
	protected void initialize(Model graph, Resource repNode) {
		
		// name: the node's value
		setProperty("name", repNode.stringValue());

		// repositoryServer / location
		Model repositoryServer = graph.filter(repNode, Vocabulary.FEDX.REPOSITORY_SERVER,
				null);
		String repoLocation = repositoryServer.iterator().next().getObject().stringValue();
		setProperty("location", repoLocation);
		setProperty("repositoryServer", repoLocation);
		
		// repositoryName
		Model repositoryName = graph.filter(repNode, Vocabulary.FEDX.REPOSITORY_NAME, null);
		String repoName = repositoryName.iterator().next().getObject().stringValue();
		setProperty("repositoryName", repoName);
		
		// id: the name of the location
		String id = repNode.stringValue().replace("http://", "");
		id = "remote_" + id.replace("/", "_");
		setProperty("id", id);
	}
}
