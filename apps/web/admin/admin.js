const SESSION_KEY = "chess2.session.v2";
const $ = (id) => document.getElementById(id);
const state = { session: null, overview: null, cards: [], logs: [], health: null, healthStale: false, selectedUser: null };
const dateTime = new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "medium" });
const categoryNames = { ADMIN: "Quản trị", AUTH: "Xác thực", GAME: "Ván đấu", INVENTORY: "Kho đồ", PUZZLE: "Puzzle", SHOP: "Cửa hàng", SYSTEM: "Hệ thống" };

class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body) headers.set("Content-Type", "application/json");
  if (state.session?.accessToken) headers.set("Authorization", `Bearer ${state.session.accessToken}`);
  let response;
  try { response = await fetch(path, { ...options, headers, signal: AbortSignal.timeout(20000) }); }
  catch { throw new ApiError("Không thể kết nối máy chủ.", 0); }
  const raw = await response.text();
  let data = null;
  try { data = raw ? JSON.parse(raw) : null; }
  catch { throw new ApiError("Máy chủ trả về dữ liệu không hợp lệ.", response.status); }
  if (!response.ok) throw new ApiError(data?.error || data?.message || `Yêu cầu thất bại (${response.status}).`, response.status);
  return data;
}

function saveSession(session) {
  state.session = session;
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function clearSession() {
  state.session = null;
  localStorage.removeItem(SESSION_KEY);
}

function restoreSession() {
  try {
    const session = JSON.parse(localStorage.getItem(SESSION_KEY));
    if (session?.accessToken) state.session = session;
  } catch { clearSession(); }
}

function toast(message, error = false) {
  const element = $("toast");
  element.textContent = message;
  element.classList.toggle("error", error);
  element.classList.add("show");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => element.classList.remove("show"), 3200);
}

function setBusy(button, busy, label) {
  if (!button) return;
  button.disabled = busy;
  if (busy) { button.dataset.label = button.textContent; button.textContent = label; }
  else button.textContent = button.dataset.label || button.textContent;
}

async function runAction(button, label, action) {
  setBusy(button, true, label);
  try { await action(); }
  catch (error) {
    if (error.status === 401 || error.status === 403) showLogin("Phiên quản trị đã hết hạn hoặc không đủ quyền.");
    else toast(error.message || "Không thể hoàn tất thao tác.", true);
  } finally { setBusy(button, false); }
}

function showLogin(message = "") {
  clearSession();
  $("portal-view").hidden = true;
  $("login-view").hidden = false;
  $("login-message").textContent = message;
  $("password").value = "";
  $("username").focus();
}

function showPortal() {
  $("login-view").hidden = true;
  $("portal-view").hidden = false;
  $("sidebar-admin").textContent = state.session.player.username;
}

async function login(event) {
  event.preventDefault();
  if (!$("login-form").reportValidity()) return;
  await runAction($("login-submit"), "Đang xác thực…", async () => {
    const result = await api("/api/auth/login", { method: "POST", body: JSON.stringify({ username: $("username").value.trim(), password: $("password").value }) });
    if (result.player.role !== "ADMIN") throw new ApiError("Tài khoản này không có quyền ADMIN.", 403);
    saveSession(result);
    showPortal();
    await refreshData();
  });
}

async function refreshData() {
  await runAction($("refresh-data"), "Đang đồng bộ…", async () => {
    let player, overview, cards, logs, health;
    try {
      [player, overview, cards, logs, health] = await Promise.all([
        api("/api/auth/me"), api("/api/admin/overview"), api("/api/cards"), api("/api/admin/logs?limit=200"), api("/actuator/health")
      ]);
    } catch (error) {
      state.healthStale = true;
      renderStatus();
      $("last-updated").textContent = "Dữ liệu cũ · lần đồng bộ mới nhất thất bại";
      throw error;
    }
    if (player.role !== "ADMIN") throw new ApiError("Tài khoản không còn quyền ADMIN.", 403);
    state.session.player = player;
    state.overview = overview;
    state.cards = cards;
    state.logs = logs;
    state.health = health;
    state.healthStale = false;
    saveSession(state.session);
    render();
    $("toast").classList.remove("show");
    $("last-updated").textContent = `Đồng bộ ${new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit" }).format(new Date())}`;
  });
}

