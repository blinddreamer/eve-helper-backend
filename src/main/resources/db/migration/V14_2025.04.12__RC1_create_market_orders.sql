CREATE TABLE IF NOT EXISTS `market_orders` (
    `order_id`       BIGINT       NOT NULL,
    `type_id`        INT          NOT NULL,
    `region_id`      INT          NOT NULL,
    `is_buy_order`   TINYINT(1)   NOT NULL,
    `price`          DECIMAL(20,2) NOT NULL,
    `volume_remain`  INT          NOT NULL,
    `volume_total`   INT          NOT NULL,
    `min_volume`     INT          NOT NULL DEFAULT 1,
    `issued`         TIMESTAMP    NOT NULL,
    `duration`       INT          NOT NULL,
    `location_id`    BIGINT       DEFAULT NULL,
    `range`          VARCHAR(20)  DEFAULT NULL,
    `fetched_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`order_id`),
    INDEX `idx_market_orders_type_region` (`type_id`, `region_id`),
    INDEX `idx_market_orders_fetched`     (`fetched_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
