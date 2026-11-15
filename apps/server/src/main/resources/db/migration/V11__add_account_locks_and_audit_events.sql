set search_path to chess, public;

alter table users add column locked_at timestamptz;
alter table users add column locked_reason varchar(200);
alter table users add column locked_by uuid references users(id) on delete set null;
create index users_locked_at_idx on users (locked_at) where locked_at is not null;
create index users_locked_by_idx on users (locked_by);

create table audit_events (
    id uuid primary key,
    actor_user_id uuid references users(id) on delete set null,
    actor_username varchar(32) not null,
    category varchar(24) not null check (category in ('ADMIN', 'AUTH', 'GAME', 'INVENTORY', 'PUZZLE', 'SHOP', 'SYSTEM')),
    method varchar(8) not null,
    path varchar(255) not null,
    status integer not null check (status between 100 and 599),
    occurred_at timestamptz not null default now()
);
create index audit_events_occurred_at_idx on audit_events (occurred_at desc);
create index audit_events_actor_occurred_idx on audit_events (actor_user_id, occurred_at desc);
