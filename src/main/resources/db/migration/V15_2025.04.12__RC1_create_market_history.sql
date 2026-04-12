CREATE TABLE IF NOT EXISTS `market_history` (
    `type_id`      INT           NOT NULL,
    `region_id`    INT           NOT NULL,
    `date`         DATE          NOT NULL,
    `average`      DECIMAL(20,2) DEFAULT NULL,
    `highest`      DECIMAL(20,2) DEFAULT NULL,
    `lowest`       DECIMAL(20,2) DEFAULT NULL,
    `volume`       BIGINT        DEFAULT NULL,
    `order_count`  INT           DEFAULT NULL,
    PRIMARY KEY (`type_id`, `region_id`, `date`),
    INDEX `idx_market_history_type_region` (`type_id`, `region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
