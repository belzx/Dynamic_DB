package com.lizhi.dynamic;


import javax.sql.DataSource;

/**
 * 动态数据源
 */
public class DataSourceProxy {

    private String id ;

    private Type type;

    private DataSource dataSource;

    public DataSourceProxy(String id, DataSource dataSource) {
        this.id = id;
        this.dataSource = dataSource;
    }

    public String getId() {
        return id;
    }

    static enum Type{

    }
}
