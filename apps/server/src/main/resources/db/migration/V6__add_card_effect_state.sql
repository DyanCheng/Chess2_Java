set search_path to chess, public;

alter table games add column if not exists frozen_square varchar(2);
alter table games add column if not exists frozen_side varchar(5);
alter table games add column if not exists frozen_turns integer not null default 0;
alter table games add column if not exists invulnerable_square varchar(2);
alter table games add column if not exists invulnerable_side varchar(5);
alter table games add column if not exists invulnerable_turns integer not null default 0;
