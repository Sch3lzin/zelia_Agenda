CREATE TABLE agendas (
                         id               BIGINT        NOT NULL AUTO_INCREMENT,
                         user_id          BIGINT        NOT NULL,
                         school_class_id  BIGINT        NOT NULL,
                         observation      VARCHAR(255),
                         period           VARCHAR(255)  NOT NULL,
                         class_time       VARCHAR(255)  NOT NULL,
                         date             DATE          NOT NULL,
                         PRIMARY KEY (id),
                         CONSTRAINT fk_agendas_user
                             FOREIGN KEY (user_id) REFERENCES users (id),
                         CONSTRAINT fk_agendas_school_class
                             FOREIGN KEY (school_class_id) REFERENCES school_classes (id),
                         CONSTRAINT uk_agenda_user_class_period_time_date
                             UNIQUE (user_id, school_class_id, period, class_time, date),
                         CONSTRAINT ck_agendas_period CHECK (period IN ('MATUTINO', 'VESPERTINO')),
                         CONSTRAINT ck_agendas_class_time CHECK (class_time IN ('AULA_1', 'AULA_2', 'AULA_3', 'AULA_4', 'AULA_5'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;