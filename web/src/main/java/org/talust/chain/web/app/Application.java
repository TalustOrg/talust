package org.talust.chain.web.app;

import org.talust.chain.client.BlockChainServer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用程序入口
 */
@ComponentScan(basePackages = {
        "org.talust.chain.**.configuration",
        "org.talust.chain.**.controller",
        "org.talust.chain.**.swagger2"})
@EnableAutoConfiguration
@EnableConfigurationProperties
@SpringBootApplication
@EnableScheduling
public class Application extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String[] args) throws Exception {
        new Application().configure(new SpringApplicationBuilder(Application.class)).run(args);
        BlockChainServer.get().initStorage();
    }

}