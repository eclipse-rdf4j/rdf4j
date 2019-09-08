package com.fluidops.fedx.server;

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.repository.ConfigurableSailRepository;


/**
 * Interface for the server:
 * 
 * {@link SPARQLEmbeddedServer} and {@link NativeStoreServer}
 * 
 * @author as
 *
 */
public interface Server {

	public void initialize(int nRepositories) throws Exception;
	
	public void shutdown() throws Exception;
	
	public Endpoint loadEndpoint(int i) throws Exception;

	/**
	 * Returns the actual {@link ConfigurableSailRepository} instance for the
	 * endpoint
	 * 
	 * @param i the endpoint index starting with 1
	 * @return
	 */
	public ConfigurableSailRepository getRepository(int i);
}
