-- expiration_token passa de TIMESTAMP (limite em 2038, conversão de fuso) para DATETIME
ALTER TABLE users
    MODIFY COLUMN expiration_token DATETIME NULL;

-- Garante no banco as regras que antes eram verificadas só no código (corrida entre requisições)
ALTER TABLE school_classes
    ADD CONSTRAINT uk_school_classes_name UNIQUE (name);

ALTER TABLE agendas
    ADD CONSTRAINT uk_agenda_period_class_time_date UNIQUE (period, class_time, date);

CREATE INDEX idx_agendas_date ON agendas (date);
