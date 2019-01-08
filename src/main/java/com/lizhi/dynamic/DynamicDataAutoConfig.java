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

    @Autowired
    private Environment env;

    public static final String BASE_PATH = "zx.dynamic.";

    public static final String NAME_TEMPLATE = BASE_PATH + "%s.jdbc.dataSourceName";

    public static final String DRIVER_TEMPLATE = BASE_PATH + "%s.jdbc.driverClassName";

    public static final String URL_TEMPLATE = BASE_PATH + "%s.jdbc.url";

    public static final String USERNAME_TEMPLATE = BASE_PATH + "%s.jdbc.username";

    public static final String PASSWORD_TEMPLATE = BASE_PATH + "%s.jdbc.password";

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

    private DynamicXaDataSourceImpl dynamicXaDataSource;

    @Primary
    @Bean(name = "dataSource", destroyMethod = "close") //在方法返回之后，init 启动init，初始化dataSource
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(@Qualifier("dynamicDataSourceService") DynamicDataSourceService dynamicDataSourceService) {
        //创建默认
        DataSource adefault = dynamicDataSourceService.createDataSourceSyn(build(DataSourceHolder.DEFAULT_DATASOURCE_NAME));
        //创建其他的数据源
        for(int i = 1 ;;i++){
            DynamicDateSourceBean build = build("silver" + i);
            if(build == null){
                break;
            }else {
                dynamicDataSourceService.createDataSourceSyn(build);
            }
        }
        dynamicXaDataSource = new DynamicXaDataSourceImpl(adefault);
        dynamicXaDataSource.setDynamicDataSourceService(dynamicDataSourceService);
        DataSourceHolder.install(dynamicXaDataSource);
        return adefault;
    }

    /**
     * dynamicDataSource 的实现类
     * @param dataSource
     * @return
     */
    @Bean(name = "dynamicDataSource")
    public DynamicXaDataSourceImpl dynamicXaDataSource(@Qualifier("dataSource") DataSource dataSource) {
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

    protected DynamicDateSourceBean build(String prefix) {
        DynamicDateSourceBean dynamicDateSourceBean = new DynamicDateSourceBean();

        if (prefix.equals(DataSourceHolder.DEFAULT_DATASOURCE_NAME)) {
            dynamicDateSourceBean.setName(DataSourceHolder.DEFAULT_DATASOURCE_NAME);
        } else {
            String datasourceName = env.getProperty(String.format(NAME_TEMPLATE, prefix));
            if (StringUtils.isNullOrEmpty(datasourceName)) {
                return null;
            } else {
                dynamicDateSourceBean.setName(datasourceName);
            }
        }

        String driverClassName = env.getProperty(String.format(DRIVER_TEMPLATE, prefix));
        String url = env.getProperty(String.format(URL_TEMPLATE, prefix));
        String userName = env.getProperty(String.format(USERNAME_TEMPLATE, prefix));
        String password = env.getProperty(String.format(PASSWORD_TEMPLATE, prefix));

        if(!StringUtils.isNullOrEmpty(driverClassName)){
            dynamicDateSourceBean.setDriverClassName(driverClassName);
        }

        if(!StringUtils.isNullOrEmpty(url)){
            dynamicDateSourceBean.setUrl(url);
        }

        if(!StringUtils.isNullOrEmpty(userName)){
            dynamicDateSourceBean.setUsername(userName);
        }

        if(!StringUtils.isNullOrEmpty(password)){
            dynamicDateSourceBean.setPassword(password);
        }
        return dynamicDateSourceBean;
    }

}
