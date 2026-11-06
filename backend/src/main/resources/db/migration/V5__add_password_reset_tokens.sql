set search_path to chess, public;

create table if not exists password_reset_tokens (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz
);
create index if not exists password_reset_tokens_user_idx on password_reset_tokens(user_id);
