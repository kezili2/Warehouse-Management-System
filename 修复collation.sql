-- ============================================================
-- 修复：Illegal mix of collations (utf8mb4_0900_ai_ci / utf8mb4_unicode_ci)
-- 原因：JSON_TABLE 派生列跟随 MySQL8 服务器默认 utf8mb4_0900_ai_ci，
--       与表上的 utf8mb4_unicode_ci 在 '=' 比较时冲突。
-- 做法：只重建多物料存储过程，在 4 处比较上显式加 COLLATE utf8mb4_unicode_ci。
-- 用法：Navicat → 选中 warehouse 库 → 新建查询 → 全文粘贴 → 运行。
--       不动表、不动数据，只重建这一个过程。
-- ============================================================

USE warehouse;

DELIMITER $$

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
           '$[*]' COLUMNS (mat_code VARCHAR(20) PATH '$.matCode',
                            qty      INT          PATH '$.qty')
         ) AS t;
  IF v_cnt = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：物料明细为空';
  END IF;

  -- 物料必须全部存在
  IF EXISTS(
       SELECT 1
         FROM JSON_TABLE(p_items, '$[*]'
                         COLUMNS (mat_code VARCHAR(20) PATH '$.matCode')) jt
         LEFT JOIN `warehouse_material` m ON m.mat_code = jt.mat_code COLLATE utf8mb4_unicode_ci
        WHERE m.mat_code IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：存在物料代码不存在';
  END IF;

  -- 出仓：所有物料库存均需充足
  IF p_io_type = 'O' THEN
    IF EXISTS(
         SELECT 1
           FROM JSON_TABLE(p_items, '$[*]'
                           COLUMNS (mat_code VARCHAR(20) PATH '$.matCode',
                                    qty      INT          PATH '$.qty')) jt
           JOIN `warehouse_material` m ON m.mat_code = jt.mat_code COLLATE utf8mb4_unicode_ci
          WHERE m.stock < jt.qty) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：存在物料库存不足';
    END IF;
  END IF;

  -- 同一单号同一物料不重复
  IF EXISTS(
       SELECT 1
         FROM JSON_TABLE(p_items, '$[*]'
                         COLUMNS (mat_code VARCHAR(20) PATH '$.matCode')) jt
         JOIN `warehouse_inout` t ON t.bill_no = p_bill_no AND t.mat_code = jt.mat_code COLLATE utf8mb4_unicode_ci) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '多物料失败：单号下物料重复';
  END IF;

  START TRANSACTION;

    -- 批量插入明细
    INSERT INTO `warehouse_inout`
      (bill_no,io_type,mat_code,qty,op_date,operator,handler,remark)
    SELECT p_bill_no, p_io_type, jt.mat_code, jt.qty,
           p_op_date, p_operator, p_handler, p_remark
      FROM JSON_TABLE(p_items, '$[*]'
                      COLUMNS (mat_code VARCHAR(20) PATH '$.matCode',
                               qty      INT          PATH '$.qty')) jt;

    -- 批量更新库存（进仓 +qty，出仓 -qty）
    UPDATE `warehouse_material` m
       JOIN JSON_TABLE(p_items, '$[*]'
                       COLUMNS (mat_code VARCHAR(20) PATH '$.matCode',
                                qty      INT          PATH '$.qty')) jt
         ON m.mat_code = jt.mat_code COLLATE utf8mb4_unicode_ci
       SET m.stock = m.stock + IF(p_io_type='I', jt.qty, -jt.qty);

  COMMIT;

  SELECT 1 AS code, 'OK' AS flag,
         CONCAT('多物料', IF(p_io_type='I','进仓','出仓'), '成功') AS msg;
END$$

DELIMITER ;

-- 验证：应能看到新过程
SHOW CREATE PROCEDURE `warehouse_sp_inout_multi`;
