CREATE TABLE IF NOT EXISTS evesde.oauth_tokens (
                                     account_id varchar(100) NOT NULL,
                                     character_id INT NOT NULL,
                                     access_token TEXT NOT NULL,
                                     refresh_token varchar(100) NOT NULL,
                                     expires_at TIMESTAMP NOT NULL,
                                     is_primary BOOL NULL,
                                     CONSTRAINT oauth_tokens_pk PRIMARY KEY (character_id)
)
    ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;
