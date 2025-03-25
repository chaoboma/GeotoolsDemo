//package com.application.config;
//
//import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
//import org.springframework.boot.web.server.WebServerFactoryCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class TomcatConfig {
//
//    @Bean
//    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
//        return factory -> {
//            factory.addConnectorCustomizers(connector -> {
//                // 设置协议为NIO
//                connector.setProperty("protocol", "org.apache.coyote.http11.Http11NioProtocol");
//                // 设置使用直接内存
//                connector.setProperty("useDirectBuffers", "true");
//            });
//        };
//    }
//}
