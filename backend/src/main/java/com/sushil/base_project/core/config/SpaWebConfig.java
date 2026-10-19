package com.sushil.base_project.core.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final List<String> BACKEND_PREFIXES =
            List.of("api/", "v3/", "swagger-ui", "actuator");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource requested = location.createRelative(path);
                        if (!path.isEmpty() && requested.exists() && requested.isReadable()) {
                            return requested;               // real file: js, css, assets
                        }
                        boolean backend = BACKEND_PREFIXES.stream().anyMatch(path::startsWith);
                        if (backend || path.contains(".")) {
                            return null;                    // genuine 404
                        }
                        return new ClassPathResource("/static/index.html"); // SPA route
                    }
                });
    }
}