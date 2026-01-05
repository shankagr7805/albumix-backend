package com.shank.AlbumsAPI.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.*;

@Configuration
@OpenAPIDefinition(
    info =@Info(
        title = "Demo API",
        version = "Version 1.0",
        contact = @Contact(
            name = "Shank" , email = "crazyshank07@gmail.com" , url = "https://github.com/shankagr7805"
        ),
        license = @License(
            name = "Apache 2.0" , url = "http://www.apache.org/licenses/LICENSE-2.0"
        ),
        termsOfService = "https://docs.github.com/en/site-policy/github-terms/github-terms-of-service",
        description = "Spring boot restful API demo"
    )
)
public class SwaggerConfig {
    
}
