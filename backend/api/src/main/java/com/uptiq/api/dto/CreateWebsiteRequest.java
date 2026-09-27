package com.uptiq.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateWebsiteRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(
            regexp = "https?://.+",
            message = "URL must start with http:// or https://"
    )
    private String url;

    @NotBlank
    private String slug;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}