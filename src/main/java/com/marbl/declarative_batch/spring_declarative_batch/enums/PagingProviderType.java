package com.marbl.declarative_batch.spring_declarative_batch.enums;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.reader.JdbcPagingReaderConfig;
import com.marbl.declarative_batch.spring_declarative_batch.model.SortKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.*;

import java.util.stream.Collectors;

public enum PagingProviderType {

    POSTGRESQL {
        @Override
        public PagingQueryProvider create(JdbcPagingReaderConfig config) {
            PostgresPagingQueryProvider provider = new PostgresPagingQueryProvider();
            setupProvider(provider, config);
            return provider;
        }
    },
    ORACLE {
        @Override
        public PagingQueryProvider create(JdbcPagingReaderConfig config) {
            OraclePagingQueryProvider provider = new OraclePagingQueryProvider();
            setupProvider(provider, config);
            return provider;
        }
    },
    MYSQL {
        @Override
        public PagingQueryProvider create(JdbcPagingReaderConfig config) {
            MySqlPagingQueryProvider provider = new MySqlPagingQueryProvider();
            setupProvider(provider, config);
            return provider;
        }
    },
    MARIADB {
        @Override
        public PagingQueryProvider create(JdbcPagingReaderConfig config) {
            // MariaDB usa lo stesso provider di MySQL
            MySqlPagingQueryProvider provider = new MySqlPagingQueryProvider();
            setupProvider(provider, config);
            return provider;
        }
    },
    SQLSERVER {
        @Override
        public PagingQueryProvider create(JdbcPagingReaderConfig config) {
            SqlServerPagingQueryProvider provider = new SqlServerPagingQueryProvider();
            setupProvider(provider, config);
            return provider;
        }
    },
    DB2 {
        @Override
        public PagingQueryProvider create(JdbcPagingReaderConfig config) {
            Db2PagingQueryProvider provider = new Db2PagingQueryProvider();
            setupProvider(provider, config);
            return provider;
        }
    },
    H2 {
        @Override
        public PagingQueryProvider create(JdbcPagingReaderConfig config) {
            H2PagingQueryProvider provider = new H2PagingQueryProvider();
            setupProvider(provider, config);
            return provider;
        }
    };

    private static final Logger log = LoggerFactory.getLogger(PagingProviderType.class);

    public abstract PagingQueryProvider create(JdbcPagingReaderConfig config);

    //Helper method used to configure select/from/where and sortKeys
    protected void setupProvider(PagingQueryProvider provider, JdbcPagingReaderConfig config) {
        if (provider instanceof AbstractSqlPagingQueryProvider sqlProvider) {
            sqlProvider.setSelectClause(config.getClause().getSelectClause());
            sqlProvider.setFromClause(config.getClause().getFromClause());
            sqlProvider.setWhereClause(config.getClause().getWhereClause());
            sqlProvider.setGroupClause(config.getClause().getGroupByClause());
            sqlProvider.setSortKeys(config.getClause().getSortClause().stream()
                    .collect(Collectors.toMap(
                            SortKey::getKey,
                            SortKey::getOrder
                    )));
            log.info("Slq provider: {}", sqlProvider);
        } else {
            throw new IllegalStateException(
                    "Provider class not supported for automatic configuration: " + provider.getClass().getName()
            );
        }
    }

}
