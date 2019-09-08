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
package com.fluidops.fedx.util;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.repository.sail.SailQuery;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.repository.FedXRepositoryConnection;

/**
 * General utility functions
 * 
 * @author Andreas Schwarte
 * @since 5.0
 */
public class FedXUtil
{

	private static final AtomicLong count = new AtomicLong(0L);

	/**
	 * @param iri
	 * @return the IRI for the full URI string
	 */
	public static IRI iri(String iri)
	{
		return valueFactory().createIRI(iri);
	}

	/**
	 * 
	 * @param literal
	 * @return the string literal
	 */
	public static Literal literal(String literal)
	{
		return valueFactory().createLiteral(literal);
	}

	/**
	 * 
	 * @return a {@link SimpleValueFactory} instance
	 */
	public static ValueFactory valueFactory()
	{
		return SimpleValueFactory.getInstance();
	}

	/**
	 * Apply query bindings to transfer information from the query into the
	 * evaluation routine, e.g. the query execution time.
	 * 
	 * @param query
	 */
	public static void applyQueryBindings(SailQuery query) {
		query.setBinding(FedXRepositoryConnection.BINDING_ORIGINAL_MAX_EXECUTION_TIME,
				FedXUtil.valueFactory().createLiteral(query.getMaxExecutionTime()));
	}

	/**
	 * Hexadecimal representation of an incremental integer.
	 * 
	 * @return an incremental hex UUID
	 */
	public static String getIncrementalUUID() {
		long id = count.incrementAndGet();
		return Long.toHexString(id);
	}

	/**
	 * Set a maximum execution time corresponding to
	 * {@link Config#getEnforceMaxQueryTime()} to this operation.
	 * 
	 * Note that this is an upper bound only as FedX applies other means for
	 * evaluation the maximum query execution time.
	 * 
	 * @param operation the {@link Operation}
	 */
	public static void applyMaxQueryExecutionTime(Operation operation) {
		int maxExecutionTime = Config.getConfig().getEnforceMaxQueryTime();
		if (maxExecutionTime <= 0) {
			return;
		}
		operation.setMaxExecutionTime(maxExecutionTime);
	}
}
