package de.werum.csgrs.nativeapi.config;

import java.io.File;

import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.operation.validator.validation.RequestValidator;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiRequestValidatorConfiguration {

	@Value("${openapi.definition-file}")
    private String openapiDefinitionFile;
	
	@Bean
	public RequestValidator getRequestValidator() throws ResolutionException, ValidationException {
		final OpenApi3 openApi = new OpenApi3Parser().parse(new File(this.openapiDefinitionFile), true);
		return new RequestValidator(openApi);
	}
}
