package com.lizhi.dynamic;


import javax.sql.DataSource;

/**
 * 作用：
 * 1、保存一个线程安全的DatabaseType容器
 */
public class DataSourceHolder  {

    private static DynamicDataSource dynamicDataSource;

    private static final ThreadLocal<String> contextHolder = new ThreadLocal(){
        @Override
        protected String initialValue() {
            return  DynamicDataSource.DEFAULT_DATASOURCE_NAME;
        }
    };

    public static void install(DynamicDataSource dynamicDataSource) {
        if (DataSourceHolder.dynamicDataSource != null) {
            throw new UnsupportedOperationException();
        }
        DataSourceHolder.dynamicDataSource = dynamicDataSource;
    }

    public static DataSource getActiveSource() {
        if (dynamicDataSource != null) {
            return dynamicDataSource.getActiveDataSource();
        }
        return dynamicDataSource.getDefaultDataSource();
    }

    public static String getActiveSourceName() {
        return contextHolder.get();
    }

    public static void setActiveSourceName(String dateSourceName){
        contextHolder.set(dateSourceName);
        String s = contextHolder.get();
    }

}