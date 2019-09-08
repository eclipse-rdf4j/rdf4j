/*
 * Copyright (C) 2019 Veritas Technologies LLC.
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
package com.fluidops.fedx.algebra;

import org.eclipse.rdf4j.query.algebra.LeftJoin;

import com.fluidops.fedx.structures.QueryInfo;

/**
 * Abstraction of {@link LeftJoin} to maintain the {@link QueryInfo}.
 * 
 * @author Andreas Schwarte
 *
 */
public class FedXLeftJoin extends LeftJoin {

	private static final long serialVersionUID = 7444318847550719096L;

	protected transient final QueryInfo queryInfo;

	public FedXLeftJoin(LeftJoin leftJoin, QueryInfo queryInfo) {
		super(leftJoin.getLeftArg(), leftJoin.getRightArg(), leftJoin.getCondition());
		this.queryInfo = queryInfo;
	}

	public QueryInfo getQueryInfo() {
		return queryInfo;
	}
}
