set search_path to chess, public;

alter table users add column if not exists account_type varchar(16) not null default 'REGISTERED'
    check (account_type in ('REGISTERED', 'GUEST'));
alter table users add column if not exists guest_key varchar(64);

create unique index if not exists users_username_ci_idx on users (lower(username));
create unique index if not exists users_email_ci_idx on users (lower(email));
create unique index if not exists users_guest_key_idx on users (guest_key);
