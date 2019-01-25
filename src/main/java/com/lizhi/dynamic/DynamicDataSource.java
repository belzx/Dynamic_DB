package com.lizhi.dynamic;/*

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * 动态数据源接口,此接口实现多数据源的动态切换
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public interface DynamicDataSource extends DataSource {

    /**默认数据源的名称*/
    String DEFAULT_DATASOURCE_NAME = "default";

    Logger logger = LoggerFactory.getLogger(DynamicDataSource.class);

    /**
     * 选中参数(数据源ID)对应的数据源,如果数据源不存在,将使用默认数据源
     * <p>
     * //     * @param dataSourceId 数据源名称
     */
    static void use(String dataSourceName) {
        if(logger.isInfoEnabled()){
            logger.info("use datasource:[{}]", dataSourceName);
        }
        DataSourceHolder.setActiveSourceName(dataSourceName);
    }

    /**
     * 获取当前使用的数据源ID,如果不存在则返回默认的数据源名称
     * 即 default
     *
     * @return 数据源ID
     */
    static String getActiveDataSourceId() {
        return DataSourceHolder.getActiveSourceName();
    }

    static void useDefault() {
        use(DEFAULT_DATASOURCE_NAME);
    }

    /**
     * 获取当前激活的数据源
     *
     * @return
     */
    DataSource getActiveDataSource();

    DataSource getDefaultDataSource();

}
