CREATE TABLE IF NOT EXISTS `appraisals` (
                              `id` uuid NOT NULL,
                              `appraisal_result` longtext DEFAULT NULL,
                              `creation_date` timestamp NOT NULL,
                              `transaction_type` varchar(100) DEFAULT NULL,
                              `market` varchar(100) DEFAULT NULL,
                              `price_percentage` float DEFAULT NULL,
                              `comment` varchar(255) DEFAULT NULL,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;