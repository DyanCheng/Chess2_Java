// Small interface icons; illustrations live in the separately generated card atlas.
const paths = {
  play: '<path d="M8 5h8l4 6v7l-4 1-3-4h-2l-3 4-4-1v-7z"/><path d="M7 10v5m-2-2.5h4m6-1h.01m3 2h.01"/>',
  cards: '<rect x="6" y="3" width="13" height="17" rx="2"/><path d="m6 6-4 1 1 15 12-1m-5-12 3-3 3 3-3 3z"/>',
  puzzle: '<path d="M9 4H4v6c5-2 5 6 0 4v6h6c-2-5 6-5 4 0h6v-6c-5 2-5-6 0-4V4h-6c2-5-6-5-4 0z"/>',
  trophy: '<path d="M8 3h8v7a4 4 0 0 1-8 0zM8 5H4v3a4 4 0 0 0 4 4m8-7h4v3a4 4 0 0 1-4 4m-4 2v6m-5 1h10"/>',
  user: '<circle cx="12" cy="7" r="4"/><path d="M4 21v-3a8 8 0 0 1 16 0v3"/>',
  home: '<path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-8h6v8"/>',
  flip: '<path d="M7 3v17m-4-4 4 4 4-4m6 5V4m-4 4 4-4 4 4"/>',
  sound: '<path d="m11 4-6 5H2v6h3l6 5zM15 8a6 6 0 0 1 0 8m3-11a10 10 0 0 1 0 14"/>',
  flag: '<path d="M5 22V3c5-5 9 5 15 0v11c-6 5-10-5-15 0"/>',
  sword: '<path d="m4 20 13-13 3-4-5 1L4 15m-1-3 9 9m-8-3-2 4 4-2"/>',
  coin: '<circle cx="12" cy="12" r="9"/><path d="m12 6 4 6-4 6-4-6z"/>',
  gem: '<path d="m3 8 4-5h10l4 5-9 13zm0 0h18M7 3l5 18 5-18"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>'
};
export function icon(name) {
  return `<svg class="ui-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.cards}</svg>`;
}
export const cardNames = {
  HEAL:'Hồi phục',FREEZE:'Đóng băng',DOUBLE_DAMAGE:'Cường kích',INVULNERABLE:'Hộ vệ',
  PAWN_DASH:'Tốt xung phong',TRAP:'Bẫy',REWIND:'Quay ngược',PAWN_BACKWARD:'Tốt lùi',
  PORTAL:'Cổng dịch chuyển',EXTRA_QUEEN:'Triệu hồi',REVIVE:'Hồi sinh',HIDE_KING:'Ẩn Vua',
  TELEPORT:'Dịch chuyển',MOSQUITO:'Muỗi',SURRENDER:'Đình chiến'
};
const illustratedCards=new Set(['HEAL','FREEZE','DOUBLE_DAMAGE','INVULNERABLE','EXTRA_QUEEN','SURRENDER']);
export function cardArt(type, className = '') {
  const art = document.createElement('span');
  art.className = `card-art ${className}`.trim();
  art.dataset.effect = type;
  art.setAttribute('aria-hidden', 'true');
  if(!illustratedCards.has(type)){art.classList.add('symbolic-card-art');art.innerHTML=icon('cards');}
  return art;
}
