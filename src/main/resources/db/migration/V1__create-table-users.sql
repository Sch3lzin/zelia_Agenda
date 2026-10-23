CREATE TABLE users (
                       id        BIGINT        NOT NULL AUTO_INCREMENT,
                       name      VARCHAR(255)  NOT NULL,
                       email     VARCHAR(255)  NOT NULL,
                       password  VARCHAR(255)  NOT NULL,
                       user_type VARCHAR(255)  NOT NULL,
                       active    BOOLEAN       NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT ck_users_user_type CHECK (user_type IN ('ADMIN', 'PROFESSOR', 'SECRETARIO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;