set search_path to chess, public;

alter table users add column if not exists rating integer not null default 1000 check (rating >= 0);
alter table users add column if not exists wins integer not null default 0 check (wins >= 0);
alter table users add column if not exists losses integer not null default 0 check (losses >= 0);
alter table users add column if not exists draws integer not null default 0 check (draws >= 0);

alter table games add column if not exists bot_difficulty varchar(12) not null default 'MEDIUM'
    check (bot_difficulty in ('EASY', 'MEDIUM', 'HARD'));
alter table games add column if not exists winner_color varchar(5)
    check (winner_color in ('WHITE', 'BLACK'));
alter table games add column if not exists completed_at timestamptz;

create index if not exists users_rating_experience_idx on users(rating desc, experience desc);
create index if not exists games_waiting_queue_idx on games(status, type, created_at) where status = 'WAITING';
create index if not exists games_completed_player_idx on games(white_player_id, black_player_id, completed_at desc) where completed_at is not null;