function render() {
  renderMetrics();
  renderStatus();
  renderAttention();
  renderUsers();
  renderShop();
  renderLogs();
  renderCardOptions();
}

function renderMetrics() {
  const data = state.overview;
  const values = [["Người dùng", data.users], ["Đã khóa", data.lockedUsers], ["Ván đấu", data.games], ["Đang chơi", data.activeGames], ["Thẻ chức năng", data.cards], ["Mặt hàng", data.shop.length]];
  $("metric-strip").replaceChildren(...values.map(([label, value]) => {
    const item = document.createElement("div"); item.className = "metric";
    const name = document.createElement("span"); name.textContent = label;
    const number = document.createElement("strong"); number.textContent = value;
    item.append(name, number); return item;
  }));
}

function detail(term, value) {
  const row = document.createElement("div"), dt = document.createElement("dt"), dd = document.createElement("dd");
  dt.textContent = term; dd.textContent = value; row.append(dt, dd); return row;
}

function renderStatus() {
  const known = Boolean(state.health);
  const stale = state.healthStale;
  const up = known && state.health.status === "UP";
  const badge = $("health-badge");
  badge.textContent = stale ? "Dữ liệu trạng thái đã cũ" : up ? "Hệ thống ổn định" : known ? "Cần kiểm tra" : "Chưa có dữ liệu";
  badge.className = `status-badge ${up && !stale ? "" : "down"}`.trim();
  const apiStatus = stale ? "Chưa xác minh · dữ liệu cũ" : up ? "Hoạt động" : known ? "Gián đoạn" : "Chưa kiểm tra";
  const databaseStatus = stale ? "Chưa xác minh · dữ liệu cũ" : up ? "Đã kết nối" : known ? "Chưa sẵn sàng" : "Chưa kiểm tra";
  const healthStatus = `${state.health?.status || "UNKNOWN"}${stale ? " · lần đồng bộ trước" : ""}`;
  $("service-status").replaceChildren(detail("Spring Boot API", apiStatus), detail("Database", databaseStatus), detail("Xác thực", "JWT + ADMIN RBAC"), detail("Nhật ký", "Chỉ ghi thao tác thay đổi dữ liệu"));
  $("runtime-details").replaceChildren(detail("Health", healthStatus), detail("Liveness", state.health?.groups?.includes("liveness") ? "Đã công bố" : "Không công bố"), detail("Readiness", state.health?.groups?.includes("readiness") ? "Đã công bố" : "Không công bố"), detail("Audit", "Không lưu secret, body hoặc IP"), detail("Dữ liệu nhạy cảm", "Đã ẩn"));
}

function userSummary(user) {
  const item = document.createElement("div"); item.className = "attention-user";
  const identity = document.createElement("div"), name = document.createElement("strong"), email = document.createElement("small");
  name.textContent = user.username; email.textContent = user.email; identity.append(name, email);
  const badge = document.createElement("span"); badge.className = `role-badge ${user.role === "PLAYER" ? "player" : ""}`; badge.textContent = user.role;
  if (user.locked) { badge.className = "access-badge locked"; badge.textContent = "ĐÃ KHÓA"; }
  item.append(identity, badge); return item;
}

function renderAttention() {
  const users = state.overview.recentUsers;
  const priority = [...users.filter(user => user.locked), ...users.filter(user => user.role === "ADMIN" && !user.locked), ...users.filter(user => user.accountType === "GUEST" && !user.locked)].slice(0, 5);
  $("attention-users").replaceChildren(...(priority.length ? priority : users.slice(0, 5)).map(userSummary));
}

