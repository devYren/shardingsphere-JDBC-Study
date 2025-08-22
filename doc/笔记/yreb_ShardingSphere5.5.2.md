

<h1 id="vFd18">Demo 代码仓库</h1>
[GitHub - devYren/shardingsphere-JDBC-Study: 学习shardingsphere-JDBC最新版本](https://github.com/devYren/shardingsphere-JDBC-Study)



<h1 id="H3M7y">官方文档</h1>
[Apache ShardingSphere](https://shardingsphere.apache.org/index_zh.html)

<h1 id="gxbLb">MySQL 一主多从</h1>
![](https://cdn.nlark.com/yuque/0/2025/png/29168630/1744903767916-16543545-51f8-4890-9101-96ee71b677ed.png)

**基本原理：**![](assets/image-20220714133617856.png)<font style="color:rgb(51, 51, 51);">slave会从master读取binlog来进行数据同步</font>

---

**<font style="color:rgb(51, 51, 51);">具体步骤</font>**

+ `<font style="color:rgb(51, 51, 51);">step1：</font>`<font style="color:rgb(51, 51, 51);">master将数据改变记录到</font>`<font style="color:rgb(51, 51, 51);">二进制日志（binary log）</font>`<font style="color:rgb(51, 51, 51);">中。</font>
+ `<font style="color:rgb(51, 51, 51);">step2：</font>`<font style="color:rgb(51, 51, 51);"> 当slave上执行 </font>`<font style="color:rgb(51, 51, 51);">start slave</font>`<font style="color:rgb(51, 51, 51);"> 命令之后，slave会创建一个 </font>`<font style="color:rgb(51, 51, 51);">IO 线程</font>`<font style="color:rgb(51, 51, 51);">用来连接master，请求master中的binlog。</font>
+ `<font style="color:rgb(51, 51, 51);">step3：</font>`<font style="color:rgb(51, 51, 51);">当slave连接master时，master会创建一个 </font>`<font style="color:rgb(51, 51, 51);">log dump 线程</font>`<font style="color:rgb(51, 51, 51);">，用于发送 binlog 的内容。在读取 binlog 的内容的操作中，会对主节点上的 binlog 加锁，当读取完成并发送给从服务器后解锁。</font>
+ `<font style="color:rgb(51, 51, 51);">step4：</font>`<font style="color:rgb(51, 51, 51);">IO 线程接收主节点 binlog dump 进程发来的更新之后，保存到 </font>`<font style="color:rgb(51, 51, 51);">中继日志（relay log）</font>`<font style="color:rgb(51, 51, 51);"> 中。</font>
+ `<font style="color:rgb(51, 51, 51);">step5：</font>`<font style="color:rgb(51, 51, 51);">slave的</font>`<font style="color:rgb(51, 51, 51);">SQL线程</font>`<font style="color:rgb(51, 51, 51);">，读取relay log日志，并解析成具体操作，从而实现主从操作一致，最终数据一致。</font>

---

<font style="color:rgb(51, 51, 51);"></font>

> 服务规划：这里使用 Docker 方式创建，主从服务器 IP 一致，端口号不一致
>

1. 主服务器：容器名 `yren-mysql-master` 端口号 3306
2. 从服务器：容器名 `yren-mysql-slave1`，端口号 3307
3. 从服务器：容器名 `yren-mysql-slave2`，端口号 3308



<h2 id="xGk8H">启动主数据库服务</h2>
<h3 id="I1Mpm">第一步：在 Docker 中创建并启动 MySQL 主服务器：`端口 3306`</h3>
```shell
docker run -d \
-p 3306:3306 \
-v /Users/yren/Documents/docker-mysql/master/conf:/etc/mysql/conf.d \
-v /Users/yren/Documents/docker-mysql/master/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
--name yren-mysql-master \
mysql:8.0.29
```

<h3 id="slZAw">第二步：创建 MySQL 主服务器配置文件 并写入配置</h3>
<font style="color:rgb(51, 51, 51);">默认情况下MySQL的binlog日志是自动开启的，可以通过如下配置定义一些可选配置</font>

```shell
yren@192 ~ % vim /Users/yren/Documents/docker-mysql/master/conf/my.cnf
```

```nginx
[mysqld]
# 服务器唯一id，默认值1
server-id=1
# 设置日志格式，默认值ROW
binlog_format=STATEMENT
# 二进制日志名，默认binlog
# log-bin=binlog
# 设置需要复制的数据库，默认复制全部数据库
#binlog-do-db=mytestdb
# 设置不需要复制的数据库
#binlog-ignore-db=mysql
#binlog-ignore-db=infomation_schema
```

<h4 id="foJ0r">`binlog格式说明：`</h4>
+ binlog_format=STATEMENT：日志记录的是主机数据库的`写指令`，性能高，但是now()之类的函数以及获取系统参数的操作会出现主从数据不同步的问题。
+ binlog_format=ROW（默认）：日志记录的是主机数据库的`写后的数据`，批量操作时性能较差，解决now()或者  user()或者  @@hostname 等操作在主从机器上不一致的问题。
+ binlog_format=MIXED：是以上两种level的混合使用，有函数用ROW，没函数用STATEMENT，但是无法识别系统变量

<h4 id="dJGvG">`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">binlog-ignore-db和binlog-do-db的优先级问题：</font>`</h4>
![](https://cdn.nlark.com/yuque/0/2025/png/29168630/1744904683789-15e51b9a-ce03-49e1-b723-07ac2994ec9c.png)

<h3 id="lWafj">第三步：重启 MySQL 主机</h3>
```shell
yren@192 ~ % docker ps                                                
CONTAINER ID   IMAGE          COMMAND                   CREATED          STATUS          PORTS                               NAMES
92a0d5542b19   mysql:8.0.29   "docker-entrypoint.s…"   13 minutes ago   Up 13 minutes   0.0.0.0:3306->3306/tcp, 33060/tcp   yren-mysql-master
yren@192 ~ % docker restart yren-mysql-master
yren-mysql-master
yren@192 ~ % 
```

<h3 id="kGpth">第四步：<font style="color:rgb(51, 51, 51);">使用命令行登录MySQL主服务器</font></h3>
```shell
#交互式命令行进入容器：env LANG=C.UTF-8 避免容器中显示中文乱码
yren@192 ~ % docker exec -it yren-mysql-master env LANG=C.UTF-8 /bin/bash
#进入容器内的mysql命令行
bash-4.4# mysql -uroot -p
Enter password: 
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 9
Server version: 8.0.29 MySQL Community Server - GPL

Copyright (c) 2000, 2022, Oracle and/or its affiliates.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.
#修改默认密码校验方式
mysql> ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
Query OK, 0 rows affected (0.00 sec)
```

> **<font style="color:#DF2A3F;">注意：</font>** 这里 `mysql8` 使用的密码校验方式是 `caching_sha2_password`，有些旧的数据库客户端暂不支持这个会导致连接失败。所以我们这边需要修改为`mysql_native_password`，这样老的数据库客户端也能连上 MySQL 服务比如 `ShardingSphere`
>



<h3 id="KeEcL">第五步：主机中创建 slave 用户</h3>
```shell
-- 创建slave用户
CREATE USER 'yren_slave'@'%';
-- 设置密码
ALTER USER 'yren_slave'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
-- 授予复制权限
GRANT REPLICATION SLAVE ON *.* TO 'yren_slave'@'%';
-- 刷新权限
FLUSH PRIVILEGES;
```

> 创建用户：用户名 @ %，%表示所有主机允许连接
>

---

> 设置密码时将密码校验规则设置为mysql_native_password 兼容一些旧的数据库客户端比如`ShardingSphere`
>

---

> 授予复制权限为：`REPLICATION SLAVE` 
>
> ON `_*.*_`_ 则代表所有数据库.所有表_
>

---

<h3 id="KKY4u">第六步：主机中查询 Master 状态</h3>
```shell
mysql> show master status;
+---------------+----------+--------------+------------------+-------------------+
| File          | Position | Binlog_Do_DB | Binlog_Ignore_DB | Executed_Gtid_Set |
+---------------+----------+--------------+------------------+-------------------+
| binlog.000005 |      157 |              |                  |                   |
+---------------+----------+--------------+------------------+-------------------+
1 row in set (0.01 sec)
```

> <font style="color:rgb(51, 51, 51);">记下</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">File</font>`<font style="color:rgb(51, 51, 51);">和</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">Position</font>`<font style="color:rgb(51, 51, 51);">的值。执行完此步骤后</font>`<font style="color:rgb(51, 51, 51);">不要再操作主服务器MYSQL</font>`<font style="color:rgb(51, 51, 51);">，防止主服务器状态值变化。</font>
>



<h2 id="e3JCM">准备从服务器</h2>
> 可以配置多台从机 slave1、slave2.....，这里以配置 slave1 为例
>

<h3 id="LZIDa">第一步：**<font style="color:rgb(51, 51, 51);">在docker中创建并启动MySQL从服务器</font>**</h3>
> `<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">端口3307</font>`
>

```shell
yren@192 ~ % docker run -d \
-p 3307:3306 \
-v /Users/yren/Documents/docker-mysql/slave1/conf:/etc/mysql/conf.d \
-v /Users/yren/Documents/docker-mysql/slave1/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
--name yren-mysql-slave1 \
mysql:8.0.29
8ffdb44424737ee81fcf55a90139095e2ec51e0baf22e99735b11e4e2f7c133c
```



<h3 id="JmrA3">第二步：<font style="color:rgb(51, 51, 51);">创建MySQL从服务器配置文件</font></h3>
```shell
yren@192 conf % cd /Users/yren/Documents/docker-mysql/slave1/conf
yren@192 conf % touch my.cnf
yren@192 conf % vim my.cnf
```

> **写入配置**
>

```yaml
[mysqld]
# 服务器唯一id，每台服务器的id必须不同，如果配置其他从机，注意修改id
server-id=2
# 中继日志名，默认xxxxxxxxxxxx-relay-bin
#relay-log=relay-bin
```



<h3 id="v3RLK">第三步：重启 Mysql Slave1</h3>
> 重启 Mysql Slave1 服务
>

```shell
yren@192 ~ % docker ps
CONTAINER ID   IMAGE          COMMAND                   CREATED         STATUS         PORTS                               NAMES
8ffdb4442473   mysql:8.0.29   "docker-entrypoint.s…"   9 minutes ago   Up 9 minutes   33060/tcp, 0.0.0.0:3307->3306/tcp   yren-mysql-slave1
92a0d5542b19   mysql:8.0.29   "docker-entrypoint.s…"   3 months ago    Up 2 hours     0.0.0.0:3306->3306/tcp, 33060/tcp   yren-mysql-master
yren@192 ~ % docker restart yren-mysql-slave1
yren-mysql-slave1
```



<h3 id="Ggon2">第四步：<font style="color:rgb(51, 51, 51);">使用命令行登录MySQL 从服务器 slave1</font></h3>
```shell
#进入容器:
yren@192 ~ % docker exec -it yren-mysql-slave1 env LANG=C.UTF-8 /bin/bash
#进入容器内的mysql命令行
bash-4.4# mysql -uroot -p
Enter password: 
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 8
Server version: 8.0.29 MySQL Community Server - GPL

Copyright (c) 2000, 2022, Oracle and/or its affiliates.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

#修改默认密码校验方式
mysql> ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
```



<h3 id="xqD8Q">第五步：<font style="color:rgb(51, 51, 51);">在从机上配置主从关系</font></h3>
```shell
mysql> CHANGE MASTER TO
MASTER_HOST='192.168.1.146',
MASTER_USER='yren_slave',
MASTER_PASSWORD='123456',
MASTER_PORT=3306,
MASTER_LOG_FILE='binlog.000005',
MASTER_LOG_POS=157;
Query OK, 0 rows affected, 9 warnings (0.03 sec)
```



<h2 id="nPTEA">启动主从同步</h2>
> 启动从机的复制功能，执行 SQL
>
> **START SLAVE：**
>
> + 启动IO线程：负责从主服务器读取二进制日志
> + 启动SQL线程：负责执行从主服务器复制过来的SQL语句
> + 如果从服务器之前停止了复制，这个命令会重新开始复制过程
>
> **SHOW SLAVE STATUS\G：**
>
> + `\G` 是MySQL客户端的特殊终结符，作用是将查询结果**垂直显示**（每个字段占一行）
> + 使用 `\G` 时确实不需要分号，因为 `\G` 本身就起到了语句终结符的作用
> + 如果使用分号结尾就是：`SHOW SLAVE STATUS;`（水平显示，字段较多时不易阅读）
>
> 重要的状态字段包括：
>
> + `Slave_IO_Running`: IO线程是否正在运行
> + `Slave_SQL_Running`: SQL线程是否正在运行
> + `Last_Error`: 最后一次错误信息
> + `Seconds_Behind_Master`: 从服务器落后主服务器的秒数
> + `Master_Log_File` 和 `Read_Master_Log_Pos`: 当前读取的主服务器日志文件和位置
>

```shell
—— 启动从服务器的复制进程
START SLAVE;
-- 查看状态（不需要分号）
SHOW SLAVE STATUS\G
```

> **两个关键进程，****<font style="color:rgb(51, 51, 51);">两个参数都是Yes，则说明主从配置成功！</font>**
>
> <font style="color:#000000;"></font><font style="color:#000000;">Slave_IO_Running: Yes</font>
>
> <font style="color:#000000;">Slave_SQL_Running: Yes</font>
>





<h2 id="QRlA2">实现主从同步</h2>
> <font style="color:rgb(51, 51, 51);">在主机中执行以下SQL，在从机中查看数据库、表和数据是否已经被同步</font>
>

```sql
mysql> CREATE DATABASE db_user;
Query OK, 1 row affected (0.01 sec)

mysql> USE db_user;
Database changed
mysql> CREATE TABLE t_user (
    ->  id BIGINT AUTO_INCREMENT,
    ->  uname VARCHAR(30),
    ->  PRIMARY KEY (id)
    -> );
Query OK, 0 rows affected (0.02 sec)

mysql> INSERT INTO t_user(uname) VALUES('yren');
Query OK, 1 row affected (0.01 sec)

mysql> INSERT INTO t_user(uname) VALUES(@@hostname);
Query OK, 1 row affected, 1 warning (0.01 sec)
```

> 从机 Slave1 查看同步情况
>

```sql
#查看数据库同步情况
mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| db_user            |
| information_schema |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
5 rows in set (0.00 sec)
mysql> USE db_user;
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

Database changed
#查看表同步情况
mysql> show tables;
+-------------------+
| Tables_in_db_user |
+-------------------+
| t_user            |
+-------------------+
1 row in set (0.00 sec)
#查看数据同步情况
mysql> select * from t_user;
+----+--------------+
| id | uname        |
+----+--------------+
|  1 | yren         |
|  2 | 8ffdb4442473 |
+----+--------------+
2 rows in set (0.00 sec)
```



<h2 id="KyKmC">停止和重置</h2>
<h3 id="M7Nx0">STOP SLAVE （停止同步线程的操作）</h3>
```sql
mysql> STOP SLAVE;
```

**执行结果：**

+ **停止IO线程**：不再从主服务器读取binlog日志
+ **停止SQL线程**：不再执行relay log中的SQL语句
+ **保留复制配置**：主从复制的配置信息仍然保存，包括主服务器信息、复制位置等
+ **数据保持不变**：已经复制的数据不会丢失
+ **状态变化**：`Slave_IO_Running` 和 `Slave_SQL_Running` 变为 `No`



<h3 id="YMbdd">RESET SLAVE （重置relaylog日志文件）</h3>
```sql
mysql> RESET SLAVE;
```

**执行结果：**

+ **删除relay log文件**：清空所有中继日志文件
+ **清除复制位置信息**：重置复制的读取位置
+ **保留主服务器连接信息**：如HOST、PORT、USER等配置仍然保留
+ **重新初始化**：为重新开始复制做准备
+ **必须重新配置**：需要重新设置复制起始位置（通常配合 `CHANGE MASTER TO` 使用）



<h3 id="oC7CK">RESET MASTER （重置binglog日志文件）</h3>
```sql
mysql> RESET MASTER;
```

**执行结果：**

+ **删除所有binlog文件**：清空所有二进制日志文件
+ **重置日志索引**：binlog索引文件被清空
+ **重新开始编号**：新的binlog从 `mysql-bin.000001` 开始
+ **影响所有从服务器**：所有连接的从服务器将无法继续复制
+ **数据不受影响**：只删除日志文件，不影响实际数据







<h1 id="OJNph"><font style="color:rgb(51, 51, 51);">ShardingSphere-JDBC读写分离</font></h1>
<h2 id="nJpgu">创建 Spring Boot 工程</h2>
<h3 id="BtOTt">工程依赖</h3>
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.4</version>
    </parent>

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.cooperation</groupId>
    <artifactId>shardingsphere-JDBC-yrendemo</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <mybatis-plus.version>3.5.12</mybatis-plus.version>
    </properties>



    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>3.4.4</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- SpringBoot 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!--参数校验-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.72</version>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- MySQL Connector -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis-spring</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <!-- 显式声明最新版本 -->
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>3.5.16</version>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis-spring</artifactId>
            <version>3.0.3</version>
        </dependency>

        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>shardingsphere-jdbc</artifactId>
            <version>5.5.2</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```



<h3 id="MQwyy">基础代码</h3>
```java
@TableName("t_user")
@Data
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uname;
}
```

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {}
```

<h3 id="nbSaY">项目配置</h3>
```yaml
server:
  port: 43541

spring:
  application:
    name: sharding-jdbc-demo-yren
  datasource:
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:application-shardingsphere-config.yaml

# MyBatis Plus 配置
mybatis-plus:
  configuration:
    # 日志
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 开启驼峰命名
    map-underscore-to-camel-case: true

# 日志配置
logging:
  level:
    com.cooperation.mapper: debug
    org.apache.shardingsphere: info
```

```yaml
# JDBC 逻辑库名称。在集群模式中，使用该参数来联通 ShardingSphere-JDBC 与 ShardingSphere-Proxy。
# 默认值：logic_db
databaseName: logic_db

mode:
  type: Standalone
  repository:
    type: JDBC

dataSources:
  master:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/db_user?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  slave1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3307/db_user?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  slave2:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3308/db_user?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000

rules:
#单表规则用于指定哪些单表需要被 ShardingSphere 管理
  - !SINGLE
    tables:
      - "*.*" # 加载全部单表
#      - ds_0.t_single # 加载指定单表
#      - ds_1.* # 加载指定数据源中的全部单表
  - !READWRITE_SPLITTING
    dataSourceGroups:
      readwrite_ds: #读写分离逻辑数据源名称
        writeDataSourceName: master #写库数据源名称
        readDataSourceNames: #读库数据源名称
          - slave1
          - slave2
        transactionalReadQueryStrategy: PRIMARY #事务内读请求的路由策略，可选值：PRIMARY（路由至主库）、FIXED（同一事务内路由至固定数据源）、DYNAMIC（同一事务内路由至非固定数据源）。默认值：DYNAMIC
        loadBalancerName: roundRobin # 负载均衡算法名称
# 负载均衡算法配置
    loadBalancers:
      # 负载均衡算法名称
      roundRobin:
        type: ROUND_ROBIN # ROUND_ROBIN:负载均衡算法类型  RANDOM:随机负载均衡算法 WEIGHT:权重负载均衡算法
#通用配置
props:
  sql-show: true #是否在日志中打印 SQL打印 SQL 可以帮助开发者快速定位系统问题。日志内容包含：逻辑 SQL，真实 SQL 和 SQL 解析结果。如果开启配置，日志将使用 Topic ShardingSphere-SQL，日志级别是 INFO
```



<h2 id="EGaNj">主从复制测试</h2>
> 发起请求 `GET` [http://localhost:43541/test?username=](http://localhost:43541/test?username=%E4%BD%A0%E5%A5%BD%E5%91%80)你好呀
>

```java
package com.yren.shardingSphereDemo.controller;
/**
 * @author ChenYu ren
 * @date 2025/7/18
 */
@RestController
@RequestMapping
public class ShardingSphereTestController {

    @Resource
    private UserMapper  userMapper;

    @GetMapping("/test")
    public String test(@RequestParam("username") String username) {
        User user = new User();
        user.setUname(username);
        userMapper.insert(user);
        return "success";
    }
```

```shell
JDBC Connection [HikariProxyConnection@1800370555 wrapping org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection@52f7d65c] will not be managed by Spring
==>  Preparing: INSERT INTO t_user ( uname ) VALUES ( ? )
==> Parameters: 你好呀(String)
2025-07-31T23:24:13.267+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: INSERT INTO t_user  ( uname )  VALUES (  ?  )
2025-07-31T23:24:13.267+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: master ::: INSERT INTO t_user  ( uname )  VALUES (  ?  ) ::: [你好呀]
<==    Updates: 1

```

```sql
mysql> select * from t_user;
+----+--------------+
| id | uname        |
+----+--------------+
|  1 | yren         |
|  2 | 92a0d5542b19 |
|  3 | yren2025     |
|  4 | 你好呀       |
+----+--------------+
4 rows in set (0.01 sec)
```

```sql
mysql> select * from t_user;
+----+--------------+
| id | uname        |
+----+--------------+
|  1 | yren         |
|  2 | 92a0d5542b19 |
|  3 | yren2025     |
|  4 | 你好呀       |
+----+--------------+
4 rows in set (0.01 sec)
```



<h2 id="HvwzS">事务测试</h2>
> <font style="color:rgb(51, 51, 51);">为了保证主从库间的事务一致性，避免跨服务的分布式事务，ShardingSphere-JDBC的</font>`**<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">主从模型中，事务中的数据读写均用主库</font>**`<font style="color:rgb(51, 51, 51);"></font>
>

+ <font style="color:rgb(51, 51, 51);">不添加</font>`<font style="color:rgb(51, 51, 51);">@Transactional</font>`<font style="color:rgb(51, 51, 51);">：</font>`<font style="color:rgb(51, 51, 51);">insert</font>`<font style="color:rgb(51, 51, 51);">对主库操作，</font>`<font style="color:rgb(51, 51, 51);">select</font>`<font style="color:rgb(51, 51, 51);">对从库操作</font>
+ <font style="color:rgb(51, 51, 51);">添加</font>`<font style="color:rgb(51, 51, 51);">@Transactional</font>`<font style="color:rgb(51, 51, 51);">：则</font>`<font style="color:rgb(51, 51, 51);">insert</font>`<font style="color:rgb(51, 51, 51);">和</font>`<font style="color:rgb(51, 51, 51);">select</font>`<font style="color:rgb(51, 51, 51);">均对主库操作</font>

<h3 id="iwg5i">不添加@Transactional</h3>
> 测试插入数据
>
> 结果：插入主库 `master`
>

```java
@GetMapping("/test")
public String test(@RequestParam("username") String username) {
    User user = new User();
    user.setUname(username);
    userMapper.insert(user);
    return "success";
}
```

```java
2025-08-01T02:01:10.892+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [io-43541-exec-7] ShardingSphere-SQL                       : Logic SQL: INSERT INTO t_user  ( uname )  VALUES (  ?  )
2025-08-01T02:01:10.892+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [io-43541-exec-7] ShardingSphere-SQL                       : Actual SQL: master ::: INSERT INTO t_user  ( uname )  VALUES (  ?  ) ::: [测试事务]
<==    Updates: 1
```

> 测试读数据
>
> 结果：查询从库 `slave1`
>

```java
@GetMapping("/list")
public String list() {
    return JSONObject.toJSONString(userMapper.selectList(null));
}
```

```shell
2025-08-01T02:02:16.124+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [o-43541-exec-10] ShardingSphere-SQL                       : Logic SQL: SELECT  id,uname  FROM t_user
2025-08-01T02:02:16.125+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [o-43541-exec-10] ShardingSphere-SQL                       : Actual SQL: slave1 ::: SELECT  id,uname  FROM t_user
2025-08-01T02:02:16.125+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [o-43541-exec-10] com.zaxxer.hikari.HikariDataSource       : HikariPool-5 - Starting...
2025-08-01T02:02:16.136+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [o-43541-exec-10] com.zaxxer.hikari.pool.HikariPool        : HikariPool-5 - Added connection com.mysql.cj.jdbc.ConnectionImpl@79b3bdbd
2025-08-01T02:02:16.136+08:00  INFO 44564 --- [sharding-jdbc-demo-yren] [o-43541-exec-10] com.zaxxer.hikari.HikariDataSource       : HikariPool-5 - Start completed.
<==    Columns: id, uname
<==        Row: 1, yren
<==        Row: 2, 8ffdb4442473
<==        Row: 3, yren2025
<==        Row: 238, 你好呀
<==        Row: 239, 测试事务
<==      Total: 5
```

<h3 id="xcF5K">添加@Transactional</h3>
> 测试插入数据
>
> 结果：插入主库 `master`
>

```java
@GetMapping("/test")
@Transactional(rollbackFor = Exception.class)
public String test(@RequestParam("username") String username) {
    User user = new User();
    user.setUname(username);
    userMapper.insert(user);
    return "success";
}
```

```shell
2025-08-01T02:05:17.897+08:00  INFO 54465 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: INSERT INTO t_user  ( uname )  VALUES (  ?  )
2025-08-01T02:05:17.897+08:00  INFO 54465 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: master ::: INSERT INTO t_user  ( uname )  VALUES (  ?  ) ::: [测试添加事务插入]
<==    Updates: 1
```



> 测试读数据
>
> 结果：查询主库 `master`
>

```java
@GetMapping("/list")
@Transactional(rollbackFor = Exception.class)
public String list() {
    return JSONObject.toJSONString(userMapper.selectList(null));
}
```

```java
2025-08-01T02:06:10.688+08:00  INFO 54465 --- [sharding-jdbc-demo-yren] [io-43541-exec-4] ShardingSphere-SQL                       : Logic SQL: SELECT  id,uname  FROM t_user
2025-08-01T02:06:10.688+08:00  INFO 54465 --- [sharding-jdbc-demo-yren] [io-43541-exec-4] ShardingSphere-SQL                       : Actual SQL: master ::: SELECT  id,uname  FROM t_user
<==    Columns: id, uname
<==        Row: 1, yren
<==        Row: 2, 92a0d5542b19
<==        Row: 3, yren2025
<==        Row: 238, 你好呀
<==        Row: 239, 测试事务
<==        Row: 240, 测试添加事务插入
<==      Total: 6
```



<h2 id="GTCEm"><font style="color:rgb(51, 51, 51);">从库负载均衡测试</font></h2>
```java
    @GetMapping("/list/dept")
    public String listDept() {
        return JSONObject.toJSONString(deptMapper.selectList(null));
    }
```

> 第一次读数据
>
> 请求：`GET` [http://localhost:43541/list/dept](http://localhost:43541/list/dept)
>
> 结果：从库`slave1`读取数据
>

```java
2025-08-01T02:07:43.029+08:00  INFO 54465 --- [sharding-jdbc-demo-yren] [io-43541-exec-6] ShardingSphere-SQL                       : Logic SQL: SELECT  id,dept_name  FROM t_dept
2025-08-01T02:07:43.030+08:00  INFO 54465 --- [sharding-jdbc-demo-yren] [io-43541-exec-6] ShardingSphere-SQL                       : Actual SQL: slave1 ::: SELECT  id,dept_name  FROM t_dept
```

> 第二次读数据
>
> 请求：`GET` [http://localhost:43541/list/dept](http://localhost:43541/list/dept)
>
> 结果：从库`slave2`读取数据
>

```shell
2025-08-01T02:08:11.974+08:00  INFO 54465 --- [sharding-jdbc-demo-yren] [io-43541-exec-8] ShardingSphere-SQL                       : Logic SQL: SELECT  id,dept_name  FROM t_dept
2025-08-01T02:08:11.975+08:00  INFO 54465 --- [sharding-jdbc-demo-yren] [io-43541-exec-8] ShardingSphere-SQL                       : Actual SQL: slave2 ::: SELECT  id,dept_name  FROM t_dept
```



<h1 id="f9ae26d5"><font style="color:rgb(51, 51, 51);">ShardingSphere-JDBC垂直分片</font></h1>


<h2 id="PVFh6">服务规划</h2>
> **服务规划：**
>
> `server-user` prot:3301
>
> `server-order` prot:3302
>

![](https://cdn.nlark.com/yuque/0/2025/png/29168630/1754020105424-615698f7-6a16-403e-828d-9cad4fa730c0.png)



<h2 id="GyOlr">服务创建</h2>
<h3 id="ndQzB">server-user:3301</h3>
```shell
#创建容器
yren@192 ~ % docker run -d \
-p 3301:3306 \
-v /Users/yren/Documents/docker-mysql/server/user/conf:/etc/mysql/conf.d \
-v /Users/yren/Documents/docker-mysql/server/user/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
--name server-user \
mysql:8.0.29
#创建成功
1c0ab405aec74452b9cfead431c315f1457b2407c4e3273b97d8e75e88004acd
#进入容器
yren@192 ~ % docker exec -it server-user env LANG=C.UTF-9 /bin/bash
#进入Mysql服务
bash-4.4# mysql -uroot -p
Enter password: 
Welcome to the MySQL monitor.  Commands end with ; or \g.
#修改默认密码插件
mysql> ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
Query OK, 0 rows affected (0.01 sec)
#创建数据库db_user
mysql> CREATE DATABASE db_user;
Query OK, 1 row affected (0.01 sec)
mysql> USE db_user;
Database changed
#创建表t_user
mysql> CREATE TABLE t_user (
      id BIGINT AUTO_INCREMENT,
      uname VARCHAR(30),
      PRIMARY KEY (id)
     );
Query OK, 0 rows affected (0.01 sec)
```



<h3 id="uASGQ">server-order:3302</h3>
```shell
#创建容器
yren@192 ~ % docker run -d \
-p 3302:3306 \
-v /Users/yren/Documents/docker-mysql/server/order/conf:/etc/mysql/conf.d \
-v /Users/yren/Documents/docker-mysql/server/order/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
--name server-order \
mysql:8.0.29
#创建成功
fa2ac282ab7802827ee0b9b64bf3435b26617a04a615e4cd23514632279066a2
#进入容器
yren@192 ~ % docker exec -it server-order env LANG=C.UTF-9 /bin/bash
#进入Mysql服务
bash-4.4# mysql -uroot -p
Enter password: 
Welcome to the MySQL monitor.  Commands end with ; or \g.
#修改默认密码插件
mysql> ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
Query OK, 0 rows affected (0.01 sec)
#创建数据库db_user
mysql> CREATE DATABASE db_user;
Query OK, 1 row affected (0.01 sec)
mysql> USE db_user;
Database changed
#创建表t_user
mysql> CREATE TABLE t_order (
  id BIGINT AUTO_INCREMENT,
  order_no VARCHAR(30),
  user_id BIGINT,
  amount DECIMAL(10,2),
  PRIMARY KEY(id) 
);
Query OK, 0 rows affected (0.01 sec)
```



<h2 id="o0rHY">代码实现</h2>
```java
@TableName("t_order")
@Data
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
}
```

```java
@Mapper
public interface OrderMapper extends BaseMapper<Order> {}
```

```java
/**
 * @author ChenYu ren
 * @date 2025/7/18
 */

@RestController
@RequestMapping("/vertical")
public class ShardingSphereVerticalTestController {

    @Resource
    private UserMapper  userMapper;

    @Resource
    private OrderMapper orderMapper;

    @GetMapping("/list")
    public String list() {
        return userMapper.selectList(null) + "\n" + orderMapper.selectList(null);
    }

    @GetMapping("/add")
    public String add() {
        User user = new User();
        user.setUname("yren");
        userMapper.insert(user);
        Order order = new Order();
        order.setOrderNo("YREN10001");
        order.setUserId(user.getId());
        order.setAmount(new BigDecimal(100));
        orderMapper.insert(order);
        return "success";
    }

}
```

<h2 id="cualS">垂直分片测试</h2>
> 发送请求新增数据 [http://localhost:43541/vertical/add](http://localhost:43541/vertical/add)
>
> **结果：**
>
> User 插入至 server-user 库
>
> Order 插入至 server-order 库
>

```java
JDBC Connection [HikariProxyConnection@2081970002 wrapping org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection@3afee9d5] will not be managed by Spring
==>  Preparing: INSERT INTO t_user ( uname ) VALUES ( ? )
==> Parameters: yren(String)
2025-08-01T16:53:32.582+08:00  INFO 80168 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: INSERT INTO t_user  ( uname )  VALUES (  ?  )
2025-08-01T16:53:32.582+08:00  INFO 80168 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-user ::: INSERT INTO t_user  ( uname )  VALUES (?) ::: [yren]
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@311bbfec]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7a969683] was not registered for synchronization because synchronization is not active
==>  SQLStructure: {"id":"com.yren.shardingSphereDemo.mapper.OrderMapper.insert","originalSql":"INSERT INTO t_order  ( order_no, user_id, amount )  VALUES (  #{orderNo}, #{userId}, #{amount}  )","completeSql":"INSERT INTO t_order ( order_no, user_id, amount ) VALUES ( 'YREN10001', 1, 100 )","parameter":"[{\"orderNo\":\"'YREN10001'\"},{\"userId\":\"1\"},{\"amount\":\"100\"}]"}
JDBC Connection [HikariProxyConnection@1462039284 wrapping org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection@3afee9d5] will not be managed by Spring
==>  Preparing: INSERT INTO t_order ( order_no, user_id, amount ) VALUES ( ?, ?, ? )
==> Parameters: YREN10001(String), 1(Long), 100(BigDecimal)
2025-08-01T16:53:32.604+08:00  INFO 80168 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: INSERT INTO t_order  ( order_no, user_id, amount )  VALUES (  ?, ?, ?  )
2025-08-01T16:53:32.604+08:00  INFO 80168 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order ::: INSERT INTO t_order  ( order_no, user_id, amount )  VALUES (?, ?, ?) ::: [YREN10001, 1, 100]
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7a969683]
```

> 发送请求查询数据 [http://localhost:43541/vertical/list](http://localhost:43541/vertical/list)
>
> **结果：**
>
> User 从 server-user 库查询
>
> Order 从 server-order 库查询
>

```java
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@49f51417] was not registered for synchronization because synchronization is not active
==>  SQLStructure: {"id":"com.yren.shardingSphereDemo.mapper.UserMapper.selectList","originalSql":"SELECT  id,uname  FROM t_user","completeSql":"SELECT id,uname FROM t_user","parameter":"[]"}
JDBC Connection [HikariProxyConnection@33196675 wrapping org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection@3afee9d5] will not be managed by Spring
==>  Preparing: SELECT id,uname FROM t_user
==> Parameters: 
2025-08-01T16:59:24.393+08:00  INFO 80168 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Logic SQL: SELECT  id,uname  FROM t_user
2025-08-01T16:59:24.394+08:00  INFO 80168 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Actual SQL: server-user ::: SELECT  id,uname  FROM t_user
<==    Columns: id, uname
<==        Row: 1, yren
<==      Total: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@49f51417]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@19aa0e5c] was not registered for synchronization because synchronization is not active
==>  SQLStructure: {"id":"com.yren.shardingSphereDemo.mapper.OrderMapper.selectList","originalSql":"SELECT  id,order_no,user_id,amount  FROM t_order","completeSql":"SELECT id,order_no,user_id,amount FROM t_order","parameter":"[]"}
JDBC Connection [HikariProxyConnection@1984838320 wrapping org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection@3afee9d5] will not be managed by Spring
==>  Preparing: SELECT id,order_no,user_id,amount FROM t_order
==> Parameters: 
2025-08-01T16:59:24.426+08:00  INFO 80168 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Logic SQL: SELECT  id,order_no,user_id,amount  FROM t_order
2025-08-01T16:59:24.427+08:00  INFO 80168 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Actual SQL: server-order ::: SELECT  id,order_no,user_id,amount  FROM t_order
<==    Columns: id, order_no, user_id, amount
<==        Row: 1, YREN10001, 1, 100.00
<==      Total: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@19aa0e5c]
```



<h1 id="ujz6Y">ShardingSphere-JDBC 水平分片</h1>
<h2 id="E2pI4">服务规划</h2>
![](https://cdn.nlark.com/yuque/0/2025/png/29168630/1754040148622-21c96b3b-de7f-4a15-9015-9901ab4d904e.png)

> + <font style="color:rgb(51, 51, 51);"></font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">server-order0</font>`<font style="color:rgb(51, 51, 51);">，端口</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">3310</font>`
> + <font style="color:rgb(51, 51, 51);"></font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">server-order1</font>`<font style="color:rgb(51, 51, 51);">，端口</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">3311</font>`
>



<h2 id="BTVWl">服务创建</h2>
<h3 id="rC4y8">server-order0:3310</h3>
```shell
#创建容器
yren@192 order0 % docker run -d \
-p 3310:3306 \
-v /Users/yren/Documents/docker-mysql/server/order0/conf:/etc/mysql/conf.d \
-v /Users/yren/Documents/docker-mysql/server/order0/data:/var/lib/mysql \
-e MYSQL_ROOT_PASSWORD=123456 \
--name server-order0 \
mysql:8.0.29
#创建成功
316ae4e46a04658189cd7ec6837234908859f29a87b2958ffaff66f430105a53
```



> 进入 Mysql 实例创建数据库
>
> 创建数据库
>
> **<font style="color:#DF2A3F;">注意：</font>**<font style="color:rgb(51, 51, 51);">水平分片的id需要在业务层实现，</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">不能依赖数据库的主键自增。</font>`
>

```shell
#进入Mysql
yren@192 ~ % docker exec -it server-order0 env LANG=C.UTF-8 /bin/bash
#登录Root账号
bash-4.4# mysql -uroot -p
Enter password: 
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 10
Server version: 8.0.29 MySQL Community Server - GPL
#修改默认密码插件
mysql> ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
Query OK, 0 rows affected (0.01 sec)
#创建数据库
mysql> CREATE DATABASE db_order;
Query OK, 1 row affected (0.00 sec)
mysql> USE db_order;
Database changed
#创建表
mysql> CREATE TABLE t_order0 (
  id BIGINT,
  order_no VARCHAR(30),
  user_id BIGINT,
  amount DECIMAL(10,2),
  PRIMARY KEY(id) 
);
mysql> CREATE TABLE t_order1 (
  id BIGINT,
  order_no VARCHAR(30),
  user_id BIGINT,
  amount DECIMAL(10,2),
  PRIMARY KEY(id) 
);
```

<h3 id="O7ZTh">server-order0:3311 </h3>
> 同上
>



<h2 id="drA3D">基本水平分片</h2>
<h3 id="Obanw">代码</h3>
> 修改 Order 实体类的主键策略
>

```java
@TableName("t_order")
@Data
public class Order {
//    @TableId(type = IdType.AUTO)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
}
```



<h3 id="Apk3M">完整配置</h3>
```yaml
# JDBC 逻辑库名称。在集群模式中，使用该参数来联通 ShardingSphere-JDBC 与 ShardingSphere-Proxy。
# 默认值：logic_db
databaseName: logic_db

mode:
  type: Standalone
  repository:
    type: JDBC

dataSources:
  server-user:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3301/db_user?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  server-order0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3310/db_order?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  server-order1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3311/db_order?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000

rules:
  # 数据分片，数据究竟写入/读取 哪个库，哪个表，按什么算法来确定
- !SHARDING
  tables:
    # 逻辑表名称
    t_user:
      actualDataNodes: server-user.t_user # 由数据源名 + 表名组成
    # 逻辑表名称
    t_order:
      actualDataNodes: server-order${0..1}.t_order${0..1} # 由数据源名 + 表名组成（参考 Inline 语法规则）
      databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
        standard: # 用于单分片键的标准分片场景
          shardingColumn: user_id # 分片列名称
          shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      tableStrategy: # 分表策略，同分库策略
        standard:
          shardingColumn: order_no
          shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      keyGenerateStrategy: # 分布式序列策略
        column: id # 自增列名称，缺省表示不使用自增主键生成器
        keyGeneratorName: snowflake # 分布式序列算法名称,这里是个名字，下边(shardingAlgorithms)有定义

#  defaultDatabaseStrategy: #默认数据库分片策略
#  defaultTableStrategy: #默认的表分片策略
#  defaultKeyGenerateStrategy: # 默认的分布式序列策略
#  defaultShardingColumn: # 默认分片列名称

  # 分片算法配置
  shardingAlgorithms:
    database_inline: # 自定义的算法名称，上边有用到
      type: INLINE # 分片算法类型,有多钟可参考官网 https://shardingsphere.apache.org/document/5.5.2/cn/user-manual/common-config/builtin-algorithm/sharding/
      props: # 分片算法属性配置
        algorithm-expression: server-order${user_id % 2} #根据用户id 对2（因为有2个数据库）取余数，type 为INLINE 的表达式写发可以找官网

    t_order_inline: # 自定义的算法名称，上边有用到
      type: CLASS_BASED #表示这个分片算法是基于自定义的Java类实现的分片算法。
      props: #算法的属性配置，传递给分片算法类的参数
        strategy: STANDARD
        # 指定具体用来做分片的Java类，这里使用的是ShardingSphere自带的基于 hash取模 的分片算法。
        # 这个算法的作用是：对分片字段（比如订单ID）进行hash计算，然后对分片数量取模，决定数据应该路由到哪个分片。
        algorithmClassName: org.apache.shardingsphere.sharding.algorithm.sharding.mod.HashModShardingAlgorithm
        sharding-count: 2 #指定分片总数为2，即分成2个分片。
  keyGenerators:
    snowflake: # 分布式序列算法名称
      type: SNOWFLAKE #分布式序列算法类型 - 雪花算法
#     props: # 分布式序列算法属性配置
#       worker-id: 123  # 工作节点 ID
#       max-tolerate-time-difference-milliseconds: 100

#    uuid:
#      type: UUID #分布式序列算法类型 - UUID

#通用配置
props:
  sql-show: true #是否在日志中打印 SQL打印 SQL 可以帮助开发者快速定位系统问题。日志内容包含：逻辑 SQL，真实 SQL 和 SQL 解析结果。如果开启配置，日志将使用 Topic ShardingSphere-SQL，日志级别是 INFO

```



<h3 id="RLaXy">分片算法配置详解</h3>
<h4 id="ZEyZx">水平分库</h4>
> **<font style="color:rgb(51, 51, 51);">分片规则：</font>**`<font style="color:rgb(51, 51, 51);">t_order</font>`<font style="color:rgb(51, 51, 51);"> 表中</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">user_id</font>`<font style="color:rgb(51, 51, 51);">为偶数时，数据插入</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">server-order0服务器</font>`<font style="color:rgb(51, 51, 51);">，</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">user_id</font>`<font style="color:rgb(51, 51, 51);">为奇数时，数据插入</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">server-order1服务器</font>`<font style="color:rgb(51, 51, 51);">。这样分片的好处是，同一个用户的订单数据，一定会被插入到同一台服务器上，查询一个用户的订单时效率较高。</font>
>

```yaml
t_order:
  actualDataNodes: server-order${0..1}.t_order${0..1} # 由数据源名 + 表名组成（参考 Inline 语法规则）
  databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
    standard: # 用于单分片键的标准分片场景
      shardingColumn: user_id # 分片列名称
      shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应

# 分片算法配置
shardingAlgorithms:
  database_inline: # 自定义的算法名称，上边有用到
    type: INLINE # 分片算法类型,有多钟可参考官网 https://shardingsphere.apache.org/document/5.5.2/cn/user-manual/common-config/builtin-algorithm/sharding/
    props: # 分片算法属性配置
      algorithm-expression: server-order${user_id % 2} #根据用户id 对2（因为有2个数据库）取余数，type 为INLINE 的表达式写发可以找官网          
```

<h4 id="XP9vk">水平分表</h4>
> <font style="color:rgb(51, 51, 51);">分片规则：</font>`<font style="color:rgb(51, 51, 51);">t_order</font>`<font style="color:rgb(51, 51, 51);">表中</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">order_no的哈希值为偶数时</font>`<font style="color:rgb(51, 51, 51);">，数据插入对应服务器的</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">t_order0表</font>`<font style="color:rgb(51, 51, 51);">，</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">order_no的哈希值为奇数时</font>`<font style="color:rgb(51, 51, 51);">，数据插入对应服务器的</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">t_order1表</font>`<font style="color:rgb(51, 51, 51);">。因为order_no是字符串形式，因此不能直接取模。</font>
>

```yaml
tableStrategy: # 分表策略，同分库策略
  standard:
    shardingColumn: order_no
    shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
    
t_order_inline: # 自定义的算法名称，上边有用到
  type: CLASS_BASED #表示这个分片算法是基于自定义的Java类实现的分片算法。
  props: #算法的属性配置，传递给分片算法类的参数
    strategy: STANDARD
    # 指定具体用来做分片的Java类，这里使用的是ShardingSphere自带的基于 hash取模 的分片算法。
    # 这个算法的作用是：对分片字段（比如订单ID）进行hash计算，然后对分片数量取模，决定数据应该路由到哪个分片。
    algorithmClassName: org.apache.shardingsphere.sharding.algorithm.sharding.mod.HashModShardingAlgorithm
    sharding-count: 2 #指定分片总数为2，即分成2个分片。
```

<h4 id="XyZoY">分布式序列算法</h4>
> **全局序列（Global Sequence）** 指的是在 **分布式系统** 中，能够保证在**整个系统范围内唯一且有序**的一组数值 ID。
>

> **<font style="color:rgb(51, 51, 51);">水平分片需要关注全局序列，因为不能简单的使用基于数据库的主键自增。</font>**
>
> **<font style="color:rgb(51, 51, 51);">两种方案：</font>**
>
> 1. <font style="color:rgb(51, 51, 51);">基于MyBatisPlus的id策略；</font>
> 2. <font style="color:rgb(51, 51, 51);">ShardingSphere-JDBC的全局序列配置；</font>
>

[https://shardingsphere.apache.org/document/current/cn/user-manual/common-config/builtin-algorithm/keygen/](https://shardingsphere.apache.org/document/current/cn/user-manual/common-config/builtin-algorithm/keygen/)

<h5 id="d3gmC">MybatisPlus 策略</h5>
```java
/**
 * @author ChenYu ren
 * @date 2025/8/1
 */

@TableName("t_order")
@Data
public class Order {
    //@TableId(type = IdType.AUTO)
    //雪花算法
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
}
```

<h5 id="JaKVp"><font style="color:rgb(51, 51, 51);">ShardingSphere-JDBC 策略</font></h5>
```yaml
rules:
  # 数据分片，数据究竟写入/读取 哪个库，哪个表，按什么算法来确定
- !SHARDING
  tables:
    # 逻辑表名称
    t_order:
      actualDataNodes: server-order${0..1}.t_order${0..1} # 由数据源名 + 表名组成（参考 Inline 语法规则）
      databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
        standard: # 用于单分片键的标准分片场景
          shardingColumn: user_id # 分片列名称
          shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      tableStrategy: # 分表策略，同分库策略
        standard:
          shardingColumn: order_no
          shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      keyGenerateStrategy: # 分布式序列策略
        column: id # 自增列名称，缺省表示不使用自增主键生成器
        keyGeneratorName: snowflake # 分布式序列算法名称,这里是个名字，下边(shardingAlgorithms)有定义
```

```yaml
  keyGenerators:
    snowflake:
      type: SNOWFLAKE
```

> <font style="color:rgb(51, 51, 51);">此时，需要将实体类中的id策略修改成以下形式：</font>
>

```java
//当配置了shardingsphere-jdbc的分布式序列时，自动使用shardingsphere-jdbc的分布式序列
//当没有配置shardingsphere-jdbc的分布式序列时，自动依赖数据库的主键自增策略
@TableId(type = IdType.AUTO)
```

---





<h3 id="qYhRa">测试</h3>
```java
/**
 * @author ChenYu ren
 * @date 2025/7/18
 */

@RestController
@RequestMapping("/horizontal")
public class ShardingSphereHorizontalTestController {

    @Resource
    private OrderMapper orderMapper;

    @GetMapping("/add")
    public String add(@RequestParam("userId")Long userId, @RequestParam("orderNo")String orderNo) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAmount(new BigDecimal(100));
        orderMapper.insert(order);
        return "success";
    }

    @GetMapping("/get")
    public String getById(@RequestParam("Long")Long id) {
        return orderMapper.selectById(id).toString();
    }

    @GetMapping("/getByUserId")
    public String getByUserId(@RequestParam("userId")Long userId) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery().eq(Order::getUserId,userId)).toString();
    }

    @GetMapping("/list")
    public String findAll() {
        return orderMapper.selectList(null).toString();
    }

    @GetMapping("/page")
    public String findAll(@RequestParam("pageNum")Long pageNum,
                          @RequestParam("pageSize")Long pageSize,
                          @RequestParam("userId")Long userId) {
        return orderMapper.selectPage(new Page<>(pageNum,pageSize),
                Wrappers.<Order>lambdaQuery()
                        .eq(Order::getUserId, userId))
                .toString();
    }

}
```



<h4 id="GZM3C">插入数据分片</h4>
> [http://localhost:43541/horizontal/add?userId=1&orderNo=ORDER100001](about:blank)
>
> **数据库分片规则：**server-order[?] = userId 1 % 2 = 1 -> server-order[1]
>
> **表分片规则：**t_order[?] = ORDER100001 -> hash -> -602972338  % 2 -> t_order[0] 
>

```http
==>  Preparing: INSERT INTO t_order ( id, order_no, user_id, amount ) VALUES ( ?, ?, ?, ? )
==> Parameters: 1953666620423557121(Long), ORDER100001(String), 1(Long), 100(BigDecimal)
2025-08-08T11:56:36.945+08:00  INFO 17111 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: INSERT INTO t_order  ( id, order_no, user_id, amount )  VALUES (  ?, ?, ?, ?  )
2025-08-08T11:56:36.945+08:00  INFO 17111 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: INSERT INTO t_order0  ( id, order_no, user_id, amount )  VALUES (?, ?, ?, ?) ::: [1953666620423557121, ORDER100001, 1, 100]
<==    Updates: 1
```



<h4 id="EwuUm">查询数据分片</h4>
> **水平分片：查询所有记录**
>
> [http://localhost:43541/horizontal/list](http://localhost:43541/horizontal/list)
>
> **结果：**查询了两个数据源，每个数据源中使用 `UNION ALL` 连接两个表
>

```shell
==>  Preparing: SELECT id,order_no,user_id,amount FROM t_order
==> Parameters: 
2025-08-08T14:39:56.934+08:00  INFO 17111 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Logic SQL: SELECT  id,order_no,user_id,amount  FROM t_order
2025-08-08T14:39:56.934+08:00  INFO 17111 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: SELECT  id,order_no,user_id,amount  FROM t_order0 UNION ALL SELECT  id,order_no,user_id,amount  FROM t_order1
2025-08-08T14:39:56.934+08:00  INFO 17111 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT  id,order_no,user_id,amount  FROM t_order0 UNION ALL SELECT  id,order_no,user_id,amount  FROM t_order1
<==    Columns: id, order_no, user_id, amount
<==        Row: 1953666620423557121, ORDER100001, 1, 100.00
<==      Total: 1
```

> **水平分片：根据 user_id 查询记录**
>
> [**http://localhost:43541/horizontal/getByUserId?userId=1**](http://localhost:43541/horizontal/getByUserId?userId=1)
>
> **结果：**查询了一个数据源，每个数据源中使用UNION ALL连接两个表，因为入参 userId 通过分片算法路由到了server-order1
>

```latex
==>  Preparing: SELECT id,order_no,user_id,amount FROM t_order WHERE (user_id = ?)
==> Parameters: 1(Long)
2025-08-08T18:07:30.483+08:00  INFO 41482 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: SELECT  id,order_no,user_id,amount  FROM t_order      WHERE  (user_id = ?)
2025-08-08T18:07:30.483+08:00  INFO 41482 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT  id,order_no,user_id,amount  FROM t_order0      WHERE  (user_id = ?) UNION ALL SELECT  id,order_no,user_id,amount  FROM t_order1      WHERE  (user_id = ?) ::: [1, 1]
<==    Columns: id, order_no, user_id, amount
<==        Row: 1953666620423557121, ORDER100001, 1, 100.00
<==      Total: 1
```

<h2 id="tUIqh"><font style="color:rgb(51, 51, 51);">子表水平分片</font></h2>
<h3 id="h5pa1">创建关联表</h3>
> <font style="color:rgb(51, 51, 51);">我们希望</font>**<font style="color:#DF2A3F;background-color:rgb(243, 244, 244);">同一个用户的订单表和订单详情表中的数据都在同一个数据源中，避免跨库关联</font>**<font style="color:rgb(51, 51, 51);">，因此这两张表我们使用相同的分片策略。所以在</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">t_order_item</font>`<font style="color:rgb(51, 51, 51);">中我们也需要创建</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">order_no</font>`<font style="color:rgb(51, 51, 51);">和</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">user_id</font>`<font style="color:rgb(51, 51, 51);">这两个分片键</font>
>
> <font style="color:rgb(51, 51, 51);">在</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">server-order0、server-order1</font>`<font style="color:rgb(51, 51, 51);">服务器中分别创建两张订单详情表</font>`<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">t_order_item0、t_order_item1</font>`
>

```sql
CREATE TABLE t_order_item0(
    id BIGINT,
    order_no VARCHAR(30),
    user_id BIGINT,
    price DECIMAL(10,2),
    `count` INT,
    PRIMARY KEY(id)
);

CREATE TABLE t_order_item1(
    id BIGINT,
    order_no VARCHAR(30),
    user_id BIGINT,
    price DECIMAL(10,2),
    `count` INT,
    PRIMARY KEY(id)
);
```



<h3 id="OLhQL">代码</h3>
```java
@TableName("t_order_item")
@Data
public class OrderItem {
    //当配置了shardingsphere-jdbc的分布式序列时，自动使用shardingsphere-jdbc的分布式序列
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal price;
    private Integer count;
}
```

```java
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {}
```

<h3 id="LyuIa">关联表分片策略配置</h3>
```yaml
# JDBC 逻辑库名称。在集群模式中，使用该参数来联通 ShardingSphere-JDBC 与 ShardingSphere-Proxy。
# 默认值：logic_db
databaseName: logic_db

mode:
  type: Standalone
  repository:
    type: JDBC

dataSources:
  server-user:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3301/db_user?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  server-order0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3310/db_order?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  server-order1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3311/db_order?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000

rules:
  # 数据分片，数据究竟写入/读取 哪个库，哪个表，按什么算法来确定
- !SHARDING
  tables:
    # 逻辑表名称
    t_user:
      actualDataNodes: server-user.t_user # 由数据源名 + 表名组成
    # 逻辑表名称
    t_order:
      actualDataNodes: server-order${0..1}.t_order${0..1} # 由数据源名 + 表名组成（参考 Inline 语法规则）
      databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
        standard: # 用于单分片键的标准分片场景
          shardingColumn: user_id # 分片列名称
          shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      tableStrategy: # 分表策略，同分库策略
        standard:
          shardingColumn: order_no
          shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      keyGenerateStrategy: # 分布式序列策略
        column: id # 自增列名称，缺省表示不使用自增主键生成器
        keyGeneratorName: snowflake # 分布式序列算法名称,这里是个名字，下边(shardingAlgorithms)有定义
    # 逻辑表名称
    t_order_item:
      actualDataNodes: server-order${0..1}.t_order_item${0..1}
      databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
        standard: # 用于单分片键的标准分片场景
          shardingColumn: user_id # 分片列名称
          shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      tableStrategy: # 分表策略，同分库策略
        standard:
          shardingColumn: order_no
          shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      keyGenerateStrategy: # 分布式序列策略
        column: id # 自增列名称，缺省表示不使用自增主键生成器
        keyGeneratorName: snowflake # 分布式序列算法名称,这里是个名字，下边(shardingAlgorithms)有定义

#  defaultDatabaseStrategy: #默认数据库分片策略
#  defaultTableStrategy: #默认的表分片策略
#  defaultKeyGenerateStrategy: # 默认的分布式序列策略
#  defaultShardingColumn: # 默认分片列名称

  # 分片算法配置
  shardingAlgorithms:
    database_inline: # 自定义的算法名称，上边有用到
      type: INLINE # 分片算法类型,有多钟可参考官网 https://shardingsphere.apache.org/document/5.5.2/cn/user-manual/common-config/builtin-algorithm/sharding/
      props: # 分片算法属性配置
        algorithm-expression: server-order${user_id % 2} #根据用户id 对2（因为有2个数据库）取余数，type 为INLINE 的表达式写发可以找官网

    t_order_inline: # 自定义的算法名称，上边有用到
      type: CLASS_BASED #表示这个分片算法是基于自定义的Java类实现的分片算法。
      props: #算法的属性配置，传递给分片算法类的参数
        strategy: STANDARD
        # 指定具体用来做分片的Java类，这里使用的是ShardingSphere自带的基于 hash取模 的分片算法。
        # 这个算法的作用是：对分片字段（比如订单ID）进行hash计算，然后对分片数量取模，决定数据应该路由到哪个分片。
        algorithmClassName: org.apache.shardingsphere.sharding.algorithm.sharding.mod.HashModShardingAlgorithm
        sharding-count: 2 #指定分片总数为2，即分成2个分片。
  keyGenerators:
    snowflake: # 分布式序列算法名称
      type: SNOWFLAKE #分布式序列算法类型 - 雪花算法
#     props: # 分布式序列算法属性配置
#       worker-id: 123  # 工作节点 ID
#       max-tolerate-time-difference-milliseconds: 100

#    uuid:
#      type: UUID #分布式序列算法类型 - UUID

#通用配置
props:
  sql-show: true #是否在日志中打印 SQL打印 SQL 可以帮助开发者快速定位系统问题。日志内容包含：逻辑 SQL，真实 SQL 和 SQL 解析结果。如果开启配置，日志将使用 Topic ShardingSphere-SQL，日志级别是 INFO

```





<h3 id="ZAyYA">测试插入数据</h3>
> [http://localhost:43541/horizontal/addOrderItem](http://localhost:43541/horizontal/addOrderItem)
>

```java
package com.yren.shardingSphereDemo.controller;

/**
 * @author ChenYu ren
 * @date 2025/7/18
 */

@RestController
@RequestMapping("/horizontal")
public class ShardingSphereHorizontalTestController {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @GetMapping("addOrderItem")
    public String addOrderItem() {
        for (long i = 1; i < 3; i++) {
            Order order = new Order();
            order.setOrderNo("YREN" + i);
            order.setUserId(1L);
            orderMapper.insert(order);
            for (long j = 1; j < 3; j++) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderNo("YREN" + i);
                orderItem.setUserId(1L);
                orderItem.setPrice(new BigDecimal(10));
                orderItem.setCount(2);
                orderItemMapper.insert(orderItem);
            }
        }

        for (long i = 5; i < 7; i++) {
            Order order = new Order();
            order.setOrderNo("YREN" + i);
            order.setUserId(2L);
            orderMapper.insert(order);
            for (long j = 1; j < 3; j++) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderNo("YREN" + i);
                orderItem.setUserId(2L);
                orderItem.setPrice(new BigDecimal(1));
                orderItem.setCount(3);
                orderItemMapper.insert(orderItem);
            }
        }
        return "success";
    }

}
```



<h2 id="Oi6ya">多表关联 - 绑定表</h2>
**需求：** <font style="color:rgb(51, 51, 51);">查询每个订单的订单号和总订单金额</font>

<font style="color:rgb(51, 51, 51);"></font>

<h3 id="fNoOo">代码</h3>
```java
@RestController
@RequestMapping("/horizontal/binding")
public class ShardingSphereHorizontalBindingController {

    @Resource
    private OrderMapper orderMapper;

    @GetMapping("/find")
    public String bindingFind(){
        List<OrderVo> orderAmount = orderMapper.getOrderAmount();
        return orderAmount.toString();
    }

}
```

```java
/**
 * @author ChenYu ren
 * @date 2025/8/13
 */

@Data
public class OrderVo {
    private String orderNo;
    private BigDecimal amount;
}
```

```sql
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Select({"SELECT o.order_no, SUM(i.price * i.count) AS amount",
            "FROM t_order o JOIN t_order_item i ON o.order_no = i.order_no",
            "GROUP BY o.order_no"})
    List<OrderVo> getOrderAmount();
}
```



<h3 id="pofsS">测试关联查询</h3>
> [http://localhost:43541/horizontal/binding/find](http://localhost:43541/horizontal/binding/find)
>
> + **<font style="color:rgb(51, 51, 51);">如果不配置绑定表：测试的结果为8个SQL。</font>**<font style="color:rgb(51, 51, 51);">多表关联查询会出现笛卡尔积关联。</font>
>

```java
==>  Preparing: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order o JOIN t_order_item i ON o.order_no = i.order_no GROUP BY o.order_no
==> Parameters: 
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order o JOIN t_order_item i ON o.order_no = i.order_no GROUP BY o.order_no
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order0 o JOIN t_order_item0 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order0 o JOIN t_order_item1 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order1 o JOIN t_order_item0 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order1 o JOIN t_order_item1 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order0 o JOIN t_order_item0 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order0 o JOIN t_order_item1 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order1 o JOIN t_order_item0 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:24:54.959+08:00  INFO 32216 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order1 o JOIN t_order_item1 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
<==    Columns: order_no, amount
<==        Row: YREN1, 40.00
<==        Row: YREN2, 40.00
<==        Row: YREN5, 6.00
<==        Row: YREN6, 6.00
<==      Total: 4
```



<h3 id="hYXss">配置绑定表</h3>
> <font style="color:rgb(51, 51, 51);">在原来水平分片配置的基础上添加如下配置：</font>
>
> <font style="color:rgb(51, 51, 51);">bindingTables:  # 绑定表规则列表  
</font><font style="color:rgb(51, 51, 51);">    - t_order,t_order_item #逻辑表名1,逻辑表名2,逻辑表名3</font>
>
> **<font style="color:rgb(51, 51, 51);background-color:rgb(243, 244, 244);">绑定表：</font>**<font style="color:rgb(51, 51, 51);">指分片规则一致的一组分片表。 使用绑定表进行多表关联查询时，必须使用分片键进行关联，否则会出现笛卡尔积关联或跨库关联，从而影响查询效率。</font>
>

```yaml
# JDBC 逻辑库名称。在集群模式中，使用该参数来联通 ShardingSphere-JDBC 与 ShardingSphere-Proxy。
# 默认值：logic_db
databaseName: logic_db

mode:
  type: Standalone
  repository:
    type: JDBC

dataSources:
  server-user:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3301/db_user?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  server-order0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3310/db_order?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  server-order1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3311/db_order?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000

rules:
  # 数据分片，数据究竟写入/读取 哪个库，哪个表，按什么算法来确定
- !SHARDING
  tables:
    # 逻辑表名称
    t_user:
      actualDataNodes: server-user.t_user # 由数据源名 + 表名组成
    # 逻辑表名称
    t_order:
      actualDataNodes: server-order${0..1}.t_order${0..1} # 由数据源名 + 表名组成（参考 Inline 语法规则）
      databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
        standard: # 用于单分片键的标准分片场景
          shardingColumn: user_id # 分片列名称
          shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      tableStrategy: # 分表策略，同分库策略
        standard:
          shardingColumn: order_no
          shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      keyGenerateStrategy: # 分布式序列策略
        column: id # 自增列名称，缺省表示不使用自增主键生成器
        keyGeneratorName: snowflake # 分布式序列算法名称,这里是个名字，下边(shardingAlgorithms)有定义

    # 逻辑表名称
    t_order_item:
      actualDataNodes: server-order${0..1}.t_order_item${0..1}
      databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
        standard: # 用于单分片键的标准分片场景
          shardingColumn: user_id # 分片列名称
          shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      tableStrategy: # 分表策略，同分库策略
        standard:
          shardingColumn: order_no
          shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      keyGenerateStrategy: # 分布式序列策略
        column: id # 自增列名称，缺省表示不使用自增主键生成器
        keyGeneratorName: snowflake # 分布式序列算法名称,这里是个名字，下边(shardingAlgorithms)有定义

  bindingTables:  # 绑定表规则列表
    - t_order,t_order_item #逻辑表名1,逻辑表名2,逻辑表名3

#  defaultDatabaseStrategy: #默认数据库分片策略
#  defaultTableStrategy: #默认的表分片策略
#  defaultKeyGenerateStrategy: # 默认的分布式序列策略
#  defaultShardingColumn: # 默认分片列名称

  # 分片算法配置
  shardingAlgorithms:
    database_inline: # 自定义的算法名称，上边有用到
      type: INLINE # 分片算法类型,有多钟可参考官网 https://shardingsphere.apache.org/document/5.5.2/cn/user-manual/common-config/builtin-algorithm/sharding/
      props: # 分片算法属性配置
        algorithm-expression: server-order${user_id % 2} #根据用户id 对2（因为有2个数据库）取余数，type 为INLINE 的表达式写发可以找官网

    t_order_inline: # 自定义的算法名称，上边有用到
      type: CLASS_BASED #表示这个分片算法是基于自定义的Java类实现的分片算法。
      props: #算法的属性配置，传递给分片算法类的参数
        strategy: STANDARD
        # 指定具体用来做分片的Java类，这里使用的是ShardingSphere自带的基于 hash取模 的分片算法。
        # 这个算法的作用是：对分片字段（比如订单ID）进行hash计算，然后对分片数量取模，决定数据应该路由到哪个分片。
        algorithmClassName: org.apache.shardingsphere.sharding.algorithm.sharding.mod.HashModShardingAlgorithm
        sharding-count: 2 #指定分片总数为2，即分成2个分片。
  keyGenerators:
    snowflake: # 分布式序列算法名称
      type: SNOWFLAKE #分布式序列算法类型 - 雪花算法
#     props: # 分布式序列算法属性配置
#       worker-id: 123  # 工作节点 ID
#       max-tolerate-time-difference-milliseconds: 100

#    uuid:
#      type: UUID #分布式序列算法类型 - UUID

#通用配置
props:
  sql-show: true #是否在日志中打印 SQL打印 SQL 可以帮助开发者快速定位系统问题。日志内容包含：逻辑 SQL，真实 SQL 和 SQL 解析结果。如果开启配置，日志将使用 Topic ShardingSphere-SQL，日志级别是 INFO

```



<h3 id="mWoEg">再次测试关联查询</h3>
> <font style="color:rgb(51, 51, 51);">配置完绑定表后再次进行关联查询的测试：</font>
>
> + **<font style="color:rgb(51, 51, 51);">不配置绑定表：测试的结果为8个SQL。</font>**<font style="color:rgb(51, 51, 51);">多表关联查询会出现笛卡尔积关联。</font>
> + **<font style="color:rgb(51, 51, 51);">配置绑定表：测试的结果为4个SQL。</font>**<font style="color:rgb(51, 51, 51);"> 多表关联查询不会出现笛卡尔积关联，关联查询效率将大大提升。</font>
>

```sql
==>  Preparing: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order o JOIN t_order_item i ON o.order_no = i.order_no GROUP BY o.order_no
==> Parameters: 
2025-08-13T17:51:16.694+08:00  INFO 33165 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order o JOIN t_order_item i ON o.order_no = i.order_no GROUP BY o.order_no
2025-08-13T17:51:16.694+08:00  INFO 33165 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order0 o JOIN t_order_item0 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:51:16.694+08:00  INFO 33165 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order1 o JOIN t_order_item1 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:51:16.694+08:00  INFO 33165 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order0 o JOIN t_order_item0 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
2025-08-13T17:51:16.694+08:00  INFO 33165 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT o.order_no, SUM(i.price * i.count) AS amount FROM t_order1 o JOIN t_order_item1 i ON o.order_no = i.order_no GROUP BY o.order_no ORDER BY o.order_no ASC 
<==    Columns: order_no, amount
<==        Row: YREN1, 40.00
<==        Row: YREN2, 40.00
<==        Row: YREN5, 6.00
<==        Row: YREN6, 6.00
<==      Total: 4
```



<h2 id="uGcY2">广播表</h2>
> <font style="color:rgb(51, 51, 51);">指所有的分片数据源中都存在的表，表结构及其数据在每个数据库中均完全一致。 适用于数据量不大且需要与海量数据的表进行关联查询的场景，例如：</font>**<font style="color:rgb(51, 51, 51);">字典表</font>**
>
> **<font style="color:rgb(51, 51, 51);">广播具有以下特性：</font>**
>
> 1. <font style="color:rgb(51, 51, 51);">插入、更新操作会实时在所有节点上执行，保持各个分片的数据一致性</font>
> 2. <font style="color:rgb(51, 51, 51);">查询操作，只从一个节点获取</font>
> 3. <font style="color:rgb(51, 51, 51);">可以跟任何一个表进行 JOIN 操作</font>
>



<h3 id="mikKC">创建广播表</h3>
```sql
CREATE TABLE t_dict(
    id BIGINT,
    dict_type VARCHAR(200),
    PRIMARY KEY(id)
);
```



<h3 id="UM64k">代码实现</h3>
```sql
/**
 * @author ChenYu ren
 * @date 2025/8/13
 */

@RestController
@RequestMapping("/horizontal/broadcast")
public class ShardingSphereHorizontalBroadcastController {

    @Resource
    private DictMapper dictMapper;

    @GetMapping("/add")
    public String broadcastTableAdd(){
        Dict dict = new Dict();
        dict.setDictType("type1");
        dictMapper.insert(dict);
        return "success";
    }

    @GetMapping("/findAll")
    public String findAll(){
        return dictMapper.selectList(null).toString();
    }


    @GetMapping("updateById")
    public String updateById(@RequestParam("id")Long id) {
        Dict dict = new Dict();
        dict.setId(id);
        dict.setDictType(String.valueOf(System.currentTimeMillis()));
        dictMapper.updateById(dict);
        return "success";
    }


    @GetMapping("delById")
    public String delById(@RequestParam("id")Long id) {
        Dict dict = new Dict();
        dict.setId(id);
        dictMapper.deleteById(dict);
        return "success";
    }

}
```

```java
/**
 * @author ChenYu ren
 * @date 2025/8/14
 */

@TableName("t_dict")
@Data
public class Dict {
    //可以使用MyBatisPlus的雪花算法
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String dictType;
}
```

```java
/**
 * @author ChenYu ren
 * @date 2025/8/14
 */

@Mapper
public interface DictMapper extends BaseMapper<Dict> {}
```



<h3 id="FGnIV"> 广播表配置</h3>
> rules:
>
> - !BROADCAST
>
>   tables: # 广播表规则列表
>
>     - <table_name>
>
>     - <table_name>
>

```yaml
# JDBC 逻辑库名称。在集群模式中，使用该参数来联通 ShardingSphere-JDBC 与 ShardingSphere-Proxy。
# 默认值：logic_db
databaseName: logic_db

mode:
  type: Standalone
  repository:
    type: JDBC

dataSources:
  server-user:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3301/db_user?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  server-order0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3310/db_order?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
  server-order1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3311/db_order?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000

rules:
  # 数据分片，数据究竟写入/读取 哪个库，哪个表，按什么算法来确定
- !SHARDING
  tables:
    # 逻辑表名称
    t_user:
      actualDataNodes: server-user.t_user # 由数据源名 + 表名组成
    # 逻辑表名称
    t_order:
      actualDataNodes: server-order${0..1}.t_order${0..1} # 由数据源名 + 表名组成（参考 Inline 语法规则）
      databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
        standard: # 用于单分片键的标准分片场景
          shardingColumn: user_id # 分片列名称
          shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      tableStrategy: # 分表策略，同分库策略
        standard:
          shardingColumn: order_no
          shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      keyGenerateStrategy: # 分布式序列策略
        column: id # 自增列名称，缺省表示不使用自增主键生成器
        keyGeneratorName: snowflake # 分布式序列算法名称,这里是个名字，下边(shardingAlgorithms)有定义

    # 逻辑表名称
    t_order_item:
      actualDataNodes: server-order${0..1}.t_order_item${0..1}
      databaseStrategy: #分库策略 缺省表示使用默认分库策略，分片策略只能选其一
        standard: # 用于单分片键的标准分片场景
          shardingColumn: user_id # 分片列名称
          shardingAlgorithmName: database_inline  # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      tableStrategy: # 分表策略，同分库策略
        standard:
          shardingColumn: order_no
          shardingAlgorithmName: t_order_inline # 分片算法名称（自定义）会与下方shardingAlgorithms对应
      keyGenerateStrategy: # 分布式序列策略
        column: id # 自增列名称，缺省表示不使用自增主键生成器
        keyGeneratorName: snowflake # 分布式序列算法名称,这里是个名字，下边(shardingAlgorithms)有定义

  bindingTables:  # 绑定表规则列表
    - t_order,t_order_item #逻辑表名1,逻辑表名2,逻辑表名3
- !BROADCAST
  tables:
    - t_dict

#  defaultDatabaseStrategy: #默认数据库分片策略
#  defaultTableStrategy: #默认的表分片策略
#  defaultKeyGenerateStrategy: # 默认的分布式序列策略
#  defaultShardingColumn: # 默认分片列名称

  # 分片算法配置
  shardingAlgorithms:
    database_inline: # 自定义的算法名称，上边有用到
      type: INLINE # 分片算法类型,有多钟可参考官网 https://shardingsphere.apache.org/document/5.5.2/cn/user-manual/common-config/builtin-algorithm/sharding/
      props: # 分片算法属性配置
        algorithm-expression: server-order${user_id % 2} #根据用户id 对2（因为有2个数据库）取余数，type 为INLINE 的表达式写发可以找官网

    t_order_inline: # 自定义的算法名称，上边有用到
      type: CLASS_BASED #表示这个分片算法是基于自定义的Java类实现的分片算法。
      props: #算法的属性配置，传递给分片算法类的参数
        strategy: STANDARD
        # 指定具体用来做分片的Java类，这里使用的是ShardingSphere自带的基于 hash取模 的分片算法。
        # 这个算法的作用是：对分片字段（比如订单ID）进行hash计算，然后对分片数量取模，决定数据应该路由到哪个分片。
        algorithmClassName: org.apache.shardingsphere.sharding.algorithm.sharding.mod.HashModShardingAlgorithm
        sharding-count: 2 #指定分片总数为2，即分成2个分片。
  keyGenerators:
    snowflake: # 分布式序列算法名称
      type: SNOWFLAKE #分布式序列算法类型 - 雪花算法
#     props: # 分布式序列算法属性配置
#       worker-id: 123  # 工作节点 ID
#       max-tolerate-time-difference-milliseconds: 100

#    uuid:
#      type: UUID #分布式序列算法类型 - UUID

#通用配置
props:
  sql-show: true #是否在日志中打印 SQL打印 SQL 可以帮助开发者快速定位系统问题。日志内容包含：逻辑 SQL，真实 SQL 和 SQL 解析结果。如果开启配置，日志将使用 Topic ShardingSphere-SQL，日志级别是 INFO

```



<h3 id="QGvST">测试广播表</h3>
> **新增数据**
>
> [http://localhost:43541/horizontal/broadcast/add](http://localhost:43541/horizontal/broadcast/add)
>
> **广播表效果：**每个服务器中的t_dict同时添加了新数据
>

```shell
==>  Preparing: INSERT INTO t_dict ( id, dict_type ) VALUES ( ?, ? )
==> Parameters: 1955827147660783618(Long), type1(String)
2025-08-14T11:01:46.752+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Logic SQL: INSERT INTO t_dict  ( id, dict_type )  VALUES (  ?, ?  )
2025-08-14T11:01:46.752+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: INSERT INTO t_dict  ( id, dict_type )  VALUES (  ?, ?  ) ::: [1955827147660783618, type1]
2025-08-14T11:01:46.752+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-user ::: INSERT INTO t_dict  ( id, dict_type )  VALUES (  ?, ?  ) ::: [1955827147660783618, type1]
2025-08-14T11:01:46.752+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-1] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: INSERT INTO t_dict  ( id, dict_type )  VALUES (  ?, ?  ) ::: [1955827147660783618, type1]
<==    Updates: 1
```



> **查询数据**
>
> [**http://localhost:43541/horizontal/broadcast/findAll**](http://localhost:43541/horizontal/broadcast/findAll)
>
>  **广播表效果：**只从一个节点获取数据，随机负载均衡规则
>

```shell
//第一次查询
==>  Preparing: SELECT id,dict_type FROM t_dict
==> Parameters: 
2025-08-14T11:04:12.226+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Logic SQL: SELECT  id,dict_type  FROM t_dict
2025-08-14T11:04:12.226+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-3] ShardingSphere-SQL                       : Actual SQL: server-user ::: SELECT  id,dict_type  FROM t_dict
<==    Columns: id, dict_type
<==        Row: 1955827147660783618, type1
<==      Total: 1

//第二次查询
==>  Preparing: SELECT id,dict_type FROM t_dict
==> Parameters: 
2025-08-14T11:04:19.905+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-4] ShardingSphere-SQL                       : Logic SQL: SELECT  id,dict_type  FROM t_dict
2025-08-14T11:04:19.905+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-4] ShardingSphere-SQL                       : Actual SQL: server-order1 ::: SELECT  id,dict_type  FROM t_dict
<==    Columns: id, dict_type
<==        Row: 1955827147660783618, type1
<==      Total: 1

//第三次查询
==>  Preparing: SELECT id,dict_type FROM t_dict
==> Parameters: 
2025-08-14T11:04:22.613+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-5] ShardingSphere-SQL                       : Logic SQL: SELECT  id,dict_type  FROM t_dict
2025-08-14T11:04:22.614+08:00  INFO 49307 --- [sharding-jdbc-demo-yren] [io-43541-exec-5] ShardingSphere-SQL                       : Actual SQL: server-order0 ::: SELECT  id,dict_type  FROM t_dict
<==    Columns: id, dict_type
<==        Row: 1955827147660783618, type1
<==      Total: 1
```



<h1 id="NxL2Z">数据分片扩容 - 基因法</h1>
<h2 id="xIefT">初始阶段 2 张表</h2>
> 按 `UserId` 分片
>
> 规则：**取用户ID的最低 1 位**（k = 1）
>

```java
public static void main(String[] args) throws ExecutionException, InterruptedException {
    int userId = 100001;
    System.out.println("二进制：" + Integer.toBinaryString(userId)); //11000011010100001
    System.out.println(userId & 1); //1 -> 最低位 = 1 → 表 1
}


public static void main(String[] args) throws ExecutionException, InterruptedException {
    int userId = 100000;
    System.out.println("二进制：" + Integer.toBinaryString(userId)); //11000011010100000
    System.out.println(userId & 1);//0 -> 最低位 = 0 → 表 0
}
```

**举例**

> 可以看到，每个用户ID最低位决定落哪张表。
>

| 用户ID (十进制) | 二进制 | shard_id (2 表) | 落表 |
| --- | --- | --- | --- |
| 2 | 010 | 0 | 表0 |
| 3 | 011 | 1 | 表1 |
| 4 | 100 | 0 | 表0 |
| 5 | 101 | 1 | 表1 |


<h2 id="lJ1e5">扩容阶段：2 -> 4 张表</h2>
> 按 `UserId` 分片
>
> 新规则：**取用户ID的最低 2 位**（k = 2）
>
> **结果：**2张表 “一分为二”，变成 4 张表
>

```java
public static void main(String[] args) {
    int userId = 100001;
    String binaryString = Integer.toBinaryString(userId);
    //数据分片目标表
    int dataShardId = userId & 3; // 3 = 0b11,这里3表示取低2位
}
```

| 旧表 (2表) | 低 2 位 = 【00,01,10,11】四种情况 | 新表 (4表) |
| --- | --- | --- |
| 表0 | 00 → 表0 | 表 0 |
| 表0 | 10 → 表2 | 表 2 |
| 表1 | 01 → 表1 | 表1 |
| 表1 | 11 → 表3 | 表 3 |


> 解释：
>
> + **<font style="color:#DF2A3F;">旧表0</font>**（最低位 = 0） → 新`表0` 和 `表2`
> + **<font style="color:#DF2A3F;">旧表1</font>**（最低位 = 1） → 新`表1` 和 `表3`
>



<h2 id="vzWvQ">数据迁移</h2>
<h3 id="P1XBP">旧表数据：</h3>
| 用户ID | 二进制 | 旧 shard_id | 原表 |
| --- | --- | --- | --- |
| 2 | 01**<font style="color:#DF2A3F;">0</font>** | 0 | 表0 |
| 3 | 01**<font style="color:#DF2A3F;">1</font>** | 1 | 表1 |
| 4 | 10**<font style="color:#DF2A3F;">0</font>** | 0 | 表0 |
| 5 | 10**<font style="color:#DF2A3F;">1</font>** | 1 | 表1 |


---

<h3 id="RaK0F">迁移逻辑</h3>
> + 遍历旧表数据 → 用 **低 2 位** 计算新分片号
> + 如果新分片号 = 原表 → **<font style="color:#DF2A3F;">不动</font>**
> + 如果新分片号 ≠ 原表 → **<font style="color:#8CCF17;">移动到对应新表</font>**
>

---

<h3 id="kb327">迁移结果</h3>
> + 表0里的 2 被迁移到表2
> + 表0里的 4 保留在表0
> + 表1里的 3 被迁移到表3
> + 表1里的 5 保留在表1
>

| 用户ID | 二进制 | 新 shard_id | 新表 |
| --- | --- | --- | --- |
| 2 | 0**<font style="color:#DF2A3F;">10</font>** | 2 | 表 2 |
| 3 | 0**<font style="color:#DF2A3F;">11</font>** | 3 | 表 3 |
| 4 | 1**<font style="color:#DF2A3F;">00</font>** | 0 | 表 0 |
| 5 | 1**<font style="color:#DF2A3F;">01</font>** | 1 | 表 1 |


<h3 id="kMfa6">总结迁移特点</h3>
1. 每张旧表 **只需要迁移一半的数据** → 避免全库洗牌
2. 迁移路径固定 → 扩容可控
3. 新表数 = 2 × 旧表数，每次扩容都是“一分为二”
4. 应用层路由公式：

```java
int shardId = userId & ((1 << k) - 1);
k = 当前分片位数（2张表 k=1，4张表 k=2）
```



<h2 id="S4ZhU">跨多倍扩容会怎么样？</h2>
> **<font style="color:#DF2A3F;">如果我从2张表 直接 扩容到8张表 还是一半迁移 一半保留吗？（2 的 </font>**`**<font style="color:#DF2A3F;">1 次方</font>**`**<font style="color:#DF2A3F;"> 到 2 的 </font>**`**<font style="color:#DF2A3F;">3 次方</font>**`**<font style="color:#DF2A3F;">）</font>**
>



<h3 id="cs6WL">规则回顾</h3>
> + **基因法**每次扩容，分片号 = 用户ID低 **K** 位
> + 扩容时，旧表的数据需要根据 **新增位** 决定目标表
>
> 
>
> + **旧表**（2 表）是根据最低 1 位分片
> + **新表**（8 表）是根据最低 3 位分片
>

| 表数 | k 值 | 分片计算 |
| --- | --- | --- |
| 2 | 1 | shard_id = userId & 1 |
| 8 | 3 | shard_id = userId & 7 |


---

<h3 id="dwdzj">  
数据迁移分析</h3>
+ 旧表 0（最低位 = 0） → 新表可能是：

| 用户ID低3位 | 新表 |
| --- | --- |
| 000 | 0 |
| 010 | 2 |
| 100 | 4 |
| 110 | 6 |


+ 旧表 1（最低位 = 1） → 新表可能是：

| 用户ID低3位 | 新表 |
| --- | --- |
| 001 | 1 |
| 011 | 3 |
| 101 | 5 |
| 111 | 7 |


可以看到：

+ **旧表 0 的数据被分成 4 个新表（0,2,4,6）**
+ **旧表 1 的数据被分成 4 个新表（1,3,5,7）**
+ **不是一半迁移一半保留**，而是 **旧表数据平均拆成多份**

---

<h3 id="teJaS">小结规律</h3>
+ **每次扩容最好逐步按 2 倍扩容**
    - 2 → 4 → 8 → 16 …
+ 如果直接跨多倍扩容，迁移量分布不再是“一半”，而是 **一张旧表拆成多张新表**
+ 迁移复杂度增加，但仍然是 **规律可控**

