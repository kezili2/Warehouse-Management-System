# 仓库管理系统（客户机/服务器版） — 服务器端骨架

> 课题3 极简方案：Java Servlet + JDBC + JSON，部署到 Tomcat 9。

## 项目结构

```
warehouse-server/
├── pom.xml                          # Maven 构建（servlet-api + mysql-connector + jackson）
├── README.md
└── src/main/
    ├── java/com/warehouse/
    │   ├── util/
    │   │   ├── DBUtil.java          # 加载 db.properties、获取连接
    │   │   ├── CharsetFilter.java   # 统一 UTF-8 + 跨域头
    │   │   ├── Result.java          # 统一响应体 {code,flag,msg,data}
    │   │   └── JsonUtil.java        # 写 JSON 响应
    │   ├── dao/                     # 持久化层（直接 JDBC）
    │   │   ├── UserDao.java
    │   │   ├── MaterialDao.java
    │   │   └── InoutDao.java
    │   ├── service/                 # 业务逻辑层
    │   │   ├── UserService.java
    │   │   ├── MaterialService.java
    │   │   └── InoutService.java
    │   └── servlet/                 # 表示层（Servlet）
    │       ├── LoginServlet.java    /api/login
    │       ├── UsersServlet.java    /api/users
    │       ├── MaterialsServlet.java/api/materials/*
    │       ├── BillServlet.java     /api/bill/*
    │       └── InoutServlet.java    /api/inout/*
    ├── resources/
    │   └── db.properties            # 数据库连接配置（账号密码仅此一处）
    └── webapp/WEB-INF/
        └── web.xml                  # Servlet 映射
```

## 三层职责

| 层 | 包 | 职责 |
|---|---|---|
| 表示层 | `servlet` | 解析 HTTP / 路径分发 / 调用 Service / 写 JSON |
| 业务逻辑层 | `service` | 参数校验、业务规则 |
| 数据持久化层 | `dao` | SQL 与存储过程调用 |

## 编译与部署

```bash
# 1. 确认 MySQL 8 已运行，且执行过仓库管理系统_数据库设计.sql
# 2. 修改 src/main/resources/db.properties 中的 jdbc.password
# 3. 编译打包
mvn clean package
# 4. 部署到 Tomcat 9
cp target/warehouse.war $TOMCAT_HOME/webapps/
# 5. 启动 Tomcat
$TOMCAT_HOME/bin/startup.sh
```

部署后访问根路径：`http://localhost:8080/warehouse/`

## 接口对照（与 HTTP接口清单.md 一致）

| Servlet | 路径模式 | 方法 | 接口 |
|---|---|---|---|
| LoginServlet    | /api/login         | POST   | 登录 |
| UsersServlet    | /api/users         | GET    | 用户列表 |
| MaterialsServlet| /api/materials     | GET    | 物料模糊查询 |
| MaterialsServlet| /api/materials/{c} | GET    | 物料详情 |
| MaterialsServlet| /api/materials     | POST   | 新增物料 |
| MaterialsServlet| /api/materials/{c} | PUT    | 修改物料 |
| MaterialsServlet| /api/materials/{c} | DELETE | 删除物料 |
| BillServlet     | /api/bill/next     | GET    | 生成单号 |
| BillServlet     | /api/bill/{no}     | GET    | 单号明细 |
| InoutServlet    | /api/inout/single  | POST   | 单物料进出仓 |
| InoutServlet    | /api/inout/multi   | POST   | 多物料进出仓 |
| InoutServlet    | /api/inout/records | GET    | 记录查询 |

## 安全红线

- `db.properties` 放在 **服务端**，**绝对不可**出现在客户端工程中。
- 客户端代码中**不得出现**数据库用户名/密码。
- 单号生成、库存扣减均在**服务端**完成，客户端不直接触库。

## 给报告的"亮点"补充

| 评分点 | 在代码中的体现 |
|---|---|
| 事务处理 | 全部交给 `warehouse_sp_in / out / inout_multi` 内部事务 |
| 异常处理 | `Result.err()` + `readProcResult()` 透传存储过程 SIGNAL 信息 |
| 检索优化 | `idx_mat_name / idx_mat_spec / idx_io_date` 等索引在 SQL 脚本中 |
| 并发冲突 | `sp_out` 使用 `SELECT...FOR UPDATE` 行级锁 |
| 网络通信 | JSON（Jackson 序列化），见 `Result.toJson()` |
| 算法时空优化 | 多物料进出仓用 `INSERT...SELECT` + `UPDATE...JOIN` 一次完成 |