function renderUsers() {
  const query = $("user-search").value.trim().toLocaleLowerCase("vi");
  const role = $("role-filter").value;
  const access = $("status-filter").value;
  const users = state.overview.recentUsers.filter(user => (role === "ALL" || user.role === role)
    && (access === "ALL" || (access === "LOCKED") === user.locked)
    && (!query || `${user.username} ${user.email}`.toLocaleLowerCase("vi").includes(query)));
  $("account-count").textContent = `${users.length} / ${state.overview.users} tài khoản`;
  $("users-empty").hidden = users.length > 0;
  $("user-rows").replaceChildren(...users.map(user => {
    const row = document.createElement("tr");
    const identity = document.createElement("td"); identity.className = "user-cell";
    const name = document.createElement("strong"), email = document.createElement("small"); name.textContent = user.username; email.textContent = user.email; identity.append(name, email);
    const type = document.createElement("td"), typeBadge = document.createElement("span"); typeBadge.className = "type-badge"; typeBadge.textContent = user.accountType; type.append(typeBadge);
    const roleCell = document.createElement("td"), roleBadge = document.createElement("span"); roleBadge.className = `role-badge ${user.role === "PLAYER" ? "player" : ""}`; roleBadge.textContent = user.role; roleCell.append(roleBadge);
    const accessCell = document.createElement("td"), accessBadge = document.createElement("span"); accessBadge.className = `access-badge ${user.locked ? "locked" : ""}`; accessBadge.textContent = user.locked ? "Đã khóa" : "Hoạt động"; accessCell.append(accessBadge);
    const rating = document.createElement("td"); rating.className = "number"; rating.textContent = user.rating;
    const record = document.createElement("td"); record.className = "number"; record.textContent = `${user.wins}T · ${user.losses}B`;
    const wallet = document.createElement("td"); wallet.className = "number"; wallet.textContent = `${user.gold} G · ${user.gems} Gem`;
    const action = document.createElement("td"), button = document.createElement("button"); button.className = "button secondary row-action"; button.type = "button"; button.textContent = "Quản lý"; button.dataset.userId = user.id; button.addEventListener("click", () => openAccount(user)); action.append(button);
    row.append(identity, type, roleCell, accessCell, rating, record, wallet, action); return row;
  }));
}

function renderLogs() {
  const query = $("log-search").value.trim().toLocaleLowerCase("vi");
  const category = $("log-category").value;
  const result = $("log-result").value;
  const logs = state.logs.filter(log => (category === "ALL" || log.category === category)
    && (result === "ALL" || (result === "SUCCESS") === (log.status < 400))
    && (!query || `${log.actorUsername} ${log.action} ${log.path}`.toLocaleLowerCase("vi").includes(query)));
  $("log-count").textContent = `${logs.length} / ${state.logs.length} sự kiện`;
  $("logs-empty").hidden = logs.length > 0;
  $("log-rows").replaceChildren(...logs.map(log => {
    const row = document.createElement("tr");
    const occurred = document.createElement("td"), time = document.createElement("time"); time.dateTime = log.occurredAt; time.textContent = dateTime.format(new Date(log.occurredAt)); occurred.append(time);
    const actor = document.createElement("td"); actor.className = "user-cell"; const name = document.createElement("strong"); name.textContent = log.actorUsername; actor.append(name);
    const categoryCell = document.createElement("td"), categoryBadge = document.createElement("span"); categoryBadge.className = "type-badge"; categoryBadge.textContent = categoryNames[log.category] || log.category; categoryCell.append(categoryBadge);
    const action = document.createElement("td"); action.textContent = log.action;
    const endpoint = document.createElement("td"), code = document.createElement("code"); code.textContent = `${log.method} ${log.path}`; endpoint.append(code);
    const status = document.createElement("td"), statusBadge = document.createElement("span"); statusBadge.className = `result-badge ${log.status >= 400 ? "error" : ""}`; statusBadge.textContent = log.status >= 400 ? `Lỗi ${log.status}` : `Thành công · ${log.status}`; status.append(statusBadge);
    row.append(occurred, actor, categoryCell, action, endpoint, status); return row;
  }));
}

