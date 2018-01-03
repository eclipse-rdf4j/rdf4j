package org.eclipse.rdf4j.sail.shacl.planNodes;

public interface SupportsDepthProvider {

	 void receiveDepthProvider(DepthProvider depthProvider);

}

interface DepthProvider{

	int depth();

}