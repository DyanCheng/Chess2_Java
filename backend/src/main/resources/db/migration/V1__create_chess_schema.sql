create table users (
    id uuid primary key,
    username varchar(32) not null unique,
    email varchar(254) not null unique,
    password_hash varchar(100) not null,
    role varchar(16) not null check (role in ('PLAYER', 'ADMIN')),
    experience bigint not null default 0 check (experience >= 0),
    level integer not null default 1 check (level >= 1),
    created_at timestamptz not null default now()
);

create table wallets (
    user_id uuid primary key references users(id) on delete cascade,
    gold bigint not null default 0 check (gold >= 0),
    gems bigint not null default 0 check (gems >= 0)
);

create table function_cards (
    id uuid primary key,
    code varchar(48) not null unique,
    name varchar(80) not null,
    effect_type varchar(32) not null,
    effect_value integer not null,
    price bigint not null check (price >= 0)
);

create table user_cards (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    card_id uuid not null references function_cards(id),
    quantity integer not null default 0 check (quantity >= 0),
    unique (user_id, card_id)
);

create table shop_items (
    id uuid primary key,
    name varchar(80) not null,
    card_id uuid references function_cards(id),
    price bigint not null check (price >= 0),
    active boolean not null default true
);

create table games (
    id uuid primary key,
    type varchar(12) not null check (type in ('PVE', 'PVP', 'LOCAL')),
    status varchar(16) not null check (status in ('WAITING', 'PLAYING', 'FINISHED', 'DRAW', 'RESIGNED', 'ABANDONED')),
    white_player_id uuid not null references users(id),
    black_player_id uuid references users(id),
    current_turn varchar(5) not null check (current_turn in ('WHITE', 'BLACK')),
    board text not null,
    white_hp integer not null default 100 check (white_hp between 0 and 100),
    black_hp integer not null default 100 check (black_hp between 0 and 100),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index games_status_created_idx on games(status, created_at);
create index games_player_updated_idx on games(white_player_id, black_player_id, updated_at desc);

create table puzzles (
    id uuid primary key,
    board text not null,
    solution_from varchar(2) not null,
    solution_to varchar(2) not null,
    reward_gold integer not null check (reward_gold > 0)
);
