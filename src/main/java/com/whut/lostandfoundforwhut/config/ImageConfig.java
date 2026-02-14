package com.whut.lostandfoundforwhut.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageConfig implements WebMvcConfigurer {

    @Value("${app.local-image.enabled:false}")
    private boolean localImageEnabled;

    @Value("${app.local-image.location:file:/}")
    private String localImageLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!localImageEnabled) {
            return;
        }

        String location = normalizeLocalImageLocation(localImageLocation);
        registry.addResourceHandler("/dev-local-images/**")
                .addResourceLocations(location);
    }

    private String normalizeLocalImageLocation(String location) {
        String resolved = StringUtils.hasText(location) ? location.trim() : "file:/";
        if (!resolved.startsWith("file:")) {
            resolved = "file:" + resolved;
        }
        if (!resolved.endsWith("/")) {
            resolved = resolved + "/";
        }
        return resolved;
    }
}
