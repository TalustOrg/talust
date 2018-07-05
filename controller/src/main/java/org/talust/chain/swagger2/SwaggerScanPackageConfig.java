package org.talust.chain.swagger2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoubo
 * @date 2018-03-19
 */
@Configuration
public interface SwaggerScanPackageConfig {

    /**
     * swagger2 扫描包名称列表
     * @return 返回swagger2 扫描包名称列表
     */
    @Bean
    default List<String> scanPackageName() {
        List<String> scanPackageName = new ArrayList<String>();
        scanPackageName.add("org.talust");
        return scanPackageName;
    }
}
