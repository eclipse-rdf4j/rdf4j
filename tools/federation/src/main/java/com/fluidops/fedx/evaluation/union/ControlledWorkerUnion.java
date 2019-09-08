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
package com.fluidops.fedx.evaluation.union;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ControlledWorkerScheduler;
import com.fluidops.fedx.structures.QueryInfo;

/**
 * Execution of union tasks with {@link ControlledWorkerScheduler}. Tasks can be added
 * using the provided functions. Note that the union operation is to be executed
 * with the {@link #run()} method (also threaded execution is possible). Results are
 * then contained in this iteration.
 *
 * @author Andreas Schwarte
 *
 */
public class ControlledWorkerUnion<T> extends WorkerUnionBase<T> {

	public static int waitingCount = 0;
	public static int awakeCount = 0;
	
	protected final ControlledWorkerScheduler<T> scheduler;
	
	protected final Phaser phaser = new Phaser(1);

	public ControlledWorkerUnion(FederationEvalStrategy strategy, ControlledWorkerScheduler<T> scheduler,
			QueryInfo queryInfo) {
		super(strategy, queryInfo);
		this.scheduler = scheduler;
	}
			
	
	@Override
	protected void union() throws Exception {
		
		// schedule all tasks and inform about finish
		phaser.bulkRegister(tasks.size());
		scheduler.scheduleAll(tasks, this);
		
		// wait until all tasks are executed
		phaser.awaitAdvanceInterruptibly(phaser.arrive(), queryInfo.getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);
	}

	@Override
	public void done()
	{
		super.done();
		phaser.arriveAndDeregister();
	}

	@Override
	public void toss(Exception e)
	{
		super.toss(e);
		phaser.arriveAndDeregister();
	}
}