function renderShop() {
  $("shop-rows").replaceChildren(...state.overview.shop.map(item => {
    const row = document.createElement("tr"), name = document.createElement("td"); name.textContent = item.name;
    const priceCell = document.createElement("td"), price = document.createElement("input"); price.type = "number"; price.min = "0"; price.value = item.price; price.setAttribute("aria-label", `Giá ${item.name}`); priceCell.append(price);
    const activeCell = document.createElement("td"), switchLabel = document.createElement("label"), active = document.createElement("input"), activeText = document.createElement("span"); switchLabel.className = "switch-field"; active.type = "checkbox"; active.checked = item.active; activeText.textContent = item.active ? "Đang bán" : "Tạm ẩn"; active.addEventListener("change", () => activeText.textContent = active.checked ? "Đang bán" : "Tạm ẩn"); switchLabel.append(active, activeText); activeCell.append(switchLabel);
    const action = document.createElement("td"), save = document.createElement("button"); save.className = "button secondary row-action"; save.type = "button"; save.textContent = "Lưu"; save.addEventListener("click", () => updateShop(item.id, Number(price.value), active.checked, save)); action.append(save);
    row.append(name, priceCell, activeCell, action); return row;
  }));
}

function renderCardOptions() {
  $("dialog-card").replaceChildren(...state.cards.map(card => {
    const option = document.createElement("option"); option.value = card.id; option.textContent = card.name; return option;
  }));
}

function openAccount(user) {
  state.selectedUser = user;
  $("dialog-title").textContent = user.username;
  $("dialog-email").textContent = `${user.email} · ${user.accountType}`;
  $("dialog-role").value = user.role;
  $("dialog-gold").value = user.gold;
  $("dialog-gems").value = user.gems;
  $("dialog-quantity").value = 1;
  $("dialog-lock-state").textContent = user.locked ? `Đã khóa ${dateTime.format(new Date(user.lockedAt))} · ${user.lockedReason || "Không có lý do"}` : "Tài khoản đang được phép đăng nhập và gọi API.";
  $("dialog-lock-reason").value = user.locked ? user.lockedReason || "" : "";
  $("dialog-lock-reason").disabled = user.locked;
  const lockButton = $("toggle-lock");
  lockButton.textContent = user.locked ? "Mở khóa tài khoản" : "Khóa tài khoản";
  lockButton.className = `button ${user.locked ? "secondary" : "danger"}`;
  lockButton.disabled = !user.locked && user.id === state.session.player.id;
  lockButton.title = lockButton.disabled ? "Bạn không thể tự khóa tài khoản đang đăng nhập." : "";
  renderAccountActivity(user.id);
  $("account-editor").hidden = false;
  $("account-editor").scrollIntoView({ behavior: "auto", block: "start" });
  $("dialog-title").focus({ preventScroll: true });
}

function renderAccountActivity(userId) {
  const logs = state.logs.filter(log => log.actorUserId === userId).slice(0, 8);
  $("user-activity").replaceChildren(...(logs.length ? logs.map(log => {
    const item = document.createElement("div"); item.className = "activity-item";
    const copy = document.createElement("div"), action = document.createElement("strong"), path = document.createElement("small");
    action.textContent = log.action; path.textContent = `${log.method} ${log.path}`; copy.append(action, path);
    const meta = document.createElement("span"); meta.textContent = `${dateTime.format(new Date(log.occurredAt))} · ${log.status}`;
    item.append(copy, meta); return item;
  }) : [Object.assign(document.createElement("p"), { className: "empty-inline", textContent: "Chưa có hoạt động thay đổi dữ liệu được ghi nhận." })]));
}

function closeAccount() {
  const userId = state.selectedUser?.id;
  $("account-editor").hidden = true;
  const trigger = [...document.querySelectorAll("[data-user-id]")].find(button => button.dataset.userId === userId);
  trigger?.focus({ preventScroll: true });
}

