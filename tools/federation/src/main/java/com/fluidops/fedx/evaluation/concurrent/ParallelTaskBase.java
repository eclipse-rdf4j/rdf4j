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
package com.fluidops.fedx.evaluation.concurrent;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParallelTaskBase<T> implements ParallelTask<T> {

	private static final Logger _log = LoggerFactory.getLogger(ParallelExecutorBase.class);

	protected Future<?> scheduledFuture;

	@Override
	public void cancel() {
		if (scheduledFuture != null) {
			if (scheduledFuture.isDone()) {
				_log.trace("Task is already done: " + toString());
			}
			else {
				_log.debug("Attempting to cancel task " + toString());
				boolean successfullyCanceled = scheduledFuture.cancel(true);
				if (!successfullyCanceled) {
					_log.debug("Task " + toString() + " could not be cancelled properly.");
				}
			}
		}
	}

	public void setScheduledFuture(Future<?> future) {
		this.scheduledFuture = future;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (Query: " + getQueryInfo().getQueryID() + ")";
	}
}
