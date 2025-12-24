package com.marbl.declarative_batch.spring_declarative_batch.utils;


import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@UtilityClass
public class DatasourceUtils {


    public DataSource getDataSource(ApplicationContext context, String dataSourceName) {
        @SuppressWarnings("unchecked")
        Map<String, DataSource> dataSources = context.getBean("dataSources", Map.class);
        DataSource ds = dataSources.get(dataSourceName);
        if (ds == null) {
            throw new IllegalArgumentException("Datasource not found in map: " + dataSourceName);
        }
        return ds;
    }
}
