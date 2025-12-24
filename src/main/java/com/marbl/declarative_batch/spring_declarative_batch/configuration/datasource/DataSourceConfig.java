package com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource;

import lombok.Data;

@Data
public class DataSourceConfig {

    private boolean main = false;

    private String url;
    private String type;
    private String username;
    private String password;
    private String driverClassName;
    private IsolationLevelEnum isolationLevelEnum = IsolationLevelEnum.ISOLATION_SERIALIZABLE;


    public enum IsolationLevelEnum {
        ISOLATION_READ_UNCOMMITTED, ISOLATION_READ_COMMITTED, ISOLATION_REPEATABLE_READ, ISOLATION_SERIALIZABLE;
    }
}