async function updateRole() {
  const user = state.selectedUser;
  await runAction($("save-role"), "Đang lưu…", async () => {
    await api(`/api/admin/users/${user.id}/role`, { method: "PATCH", body: JSON.stringify({ role: $("dialog-role").value }) });
    toast("Đã cập nhật quyền tài khoản."); await refreshData(); closeAccount();
  });
}

async function updateWallet() {
  const user = state.selectedUser;
  await runAction($("save-wallet"), "Đang lưu…", async () => {
    await api(`/api/admin/users/${user.id}/wallet`, { method: "PATCH", body: JSON.stringify({ gold: Number($("dialog-gold").value), gems: Number($("dialog-gems").value) }) });
    toast("Đã cập nhật tài nguyên."); await refreshData(); closeAccount();
  });
}

async function toggleAccountLock() {
  const user = state.selectedUser;
  const locked = !user.locked;
  await runAction($("toggle-lock"), locked ? "Đang khóa…" : "Đang mở…", async () => {
    await api(`/api/admin/users/${user.id}/lock`, { method: "PATCH", body: JSON.stringify({ locked, reason: $("dialog-lock-reason").value.trim() }) });
    toast(locked ? "Đã khóa tài khoản và vô hiệu hóa JWT." : "Đã mở khóa tài khoản.");
    await refreshData(); closeAccount();
  });
}

async function grantCard() {
  const user = state.selectedUser;
  await runAction($("grant-card"), "Đang cấp…", async () => {
    await api(`/api/admin/users/${user.id}/cards`, { method: "POST", body: JSON.stringify({ cardId: $("dialog-card").value, quantity: Number($("dialog-quantity").value) }) });
    toast("Đã cấp thẻ vào kho người chơi."); closeAccount();
  });
}

async function updateShop(id, price, active, button) {
  await runAction(button, "Đang lưu…", async () => {
    await api(`/api/admin/shop/${id}`, { method: "PATCH", body: JSON.stringify({ price, active }) });
    toast("Đã cập nhật cửa hàng."); await refreshData();
  });
}

function showPage(name) {
  document.querySelectorAll(".admin-page").forEach(page => { const active = page.dataset.page === name; page.hidden = !active; page.classList.toggle("is-active", active); });
  document.querySelectorAll("[data-section]").forEach(button => activeAttribute(button, button.dataset.section === name));
  history.replaceState(null, "", `#${name}`);
  $("admin-content").focus({ preventScroll: true });
}

function activeAttribute(button, active) {
  if (active) button.setAttribute("aria-current", "page"); else button.removeAttribute("aria-current");
}

function bind() {
  $("login-form").addEventListener("submit", login);
  $("logout").addEventListener("click", () => showLogin("Đã đăng xuất khỏi cổng quản trị."));
  $("refresh-data").addEventListener("click", refreshData);
  $("user-search").addEventListener("input", renderUsers);
  $("role-filter").addEventListener("change", renderUsers);
  $("status-filter").addEventListener("change", renderUsers);
  $("log-search").addEventListener("input", renderLogs);
  $("log-category").addEventListener("change", renderLogs);
  $("log-result").addEventListener("change", renderLogs);
  $("save-role").addEventListener("click", updateRole);
  $("toggle-lock").addEventListener("click", toggleAccountLock);
  $("save-wallet").addEventListener("click", updateWallet);
  $("grant-card").addEventListener("click", grantCard);
  $("close-editor").addEventListener("click", closeAccount);
  document.querySelectorAll("[data-section]").forEach(button => button.addEventListener("click", () => showPage(button.dataset.section)));
  document.querySelectorAll("[data-go]").forEach(button => button.addEventListener("click", () => showPage(button.dataset.go)));
}

async function start() {
  bind(); restoreSession();
  if (!state.session?.accessToken || state.session.player?.role !== "ADMIN") { showLogin(); return; }
  showPortal();
  const route = location.hash.slice(1);
  showPage(["overview", "accounts", "shop", "logs", "system"].includes(route) ? route : "overview");
  await refreshData();
}

start();
