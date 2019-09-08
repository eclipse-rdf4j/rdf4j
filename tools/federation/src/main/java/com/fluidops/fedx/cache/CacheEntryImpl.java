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
package com.fluidops.fedx.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.cache.Cache.StatementSourceAssurance;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.exception.EntryUpdateException;


/**
 * Implementation for Cache Entry
 * 
 * @author Andreas Schwarte
 *
 */
public class CacheEntryImpl implements CacheEntry{

	private static final long serialVersionUID = -2078321733800349639L;
	
	
	/* map endpoint.id to the corresponding entry */
	protected Map<String, EndpointEntry> entries = new HashMap<String, EndpointEntry>();
	
	
	@Override
	public StatementSourceAssurance canProvideStatements(Endpoint endpoint) {
		EndpointEntry entry = entries.get(endpoint.getId());
		return entry == null ? StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS : 
					( entry.doesProvideStatements() ? StatementSourceAssurance.HAS_REMOTE_STATEMENTS : StatementSourceAssurance.NONE );
	}

	@Override
	public List<Endpoint> getEndpoints() {
		return EndpointManager.getEndpointManager().getEndpoints(entries.keySet());
	}

	@Override
	public CloseableIteration<? extends Statement, Exception> getStatements() {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}

	@Override
	public CloseableIteration<? extends Statement, Exception> getStatements(
			Endpoint endpoint) {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}

	@Override
	public List<Endpoint> hasLocalStatements() {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}

	@Override
	public boolean hasLocalStatements(Endpoint endpoint) {
		// not yet implemented
		return false;
	}

	@Override
	public boolean isUpToDate() {
		return true;
	}

	@Override
	public void merge(CacheEntry other) throws EntryUpdateException {
		// XXX make a check if we can safely cast?
		
		CacheEntryImpl o = (CacheEntryImpl)other;
		
		for (String k : o.entries.keySet()) {
			if (!entries.containsKey(k))
				entries.put(k, o.entries.get(k));
			else {
				
				EndpointEntry _merge = o.entries.get(k);
				EndpointEntry _old = entries.get(k);
				
				_old.setCanProvideStatements( _merge.doesProvideStatements());
			}
				
		}
	}

	@Override
	public void update() throws EntryUpdateException {
		throw new UnsupportedOperationException("This operation is not yet supported.");		
	}

	@Override
	public void add(EndpointEntry endpointEntry) {
		entries.put(endpointEntry.getEndpointID(), endpointEntry);		
	}
}
