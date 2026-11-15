set search_path to chess, public;

alter table games add column if not exists room_code varchar(6);
create unique index if not exists games_room_code_unique_idx on games(room_code) where room_code is not null;
create index if not exists games_waiting_expiry_idx on games(status, created_at) where type = 'PVP' and status = 'WAITING';
