package com.sanfosys.app.config;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import javax.servlet.Filter;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import com.atlassian.oai.validator.springmvc.SpringMVCLevelResolverFactory;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.util.ReferenceSerializationConfigurer;
import springfox.documentation.spring.web.json.JacksonModuleRegistrar;

@Configuration
public class OpenApiValidationConfig implements WebMvcConfigurer, JacksonModuleRegistrar {

	private final OpenApiValidationInterceptor validationInterceptor;

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	/**
	 * @param apiSpecification the {@link Resource} to your OpenAPI / Swagger schema
	 */
	@Autowired
	public OpenApiValidationConfig(@Value("classpath:swagger.json") final Resource apiSpecification)
			throws IOException {
		final EncodedResource specResource = new EncodedResource(apiSpecification, "UTF-8");
		// this.validationInterceptor = new OpenApiValidationInterceptor(specResource);
		this.validationInterceptor = new OpenApiValidationInterceptor(getOpenApiInteractionValidator(specResource));
	}

	@Bean
	public Filter validationFilter() {
		return new OpenApiValidationFilter(true, // enable request validation
				true // enable response validation
		);
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(validationInterceptor);
	}

	/**
	 * Added this due to OpenApiValidationInterceptor does not like "OriginalRef"
	 * https://github.com/springfox/springfox/issues/2917
	 */
	@Override
	public void maybeRegisterModule(ObjectMapper objectMapper) {
		ReferenceSerializationConfigurer.serializeAsComputedRef(objectMapper);
	}

	private static OpenApiInteractionValidator getOpenApiInteractionValidator(final EncodedResource specResource)
			throws IOException {
		return OpenApiInteractionValidator.createForInlineApiSpecification(readReader(specResource.getReader(), -1))
				.withLevelResolver(SpringMVCLevelResolverFactory.create()).withWhitelist(getWhitelist()).build();
	}

	private static ValidationErrorsWhitelist getWhitelist() {
		return ValidationErrorsWhitelist.create()
				.withRule("Ignore explicit null fields in request payload",
						WhitelistRules.allOf(WhitelistRules.messageHasKey("validation.request.body.schema.type"),
								WhitelistRules.messageContains(
										"Instance type \\(null\\) does not match any allowed primitive type")))
				.withRule("Ignore explicit null fields in response payload",
						WhitelistRules.allOf(WhitelistRules.messageHasKey("validation.response.body.schema.type"),
								WhitelistRules.messageContains(
										"Instance type \\(null\\) does not match any allowed primitive type")));
	}

	private static String readReader(final Reader reader, final int length) throws IOException {
		try (Reader reassignedReader = reader) {
			final StringWriter writer = length > 0 ? new StringWriter(length) : new StringWriter();
			IOUtils.copy(reassignedReader, writer);
			return writer.toString();
		}
	}
}
