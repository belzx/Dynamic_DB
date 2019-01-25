package com.lizhi.dynamic;

import com.atomikos.jdbc.AtomikosSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jta.atomikos.AtomikosDataSourceBean;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1：维护cache datasourceName ：datasource
 * 2：创建datasource
 * 3：动态创建datasource
 */
public class DynamicDataSourceService {

    private static Logger logger = LoggerFactory.getLogger(DynamicDataSourceService.class);

    //用来维护缓存的 数据源名称  ： 数据源信息
    private static Map<String, CacheInfo> cache = new ConcurrentHashMap<>();

    protected CacheInfo getCache(String datasourceName) {
        return cache.get(datasourceName);
    }

    public void destroyAll() {
        cache.values().stream().map(CacheInfo::getDataSource).forEach(this::closeDataSource);
        cache.clear();
    }

    public DataSource getDataSource(String dataSourceName) {
        return getCache(dataSourceName).getDataSource();
    }

    /**
     * 同步创建数据源，适用于数据源初始化
     *
     * @return
     */
    public DataSource createDataSourceSyn(DynamicDateSourceBean dynamicDateSourceBean) {
        try {
            return initDataSource(dynamicDateSourceBean);
        } catch (Exception e) {
            logger.error("创建数据源失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化数据源
     */
    private DataSource initDataSource(DynamicDateSourceBean dynamicDateSourceBean) {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
        ds.setXaDataSourceClassName("com.alibaba.druid.pool.xa.DruidXADataSource");
        ds.setUniqueResourceName(dynamicDateSourceBean.getName());
        //配置账号密码
        ds.setXaProperties(getProperties(dynamicDateSourceBean));
        //配置其它参数
        ds.setMaxLifetime(dynamicDateSourceBean.getMaxLifetime());
        ds.setMinPoolSize(dynamicDateSourceBean.getMinPoolSize());
        ds.setMaxPoolSize(dynamicDateSourceBean.getMaxPoolSize());
        ds.setBorrowConnectionTimeout(dynamicDateSourceBean.getBorrowConnectionTimeout());
        ds.setReapTimeout(dynamicDateSourceBean.getReapTimeout());
        ds.setMaxIdleTime(dynamicDateSourceBean.getMaxIdleTime());
        ds.setMaintenanceInterval(dynamicDateSourceBean.getMaintenanceInterval());
        ds.setDefaultIsolationLevel(dynamicDateSourceBean.getDefaultIsolationLevel());

        try {
            ds.init();
            //添加至缓存中
            cache.put(dynamicDateSourceBean.getName(), new CacheInfo(dynamicDateSourceBean.getName(), ds));
        } catch (AtomikosSQLException e) {
            closeDataSource(ds);
            throw new RuntimeException(e);
        }
        return ds;
    }

    /**
     * 30s 之内没有创建好数据源，则看作超时
     */
    public DataSource createDataSourceAyn(DynamicDateSourceBean dynamicDateSourceBean) {
        //0 创建中 1创建失败 2 创建成功
        final AtomicInteger success = new AtomicInteger(0);
        new Thread(() -> {
            try {
                initDataSource(dynamicDateSourceBean);
                success.set(2);
            } catch (Exception e) {
                success.set(1);
                logger.error("创建数据源失败", e);
            }
        }).start();

        Long startTime = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - startTime < 30 * 1000) {
                if (success.get() == 0) {
                    Thread.sleep(100);
                } else if (success.get() == 1) {
                    throw new RuntimeException("创建数据源失败");
                } else {
                    return getDataSource(dynamicDateSourceBean.getName());
                }
            }
        } catch (InterruptedException e) {
        }

        throw new RuntimeException("创建数据源超时");
    }


    protected void closeDataSource(javax.sql.CommonDataSource ds) {
        if (ds instanceof com.atomikos.jdbc.AtomikosDataSourceBean) {
            closeDataSource(((com.atomikos.jdbc.AtomikosDataSourceBean) ds).getXaDataSource());
            ((com.atomikos.jdbc.AtomikosDataSourceBean) ds).close();
        } else if (ds instanceof Closeable) {
            try {
                ((Closeable) ds).close();
            } catch (IOException e) {
                logger.error("close datasource error", e);
            }
        }
    }

    /**
     * 默认参数配置
     */
    public Properties getProperties(DynamicDateSourceBean dynamicDateSourceBean) {
        return DataSourcePropertiesUtil.getProperties(dynamicDateSourceBean);
    }

    static class DataSourcePropertiesUtil {

        public static Properties getProperties(DynamicDateSourceBean dynamicDateSourceBean) {
            Properties props = new Properties();
            props.put("driverClassName", dynamicDateSourceBean.getDriverClassName());
            props.put("url", dynamicDateSourceBean.getUrl());
            props.put("username", dynamicDateSourceBean.getUsername());
            props.put("password", dynamicDateSourceBean.getPassword());
//            props.put("transactionTimeout", dynamicDateSourceBean.getTransactionTimeout());
            return props;
        }
    }


    /**
     * 数据源缓存的信息
     */
    class CacheInfo {

        String dataSourceName;

        /**
         * 暂时不适用，后续扩展
         */
        Type datasourceType = Type.mysql;

        DataSource dataSource;

        public CacheInfo(String dataSourceName, DataSource dataSource) {
            this.dataSourceName = dataSourceName;
            this.dataSource = dataSource;
        }

        public String getDataSourceName() {
            return dataSourceName;
        }

        public DataSource getDataSource() {
            return dataSource;
        }

    }

    enum Type {
        mysql;
    }

}