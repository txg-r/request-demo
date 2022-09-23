package com.example.requestdemo.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@EnableOpenApi
@Configuration
public class SwaggerConfig {


    @Bean
    public Docket createRestApi(){
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("接口文档")
                .description("接口文档")
                .version("1.0")
                .contact(new Contact("tyfff", "", "tanxiaogang2020@outlook.com"))
//                .license("")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
/*                .groupName("1")*/
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example.requestdemo.controller"))
                .paths(PathSelectors.any())
                .build();
    }
}