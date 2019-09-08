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
package com.fluidops.fedx.optimizer;

import java.util.Comparator;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.algebra.ExclusiveGroup;
import com.fluidops.fedx.algebra.ExclusiveStatement;


/**
 * Comparator:
 * 
 * partial order: OwnedStatementSourcePatternGroup -> OwnedStatementSourcePattern -> StatementSourcePattern
 * 
 * @author Andreas
 *
 */
public class NaryJoinArgumentsComparator implements Comparator<TupleExpr>{

	
	@Override
	public int compare(TupleExpr a, TupleExpr b) {

		if (a instanceof ExclusiveGroup) {
			if (b instanceof ExclusiveGroup)
				return 0;
			else
				return -1;
		}
		
		else if (b instanceof ExclusiveGroup) {
			return 1;
		}
		
		else if (a instanceof ExclusiveStatement) {
			if (b instanceof ExclusiveStatement)
				return 0;		// 0
			else
				return -1;		// -1
		}
		
		else if (b instanceof ExclusiveStatement) {
			return 1;			// 1
		}
			
		// XXX compare number of free variables
		
		return 0;
	}

}
