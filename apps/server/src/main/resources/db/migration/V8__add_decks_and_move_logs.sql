set search_path to chess, public;

alter table user_cards add column if not exists deck_slot integer;
alter table user_cards add constraint user_cards_deck_slot_check check (deck_slot between 1 and 3);
create unique index if not exists user_cards_user_deck_slot_uq on user_cards(user_id, deck_slot);

alter table games add column if not exists white_deck text not null default '';
alter table games add column if not exists black_deck text not null default '';

create table game_move_logs (
    id uuid primary key,
    game_id uuid not null references games(id) on delete cascade,
    sequence_no integer not null check (sequence_no > 0),
    actor_user_id uuid references users(id) on delete set null,
    actor_side varchar(5) not null check (actor_side in ('WHITE', 'BLACK')),
    event_type varchar(12) not null check (event_type in ('MOVE', 'CARD', 'EFFECT', 'RESIGN')),
    from_square varchar(2),
    to_square varchar(8),
    captured_piece varchar(2),
    effect_code varchar(32),
    occurred_at timestamptz not null default now(),
    unique (game_id, sequence_no)
);
create index game_move_logs_game_sequence_idx on game_move_logs(game_id, sequence_no);
