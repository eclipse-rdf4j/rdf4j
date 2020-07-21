/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

/**
 * Classes implementing this interface can accept a custom {@link TaskWrapper}.
 * 
 * @author Andreas Schwarte
 *
 */
public interface TaskWrapperAware {

	/**
	 * Set the {@link TaskWrapper} to the respective instance
	 * 
	 * @param taskWrapper
	 * @return
	 */
	public void setTaskWrapper(TaskWrapper taskWrapper);

}
