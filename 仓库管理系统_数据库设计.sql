-- =====================================================================
--   仓库管理系统（客户机/服务器版） 数据库脚本
--   课题3  ——  极简方案（3 张表 + 3 个存储过程）
--   数据库：MySQL 8.0+，字符集 utf8mb4，存储引擎 InnoDB
--   提示：  实际使用时请把 `warehouse` 全局替换为你的"班级+座号"字符串。
-- =====================================================================

-- ---------- 0. 建库 ----------
DROP DATABASE IF EXISTS `warehouse`;
CREATE DATABASE `warehouse`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `warehouse`;

-- ============================================================
-- 1) 人员档案（同时承担登录账号职责）
--    界面"关于"窗口中显示真实姓名与家庭住址
-- ============================================================
CREATE TABLE `warehouse_user` (
  id          INT          NOT NULL AUTO_INCREMENT,
  user_code   VARCHAR(20)  NOT NULL COMMENT '操作人员代码',
  username    VARCHAR(40)  NOT NULL COMMENT '登录账号',
  password    VARCHAR(64)  NOT NULL COMMENT '密码（演示用 SHA2-256）',
  real_name   VARCHAR(40)  NOT NULL COMMENT '姓名',
  address     VARCHAR(200) NOT NULL COMMENT '家庭住址',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_code  (user_code),
  UNIQUE KEY uk_username   (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员档案 / 登录账号';

-- ============================================================
-- 2) 物料档案
--    计量单位：件/套/公斤/吨/升/米/毫米/个 + 可手动输入其他
--    库存数量默认 0，模糊查询命中 (name, spec)
-- ============================================================
CREATE TABLE `warehouse_material` (
  mat_code  VARCHAR(20)  NOT NULL COMMENT '物料代码（主键，手输或自动生成）',
  mat_name  VARCHAR(50)  NOT NULL COMMENT '物料名称',
  spec      VARCHAR(50)  DEFAULT NULL COMMENT '规格型号',
  unit      VARCHAR(10)  NOT NULL DEFAULT '个' COMMENT '计量单位',
  stock     INT          NOT NULL DEFAULT 0 COMMENT '库存数量',
  remark    VARCHAR(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (mat_code),
  KEY idx_mat_name (mat_name),
  KEY idx_mat_spec (spec)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料档案';

-- ============================================================
-- 3) 进出仓记录（1 张表方案）
--    同一 bill_no 可有多条记录 → 直接对应"多物料"业务
--    io_type: 'I'=进仓  'O'=出仓
-- ============================================================
CREATE TABLE `warehouse_inout` (
  id        BIGINT       NOT NULL AUTO_INCREMENT,
  bill_no   VARCHAR(30)  NOT NULL COMMENT '进出仓单号',
  io_type   CHAR(1)      NOT NULL COMMENT 'I=进仓 O=出仓',
  mat_code  VARCHAR(20)  NOT NULL COMMENT '物料代码',
  qty       INT          NOT NULL COMMENT '数量',
  op_date   DATE         NOT NULL COMMENT '进出仓日期',
  operator  VARCHAR(20)  NOT NULL COMMENT '操作人员代码',
  handler   VARCHAR(20)  NOT NULL COMMENT '经手人代码',
  remark    VARCHAR(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_bill_mat (bill_no, mat_code),
  KEY idx_io_date  (op_date),
  KEY idx_io_op    (operator),
  KEY idx_io_mat   (mat_code),
  KEY idx_io_bill  (bill_no),
  CONSTRAINT chk_io_type CHECK (io_type IN ('I','O')),
  CONSTRAINT chk_io_qty  CHECK (qty > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进出仓记录';

-- ============================================================
-- 初始化数据
-- ============================================================
INSERT INTO `warehouse_user` (user_code,username,password,real_name,address) VALUES
 ('U001','admin', SHA2('admin',256), '你的姓名', '你的家庭住址'),
 ('U002','user1', SHA2('123456',256), '经手人甲', '地址甲');

INSERT INTO `warehouse_material` (mat_code,mat_name,spec,unit,stock) VALUES
 ('M0001','螺丝刀','6寸',  '把', 100),
 ('M0002','锤子',  '常规','把',  50),
 ('M0003','螺栓',  'M8',  '个', 500);

-- ============================================================
-- 存储过程统一约定
--   成功：SELECT 1 AS code,'OK' AS flag,'描述' AS msg;
--   失败：通过 SIGNAL 抛出 SQLSTATE '45000'，事务自动回滚
-- ============================================================
DELIMITER $$

-- ----------------------------------------------------------
-- A. 单物料进仓
-- ----------------------------------------------------------
DROP PROCEDURE IF EXISTS `warehouse_sp_in`$$
CREATE PROCEDURE `warehouse_sp_in`(
  IN  p_bill_no  VARCHAR(30),
  IN  p_mat_code VARCHAR(20),
  IN  p_qty      INT,
  IN  p_op_date  DATE,
  IN  p_operator VARCHAR(20),
  IN  p_handler  VARCHAR(20),
  IN  p_remark   VARCHAR(200)
)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  -- 物料必须存在
  IF NOT EXISTS(SELECT 1 FROM `warehouse_material` WHERE mat_code = p_mat_code) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '进仓失败：物料代码不存在';
  END IF;

  -- 同一单号同一物料不重复
  IF EXISTS(SELECT 1 FROM `warehouse_inout`
             WHERE bill_no = p_bill_no AND mat_code = p_mat_code) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '进仓失败：该单号下物料已存在';
  END IF;

  START TRANSACTION;
    INSERT INTO `warehouse_inout`
      (bill_no,io_type,mat_code,qty,op_date,operator,handler,remark)
    VALUES
      (p_bill_no,'I',p_mat_code,p_qty,p_op_date,p_operator,p_handler,p_remark);

    UPDATE `warehouse_material`
       SET stock = stock + p_qty
     WHERE mat_code = p_mat_code;
  COMMIT;

  SELECT 1 AS code, 'OK' AS flag, '进仓成功' AS msg;
END$$

-- ----------------------------------------------------------
-- B. 单物料出仓（带库存与并发安全检查）
-- ----------------------------------------------------------
DROP PROCEDURE IF EXISTS `warehouse_sp_out`$$
CREATE PROCEDURE `warehouse_sp_out`(
  IN  p_bill_no  VARCHAR(30),
  IN  p_mat_code VARCHAR(20),
  IN  p_qty      INT,
  IN  p_op_date  DATE,
  IN  p_operator VARCHAR(20),
  IN  p_handler  VARCHAR(20),
  IN  p_remark   VARCHAR(200)
)
BEGIN
  DECLARE v_stock INT DEFAULT NULL;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

    -- 行级锁，防止并发出仓造成超扣
    SELECT stock INTO v_stock
      FROM `warehouse_material`
     WHERE mat_code = p_mat_code
     FOR UPDATE;

    IF v_stock IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '出仓失败：物料代码不存在';
    END IF;
    IF v_stock < p_qty THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '出仓失败：库存不足';
    END IF;
    IF EXISTS(SELECT 1 FROM `warehouse_inout`
               WHERE bill_no = p_bill_no AND mat_code = p_mat_code) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '出仓失败：该单号下物料已存在';
    END IF;

    INSERT INTO `warehouse_inout`
      (bill_no,io_type,mat_code,qty,op_date,operator,handler,remark)
    VALUES
      (p_bill_no,'O',p_mat_code,p_qty,p_op_date,p_operator,p_handler,p_remark);

    UPDATE `warehouse_material`
       SET stock = stock - p_qty
     WHERE mat_code = p_mat_code;
  COMMIT;

  SELECT 1 AS code, 'OK' AS flag, '出仓成功' AS msg;
END$$

-- ----------------------------------------------------------
-- C. 多物料进出仓
--    p_items 形如  [{"mat_code":"M0001","qty":2},{"mat_code":"M0002","qty":1}]
--    同一单号只能同进或同出，任一物料失败 → 全部回滚
-- ----------------------------------------------------------
DROP PROCEDURE IF EXISTS `warehouse_sp_inout_multi`$$
CREATE PROCEDURE `warehouse_sp_inout_multi`(
  IN  p_bill_no  VARCHAR(30),
  IN  p_io_type  CHAR(1),
  IN  p_items    JSON,
  IN  p_op_date  DATE,
  IN  p_operator VARCHAR(20),
  IN  p_handler  VARCHAR(20),
  IN  p_remark   VARCHAR(200)
)
BEGIN
  DECLARE v_cnt INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  -- 校验：单号内已存在记录的进出类型必须一致
  IF EXISTS(SELECT 1 FROM `warehouse_inout`
             WHERE bill_no = p_bill_no AND io_type <> p_io_type) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '多物料失败：同一单号不能进仓与出仓混用';
  END IF;

  -- 解析 JSON 明细
  SELECT COUNT(*) INTO v_cnt
    FROM JSON_TABLE(
           p_items,
           '$[*]' COLUMNS (mat_code VARCHAR(20) PATH '$.mat_code',
                            qty      INT          PATH '$.qty')
         ) AS t;
  IF v_cnt = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：物料明细为空';
  END IF;

  -- 物料必须全部存在
  IF EXISTS(
       SELECT 1
         FROM JSON_TABLE(p_items, '$[*]'
                         COLUMNS (mat_code VARCHAR(20) PATH '$.mat_code')) jt
         LEFT JOIN `warehouse_material` m ON m.mat_code = jt.mat_code
        WHERE m.mat_code IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：存在物料代码不存在';
  END IF;

  -- 出仓：所有物料库存均需充足
  IF p_io_type = 'O' THEN
    IF EXISTS(
         SELECT 1
           FROM JSON_TABLE(p_items, '$[*]'
                           COLUMNS (mat_code VARCHAR(20) PATH '$.mat_code',
                                    qty      INT          PATH '$.qty')) jt
           JOIN `warehouse_material` m ON m.mat_code = jt.mat_code
          WHERE m.stock < jt.qty) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：存在物料库存不足';
    END IF;
  END IF;

  -- 同一单号同一物料不重复
  IF EXISTS(
       SELECT 1
         FROM JSON_TABLE(p_items, '$[*]'
                         COLUMNS (mat_code VARCHAR(20) PATH '$.mat_code')) jt
         JOIN `warehouse_inout` t ON t.bill_no = p_bill_no AND t.mat_code = jt.mat_code) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：单号下物料重复';
  END IF;

  START TRANSACTION;

    -- 批量插入明细
    INSERT INTO `warehouse_inout`
      (bill_no,io_type,mat_code,qty,op_date,operator,handler,remark)
    SELECT p_bill_no, p_io_type, jt.mat_code, jt.qty,
           p_op_date, p_operator, p_handler, p_remark
      FROM JSON_TABLE(p_items, '$[*]'
                      COLUMNS (mat_code VARCHAR(20) PATH '$.mat_code',
                               qty      INT          PATH '$.qty')) jt;

    -- 批量更新库存（进仓 +qty，出仓 -qty）
    UPDATE `warehouse_material` m
       JOIN JSON_TABLE(p_items, '$[*]'
                       COLUMNS (mat_code VARCHAR(20) PATH '$.mat_code',
                                qty      INT          PATH '$.qty')) jt
         ON m.mat_code = jt.mat_code
       SET m.stock = m.stock + IF(p_io_type='I', jt.qty, -jt.qty);

  COMMIT;

  SELECT 1 AS code, 'OK' AS flag,
         CONCAT('多物料', IF(p_io_type='I','进仓','出仓'), '成功') AS msg;
END$$

DELIMITER ;

-- ============================================================
-- 存储过程调用示例
-- ============================================================
-- 单物料进仓
-- CALL `warehouse_sp_in`('I20261009001','M0001',5,CURDATE(),'U001','U002','测试');

-- 单物料出仓
-- CALL `warehouse_sp_out`('O20261009001','M0001',2,CURDATE(),'U001','U002','测试');

-- 多物料进仓
-- CALL `warehouse_sp_inout_multi`(
--   'MI20261009001','I',
--   JSON_ARRAY(JSON_OBJECT('mat_code','M0001','qty',2),
--              JSON_OBJECT('mat_code','M0003','qty',10)),
--   CURDATE(),'U001','U002','批量进仓');

-- 多物料出仓
-- CALL `warehouse_sp_inout_multi`(
--   'MO20261009001','O',
--   JSON_ARRAY(JSON_OBJECT('mat_code','M0001','qty',1),
--              JSON_OBJECT('mat_code','M0002','qty',1)),
--   CURDATE(),'U001','U002','批量出仓');
