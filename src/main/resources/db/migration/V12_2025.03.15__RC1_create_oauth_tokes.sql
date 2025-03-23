CREATE TABLE IF NOT EXISTS evesde.oauth_tokens (
                                     id varchar(100) NOT NULL,
                                     character_id varchar(100) NULL,
                                     access_token varchar(100) NULL,
                                     refresh_token varchar(100) NULL,
                                     expires_at TIMESTAMP NULL,
                                     CONSTRAINT oauth_tokens_pk PRIMARY KEY (id)
)
    ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;
