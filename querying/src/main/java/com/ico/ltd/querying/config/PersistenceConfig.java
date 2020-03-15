package com.ico.ltd.querying.config;

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

    public static final String[] MAPPING_FILES = new String[]{
            "querying/ExternalizedQueries.xml",
            "querying/ExternalizedQueries.hbm.xml",
            "querying/NativeQueries.xml",
            "querying/SQLQueries.hbm.xml"
    };

    @Bean
    LocalSessionFactoryBean entityManagerFactory() {
        LocalSessionFactoryBean emf =
                new LocalSessionFactoryBean();
        emf.setPackagesToScan("com.ico.ltd.querying.domain");
        emf.setMappingResources(MAPPING_FILES);
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
        return properties;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

}
