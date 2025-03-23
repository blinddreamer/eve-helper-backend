CREATE TABLE IF NOT EXISTS `character_info` (
                                  `char_id` int(11) NOT NULL,
                                  `char_name` varchar(100) DEFAULT NULL,
                                  `char_avatar` varchar(100) DEFAULT NULL,
                                  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;