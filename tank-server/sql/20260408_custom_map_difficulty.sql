-- 已有库升级：自定义地图难度（用于最难榜）
ALTER TABLE `custom_map_record`
    ADD COLUMN `difficulty` INT NOT NULL DEFAULT 0 AFTER `heat`;
