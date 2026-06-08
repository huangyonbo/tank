CREATE TABLE `player_weapon_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uid` INT NOT NULL,
    `weapons_levels` VARCHAR(1024) NOT NULL DEFAULT '',
    `tank_speed_level` INT NOT NULL DEFAULT 0,
    `tank_armor_level` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `custom_map_record` (
     `map_id` BIGINT NOT NULL AUTO_INCREMENT,
     `owner_uid` INT NOT NULL,
     `map_name` VARCHAR(128) NOT NULL DEFAULT '',
     `map_data` TEXT NOT NULL,
     `width` INT NOT NULL DEFAULT 0,
     `height` INT NOT NULL DEFAULT 0,
     `heat` BIGINT NOT NULL DEFAULT 0,
     `difficulty` INT NOT NULL DEFAULT 0,
     `play_count` BIGINT NOT NULL DEFAULT 0,
     `success_count` BIGINT NOT NULL DEFAULT 0,
     `fail_count` BIGINT NOT NULL DEFAULT 0,
     `like_count` BIGINT NOT NULL DEFAULT 0,
     `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`map_id`),
     KEY `idx_owner_uid` (`owner_uid`),
     KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;