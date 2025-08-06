//package com.yren.shardingSphereDemo.config;
//
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.mybatis.spring.SqlSessionFactoryBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//
//import javax.sql.DataSource;
//
///**
// * @author ChenYu ren
// * @date 2025/7/18
// */
//
//@Configuration
//public class MyBatisConfig {
//
//    @Bean
//    public SqlSessionFactory sqlSessionFactory(DataSource shardingSphereDataSource) throws Exception {
//        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
//        factoryBean.setDataSource(shardingSphereDataSource);
//
//        // 如果有 XML 文件
//        factoryBean.setMapperLocations(
//                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml")
//        );
//
//        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
//        configuration.setMapUnderscoreToCamelCase(true);
//        factoryBean.setConfiguration(configuration);
//
//        return factoryBean.getObject();
//    }
//}
