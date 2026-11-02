-- ============================================================
-- 把 admin 账号的「作者」和「住址」改成答辩展示用的内容
-- 用法：Navicat → 选中 warehouse 库 → 新建查询 →
--       粘贴 → 点「运行」
-- 只改 U001 这一条，物料/进出仓数据不受影响
-- ============================================================

USE warehouse;

UPDATE warehouse_user
SET real_name = '占泽华-施炜烨-涂遵伟',
    address   = '软件工程2402'
WHERE user_code = 'U001';

-- 回显确认（应看到上面的姓名和班级）
SELECT user_code, username, real_name, address FROM warehouse_user WHERE user_code = 'U001';
