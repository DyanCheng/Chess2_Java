import { state } from "./state.js";
const pieces = { wK: "♔", wQ: "♕", wR: "♖", wB: "♗", wN: "♘", wP: "♙", bK: "♚", bQ: "♛", bR: "♜", bB: "♝", bN: "♞", bP: "♟" };
const $ = (id) => document.getElementById(id); const node = (tag, className, text) => { const el = document.createElement(tag); if (className) el.className = className; if (text !== undefined) el.textContent = text; return el; }; const cardFor = (id) => state.cards.find((card) => card.id === id); const effectText = (card) => ({ HEAL: `Hồi ${card.effectValue} HP`, DOUBLE_DAMAGE: `Gây ${card.effectValue} sát thương`, FREEZE: "Khóa một quân đối thủ trong lượt kế", INVULNERABLE: `Bảo vệ một quân trong ${card.effectValue} lượt`, EXTRA_QUEEN: "Đổi 80% HP để gọi thêm Hậu", SURRENDER: "Kết thúc trận và chấp nhận thua" }[card?.effectType] || "Hiệu ứng đang được phát triển");
export function toast(message, error = false) { const el = $("toast"); el.textContent = message; el.classList.toggle("error", error); el.classList.add("show"); clearTimeout(toast.timer); toast.timer = setTimeout(() => el.classList.remove("show"), 3600); }
export function showRoute(route) { document.querySelectorAll("[data-page]").forEach((page) => page.classList.toggle("is-active", page.dataset.page === route)); document.querySelectorAll("[data-route]").forEach((link) => link.classList.toggle("is-active", link.dataset.route === route)); }
export function renderPlayer() { const p = state.player; if (!p) return; $("player-name").textContent = p.username; $("header-name").textContent = p.username; $("header-avatar").textContent = p.username[0].toUpperCase(); $("player-subtitle").textContent = p.accountType === "GUEST" ? "Tài khoản khách được lưu trên thiết bị này." : "Sẵn sàng cho một ván đấu mới?"; $("player-rating").textContent = p.rating; $("player-record").textContent = `${p.wins} thắng · ${p.losses} thua · ${p.draws} hòa`; $("player-level").textContent = p.level; $("player-xp").textContent = p.experience; $("dashboard-gold").textContent = p.gold; $("header-gold").textContent = p.gold; $("header-gems").textContent = p.gems; }
export function renderBoard(onSquare) { const root = $("board"); root.replaceChildren(); const board = state.game?.board; for (let row = 0; row < 8; row += 1) for (let col = 0; col < 8; col += 1) { const square = node("button", "square"); const coordinate = `${String.fromCharCode(97 + col)}${8 - row}`; const piece = board?.[row]?.[col]; square.type = "button"; square.draggable = false; square.dataset.square = coordinate; square.classList.toggle("is-light", (row + col) % 2 === 0); square.classList.toggle("is-selected", state.selectedSquare?.row === row && state.selectedSquare?.col === col); square.classList.toggle("is-last", state.lastMove.includes(coordinate)); square.classList.toggle("is-frozen", coordinate === state.game?.frozenSquare); square.classList.toggle("is-protected", coordinate === state.game?.invulnerableSquare); square.setAttribute("aria-label", piece ? `${coordinate}: ${piece}` : coordinate); if (piece && pieces[piece]) { const icon = node("span", `piece piece-${piece[0] === "w" ? "white" : "black"}`, pieces[piece]); icon.draggable = false; icon.setAttribute("aria-hidden", "true"); square.append(icon); } square.addEventListener("click", () => onSquare(row, col)); root.append(square); } }
export function renderGame(onSquare, onUseCard) { const game = state.game; $("game-title").textContent = game ? `${game.type}${game.type === "PVE" ? ` · ${game.difficulty}` : ""}` : "Bàn cờ"; $("game-status").textContent = !game ? "Chọn một chế độ từ trang Tổng quan." : game.status === "WAITING" ? "Đang chờ một kỳ thủ khác tham gia." : game.status === "PLAYING" ? `Lượt ${game.currentTurn === "WHITE" ? "Trắng" : "Đen"}.` : `Ván đã kết thúc${game.winnerColor ? ` · ${game.winnerColor === "WHITE" ? "Trắng" : "Đen"} thắng` : ""}.`; $("resign-game").hidden = !game || game.status !== "PLAYING"; setHealth("white", game?.whiteHp ?? 100); setHealth("black", game?.blackHp ?? 100); renderBoard(onSquare); const root = $("game-cards"); root.replaceChildren(); const inventory = state.inventory.filter((item) => item.quantity > 0 && cardFor(item.cardId)); if (!game || !inventory.length) { root.className = "list-state"; root.textContent = game ? "Bạn chưa có thẻ. Mở Bộ sưu tập để mua." : "Tạo một ván để dùng thẻ."; return; } root.className = "action-stack"; inventory.forEach((item) => { const card = cardFor(item.cardId); const targeted = ["FREEZE", "INVULNERABLE"].includes(card.effectType); const button = node("button", `button secondary${state.pendingCard?.id === card.id ? " is-armed" : ""}`, `${card.name} ×${item.quantity}${targeted ? " · chọn ô" : ""}`); button.addEventListener("click", () => onUseCard(card, targeted)); root.append(button); }); }
function setHealth(side, value) { $(`${side}-hp`).style.width = `${Math.max(0, Math.min(100, value))}%`; $(`${side}-hp-label`).textContent = value; }
export function renderInventory() { const root = $("card-inventory"); root.replaceChildren(); if (!state.inventory.length) { root.textContent = "Chưa sở hữu thẻ. Hãy mua từ cửa hàng."; return; } state.inventory.forEach((item) => { const card = cardFor(item.cardId); if (card) root.append(cardTile(card, `Số lượng: ${item.quantity}`)); }); }
export function renderShop(onBuy) { const root = $("shop-items"); root.replaceChildren(); if (!state.shop.length) { root.textContent = "Cửa hàng chưa có thẻ."; return; } state.shop.filter((item) => item.active).forEach((item) => { const card = cardFor(item.cardId) || { name: item.name }; const tile = cardTile(card, effectText(card)); const button = node("button", "button primary compact", `Mua · ${item.price} Gold`); button.addEventListener("click", () => onBuy(item.id)); tile.append(button); root.append(tile); }); }
function cardTile(card, subtitle) { const tile = node("article", "card-tile"); tile.append(node("h3", "", card.name), node("p", "", subtitle)); return tile; }
export function renderPuzzles(onSubmit) { const root = $("puzzles"); root.replaceChildren(); if (!state.puzzles.length) { root.textContent = "Chưa có puzzle."; return; } state.puzzles.forEach((puzzle, index) => { const item = node("article", "puzzle-card"); const form = node("form", "puzzle-form"); form.append(input("from", "Từ (e2)"), input("to", "Đến (e4)")); const submit = node("button", "button primary compact", "Giải"); submit.type = "submit"; form.append(submit); form.addEventListener("submit", (event) => { event.preventDefault(); onSubmit(puzzle.id, form.elements.from.value.toLowerCase(), form.elements.to.value.toLowerCase()); }); item.append(node("p", "kicker", `PUZZLE ${index + 1}`), node("h3", "", "Tìm nước đi mạnh nhất"), node("p", "", `Phần thưởng: ${puzzle.rewardGold} Gold`), form); root.append(item); }); }
function input(name, placeholder) { const el = document.createElement("input"); el.name = name; el.placeholder = placeholder; el.maxLength = 2; el.pattern = "[a-h][1-8]"; el.required = true; return el; }
export function renderLeaderboard() { const root = $("leaderboard"); root.replaceChildren(); if (!state.leaderboard.length) { root.textContent = "Chưa có kỳ thủ."; return; } state.leaderboard.forEach((player, index) => { const row = node("div", "leader-row"); const info = node("div", "leader-player"); info.append(node("strong", "", player.username), node("small", "", `${player.wins} thắng · ${player.losses} thua · ${player.draws} hòa`)); row.append(node("span", "rank", `#${index + 1}`), info, node("span", "score", `${player.rating} ELO`)); root.append(row); }); }
export function renderWaiting(onJoin) { const root = $("waiting-games"); root.replaceChildren(); if (!state.waitingGames.length) { root.textContent = "Chưa có phòng đang chờ."; return; } state.waitingGames.forEach((game) => { const row = node("div", "waiting-row"); const text = node("div"); text.append(node("strong", "", "Phòng PvP"), node("small", "", "Chờ quân Đen tham gia")); const join = node("button", "button secondary compact", "Tham gia"); join.addEventListener("click", () => onJoin(game.id)); row.append(text, join); root.append(row); }); }

