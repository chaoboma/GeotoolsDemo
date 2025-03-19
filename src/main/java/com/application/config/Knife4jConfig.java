package com.application.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//@EnableKnife4j
public class Knife4jConfig {
    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("接口文档")
                        .description("接口文档")
                        .version("1.0")
                        .contact(new Contact()
                                .name("m")
                                .email("123@qq.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("springboot基础框架")
                        .url("http://localhost:8080"));

    }
//    下面是分组

    @Bean
    public GroupedOpenApi groupedOpenApi2(){
        return GroupedOpenApi.builder()
                .group("文件操作")
                .pathsToMatch("/file/**")
                .packagesToScan("com.application.controller")
                .build();
    }

}

