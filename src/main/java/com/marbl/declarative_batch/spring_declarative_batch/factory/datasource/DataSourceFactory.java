package com.marbl.declarative_batch.spring_declarative_batch.factory.datasource;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource.DataSourceConfig;
import com.marbl.declarative_batch.spring_declarative_batch.exception.BatchException;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Slf4j
public class DataSourceFactory {

    public DataSource create(String name, DataSourceConfig cfg) {
        if (cfg == null || cfg.getType() == null) {
            log.error("Cannot create datasource: configuration or type is null for name '{}'", name);
            throw new IllegalArgumentException("DataSource type must be provided");
        }

        log.info("Creating datasource '{}' of type '{}'", name, cfg.getType());

        return switch (cfg.getType().toLowerCase()) {
            case "postgres" -> buildHikari(name, cfg);
            case "oracle" -> buildOracle(name, cfg);
            case "h2" -> buildH2(name, cfg);
            default -> {
                log.error("Unsupported datasource type '{}' for name '{}'", cfg.getType(), name);
                throw new IllegalArgumentException("Unsupported datasource type: " + cfg.getType());
            }
        };
    }

    private DataSource buildHikari(String name, DataSourceConfig cfg) {
        log.debug("Initializing HikariDataSource '{}' with URL '{}'", name, cfg.getUrl());
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(cfg.getUrl());
        ds.setUsername(cfg.getUsername());
        ds.setPassword(cfg.getPassword());
        ds.setDriverClassName(cfg.getDriverClassName());
        ds.setPoolName(name);
        log.info("HikariDataSource '{}' created successfully", name);
        return ds;
    }

    private DataSource buildOracle(String name, DataSourceConfig cfg) {
        log.debug("Initializing Oracle PoolDataSource '{}' with URL '{}'", name, cfg.getUrl());
        try {
            PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
            pds.setURL(cfg.getUrl());
            pds.setUser(cfg.getUsername());
            pds.setPassword(cfg.getPassword());
            pds.setConnectionFactoryClassName(cfg.getDriverClassName());
            pds.setConnectionPoolName(name);
            log.info("Oracle PoolDataSource '{}' created successfully", name);
            return pds;
        } catch (SQLException e) {
            log.error("Failed to create Oracle PoolDataSource '{}': {}", name, e.getMessage(), e);
            throw new BatchException("Failed to create Oracle PoolDataSource: " + name, e);
        }
    }

    private DataSource buildH2(String name, DataSourceConfig cfg) {
        log.debug("Initializing H2 DataSource '{}' with URL '{}'", name, cfg.getUrl());
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(cfg.getUrl());
        ds.setUser(cfg.getUsername());
        ds.setPassword(cfg.getPassword());
        log.info("H2 DataSource '{}' created successfully", name);
        return ds;
    }
}
