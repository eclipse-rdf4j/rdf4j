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
package com.fluidops.fedx.endpoint;

import com.fluidops.fedx.evaluation.TripleSource;

/**
 * Additional {@link EndpointConfiguration} for SPARQL endpoints.
 * 
 * @author Andreas Schwarte
 *
 */
public class SparqlEndpointConfiguration implements EndpointConfiguration {

	private boolean supportsASKQueries = true;
	
	/**
	 * Flag indicating whether ASK queries are supported. Specific
	 * {@link TripleSource} implementations may use this information
	 * to decide whether to use ASK or SELECT for source selection.
	 * 
	 * @return boolean indicating whether ASK queries are supported
	 */
	public boolean supportsASKQueries() {
		return supportsASKQueries;
	}
	
	/**
	 * Define whether this endpoint supports ASK queries.
	 * 
	 * @param flag
	 */
	public void setSupportsASKQueries(boolean flag) {
		this.supportsASKQueries = flag;
	}
}
