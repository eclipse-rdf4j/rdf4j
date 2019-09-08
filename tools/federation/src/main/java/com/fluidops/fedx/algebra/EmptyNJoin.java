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
package com.fluidops.fedx.algebra;

import com.fluidops.fedx.structures.QueryInfo;


/**
 * Algebra construct representing an empty join.
 * 
 * @author Andreas Schwarte
 *
 */
public class EmptyNJoin extends NTuple implements EmptyResult{

	private static final long serialVersionUID = -3895999439111284174L;

	public EmptyNJoin(NJoin njoin, QueryInfo queryInfo) {
		super(njoin.getArgs(), queryInfo);
	}

}
