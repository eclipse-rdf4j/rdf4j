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

import java.io.File;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.endpoint.EndpointType;
import com.fluidops.fedx.util.Vocabulary;


/**
 * Graph information for RDF4J {@link NativeStore} initialization.
 * 
 * <p>
 * Format:
 * </p>
 * 
 * <pre>
 * <%name%> a sd:Service ;
 *  	fedx:store "NativeStore" ;
 *  	fedx:RepositoryLocation "%location%".
 * 
 * relative path (to {@link Config#getBaseDir()}) in a "repositories" subfolder
 * 
 * <http://DBpedia> a sd:Service ;
 *  	fedx:store "NativeStore" ;
 *  	fedx:repositoryLocation "data\\repositories\\native-storage.dbpedia".
 *  
 * absolute Path
 * 
 * <http://DBpedia> a sd:Service ;
 *  	fedx:store "NativeStore" ;
 *  	fedx:repositoryLocation "D:\\data\\repositories\\native-storage.dbpedia".
 * </pre>
 * 
 * <p>
 * Note: the id is constructed from the location:
 * repositories\\native-storage.dbpedia => native-storage.dbpedia
 * </p>
 * 
 * 
 * @author Andreas Schwarte
 *
 */
public class NativeRepositoryInformation extends RepositoryInformation {

	public NativeRepositoryInformation(Model graph, Resource repNode) {
		super(EndpointType.NativeStore);
		initialize(graph, repNode);
	}

	public NativeRepositoryInformation(String name, String location) {
		super(new File(location).getName(), name, location, EndpointType.NativeStore);
	}

	protected void initialize(Model graph, Resource repNode) {
		
		// name: the node's value
		setProperty("name", repNode.stringValue());
				
		// location
		Model location = graph.filter(repNode, Vocabulary.FEDX.REPOSITORY_LOCATION, null);
		String repoLocation = location.iterator().next().getObject().stringValue();
		setProperty("location", repoLocation);
		
		// id: the name of the location
		setProperty("id", new File(repoLocation).getName());
	}
}
