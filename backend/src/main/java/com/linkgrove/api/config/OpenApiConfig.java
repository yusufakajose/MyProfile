package com.linkgrove.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI linkgroveOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LinkGrove API")
                        .description("REST API for LinkGrove")
                        .version("v1")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .contact(new Contact().name("LinkGrove").url("https://example.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/yusufakajose/MyProfile"));
    }
}


