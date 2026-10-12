# 仓库管理系统（客户机/服务器版） — Swing 客户端

> 课题3 极简方案：Java Swing + Jackson + HttpURLConnection。
> **客户端不包含任何数据库账号密码**，仅保存服务器地址。

## 项目结构

```
warehouse-client/
├── pom.xml                                   Maven + maven-shade-plugin（fat jar）
├── README.md
└── src/main/java/com/warehouse/client/
    ├── App.java                              启动入口
    ├── net/
    │   ├── Session.java                      当前登录用户 + 服务器地址
    │   └── ApiClient.java                    12 个接口的 HTTP 封装
    └── ui/
        ├── LoginFrame.java                   登录窗口
        ├── MainFrame.java                    主窗口（3 个 Tab + 菜单 + 状态栏）
        ├── AboutDialog.java                  关于（显示姓名+家庭住址）
        ├── MaterialPanel.java                子系统1：物料档案
        ├── SingleInoutPanel.java             子系统2：单物料进出仓
        └── MultiInoutPanel.java              子系统3：多物料进出仓
```

## 启动步骤

```bash
# 1. 确认服务端已启动（参见 warehouse-server/README.md）
# 2. 在 warehouse-client 目录：
mvn clean package
# 3. 运行
java -jar target/warehouse-client.jar
# 4. 登录窗口填入：
#    - 服务器地址：http://localhost:8080/warehouse
#    - 用户名：admin   密码：admin
```

## 三个 Tab 的功能

| Tab | 关键控件 | 题目要求对应 |
|---|---|---|
| 物料档案管理 | 模糊搜索 + 表格 + 表单；单位下拉可编辑 | 代码不空不重复、单位下拉+可自输、默认库存0 |
| 单物料进出仓 | 单号生成按钮 + 单/多条件查询 + 提交 | 操作人员=当前用户、经手人下拉、库存判断 |
| 多物料进出仓 | 单号 + 明细表格（物料下拉+数量）+ 提交 | 同一单号只能同进或同出、失败全部回滚 |

## 客户端安全红线（自检清单）

- ✅ 客户端工程中**不包含** `db.properties`、数据库用户名/密码
- ✅ 所有数据库操作通过 HTTP 接口（/api/...）间接完成
- ✅ 操作人员由 `Session.userCode` 决定，提交时通过 `X-User-Code` 头传给服务端
- ✅ 单号、库存判断、并发安全等全部由服务端保证
