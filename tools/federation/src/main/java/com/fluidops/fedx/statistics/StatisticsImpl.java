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
package com.fluidops.fedx.statistics;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

import com.fluidops.fedx.algebra.ExclusiveGroup;
import com.fluidops.fedx.algebra.StatementSource;

public class StatisticsImpl implements Statistics {

	@Override
	public int estimatedResults(Statement stmt, StatementSource source) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasResults(Statement stmt, StatementSource source) {
		return false;
	}

	@Override
	public double selectivity(StatementPattern stmt) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double selectivity(ExclusiveGroup group) {
		// TODO Auto-generated method stub
		return 0;
	}

}