// Pointer Events keep drag responsive without relying on the browser's native image drag.
export function attachBoardDrag(onMove) {
  const board = $("board");
  if (!board || board.dataset.dragReady) return;
  board.dataset.dragReady = "true";
  let drag = null;
  const clearTarget = () => board.querySelectorAll(".is-drop-target").forEach((square) => square.classList.remove("is-drop-target"));
  const squareAt = (x, y) => document.elementFromPoint(x, y)?.closest?.(".square") || null;
  const end = (event, cancelled = false) => {
    if (!drag || event.pointerId !== drag.pointerId) return;
    const current = drag; drag = null; board.classList.remove("is-dragging"); clearTarget(); current.origin.classList.remove("is-drag-origin");
    try { board.releasePointerCapture(event.pointerId); } catch { /* pointer may already be released */ }
    const target = !cancelled && current.dragging ? squareAt(event.clientX, event.clientY) : null;
    const destination = target?.dataset.square;
    if (current.ghost) {
      if (destination && destination !== current.from) {
        const rect = target.getBoundingClientRect(); current.ghost.style.left = `${rect.left + rect.width / 2}px`; current.ghost.style.top = `${rect.top + rect.height / 2}px`;
        current.ghost.classList.add("is-dropping"); setTimeout(() => current.ghost.remove(), 150);
      } else current.ghost.remove();
    }
    if (!destination || destination === current.from) return;
    board.dataset.ignoreClick = "true"; target.classList.add("is-drop-success"); setTimeout(() => target.classList.remove("is-drop-success"), 280);
    onMove(current.from, destination);
  };
  board.addEventListener("click", (event) => { if (!board.dataset.ignoreClick) return; delete board.dataset.ignoreClick; event.preventDefault(); event.stopImmediatePropagation(); }, true);
  board.addEventListener("pointermove", (event) => {
    if (!drag || event.pointerId !== drag.pointerId) return;
    if (!drag.dragging && Math.hypot(event.clientX - drag.x, event.clientY - drag.y) < 7) return;
    if (!drag.dragging) {
      drag.dragging = true; board.classList.add("is-dragging"); drag.origin.classList.add("is-drag-origin");
      drag.ghost = node("span", `piece chess-drag-ghost piece-${drag.piece[0] === "w" ? "white" : "black"}`, pieces[drag.piece]);
      drag.ghost.setAttribute("aria-hidden", "true"); document.body.append(drag.ghost);
    }
    drag.ghost.style.left = `${event.clientX}px`; drag.ghost.style.top = `${event.clientY}px`; clearTarget();
    const target = squareAt(event.clientX, event.clientY); if (target && target.dataset.square !== drag.from) target.classList.add("is-drop-target");
  });
  board.addEventListener("pointerup", (event) => end(event)); board.addEventListener("pointercancel", (event) => end(event, true));
  board.addEventListener("pointerdown", (event) => {
    const square = event.target.closest?.(".square"); if (!square) return;
    const piece = state.game?.board?.[8 - Number(square.dataset.square[1])]?.[square.dataset.square.charCodeAt(0) - 97];
    const side = state.game?.currentTurn === "WHITE" ? "w" : "b";
    if (event.button !== 0 || !piece || piece[0] !== side || state.pendingCard || state.game?.status !== "PLAYING") return;
    drag = { pointerId: event.pointerId, x: event.clientX, y: event.clientY, from: square.dataset.square, piece, origin: square, dragging: false, ghost: null };
    board.setPointerCapture(event.pointerId);
  });
}

