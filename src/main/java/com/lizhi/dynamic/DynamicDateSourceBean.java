package com.lizhi.dynamic;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class DynamicDateSourceBean implements Serializable {
    private static final long serialVersionUID = 5328848488856425388L;
    private String         name="d2";
    private String         url;
    private String         username;
    private String         testSql;
    private String         password;
    private String         driverClassName = "com.mysql.jdbc.Driver";
    private int            enabled;
    private java.util.Date createDate;
    private int maxLifetime = 0;
    private int minPoolSize = 3;
    private int maxPoolSize = 80;
    private int borrowConnectionTimeout = 60;
    private int reapTimeout = 0;
    private int maxIdleTime = 60;
    private int maintenanceInterval = 60;
    private int defaultIsolationLevel = -1;
    private int transactionTimeout = 300;
}
