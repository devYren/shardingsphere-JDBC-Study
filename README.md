# ShardingSphere JDBC 学习Demo

## 项目简介

这是一个基于 **Apache ShardingSphere JDBC** 的学习演示项目，用于学习和实践数据库分片、读写分离等分布式数据库解决方案。

## 技术栈

- **Spring Boot**: 3.4.4
- **Apache ShardingSphere JDBC**: 5.5.2
- **MyBatis Plus**: 3.5.12
- **MySQL**: 数据库
- **Java**: 21
- **Maven**: 项目管理工具

## 功能特性

### 🔄 读写分离
- 主从数据库配置
- 读写请求自动路由
- 负载均衡策略
- 事务一致性保证

### 📊 垂直分片
- 按业务模块分库
- 用户库与订单库分离
- 跨库查询支持

### 🔀 水平分片
- 按分片键进行数据分表
- 支持多种分片算法
- 绑定表关联查询
- 广播表配置

## 项目结构

```
src/
├── main/
│   ├── java/com/yren/shardingSphereDemo/
│   │   ├── controller/          # 控制器层
│   │   ├── entity/             # 实体类
│   │   └── mapper/             # 数据访问层
│   └── resources/
│       ├── application.yaml                                    # 主配置文件
│       ├── application-shardingsphere-readwrite-config.yaml   # 读写分离配置
│       ├── application-shardingsphere-vertical-config.yaml    # 垂直分片配置
│       └── application-shardingsphere-horizontal-config.yaml  # 水平分片配置
└── doc/
    ├── 笔记/                   # 学习笔记
    ├── 源码/                   # 参考源码
    └── 资料/                   # 相关资料
```

## 快速开始

### 1. 环境要求
- JDK 21+
- Maven 3.6+
- MySQL 8.0+

### 2. 启动项目
```bash
# 克隆项目
git clone <repository-url>

# 进入项目目录
cd shardingsphere-JDBC-yrendemo

# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

### 3. 访问测试
项目启动后，访问 `http://localhost:43541` 进行功能测试。

## 测试接口

### 读写分离测试
- `GET /test?username=张三` - 插入用户数据（写主库）
- `GET /list` - 查询用户列表（读从库）

### 垂直分片测试
- `GET /vertical/add` - 添加用户和订单
- `GET /vertical/list` - 查询分库数据

### 水平分片测试
- `GET /horizontal/add` - 添加分片数据
- `GET /horizontal/find` - 查询分片数据

### 广播表测试
- `GET /horizontal/broadcast/add` - 添加广播表数据
- `GET /horizontal/broadcast/findAll` - 查询广播表

## 学习要点

1. **配置管理**: 学习不同场景下的 ShardingSphere 配置
2. **分片策略**: 理解各种分片算法的应用场景
3. **读写分离**: 掌握主从架构下的数据路由
4. **事务处理**: 了解分布式事务的处理机制
5. **性能优化**: 学习分片带来的性能提升

## 注意事项

- 本项目仅用于学习目的，生产环境使用需要进一步优化
- 数据库连接配置需要根据实际环境调整
- 建议先阅读 `doc/笔记/` 目录下的学习资料

## 参考资料

- [Apache ShardingSphere 官方文档](https://shardingsphere.apache.org/)
- 项目内置学习笔记：`doc/笔记/`
- 示例源码：`doc/源码/`

---

**Happy Learning! 🎉**