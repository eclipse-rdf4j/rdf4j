package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.ValueFactoryTest;

public class AbstractValueFactoryTest extends ValueFactoryTest {

	@Override
	protected ValueFactory factory() {
		return new GenericValueFactory();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static class GenericValueFactory extends AbstractValueFactory {
	}

}