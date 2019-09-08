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

import java.util.Arrays;
import java.util.List;

/**
 * Information about the type of an endpoint
 * 
 * @author Andreas Schwarte
 *
 */
public enum EndpointType {
	NativeStore(Arrays.asList("NativeStore", "lsail/NativeStore")), 
	SparqlEndpoint(Arrays.asList("SparqlEndpoint", "api/sparql")), 
	RemoteRepository(Arrays.asList("RemoteRepository")), 
	Other(Arrays.asList("Other"));
	
	private List<String> formatNames;
	private EndpointType(List<String> formatNames) {
		this.formatNames = formatNames;
	}	
	
	/**
	 * Returns true if the endpoint type supports the
	 * given format (e.g. mime-type). Consider as an
	 * example the SparqlEndpoint which supports
	 * format "api/sparql".
	 * @param format
	 * @return true if the endpoint supports the given format
	 */
	public boolean supportsFormat(String format) {
		return formatNames.contains(format);
	}
	
	/**
	 * returns true if the given format is supported by
	 * some repository type.
	 * 
	 * @param format
	 * @return wheter the given format is supported
	 */
	public static boolean isSupportedFormat(String format) {
		if (format==null)
			return false;
		for (EndpointType e  : values())
			if (e.supportsFormat(format))
				return true;
		return false;
	}
}