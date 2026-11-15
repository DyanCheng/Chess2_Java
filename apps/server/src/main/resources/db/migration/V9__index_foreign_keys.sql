set search_path to chess, public;

create index if not exists games_black_player_idx on games(black_player_id);
create index if not exists puzzle_attempts_puzzle_idx on puzzle_attempts(puzzle_id);
create index if not exists shop_items_card_idx on shop_items(card_id);
create index if not exists user_cards_card_idx on user_cards(card_id);
