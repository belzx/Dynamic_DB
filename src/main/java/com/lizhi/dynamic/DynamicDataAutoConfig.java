package com.lizhi.dynamic;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.mysql.jdbc.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;


@Configuration // 该注解类似于spring配置文件
@ComponentScan("com.lizhi.dynamic")
@EnableTransactionManagement
public class DynamicDataAutoConfig {

    private static Logger logger = LoggerFactory.getLogger(DynamicDataAutoConfig.class);

    public static final String BASE_PATH = "dynamic.%s.";

    /**
     * 配置默认数据库填写参数如下
     * dynamic.default.url=xxx
     * dynamic.default.username=xxx
     * ....
     */
    public static final String DYNAMIC_PROPERTIE_DEFAULT= DynamicDataSource.DEFAULT_DATASOURCE_NAME;

    /**
     * 配置动态数据库填写参数如下
     * dynamic.silver1.url=xxx
     * dynamic.silver1.username=xxx
     * ....
     */
    public static final String DYNAMIC_PROPERTIE_PRIFIX= "silver";

    @Autowired
    private Environment env;

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setVfs(SpringBootVFS.class);
        factory.setSqlSessionFactoryBuilder(new DynamicDataSourceSqlSessionFactoryBuilder());

        factory.setTransactionFactory(new SpringManagedTransactionFactory() {
            @Override
            public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
                return new DynamicSpringManagedTransaction();
            }
        });
        factory.setDataSource(dataSource);

        factory.setTypeAliasesPackage(env.getProperty("mybatis.typeAliasesPackage"));
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(env.getProperty("mybatis.mapperLocations")));

        return factory.getObject();
    }

    @Bean
    public DynamicDataSourceService dynamicDataSourceService() {
        return new DynamicDataSourceService();
    }



    @Primary
    @Bean(name = "dataSource", destroyMethod = "close") //在方法返回之后，init 启动init，初始化dataSource
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(@Qualifier("dynamicDataSourceService") DynamicDataSourceService dynamicDataSourceService) {
        //创建默认
        DataSource adefault = dynamicDataSourceService.createDataSourceSyn(build(DYNAMIC_PROPERTIE_DEFAULT));
        //创建其他的数据源
        for(int i = 1 ;;i++){
            DynamicDateSourceBean build = build(DYNAMIC_PROPERTIE_PRIFIX + i);
            if(build == null){
                break;
            }else {
               dynamicDataSourceService.createDataSourceSyn(build);
            }
        }
        return adefault;
    }

    /**
     * dynamicDataSource 的实现类
     * @param dataSource
     * @return
     */
    @Bean(name = "dynamicDataSource")
    public DynamicXaDataSource dynamicXaDataSource(@Qualifier("dataSource") DataSource dataSource,
                                                       @Qualifier("dynamicDataSourceService") DynamicDataSourceService dynamicDataSourceService) {
        DynamicXaDataSource  dynamicXaDataSource = new DynamicXaDataSource(dataSource);
        dynamicXaDataSource.setDynamicDataSourceService(dynamicDataSourceService);
        DataSourceHolder.install(dynamicXaDataSource);
        return dynamicXaDataSource;
    }


    @Bean(name = "userTransaction")
    public UserTransaction userTransaction() throws Throwable {
        UserTransactionImp userTransactionImp = new UserTransactionImp();
        userTransactionImp.setTransactionTimeout(10000);
        return userTransactionImp;
    }


    @Bean(name = "atomikosTransactionManager", initMethod = "init", destroyMethod = "close")
    public TransactionManager atomikosTransactionManager() {
        UserTransactionManager userTransactionManager = new UserTransactionManager();
        userTransactionManager.setForceShutdown(false);
        return userTransactionManager;
    }

    @Bean(name = "transactionManager")
    @DependsOn({"userTransaction", "atomikosTransactionManager"})
    public PlatformTransactionManager transactionManager() throws Throwable {
        UserTransaction userTransaction = userTransaction();
        JtaTransactionManager manager = new JtaTransactionManager(userTransaction, atomikosTransactionManager());
        return manager;
    }

    /**
     * 读取环境变量信息
     */
    protected DynamicDateSourceBean build(String prefix) {
        DynamicDateSourceBean dynamicDateSourceBean = new DynamicDateSourceBean();

        if (prefix.equals(DynamicDataSource.DEFAULT_DATASOURCE_NAME)) {
            dynamicDateSourceBean.setName(DynamicDataSource.DEFAULT_DATASOURCE_NAME);
        } else {
            String datasourceName = env.getProperty(String.format( BASE_PATH + "dataSourceName", prefix));
            if (StringUtils.isNullOrEmpty(datasourceName)) {
                return null;
            } else {
                dynamicDateSourceBean.setName(datasourceName);
            }
        }

        logger.info("Start to init datasource name is :[{}],DynamicDataSource index[{}]",dynamicDateSourceBean.getName(),prefix);

        String url = env.getProperty(String.format(BASE_PATH + "url", prefix));
        String userName = env.getProperty(String.format(BASE_PATH + "username", prefix));
        String password = env.getProperty(String.format(BASE_PATH + "password", prefix));
        String driverClassName = env.getProperty(String.format( BASE_PATH + "driverClassName", prefix));
        String testSql = env.getProperty(String.format(BASE_PATH + "testSql", prefix));
        String maxLifetime = env.getProperty(String.format(BASE_PATH + "maxLifetime", prefix));
        String minPoolSize = env.getProperty(String.format(BASE_PATH + "minPoolSize", prefix));
        String maxPoolSize = env.getProperty(String.format(BASE_PATH + "maxPoolSize", prefix));
        String borrowConnectionTimeout = env.getProperty(String.format(BASE_PATH + "borrowConnectionTimeout", prefix));
        String reapTimeout = env.getProperty(String.format(BASE_PATH + "reapTimeout", prefix));
        String maxIdleTime = env.getProperty(String.format(BASE_PATH + "maxIdleTime", prefix));
        String maintenanceInterval = env.getProperty(String.format(BASE_PATH + "maintenanceInterval", prefix));
        String defaultIsolationLevel = env.getProperty(String.format(BASE_PATH + "defaultIsolationLevel", prefix));

        if(!StringUtils.isNullOrEmpty(driverClassName)) dynamicDateSourceBean.setDriverClassName(driverClassName);
        if(!StringUtils.isNullOrEmpty(url)) dynamicDateSourceBean.setUrl(url);
        if(!StringUtils.isNullOrEmpty(userName)) dynamicDateSourceBean.setUsername(userName);
        if(!StringUtils.isNullOrEmpty(password)) dynamicDateSourceBean.setPassword(password);
        if(!StringUtils.isNullOrEmpty(testSql)) dynamicDateSourceBean.setTestSql(testSql);
        if(!StringUtils.isNullOrEmpty(maxLifetime)) dynamicDateSourceBean.setMaxLifetime(Integer.valueOf(maxLifetime));
        if(!StringUtils.isNullOrEmpty(minPoolSize)) dynamicDateSourceBean.setMinPoolSize(Integer.valueOf(minPoolSize));
        if(!StringUtils.isNullOrEmpty(maxPoolSize)) dynamicDateSourceBean.setMaxPoolSize(Integer.valueOf(maxPoolSize));
        if(!StringUtils.isNullOrEmpty(borrowConnectionTimeout)) dynamicDateSourceBean.setBorrowConnectionTimeout(Integer.valueOf(borrowConnectionTimeout));
        if(!StringUtils.isNullOrEmpty(reapTimeout)) dynamicDateSourceBean.setReapTimeout(Integer.valueOf(reapTimeout));
        if(!StringUtils.isNullOrEmpty(maxIdleTime)) dynamicDateSourceBean.setMaxIdleTime(Integer.valueOf(maxIdleTime));
        if(!StringUtils.isNullOrEmpty(maintenanceInterval)) dynamicDateSourceBean.setMaintenanceInterval(Integer.valueOf(maintenanceInterval));
        if(!StringUtils.isNullOrEmpty(defaultIsolationLevel)) dynamicDateSourceBean.setDefaultIsolationLevel(Integer.valueOf(defaultIsolationLevel));

        return dynamicDateSourceBean;
    }

}
