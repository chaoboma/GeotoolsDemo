package com.application.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.application.mapper")
public class MybatisPlusConfig {

    /**
     * 添加分页插件
     */
//    @Bean
//    public MybatisPlusInterceptor mybatisPlusInterceptor() {
//        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
//        interceptor.addInnerInterceptor(new PaginationInnerInterceptor()); // 如果配置多个插件, 切记分页最后添加
//        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
//        return interceptor;
//    }

}
