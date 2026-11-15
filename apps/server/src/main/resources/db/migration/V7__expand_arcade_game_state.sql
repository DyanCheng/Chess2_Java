set search_path to chess, public;

alter table games add column if not exists pve_player_side varchar(5) not null default 'WHITE';
alter table games add column if not exists previous_board text;
alter table games add column if not exists trap_square varchar(2);
alter table games add column if not exists trap_side varchar(5);
alter table games add column if not exists trap_turns integer not null default 0;
alter table games add column if not exists mosquito_square varchar(2);
alter table games add column if not exists mosquito_side varchar(5);
alter table games add column if not exists backward_pawns_white boolean not null default false;
alter table games add column if not exists backward_pawns_black boolean not null default false;
alter table games add column if not exists captured_white varchar(32) not null default '';
alter table games add column if not exists captured_black varchar(32) not null default '';
