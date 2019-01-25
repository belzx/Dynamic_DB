## 开源组件
Atomikos  是一个为Java平台提供增值服务的并且开源类事务管理器。...... Atomikos 是一个为Java平台提供增值服务的并且开源类事务管理器
ps：以上来自百度

### 详细可以参考 https://gitee.com/hsweb/hsweb-framework/blob/master/hsweb-datasource

## 使用方式
~~~
集合mybatis  mysql框架使用
~~~
#### 至于利用aop切换数据源的话，则需要自己去配置了~~~

####  配置
~~~
mybatis.typeAliasesPackage=com.lizhi.bean
mybatis.mapperLocations=classpath:mapper/*.xml

spring.jta.atomikos.properties.log-base-dir=  ../logs
spring.jta.transaction-manager-id= txManager

#the default dynamic
dynamic.default.url=jdbc:mysql://xxx/lzx-blog?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true
dynamic.default.username=root
dynamic.default.password=123456

#the first dynamic
dynamic.silver1.dataSourceName=t1
dynamic.silver1.url=jdbc:mysql://xxx:3306/test1?useUnicode=true&characterEncoding=utf-8
dynamic.silver1.username=root
dynamic.silver1.password=121929

#the second dynamic
dynamic.silver2.dataSourceName=t2
dynamic.silver2.url=jdbc:mysql://xxx:3306/test2?useUnicode=true&characterEncoding=utf-8
dynamic.silver2.username=root
dynamic.silver2.password=121929

~~~
#### 如何使用
~~~
DynamicDataSource.use("t1") //切换数据源到t1
DynamicDataSource.use("t2") //切换数据源到t2
DynamicDataSource.useDefault(); //切换数据源为默认
~~~
### 效果
~~~
   /**
     * 同时修改两个数据库中的数据
     * 只要某一个抛出异常，则两边同时回滚
     * 保证一致性
     */
    @Transactional
    public void getShop(User user ,TestTask testtask) {
        //修改默认数据库中的用户
        DynamicDataSource.useDefault();
        userMapper.updateUser(user1);
        
        //修改t1数据库中的testTask属性
        DynamicDataSource.use("t1");
        testTaskMapper.updateTestTask(testtask);
    }
~~~
