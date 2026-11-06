set search_path to chess, public;

create table if not exists puzzle_attempts (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    puzzle_id uuid not null references puzzles(id) on delete cascade,
    solved_at timestamptz,
    created_at timestamptz not null default now(),
    unique (user_id, puzzle_id)
);

create index if not exists puzzle_attempts_player_idx on puzzle_attempts(user_id, solved_at desc);
