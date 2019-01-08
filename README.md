## 开源组件
Atomikos  是一个为Java平台提供增值服务的并且开源类事务管理器。...... Atomikos 是一个为Java平台提供增值服务的并且开源类事务管理器
ps：以上来自百度

### 详细可以参考 https://gitee.com/hsweb/hsweb-framework/blob/master/hsweb-datasource

## 使用方式
~~~
集合mybatis  mysql框架使用
~~~
####  配置
~~~
mybatis.typeAliasesPackage=com.lizhi.bean
mybatis.mapperLocations=classpath:mapper/*.xml

spring.jta.atomikos.properties.log-base-dir=  ../logs
spring.jta.transaction-manager-id= txManager

#添加默认数据源
zx.dynamic.default.jdbc.url=jdbc:mysql://localhost:3306/lzx-blog?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true
zx.dynamic.default.jdbc.username=root
zx.dynamic.default.jdbc.password=123456

#添加第二个数据源
#dataSourceName  是数据库的名称，默认数据源的名称为default
zx.dynamic.silver1.jdbc.dataSourceName=t1
zx.dynamic.silver1.jdbc.url=jdbc:mysql://182.254.xxx.xxx:3306/lizhixiongdeblog?useUnicode=true&characterEncoding=utf-8
zx.dynamic.silver1.jdbc.username=root
zx.dynamic.silver1.jdbc.password=123456

~~~
####  配置
#### 如何使用
~~~
DynamicDataSource.use("t1") //切换数据源到t1
DynamicDataSource.useDefault(); //切换数据源为默认
~~~