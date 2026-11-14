-- Server-side login sessions. The browser cookie holds a random token; only its SHA-256 is stored,
-- so a database leak does not leak usable sessions. Logout deletes the row.
CREATE TABLE IF NOT EXISTS `user_sessions` (
    `token_hash`  VARCHAR(64)  NOT NULL,
    `account_id`  VARCHAR(100) NOT NULL,
    `expires_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`token_hash`),
    INDEX `idx_user_sessions_account` (`account_id`),
    INDEX `idx_user_sessions_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
