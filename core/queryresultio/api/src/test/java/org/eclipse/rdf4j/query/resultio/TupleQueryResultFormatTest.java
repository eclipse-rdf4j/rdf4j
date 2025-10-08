package org.eclipse.rdf4j.query.resultio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TupleQueryResultFormatTest {

	@Test
	@DisplayName("CSV default MIME type should include UTF-8 charset parameter as required by spec")
	void csvMimeTypeShouldIncludeCharsetParameter() {
		assertThat(TupleQueryResultFormat.CSV.getDefaultMIMEType())
				.as("MIME type should advertise UTF-8 charset")
				.isEqualTo("text/csv;charset=UTF-8");
	}
}
