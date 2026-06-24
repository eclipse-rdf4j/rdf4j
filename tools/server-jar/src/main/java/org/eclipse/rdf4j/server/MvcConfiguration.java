package org.eclipse.rdf4j.server;

import java.util.List;

import org.eclipse.rdf4j.http.server.ProtocolExceptionResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfiguration implements WebMvcConfigurer {
	@Override
	public void configureHandlerExceptionResolvers(@NonNull final List<HandlerExceptionResolver> resolvers) {
		resolvers.add(new ProtocolExceptionResolver());
	}
}
