package com.ico.ltd.cache.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class PersistenceConfig {

    @Bean
    LocalSessionFactoryBean entityManagerFactory() {
        LocalSessionFactoryBean emf =
                new LocalSessionFactoryBean();
        emf.setPackagesToScan("com.ico.ltd.cache.domain");
        emf.setDataSource(createDataSource());
        emf.setHibernateProperties(createHibernateProperties());
        return emf;
    }

    private DataSource createDataSource() {
        EmbeddedDatabaseBuilder builder =
                new EmbeddedDatabaseBuilder();
        return builder.setType(EmbeddedDatabaseType.H2).build();
    }

    private Properties createHibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty(
                "hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");

        cacheProperties(properties);

        return properties;
    }

    private void cacheProperties(Properties properties) {
        /*
            The shared cache mode controls how entity classes of this persistence
             unit become cacheable. Usually you prefer to enable caching selectively
             for only some entity classes. Other options are
             <code>DISABLE_SELECTIVE</code>, <code>ALL</code>, and <code>NONE</code>.
         */
        properties.setProperty("javax.persistence.sharedCache.mode", "ENABLE_SELECTIVE");

        /*
            Hibernate’s second-level cache system has to be enabled explicitly; it isn’t enabled by
            default. You can separately enable the query result cache; it’s disabled by default as
            well.
         */
        properties.setProperty("hibernate.cache.use_second_level_cache", "true");
        properties.setProperty("hibernate.cache.use_query_cache", "true");


        /*
            Next, you pick a provider for the second-level cache system. For EHCache,
            add the <code>org.hibernate:hibernate-ehcache</code> Maven artifact
            dependency to your classpath. Then, pick how Hibernate uses EHCache with this
            region factory setting; here we tell Hibernate to manage a single
            EHCache instance internally as the second-level cache provider.
         */
        properties.setProperty("hibernate.cache.region.factory_class",
                "org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory");

        /*
            Hibernate will pass this property along to EHCache when the provider
            is started, setting the location of the EHCache configuration file. All
            physical cache settings for cache regions are in this file.
         */
        properties.setProperty("net.sf.ehcache.configurationResourceName",
                "/cache/ehcache.xml");

        /*
            This setting controls how Hibernate disassembles and assembles
            entity state when data is stored and loaded from the second-level
            cache. The "structured" cache entry format is less efficient but
            necessary in a clustered environment. For a non-clustered
            second-level cache, like our singleton EHCache on this JVM, you
            can disable this setting and a more efficient format is used.
         */
        properties.setProperty("hibernate.cache.use_structured_entries", "false");

        /*
            While you experiment with the second-level cache, you usually
            want to see and examine what's happening behind the scenes. Hibernate
            has a statistics collector and an API to access these statistics. For
            performance reasons, it is disabled by default (and should be
            disabled in production).
         */
        properties.setProperty("hibernate.generate_statistics", "true");
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

}
