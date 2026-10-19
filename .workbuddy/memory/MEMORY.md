# 仓库管理系统（课题3）长期备忘

## 架构
- 课题3：Swing 客户端 + Servlet 服务端 + MySQL，HTTP/JSON 通信，客户端不含数据库凭证。
- warehouse-client：Maven + shade fat jar，入口 com.warehouse.client.App；maven.compiler.release=11（源码用了 var/Map.of，JDK 11 起）。
- warehouse-server：war 包 finalName=warehouse，Tomcat 9（javax.*），Servlet+JDBC 调存储过程。

## 数据库（MySQL 8.4，本机 localhost:3306）
- 库 warehouse，3 表（warehouse_user/_material/_inout）+ 3 存储过程（warehouse_sp_in/_out/_inout_multi）。
- 排序规则必须全链路统一 utf8mb4_0900_ai_ci：SP 参数继承库排序规则、表列默认用字符集默认规则，混用会报 Illegal mix of collations。脚本已改好，勿改回 unicode_ci。
- 出仓并发安全：SELECT...FOR UPDATE 行级锁；多物料：JSON_TABLE 解析明细，事务+回滚。
- 凭证：root/123456（与 2026-10-10 日志记录一致）。

## 运行要点
- 客户端：warehouse-client 下 mvn clean package → java -jar target/warehouse-client.jar；登录 http://localhost:8080/warehouse，admin/admin。
- 服务端：target/warehouse.war → Tomcat webapps；Tomcat 9.0.122 在 D:\1software\javasoft\apache-tomcat-9.0.122。
- 用户环境：JDK 23（D:\jdk\ddd），Maven 3.9.4（阿里云镜像），mysql.exe 在 PATH。

## 已修过的坑（答辩可讲）
- InoutDao 缺 import Result（10-09）；JsonUtil.write/Result.toJson 的 throws Exception 连锁报错（10-10）；SQL 库名被双重替换成 warehouse_warehouse（10-10）；client pom 1.8 编不过 var（10-16）；collation 混用（10-16）。
