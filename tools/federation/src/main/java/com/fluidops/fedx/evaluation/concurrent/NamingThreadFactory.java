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
package com.fluidops.fedx.evaluation.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for named threads
 */
public class NamingThreadFactory implements ThreadFactory
{
	private final AtomicInteger nextThreadId = new AtomicInteger();

	private final String baseName;

	public NamingThreadFactory(String baseName)
	{
		super();
		this.baseName = baseName;
	}

	@Override
	public Thread newThread(Runnable r)
	{
		Thread t = Executors.defaultThreadFactory().newThread(r);
		t.setName(baseName + "-" + nextThreadId.incrementAndGet());
		return t;
	}

}
