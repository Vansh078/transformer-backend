package com.smart.transformer.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerGroupConfig {

	@Bean
	public GroupedOpenApi transformerApi() {
	    return GroupedOpenApi.builder()
	            .group("transformers")
	            .pathsToMatch("/api/v1/transformers/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi sensorApi() {
	    return GroupedOpenApi.builder()
	            .group("sensor-readings")
	            .pathsToMatch("/api/v1/readings/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi alertApi() {
	    return GroupedOpenApi.builder()
	            .group("alerts")
	            .pathsToMatch("/api/v1/alerts/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi deviceApi() {
	    return GroupedOpenApi.builder()
	            .group("devices")
	            .pathsToMatch("/api/v1/devices/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi maintenanceApi() {
	    return GroupedOpenApi.builder()
	            .group("maintenance")
	            .pathsToMatch("/api/v1/maintenance/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi decisionSupportApi() {
	    return GroupedOpenApi.builder()
	            .group("decision-support")
	            .pathsToMatch("/api/v1/decision-support/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi chatApi() {
	    return GroupedOpenApi.builder()
	            .group("ai-assistant")
	            .pathsToMatch("/api/v1/chat/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi reportApi() {
	    return GroupedOpenApi.builder()
	            .group("reports")
	            .pathsToMatch("/api/v1/reports/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi searchApi() {
	    return GroupedOpenApi.builder()
	            .group("search")
	            .pathsToMatch("/api/v1/search/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi activityLogApi() {
	    return GroupedOpenApi.builder()
	            .group("activity-logs")
	            .pathsToMatch("/api/v1/activity-logs/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi userApi() {
	    return GroupedOpenApi.builder()
	            .group("users")
	            .pathsToMatch("/api/v1/users/**")
	            .build();
	}

	@Bean
	public GroupedOpenApi authApi() {
	    return GroupedOpenApi.builder()
	            .group("auth")
	            .pathsToMatch("/api/v1/auth/**")
	            .build();
	}

}