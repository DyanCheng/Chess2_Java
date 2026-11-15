import { api, setUnauthorizedHandler } from "./api.js";
import { clearSession, restoreSession, saveSession, stableGuestKey, state, playerSide } from "./state.js";
import { attachBoardDrag, boardView, renderGame, renderInventory, renderLeaderboard, renderPlayer, renderPuzzles, renderShop, renderWaiting, showRoute, toast } from "./render.js";

import { coordinate, point, at, destinations, reducedMotion } from "./board.js";
import { mountExperience, renderProfile, renderOnboarding, dismissOnboarding, decorateCards, playSound, cardBurst } from "./experience.js";

const $ = (id) => document.getElementById(id);
const routes = new Set(["home", "play", "collection", "puzzles", "leaderboard", "profile"]);
const POLL_TICK_MS=250,POLL_WAITING_MS=500,POLL_ACTIVE_MS=2000,POLL_SOCKET_FALLBACK_MS=2000;
let pollingTimer=null,lastPollAt=0,revision=0,refreshing=null,buying=false,deckUpdating=false,opening=false,polling=false;
let gameSocket=null,socketGameId=null,socketReconnectTimer=null,socketReconnectDelay=500,socketSyncPending=false;

function renderGameView() { renderGame(selectSquare, useCard); attachBoardDrag(movePiece); }
function socketOpen(gameId){return socketGameId===gameId&&gameSocket?.readyState===WebSocket.OPEN;}
function closeGameSocket(){socketGameId=null;socketSyncPending=false;clearTimeout(socketReconnectTimer);const socket=gameSocket;gameSocket=null;if(socket&&socket.readyState<2)socket.close(1000,"leaving");}
function connectGameSocket(gameId){
  const token=state.session?.accessToken;
  if(!token||socketOpen(gameId)||(socketGameId===gameId&&gameSocket?.readyState===WebSocket.CONNECTING))return;
  const previous=gameSocket;gameSocket=null;if(previous&&previous.readyState<2)previous.close(1000,"switching");
  socketGameId=gameId;
  const scheme=location.protocol==="https:"?"wss:":"ws:",socket=new WebSocket(`${scheme}//${location.host}/ws/games`,["chess2",`jwt.${token}`]);gameSocket=socket;
  socket.addEventListener("open",()=>{if(gameSocket!==socket)return;socketReconnectDelay=500;socket.send(JSON.stringify({type:"subscribe",gameId}));});
  socket.addEventListener("message",event=>{if(gameSocket!==socket)return;try{const message=JSON.parse(event.data);if(message.gameId===gameId&&["ready","game-updated"].includes(message.type))requestRealtimeSync();}catch{}});
  socket.addEventListener("close",()=>{if(gameSocket!==socket)return;gameSocket=null;if(socketGameId!==gameId||state.game?.id!==gameId||!["PLAYING","WAITING"].includes(state.game.status))return;clearTimeout(socketReconnectTimer);socketReconnectTimer=setTimeout(()=>connectGameSocket(gameId),socketReconnectDelay);socketReconnectDelay=Math.min(socketReconnectDelay*2,10000);});
}
function requestRealtimeSync(){socketSyncPending=true;void pollGame(true);}

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
  catch (error) { console.error(error); toast(error.message || "Không thể hoàn tất thao tác.", true); return null; }
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
  revision++; refreshing=null; boardView?.cancel(); clearInterval(pollingTimer);closeGameSocket();clearSession(); setScreen(true); $("auth-message").textContent = message;
}
function renderEverything() {
  renderPlayer(); renderInventory(toggleDeck); renderShop(buyCard); renderPuzzles(submitPuzzle); renderLeaderboard(); renderWaiting(joinGame); renderGameView(); decorateCards(); renderProfile(resumeGame); renderOnboarding();
}
async function refreshData() {
  if (refreshing) return refreshing;
  const token=state.session?.accessToken;
  const task=(async()=>{
    const player=await api("/api/auth/me");
    const keys=["cards","inventory","shop","puzzles","leaderboard","waitingGames","history"];
    const paths=["cards","cards/inventory","shop","puzzles","leaderboard","games/waiting","games/history"];
    const results=await Promise.allSettled(paths.map(path=>api("/api/"+path)));
    if(token!==state.session?.accessToken)return;
    state.player=player;
    results.forEach((result,i)=>{if(result.status==="fulfilled")state[keys[i]]=result.value;});
    saveSession({...state.session,player}); renderEverything();
    if(results.some(r=>r.status==="rejected"))toast("Một số dữ liệu chưa tải được. Bạn có thể bấm Làm mới để thử lại.",true);
  })();
  refreshing=task;
  try{await task;}finally{if(refreshing===task)refreshing=null;}
}
async function startSession(result) {
  saveSession(result); await refreshData(); setScreen(false);
  const active=state.history.find(game=>["PLAYING","WAITING"].includes(game.status));
  if(routeName()==="play"&&active){
    const resumed=await resumeGame(active.id);
    if(!resumed)renderRoute();
  }else renderRoute();
  if (!location.hash) go("home");
  $("password").value="";
  clearInterval(pollingTimer); pollingTimer=setInterval(pollGame,POLL_TICK_MS);
  const room=new URLSearchParams(location.search).get("room");
  if(room && (/^\d{6}$/.test(room)||/^[0-9a-f-]{36}$/i.test(room))) await joinGame(room);
}
async function authenticate(event) {
  event.preventDefault();
  if (!$("auth-form").reportValidity()) return;
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
function canAct() {
  return !state.busy && !state.syncIssue && state.game?.status==="PLAYING" && ["BOTH",state.game.currentTurn].includes(playerSide());
}
function openGame(game) {
  if(state.game?.id!==game.id)state.matchStartRating=["PLAYING","WAITING"].includes(game.status)?state.player?.rating??null:null;
  revision++; lastPollAt=0;boardView.cancel(); state.syncIssue=false;state.busy=false;state.game=game;state.selectedSquare=null;state.pendingCard=null;state.pendingTarget=null;state.lastMove=[];state.moveLog=[];state.moveLogStatus="loading";
  boardView.flipped=playerSide()==="BLACK";
  if(game.type==="PVP"&&["PLAYING","WAITING"].includes(game.status))connectGameSocket(game.id);else closeGameSocket();
  renderGameView();go("play");void loadMoveLog(game.id);
}
async function createGame(type,button) {
  if(opening||state.busy)return;opening=true;
  const token=state.session?.accessToken;
  try { await withAction(button,"Đang tạo ván…",async()=>{
    const game=await api("/api/games",{method:"POST",body:JSON.stringify({type,difficulty:type==="PVE"?$("pve-difficulty").value:null,side:type==="PVE"?$("pve-side").value:null})});
    if(token!==state.session?.accessToken)return;
    openGame(game);await refreshData();
  }); } finally { opening=false; }
}
async function quickMatch(button) {
  if(opening||state.busy)return;opening=true;const token=state.session?.accessToken;
  try{await withAction(button,"Đang tìm đối thủ…",async()=>{
    const game=await api("/api/games/quick-match",{method:"POST"});
    if(token!==state.session?.accessToken)return;
    openGame(game);toast(game.status==='WAITING'?`Đã tạo phòng #${game.roomCode}. Chia sẻ mã để mời bạn.`:`Đã ghép với ${game.whitePlayerId===state.player?.id?game.blackUsername:game.whiteUsername}.`);await refreshData();
  });}finally{opening=false;}
}
async function resumeGame(id) {
  if(state.busy)return;
  const token=state.session?.accessToken;
  return withAction(null,"",async()=>{const game=await api(`/api/games/${id}`);if(token===state.session?.accessToken){openGame(game);return game;}return null;});
}
async function joinGame(id,button) {
  if(opening||state.busy)return;opening=true;const token=state.session?.accessToken;
  try{await withAction(button,"Đang tham gia…",async()=>{
    let game;
    if(/^\d{6}$/.test(id))game=await api(`/api/games/rooms/${id}/join`,{method:"POST"});
    else try {game=await api(`/api/games/${id}`);}
    catch(error){if(error.status!==403)throw error;game=await api(`/api/games/${id}/join`,{method:"POST"});}
    if(token!==state.session?.accessToken)return;
    openGame(game);history.replaceState({}, "",location.pathname+"#/play");await refreshData();
  });}finally{opening=false;}
}
function selectSquare(row,col) {
  if(!canAct())return;
  const square=coordinate(row,col),game=state.game,piece=game.board[row][col],colour=game.currentTurn==="WHITE"?"w":"b";
  if(state.pendingCard)return useCard(state.pendingCard,square);
  if(piece?.[0]===colour) {
    const same=state.selectedSquare?.row===row&&state.selectedSquare?.col===col;
    state.selectedSquare=same?null:{row,col};renderGameView();return;
  }
  if(state.selectedSquare)return movePiece(coordinate(state.selectedSquare.row,state.selectedSquare.col),square);
}
function noteMove(before,after) {
  for(const colour of ["w","b"]) {
    const from=[],to=[];
    for(let r=0;r<8;r++)for(let c=0;c<8;c++){
      if(before[r][c]===after[r][c])continue;
      if(before[r][c]?.[0]===colour)from.push(coordinate(r,c));
      if(after[r][c]?.[0]===colour)to.push(coordinate(r,c));
    }
    if(from.length===1&&to.length===1){
      state.lastMove=[from[0],to[0]];
    }
  }
}
async function movePiece(from,to) {
  if(!canAct()||!boardView.selectable(from))return;
  if(!destinations(state.game,from).includes(to)){
    state.selectedSquare={row:point(from)[0],col:point(from)[1]};renderGameView();toast("Ô này không phải nước đi hợp lệ.",true);return;
  }
  const original=state.game,previousLast=state.lastMove,version=++revision;
  const preview=original.board.map(row=>[...row]),[r,c]=point(from),[y,x]=point(to),piece=preview[r][c];
  preview[y][x]=piece[1]==="P"&&(y===0||y===7)?piece[0]+"Q":piece;preview[r][c]=null;
  state.busy=true;state.selectedSquare=null;state.lastMove=[from,to];state.game={...original,board:preview};
  renderGameView();playSound(at(original.board,to)?"capture":"move");
  try {
    const confirmed=await api(`/api/games/${original.id}/moves`,{method:"POST",body:JSON.stringify({from,to})});
    if(version!==revision)return;
    noteMove(preview,confirmed.board);
    state.game=confirmed;state.syncIssue=false;if(!["PLAYING","WAITING"].includes(confirmed.status))closeGameSocket();
    await loadMoveLog(confirmed.id);
    if(!["PLAYING","WAITING"].includes(confirmed.status))void refreshData();
  } catch(error) {
    if(version!==revision)return;
    state.game=original;state.lastMove=previousLast;state.syncIssue=error.status===0;toast(error.message,true);
  } finally { if(version===revision){state.busy=false;renderGameView();} }
}
async function useCard(card,targeted=false) {
  if(!canAct())return;
  if(targeted===true){
    state.pendingCard=state.pendingCard?.id===card.id?null:card;state.pendingTarget=null;state.selectedSquare=null;renderGameView();
    if(state.pendingCard)toast(cardTargetPrompt(card.effectType,false));
    return;
  }
  if(card.effectType==="PORTAL"&&typeof targeted==="string"&&!state.pendingTarget){state.pendingTarget=targeted;renderGameView();toast(cardTargetPrompt(card.effectType,true));return;}
  if(card.effectType==="SURRENDER"&&!confirm("Dùng thẻ đầu hàng và kết thúc ván đấu?"))return;
  const game=state.game,version=++revision;state.busy=true;renderGameView();
  try{
    const target=card.effectType==="PORTAL"&&state.pendingTarget?`${state.pendingTarget}:${targeted}`:typeof targeted==="string"?targeted:"OPPONENT";
    const result=await api(`/api/games/${game.id}/cards`,{method:"POST",body:JSON.stringify({cardId:card.id,target})});
    if(version!==revision)return;
    state.game=result;state.pendingCard=null;state.pendingTarget=null;if(!["PLAYING","WAITING"].includes(result.status))closeGameSocket();cardBurst(card);boardView.effect(card.effectType,typeof targeted==="string"?targeted:null);playSound("card");
    await refreshData();
    await loadMoveLog(result.id);
  }catch(error){if(version===revision)toast(error.message,true);}
  finally{if(version===revision){state.busy=false;renderGameView();}}
}
function cardTargetPrompt(effect,second){return effect==="PORTAL"?(second?"Chọn quân thứ hai để mở Portal.":"Chọn quân thứ nhất cho Portal."):effect==="FREEZE"||effect==="MOSQUITO"?"Chọn quân đối thủ.":effect==="TRAP"?"Chọn ô muốn đặt bẫy.":effect==="PAWN_DASH"?"Chọn một Tốt của bạn.":"Chọn quân của bạn.";}
async function buyCard(id,button) {
  if(buying)return;buying=true;
  try{await withAction(button,"Đang mua…",async()=>{const item=state.shop.find(entry=>entry.id===id);await api(`/api/shop/${id}/purchase`,{method:"POST"});await refreshData();const owned=state.inventory.find(entry=>entry.cardId===item?.cardId);toast(owned?.deckSlot?`Đã thêm vào kho và trang bị ở ô ${owned.deckSlot}.`:"Đã thêm thẻ vào kho.");});}
  finally{buying=false;}
}
async function toggleDeck(cardId) {
  if(deckUpdating)return;
  const item=state.inventory.find(entry=>entry.cardId===cardId),equipped=state.inventory.filter(entry=>entry.deckSlot).sort((a,b)=>a.deckSlot-b.deckSlot);
  if(!item)return;
  const cardIds=item.deckSlot?equipped.filter(entry=>entry.cardId!==cardId).map(entry=>entry.cardId):[...equipped.map(entry=>entry.cardId),cardId];
  if(cardIds.length>3)return toast("Deck chỉ có tối đa 3 loại thẻ.",true);
  deckUpdating=true;
  try{
    state.inventory=await api("/api/cards/deck",{method:"PUT",body:JSON.stringify({cardIds})});
    renderInventory(toggleDeck);decorateCards();
    document.querySelector(`#card-inventory .card-tile[data-card-id="${CSS.escape(cardId)}"] button`)?.focus();
    toast(state.game?"Đã lưu Deck cho ván tiếp theo.":"Đã cập nhật Deck.");
  }catch(error){toast(error.message,true);}
  finally{deckUpdating=false;}
}
async function loadMoveLog(gameId) {
  const current=gameId;
  state.moveLogStatus="loading";
  try{
    const logs=await api(`/api/games/${gameId}/logs`);
    if(state.game?.id!==current)return;
    state.moveLog=logs;state.moveLogStatus="ready";renderGameView();
  }catch(error){
    if(state.game?.id!==current)return;
    state.moveLogStatus="error";renderGameView();console.error(error);
  }
}
async function submitPuzzle(id,from,to,button) {
  await withAction(button,"Đang kiểm tra…",async()=>{
    const result=await api(`/api/puzzles/${id}/submit`,{method:"POST",body:JSON.stringify({from,to})});
    toast(result.correct?result.rewardGranted?"Chính xác! Gold đã được cộng.":"Bạn đã giải puzzle này rồi.":"Chưa đúng, thử một nước khác.",!result.correct);
    if(result.correct)await refreshData();
  });
}
async function resign() {
  if(state.busy||state.game?.status!=="PLAYING"||!confirm("Bạn muốn đầu hàng ván đấu này?"))return;
  const game=state.game,version=++revision;state.busy=true;renderGameView();
  try{const result=await api(`/api/games/${game.id}/resign`,{method:"POST"});if(version!==revision)return;state.game=result;closeGameSocket();await refreshData();}
  catch(error){toast(error.message,true);}
  finally{if(version===revision){state.busy=false;renderGameView();}}
}
async function leave() {
  if(state.busy)return;
  await withAction($("leave-game"),"Đang rời bàn…",async()=>{
    const game=state.game;
    if(game?.status==="WAITING")await api(`/api/games/${game.id}/cancel`,{method:"POST"});
    revision++;boardView.cancel();closeGameSocket();state.game=null;state.matchStartRating=null;state.pendingCard=null;state.selectedSquare=null;go("home");await refreshData();
  });
}
function renderRoute() {
  boardView?.cancel();const route=routeName();
  const changed=document.querySelector(".page.is-active")?.dataset.page!==route;
  showRoute(route);document.body.classList.toggle("in-match",route==="play");if(changed)window.scrollTo({top:0,behavior:"instant"});
  if(route==="play")renderGameView();
  if(route==="profile")renderProfile(resumeGame);
  document.querySelectorAll("[data-route]").forEach(link=>{if(link.dataset.route===route)link.setAttribute("aria-current","page");else link.removeAttribute("aria-current");});
}
async function pollGame(force=false) {
  if(socketSyncPending)force=true;
  const game=state.game,version=revision;
  if(polling||document.hidden||state.busy||boardView.drag||!game||(game.type!=="PVP"&&!state.syncIssue)||!["WAITING","PLAYING"].includes(game.status))return;
  const waiting=game.status==="WAITING"||!["BOTH",game.currentTurn].includes(playerSide()),delay=socketOpen(game.id)?POLL_SOCKET_FALLBACK_MS:waiting||state.syncIssue?POLL_WAITING_MS:POLL_ACTIVE_MS,now=performance.now();
  if(!force&&now-lastPollAt<delay)return;
  socketSyncPending=false;
  lastPollAt=now;
  polling=true;
  try{
    const result=await api(`/api/games/${game.id}`);
    if(version!==revision||state.busy||boardView.drag||state.game?.id!==game.id)return;
    const reconnecting=state.syncIssue;state.syncIssue=false;
    if(JSON.stringify(result)===JSON.stringify(game)){if(reconnecting)renderGameView();return;}
    noteMove(game.board,result.board);state.game=result;state.selectedSquare=null;state.pendingCard=null;renderGameView();void loadMoveLog(result.id);
    if(game.status==="WAITING"&&result.status==="PLAYING")toast("Bạn chơi đã vào phòng. Bắt đầu thôi!");
    if(!["PLAYING","WAITING"].includes(result.status)){closeGameSocket();void refreshData();}
  }catch{if(version===revision){state.syncIssue=true;renderGameView();}}
  finally{polling=false;if(socketSyncPending&&!state.busy&&!boardView.drag)queueMicrotask(()=>void pollGame(true));}
}
function bind() {
  document.querySelectorAll("[data-auth-mode]").forEach(button=>button.addEventListener("click",()=>setAuthMode(button.dataset.authMode)));
  $("auth-form").addEventListener("submit",authenticate);$("guest-login").addEventListener("click",guestLogin);
  $("forgot-password").addEventListener("click",showForgot);$("back-to-login").addEventListener("click",showLogin);
  $("forgot-form").addEventListener("submit",requestPasswordReset);$("new-password-form").addEventListener("submit",confirmPasswordReset);
  $("sign-out").addEventListener("click",()=>showAuth("Bạn đã đăng xuất."));$("profile-button").addEventListener("click",()=>go("profile"));
  document.querySelectorAll("[data-create-game]").forEach(button=>button.addEventListener("click",()=>createGame(button.dataset.createGame,button)));
  $("quick-match").addEventListener("click",()=>quickMatch($("quick-match")));
  $("starter-play").addEventListener("click",()=>{dismissOnboarding();$("pve-difficulty").value="EASY";$("pve-side").value="WHITE";void createGame("PVE",$("starter-play"));});
  $("starter-skip").addEventListener("click",dismissOnboarding);
  $("refresh-waiting").addEventListener("click",()=>withAction($("refresh-waiting"),"Đang tải…",refreshData));
  $("refresh-profile").addEventListener("click",()=>withAction($("refresh-profile"),"Đang tải…",refreshData));
  $("resign-game").addEventListener("click",resign);$("leave-game").addEventListener("click",leave);
  $("flip-board").addEventListener("click",()=>{boardView.flip();renderGameView();});
  $("cancel-card").addEventListener("click",()=>{state.pendingCard=null;state.pendingTarget=null;renderGameView();});
  const sound=$("sound-toggle");
  function syncSound(){const on=localStorage.getItem("chess2.sound")==="on";sound.setAttribute("aria-pressed",String(on));sound.setAttribute("aria-label",on?"Tắt âm thanh":"Bật âm thanh");}
  syncSound();sound.addEventListener("click",()=>{localStorage.setItem("chess2.sound",localStorage.getItem("chess2.sound")==="on"?"off":"on");syncSound();playSound();});
  $("play-again").addEventListener("click",()=>createGame(state.game?.type||"PVE",$("play-again")));
  $("join-form").addEventListener("submit",event=>{
    event.preventDefault();let id=$("join-code").value.trim().replace(/^#/,"");
    try{if(id.includes("://"))id=new URL(id).searchParams.get("room")||"";}catch{id="";}
    if(!/^\d{6}$/.test(id)&&!/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(id))return toast("Nhập mã phòng gồm 6 chữ số hoặc dán liên kết mời.",true);
    void joinGame(id,event.submitter);
  });
  $("copy-invite").addEventListener("click",async()=>{
    try{await navigator.clipboard.writeText($("invite-url").value);toast("Đã sao chép lời mời. Gửi link này cho bạn bè.");}
    catch{$("invite-url").select();toast("Chọn Sao chép để gửi liên kết mời.");}
  });
  addEventListener("hashchange",renderRoute);
  document.addEventListener("visibilitychange",()=>{if(!document.hidden)void pollGame();});
}
async function init() {
  mountExperience();bind();setAuthMode("login");renderGameView();
  setUnauthorizedHandler(()=>showAuth("Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."));
  const params=new URLSearchParams(location.search);
  if(params.has("username")||params.has("password"))history.replaceState({}, "",location.pathname+location.hash);
  if(params.has("reset")){showForgot();showNewPassword();return;}
  const session=restoreSession();if(!session)return;
  try{await startSession(session);}
  catch(error){if(error.status!==401)showAuth(error.message);}
}
init().catch(error=>{$("auth-message").textContent=error.message||"Không thể khởi động giao diện. Hãy tải lại trang.";});
