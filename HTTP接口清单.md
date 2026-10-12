# HTTP 接口清单（极简方案）

- 基础路径：`http://服务器地址:8080/warehouse`
- 数据格式：均为 **JSON**
- 鉴权：登录后所有受保护接口需在 HTTP 头携带 `X-User-Code: <操作人员代码>`
- 统一响应体：
```json
{ "code": 1, "flag": "OK", "msg": "成功", "data": { ... } }
```
- 错误时 `code` 为 0 或负数，`flag` 为 `ERR`，`msg` 为可向用户展示的中文说明

---

## 一、登录（子系统通用前置）

| 方法 | 路径 | 入参 | 返回 data |
|---|---|---|---|
| POST | `/api/login` | `{username, password}` | `{userCode, realName, address}` |

---

## 二、用户列表（用于"经手人"下拉）

| 方法 | 路径 | 入参 | 返回 data |
|---|---|---|---|
| GET | `/api/users` | — | `[{userCode, realName}, ...]` |

---

## 三、物料档案（子系统1）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/api/materials` | `q`（模糊关键字，可选） | 列表，模糊匹配代码/名称/规格 |
| GET | `/api/materials/{code}` | — | 单条详情 |
| POST | `/api/materials` | `{matCode, matName, spec, unit, stock, remark}` | 新增；代码为空时由服务器自动生成 |
| PUT | `/api/materials/{code}` | `{matName, spec, unit, stock, remark}` | 修改（不改代码） |
| DELETE | `/api/materials/{code}` | — | 删除（有进出记录时拒绝） |

**物料自动生成代码规则**（写进报告）：`M` + `yyyyMMdd` + 3 位流水，例如 `M20261009001`。

---

## 四、单号生成（子系统2/3 共用）

| 方法 | 路径 | 入参 | 返回 data |
|---|---|---|---|
| GET | `/api/bill/next` | `type`（I/O/MI/MO），`date`（yyyy-MM-dd） | `{"billNo":"I20261009001"}` |

**单号规则**（写进报告）：

| 类型 | 格式 | 示例 |
|---|---|---|
| 单物料进 | `I`+`yyyyMMdd`+3 位流水 | `I20261009001` |
| 单物料出 | `O`+`yyyyMMdd`+3 位流水 | `O20261009001` |
| 多物料进 | `MI`+`yyyyMMdd`+3 位流水 | `MI20261009001` |
| 多物料出 | `MO`+`yyyyMMdd`+3 位流水 | `MO20261009001` |

---

## 五、单物料进出仓（子系统2）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| POST | `/api/inout/single` | 见下方 JSON | 调用 `warehouse_sp_in` / `warehouse_sp_out` |

请求示例（进仓）：
```json
{
  "ioType": "I",
  "billNo": "I20261009001",
  "matCode": "M0001",
  "qty": 5,
  "opDate": "2026-10-09",
  "operator": "U001",
  "handler": "U002",
  "remark": "测试进仓"
}
```

> 请求中 `operator` 与 HTTP 头 `X-User-Code` 需一致，否则服务端拒绝（保证操作人员=当前登录用户）。

---

## 六、多物料进出仓（子系统3）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| POST | `/api/inout/multi` | 见下方 JSON | 调用 `warehouse_sp_inout_multi` |

请求示例（多物料进仓）：
```json
{
  "ioType": "I",
  "billNo": "MI20261009001",
  "opDate": "2026-10-09",
  "operator": "U001",
  "handler": "U002",
  "remark": "批量进仓",
  "items": [
    {"matCode": "M0001", "qty": 2},
    {"matCode": "M0003", "qty": 10}
  ]
}
```

---

## 七、进出仓记录查询（子系统2/3 共用）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/api/inout/records` | `dateFrom`, `dateTo`, `matCode`, `operator`, `remark`（全可选） | 组合条件查询 |
| GET | `/api/bill/{billNo}` | — | 按单号查全部明细（多物料单据用） |

返回 data 元素结构：
```json
{
  "billNo":"MI20261009001",
  "ioType":"I",
  "opDate":"2026-10-09",
  "operator":"U001",
  "handler":"U002",
  "remark":"批量进仓",
  "details":[
    {"matCode":"M0001","matName":"螺丝刀","qty":2},
    {"matCode":"M0003","matName":"螺栓","qty":10}
  ]
}
```

---

## 总览（共 11 个接口）

| # | 方法 | 路径 | 用途 |
|---|---|---|---|
| 1 | POST | `/api/login` | 登录 |
| 2 | GET | `/api/users` | 经手人下拉 |
| 3 | GET | `/api/materials` | 物料模糊查询 |
| 4 | GET | `/api/materials/{code}` | 物料详情 |
| 5 | POST | `/api/materials` | 新增物料 |
| 6 | PUT | `/api/materials/{code}` | 修改物料 |
| 7 | DELETE | `/api/materials/{code}` | 删除物料 |
| 8 | GET | `/api/bill/next` | 生成新单号 |
| 9 | POST | `/api/inout/single` | 单物料进出仓 |
| 10 | POST | `/api/inout/multi` | 多物料进出仓 |
| 11 | GET | `/api/inout/records` | 进出仓记录查询 |
| 12 | GET | `/api/bill/{billNo}` | 按单号查明细 |

---

## 写报告可引用的两点设计说明

1. **为什么选 JSON 不选 XML/字符串**：JSON 体量小、解析快（Java 有 `jackson`/内置库），且与"封装效率"评分点对应。字符串最差（易错），XML 解析开销大。
2. **为什么把单号生成放在服务端**：避免多客户端同时生成导致流水号冲突；服务端用 `t_billseq(日期, 类型, 流水)` 维护原子自增。
