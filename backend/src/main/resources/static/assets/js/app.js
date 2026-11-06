import { api, setUnauthorizedHandler } from "./api.js";
import { clearSession, restoreSession, saveSession, stableGuestKey, state } from "./state.js";
import { attachBoardDrag, renderGame, renderInventory, renderLeaderboard, renderPlayer, renderPuzzles, renderShop, renderWaiting, showRoute, toast } from "./render.js";

const $ = (id) => document.getElementById(id);
const routes = new Set(["home", "play", "collection", "puzzles", "leaderboard"]);
let pollingTimer = null;

function renderGameView() { renderGame(selectSquare, useCard); attachBoardDrag(movePiece); }

function routeName() { const value = location.hash.replace("#/", ""); return routes.has(value) ? value : "home"; }
function go(route) { location.hash = `#/${route}`; }
function setBusy(button, pending, label) {
  if (!button) return;
  button.disabled = pending;
  button.classList.toggle("is-loading", pending);
  if (pending) { button.dataset.label = button.textContent; button.textContent = label; }
  else button.textContent = button.dataset.label || button.textContent;
}
async function withAction(button, label, action) {
  setBusy(button, true, label);
  try { return await action(); }
  catch (error) { toast(error.message || "Không thể hoàn tất thao tác.", true); return null; }
  finally { setBusy(button, false); }
}
function setAuthMode(mode) {
  const register = mode === "register";
  $("auth-form").dataset.authMode = mode;
  document.querySelectorAll("[data-auth-mode]").forEach((tab) => tab.classList.toggle("active", tab.dataset.authMode === mode));
  $("email-field").hidden = !register;
  $("email").required = register;
  $("password").autocomplete = register ? "new-password" : "current-password";
  $("auth-title").textContent = register ? "Tạo tài khoản" : "Chào mừng trở lại";
  $("auth-submit").textContent = register ? "Tạo tài khoản" : "Đăng nhập";
  $("auth-message").textContent = "";
}
function setScreen(auth) {
  $("auth-view").hidden = !auth;
  $("app-view").hidden = auth;
  $("session-bar").hidden = auth;
}
function showAuth(message = "") {
  clearInterval(pollingTimer); clearSession(); setScreen(true); $("auth-message").textContent = message;
}
function renderEverything() {
  renderPlayer(); renderInventory(); renderShop(buyCard); renderPuzzles(submitPuzzle); renderLeaderboard(); renderWaiting(joinGame); renderGameView();
}
async function refreshData() {
  const [player, cards, inventory, shop, puzzles, leaderboard, waitingGames] = await Promise.all([
    api("/api/auth/me"), api("/api/cards"), api("/api/cards/inventory"), api("/api/shop"), api("/api/puzzles"), api("/api/leaderboard"), api("/api/games/waiting")
  ]);
  Object.assign(state, { player, cards, inventory, shop, puzzles, leaderboard, waitingGames });
  saveSession({ ...state.session, player }); renderEverything();
}
async function startSession(result) {
  saveSession(result); await refreshData(); setScreen(false); renderRoute();
  if (!location.hash) go("home");
  clearInterval(pollingTimer); pollingTimer = setInterval(pollGame, 3500);
}
async function authenticate(event) {
  event.preventDefault();
  const mode = $("auth-form").dataset.authMode || "login";
  const payload = { username: $("username").value.trim(), password: $("password").value };
  if (mode === "register") payload.email = $("email").value.trim();
  await withAction($("auth-submit"), "Đang xác thực…", async () => {
    try { await startSession(await api(`/api/auth/${mode}`, { method: "POST", body: JSON.stringify(payload) })); }
    catch (error) { $("auth-message").textContent = error.message; throw error; }
  });
}
async function guestLogin() {
  await withAction($("guest-login"), "Đang vào bàn…", () => startSessionFromGuest());
}
async function startSessionFromGuest() {
  const result = await api("/api/auth/guest", { method: "POST", body: JSON.stringify({ guestKey: stableGuestKey() }) });
  await startSession(result);
}
function showForgot() {
  $("auth-form").hidden = true; $("forgot-form").hidden = false; $("new-password-form").hidden = true;
  $("reset-message").textContent = "";
}
function showLogin() {
  $("auth-form").hidden = false; $("forgot-form").hidden = true; $("new-password-form").hidden = true;
}
function showNewPassword() {
  $("auth-form").hidden = true; $("forgot-form").hidden = true; $("new-password-form").hidden = false;
}
async function requestPasswordReset(event) {
  event.preventDefault();
  await withAction($("reset-request"), "Đang gửi…", async () => {
    try { await api("/api/auth/password-reset/request", { method: "POST", body: JSON.stringify({ email: $("reset-email").value.trim() }) }); $("reset-message").textContent = "Nếu email tồn tại, liên kết đặt lại đã được gửi."; }
    catch (error) { $("reset-message").textContent = error.message; throw error; }
  });
}
async function confirmPasswordReset(event) {
  event.preventDefault();
  const token = new URLSearchParams(location.search).get("reset");
  if (!token) return;
  await withAction($("new-password-submit"), "Đang lưu…", async () => {
    try { await api("/api/auth/password-reset/confirm", { method: "POST", body: JSON.stringify({ token, password: $("new-password").value }) }); history.replaceState({}, "", location.pathname); showLogin(); $("auth-message").textContent = "Mật khẩu đã được thay đổi. Hãy đăng nhập."; }
    catch (error) { $("new-password-message").textContent = error.message; throw error; }
  });
}
async function createGame(type, button) {
  await withAction(button, "Đang tạo ván…", async () => {
    state.game = await api("/api/games", { method: "POST", body: JSON.stringify({ type, difficulty: type === "PVE" ? $("pve-difficulty").value : null }) });
    state.selectedSquare = null; state.lastMove = []; renderGameView(); go("play"); await refreshData();
  });
}
async function quickMatch(button) {
  await withAction(button, "Đang tìm đối thủ…", async () => {
    state.game = await api("/api/games/quick-match", { method: "POST" }); state.selectedSquare = null; state.lastMove = []; go("play"); await refreshData();
  });
}
async function joinGame(id, button) {
  await withAction(button, "Đang tham gia…", async () => { state.game = await api(`/api/games/${id}/join`, { method: "POST" }); state.selectedSquare = null; go("play"); await refreshData(); });
}
async function selectSquare(row, col) {
  const game = state.game; if (!game || game.status !== "PLAYING") return;
  if (state.pendingCard) { const card = state.pendingCard; state.pendingCard = null; return useCard(card, `${String.fromCharCode(97 + col)}${8 - row}`); }
  const piece = game.board[row][col]; const colour = game.currentTurn === "WHITE" ? "w" : "b";
  if (!state.selectedSquare) { if (!piece || piece[0] !== colour) return; state.selectedSquare = { row, col }; return renderGameView(); }
  if (piece?.[0] === colour) { state.selectedSquare = { row, col }; return renderGameView(); }
  const from = `${String.fromCharCode(97 + state.selectedSquare.col)}${8 - state.selectedSquare.row}`;
  const to = `${String.fromCharCode(97 + col)}${8 - row}`;
  return movePiece(from, to);
}
async function movePiece(from, to) {
  const game = state.game; if (!game || from === to) return;
  try { state.game = await api(`/api/games/${game.id}/moves`, { method: "POST", body: JSON.stringify({ from, to }) }); state.lastMove = [from, to]; state.selectedSquare = null; renderGameView(); }
  catch (error) { state.selectedSquare = null; renderGameView(); toast(error.message, true); }
}
async function useCard(card, targeted = false) {
  if (!state.game) return;
  if (targeted === true) { state.pendingCard = card; state.selectedSquare = null; renderGameView(); toast(card.effectType === "FREEZE" ? "Chọn một quân đối thủ để đóng băng." : "Chọn một quân của bạn để bảo vệ."); return; }
  try { state.game = await api(`/api/games/${state.game.id}/cards`, { method: "POST", body: JSON.stringify({ cardId: card.id, target: typeof targeted === "string" ? targeted : "OPPONENT" }) }); toast("Đã kích hoạt thẻ."); await refreshData(); }
  catch (error) { toast(error.message, true); }
}
async function buyCard(id) { try { await api(`/api/shop/${id}/purchase`, { method: "POST" }); toast("Đã thêm thẻ vào kho."); await refreshData(); } catch (error) { toast(error.message, true); } }
async function submitPuzzle(id, from, to) { try { const result = await api(`/api/puzzles/${id}/submit`, { method: "POST", body: JSON.stringify({ from, to }) }); toast(result.correct ? result.rewardGranted ? "Chính xác! Gold đã được cộng." : "Bạn đã giải puzzle này rồi." : "Chưa đúng, thử một nước khác.", !result.correct); if (result.correct) await refreshData(); } catch (error) { toast(error.message, true); } }
async function resign() { if (!state.game) return; try { state.game = await api(`/api/games/${state.game.id}/resign`, { method: "POST" }); await refreshData(); } catch (error) { toast(error.message, true); } }
async function leave() { try { if (state.game?.status === "WAITING") await api(`/api/games/${state.game.id}/cancel`, { method: "POST" }); state.game = null; state.selectedSquare = null; state.lastMove = []; go("home"); await refreshData(); } catch (error) { toast(error.message, true); } }
function renderRoute() { showRoute(routeName()); if (routeName() === "play") renderGameView(); }
async function pollGame() { const game = state.game; if (document.hidden || !game || game.type !== "PVP" || !["WAITING", "PLAYING"].includes(game.status)) return; try { state.game = await api(`/api/games/${game.id}`); renderGameView(); } catch { /* next poll can recover */ } }
function bind() {
  document.querySelectorAll("[data-auth-mode]").forEach((button) => button.addEventListener("click", () => setAuthMode(button.dataset.authMode)));
  $("auth-form").addEventListener("submit", authenticate); $("guest-login").addEventListener("click", guestLogin);
  $("forgot-password").addEventListener("click", showForgot); $("back-to-login").addEventListener("click", showLogin);
  $("forgot-form").addEventListener("submit", requestPasswordReset); $("new-password-form").addEventListener("submit", confirmPasswordReset);
  $("sign-out").addEventListener("click", () => showAuth("Bạn đã đăng xuất.")); $("profile-button").addEventListener("click", () => go("home"));
  document.querySelectorAll("[data-create-game]").forEach((button) => button.addEventListener("click", () => createGame(button.dataset.createGame, button)));
  $("quick-match").addEventListener("click", () => quickMatch($("quick-match")));
  $("refresh-waiting").addEventListener("click", () => withAction($("refresh-waiting"), "Đang tải…", refreshData));
  $("resign-game").addEventListener("click", resign); $("leave-game").addEventListener("click", leave); addEventListener("hashchange", renderRoute);
}
async function init() {
  bind(); setAuthMode("login"); renderGameView();
  setUnauthorizedHandler(() => showAuth("Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."));
  if (new URLSearchParams(location.search).has("reset")) { showForgot(); showNewPassword(); return; }
  if (!restoreSession()) return;
  try { await refreshData(); setScreen(false); renderRoute(); pollingTimer = setInterval(pollGame, 3500); }
  catch (error) { if (error.status !== 401) showAuth(error.message); }
}
init();

