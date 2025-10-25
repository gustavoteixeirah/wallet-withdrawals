package com.teixeirah.withdrawals.infrastructure.secondary.persistence;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource writeDataSource(
            @Value("${spring.datasource.write.url}") String url,
            @Value("${spring.datasource.write.username}") String username,
            @Value("${spring.datasource.write.password}") String password,
            @Value("${spring.datasource.write.driver-class-name}") String driverClassName) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean
    public DataSource readDataSource(
            @Value("${spring.datasource.read.url}") String url,
            @Value("${spring.datasource.read.username}") String username,
            @Value("${spring.datasource.read.password}") String password,
            @Value("${spring.datasource.read.driver-class-name}") String driverClassName) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean
    public DSLContext writeDsl(@Qualifier("writeDataSource") DataSource dataSource) {
        Settings settings = new Settings()
                .withRenderNameCase(RenderNameCase.LOWER)
                .withRenderQuotedNames(RenderQuotedNames.NEVER);
        return new DefaultDSLContext(new DefaultConfiguration()
                .set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)))
                .set(SQLDialect.POSTGRES)
                .set(settings));
    }

    @Bean
    public DSLContext readDsl(@Qualifier("readDataSource") DataSource dataSource) {
        Settings settings = new Settings()
                .withRenderNameCase(RenderNameCase.LOWER)
                .withRenderQuotedNames(RenderQuotedNames.NEVER)
                .withMapRecordComponentParameterNames(true)
                .withMapConstructorParameterNames(true);
        return new DefaultDSLContext(new DefaultConfiguration()
                .set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)))
                .set(SQLDialect.POSTGRES)
                .set(settings));
    }

}