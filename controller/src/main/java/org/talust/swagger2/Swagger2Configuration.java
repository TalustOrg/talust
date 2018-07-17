/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.talust.swagger2;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.async.DeferredResult;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Swagger2自动配置
 *
 */
@Configuration
@EnableSwagger2
@Profile(value = {"dev", "test"})
//@PropertySource(ignoreResourceNotFound = true, value = {"classpath:api.properties"}, encoding = "UTF-8")
public class Swagger2Configuration {

    /**
     * 需要扫描接口包
     */
    @Autowired
    List<String> scanPackageName;

    /**
     * 需要扫描排除Path
     */
    @Autowired
    List<String> scanExcludePathName;

    /**
     * 需要扫描Path
     */
    @Autowired
    List<String> scanPathName;

    @Bean
    public Docket createRestApi() {
        List<Predicate<RequestHandler>> basePackages = new LinkedList<>();
        if (null != scanPackageName) {
            for (String resourcePackage : scanPackageName) {
                basePackages.add(RequestHandlerSelectors.basePackage(resourcePackage));
            }
        }

        List<Predicate<String>> basePaths = new ArrayList<>();
        basePaths.add(PathSelectors.any());

        for (String basePath : scanPathName) {
            basePaths.add(PathSelectors.ant(basePath));
        }
        List<Predicate<String>> excludePaths = new ArrayList<>();
        if (null != scanExcludePathName) {
            for (String excludePath : scanExcludePathName) {
                excludePaths.add(PathSelectors.ant(excludePath));
            }
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("api")
                .apiInfo(apiInfo())
                .genericModelSubstitutes(DeferredResult.class)
                .forCodeGeneration(true)
                .enable(true)
                .select()
                .apis(Predicates.or(basePackages))
                .paths(Predicates.and(Predicates.not(Predicates.or(excludePaths)), Predicates.or(basePaths)))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("基于Swagger2区块链接口")
                .description("API接口")
                //.contact(new Contact(contact_name,contact_url,contact_email))
                .version("v1.0")
                .build();
    }
}
