package org.talust.chain.web.configuration;

import org.talust.chain.swagger2.SwaggerScanPackageConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;


import java.util.ArrayList;
import java.util.List;

/**
 * 系统框架swagger扫描配置参数
 *
 * @date 2018-03-19
 */
@Configuration
@Order(1)
public class FrameworkSwaggerScanConfig implements SwaggerScanPackageConfig {

    @Override
    @Bean
    public List<String> scanPackageName() {
        List<String> scanPackageName = new ArrayList<>();
        scanPackageName.add("com.talust");
        return scanPackageName;
    }
}
