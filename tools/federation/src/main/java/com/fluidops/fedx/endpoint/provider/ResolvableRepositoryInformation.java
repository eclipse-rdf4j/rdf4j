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
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResolver;

import com.fluidops.fedx.endpoint.EndpointType;
import com.fluidops.fedx.endpoint.ResolvableEndpoint;
import com.fluidops.fedx.util.Vocabulary;


/**
 * Graph information for a {@link ResolvableEndpoint} where the
 * {@link Repository} is looked up using the configured
 * {@link RepositoryResolver}.
 * <p>
 * 
 * Format:
 * <p>
 * 
 * <pre>
 * &#64;prefix sd: &lt;http://www.w3.org/ns/sparql-service-description#&gt; .
 * &#64;prefix fedx: &lt;http://www.fluidops.com/config/fedx#&gt; .
 * 
 * &lt;http://myname&gt; a sd:Service ;
 *  	fedx:store "ResolvableRepository";
 *  	fedx:repositoryName "myRepoId"
 * </pre>
 * 
 * @author Andreas Schwarte
 * @see ResolvableEndpoint
 * @see ResolvableRepositoryProvider
 */
public class ResolvableRepositoryInformation extends RepositoryInformation {


	public ResolvableRepositoryInformation(Model graph, Resource repNode) {
		super(EndpointType.Other);
		initialize(graph, repNode);
	}

	public ResolvableRepositoryInformation(String repositoryId) {
		super(repositoryId, "http://" + repositoryId, location(repositoryId), EndpointType.Other);
	}

	protected void initialize(Model graph, Resource repNode) {
		
		// name: the node's value
		setProperty("name", repNode.stringValue());
				
		// location		
		Model repositoryId = graph.filter(repNode, Vocabulary.FEDX.REPOSITORY_NAME, null);
		String repoId = repositoryId.iterator().next().getObject().stringValue();


		setProperty("location", location(repoId));
		
		// id: the name of the location
		String id = repoId;
		setProperty("id", id);
	}
	
	static String location(String repoId) {
		return "resolvable:" + repoId;
	}
}
