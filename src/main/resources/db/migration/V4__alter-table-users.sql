alter table users
    add column token varchar(64),
    add column expiration_token timestamp;

create unique index idx_users_token on users (token);