package org.eclipse.rdf4j.spring.repository.remote;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdf4j.spring.repository.remote")
public class RemoteRepositoryProperties {
	/** URL of the SPARQL endpoint */
	@NotBlank
	@Pattern(regexp = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
	private String managerUrl = null;
	/** Name of the repository */
	@NotBlank
	@Length(min = 1)
	private String name = null;

	public String getManagerUrl() {
		return managerUrl;
	}

	public void setManagerUrl(String managerUrl) {
		this.managerUrl = managerUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "RemoteRepositoryConfig{"
				+ "managerUrl='"
				+ managerUrl
				+ '\''
				+ ", name='"
				+ name
				+ '\''
				+ '}';
	}
}
