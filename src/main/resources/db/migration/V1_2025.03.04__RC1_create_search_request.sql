CREATE TABLE IF NOT EXISTS `search_requests` (
                                                 `id` VARCHAR(100) NOT NULL,
    `blueprint_data` LONGTEXT DEFAULT NULL,
    `creation_date` DATE DEFAULT NULL,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
