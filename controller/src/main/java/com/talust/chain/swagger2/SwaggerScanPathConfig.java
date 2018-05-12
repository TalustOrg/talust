package com.talust.chain.swagger2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoubo
 * @date 2018-03-19
 * Swagger2 扫描路径配置
 */
@Configuration
public interface SwaggerScanPathConfig {
    /**
     * 扫描路径
     * @return 返回定义扫描路径
     */
    @Bean
    default List<String> scanPathName() {
        List<String> scanPackageName =new ArrayList<String>();
        scanPackageName.add("/api/.*");
        return scanPackageName;
    }

    /**
     * swagger2 扫描排除路径
     * @return 返回swagger2 扫描排除路径
     */
    @Bean
    default List<String> scanExcludePathName() {
        return null;
    }
}
