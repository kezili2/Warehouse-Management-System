-- ============================================================
-- 统一排序规则：全链路改为 MySQL8 默认的 utf8mb4_0900_ai_ci
-- 范围：库默认 + 3 张表 + 重建 3 个存储过程（参数跟随库新默认）
-- 数据不受影响（同字符集只改排序规则）
-- ============================================================

USE warehouse;

-- 1) 库默认改为 0900_ai_ci
ALTER DATABASE `warehouse` COLLATE utf8mb4_0900_ai_ci;

-- 2) 三张表连同所有字符列一起改为 0900_ai_ci（数据保留）
ALTER TABLE `warehouse_user`     CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `warehouse_material` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `warehouse_inout`    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 3) 重建三个存储过程（参数排序规则在创建时固化，必须重建才生效）
DELIMITER $$

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

  IF NOT EXISTS(SELECT 1 FROM `warehouse_material` WHERE mat_code = p_mat_code) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '进仓失败：物料代码不存在';
  END IF;

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

  IF EXISTS(SELECT 1 FROM `warehouse_inout`
             WHERE bill_no = p_bill_no AND io_type <> p_io_type) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '多物料失败：同一单号不能进仓与出仓混用';
  END IF;

  SELECT COUNT(*) INTO v_cnt
    FROM JSON_TABLE(
           p_items,
           '$[*]' COLUMNS (mat_code VARCHAR(20) PATH '$.matCode',
                            qty      INT          PATH '$.qty')
         ) AS t;
  IF v_cnt = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：物料明细为空';
  END IF;

  IF EXISTS(
       SELECT 1
         FROM JSON_TABLE(p_items, '$[*]'
                         COLUMNS (mat_code VARCHAR(20) PATH '$.matCode')) jt
         LEFT JOIN `warehouse_material` m ON m.mat_code = jt.mat_code
        WHERE m.mat_code IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：存在物料代码不存在';
  END IF;

  IF p_io_type = 'O' THEN
    IF EXISTS(
         SELECT 1
           FROM JSON_TABLE(p_items, '$[*]'
                           COLUMNS (mat_code VARCHAR(20) PATH '$.matCode',
                                    qty      INT          PATH '$.qty')) jt
           JOIN `warehouse_material` m ON m.mat_code = jt.mat_code
          WHERE m.stock < jt.qty) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：存在物料库存不足';
    END IF;
  END IF;

  IF EXISTS(
       SELECT 1
         FROM JSON_TABLE(p_items, '$[*]'
                         COLUMNS (mat_code VARCHAR(20) PATH '$.matCode')) jt
         JOIN `warehouse_inout` t ON t.bill_no = p_bill_no AND t.mat_code = jt.mat_code) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：单号下物料重复';
  END IF;

  START TRANSACTION;

    INSERT INTO `warehouse_inout`
      (bill_no,io_type,mat_code,qty,op_date,operator,handler,remark)
    SELECT p_bill_no, p_io_type, jt.mat_code, jt.qty,
           p_op_date, p_operator, p_handler, p_remark
      FROM JSON_TABLE(p_items, '$[*]'
                      COLUMNS (mat_code VARCHAR(20) PATH '$.matCode',
                               qty      INT          PATH '$.qty')) jt;

    UPDATE `warehouse_material` m
       JOIN JSON_TABLE(p_items, '$[*]'
                       COLUMNS (mat_code VARCHAR(20) PATH '$.matCode',
                                qty      INT          PATH '$.qty')) jt
         ON m.mat_code = jt.mat_code
       SET m.stock = m.stock + IF(p_io_type='I', jt.qty, -jt.qty);

  COMMIT;

  SELECT 1 AS code, 'OK' AS flag,
         CONCAT('多物料', IF(p_io_type='I','进仓','出仓'), '成功') AS msg;
END$$

DELIMITER ;
