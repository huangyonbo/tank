-- 已有库：自定义地图游玩/点赞统计
ALTER TABLE `custom_map_record`
    ADD COLUMN `play_count` BIGINT NOT NULL DEFAULT 0 AFTER `difficulty`,
    ADD COLUMN `success_count` BIGINT NOT NULL DEFAULT 0 AFTER `play_count`,
    ADD COLUMN `fail_count` BIGINT NOT NULL DEFAULT 0 AFTER `success_count`,
    ADD COLUMN `like_count` BIGINT NOT NULL DEFAULT 0 AFTER `fail_count`;
