import { api, setUnauthorizedHandler } from "./api.js";
import { clearSession, restoreSession, saveSession, stableGuestKey, state, playerSide } from "./state.js";
import { attachBoardDrag, boardView, renderGame, renderInventory, renderLeaderboard, renderPlayer, renderPuzzles, renderShop, renderWaiting, showRoute, toast } from "./render.js";

import { coordinate, point, at, destinations, reducedMotion } from "./board.js";
import { mountExperience, renderProfile, decorateCards, playSound, cardBurst } from "./experience.js";

const $ = (id) => document.getElementById(id);
const routes = new Set(["home", "play", "collection", "puzzles", "leaderboard", "profile"]);
let pollingTimer = null, revision = 0, refreshing = null, buying = false, opening = false, polling = false;

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
  revision++; refreshing=null; boardView?.cancel(); clearInterval(pollingTimer); clearSession(); setScreen(true); $("auth-message").textContent = message;
}
function renderEverything() {
  renderPlayer(); renderInventory(); renderShop(buyCard); renderPuzzles(submitPuzzle); renderLeaderboard(); renderWaiting(joinGame); renderGameView(); decorateCards(); renderProfile(resumeGame);
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
  saveSession(result); await refreshData(); setScreen(false); renderRoute();
  if (!location.hash) go("home");
  $("password").value="";
  clearInterval(pollingTimer); pollingTimer=setInterval(pollGame,1800);
  const room=new URLSearchParams(location.search).get("room");
  if(room && /^[0-9a-f-]{36}$/i.test(room)) await joinGame(room);
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
  revision++; boardView.cancel(); state.syncIssue=false;state.busy=false;state.game=game;state.selectedSquare=null;state.pendingCard=null;state.lastMove=[];state.moveLog=[];
  boardView.flipped=playerSide()==="BLACK";
  renderGameView();go("play");
}
async function createGame(type,button) {
  if(opening||state.busy)return;opening=true;
  const token=state.session?.accessToken;
  try { await withAction(button,"Đang tạo ván…",async()=>{
    const game=await api("/api/games",{method:"POST",body:JSON.stringify({type,difficulty:type==="PVE"?$("pve-difficulty").value:null})});
    if(token!==state.session?.accessToken)return;
    openGame(game);await refreshData();
  }); } finally { opening=false; }
}
async function quickMatch(button) {
  if(opening||state.busy)return;opening=true;const token=state.session?.accessToken;
  try{await withAction(button,"Đang tìm đối thủ…",async()=>{
    const game=await api("/api/games/quick-match",{method:"POST"});
    if(token!==state.session?.accessToken)return;
    openGame(game);await refreshData();
  });}finally{opening=false;}
}
async function resumeGame(id) {
  if(state.busy)return;
  const token=state.session?.accessToken;
  await withAction(null,"",async()=>{const game=await api(`/api/games/${id}`);if(token===state.session?.accessToken)openGame(game);});
}
async function joinGame(id,button) {
  if(opening||state.busy)return;opening=true;const token=state.session?.accessToken;
  try{await withAction(button,"Đang tham gia…",async()=>{
    let game;
    try {game=await api(`/api/games/${id}`);}
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
function noteMove(before,after,label) {
  for(const colour of ["w","b"]) {
    const from=[],to=[];
    for(let r=0;r<8;r++)for(let c=0;c<8;c++){
      if(before[r][c]===after[r][c])continue;
      if(before[r][c]?.[0]===colour)from.push(coordinate(r,c));
      if(after[r][c]?.[0]===colour)to.push(coordinate(r,c));
    }
    if(from.length===1&&to.length===1){
      state.moveLog.push(`${label|| (colour==="w"?"Trắng":"Đen")}: ${from[0]} → ${to[0]}`);
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
    state.moveLog.push(`${original.currentTurn==="WHITE"?"Trắng":"Đen"}: ${from} → ${to}`);
    noteMove(preview,confirmed.board,original.type==="PVE"?"Bot":null);
    state.game=confirmed;state.syncIssue=false;
    if(!["PLAYING","WAITING"].includes(confirmed.status))void refreshData();
  } catch(error) {
    if(version!==revision)return;
    state.game=original;state.lastMove=previousLast;state.syncIssue=error.status===0;toast(error.message,true);
  } finally { if(version===revision){state.busy=false;renderGameView();} }
}
async function useCard(card,targeted=false) {
  if(!canAct())return;
  if(targeted===true){
    state.pendingCard=state.pendingCard?.id===card.id?null:card;state.selectedSquare=null;renderGameView();
    if(state.pendingCard)toast(card.effectType==="FREEZE"?"Chọn quân đối thủ để đóng băng.":"Chọn quân của bạn để bảo vệ.");
    return;
  }
  if(card.effectType==="SURRENDER"&&!confirm("Dùng thẻ đầu hàng và kết thúc ván đấu?"))return;
  const game=state.game,version=++revision;state.busy=true;renderGameView();
  try{
    const result=await api(`/api/games/${game.id}/cards`,{method:"POST",body:JSON.stringify({cardId:card.id,target:typeof targeted==="string"?targeted:"OPPONENT"})});
    if(version!==revision)return;
    state.game=result;state.pendingCard=null;state.moveLog.push(`✦ ${card.name}`);cardBurst(card);boardView.effect(card.effectType,targeted);playSound("card");
    await refreshData();
  }catch(error){if(version===revision)toast(error.message,true);}
  finally{if(version===revision){state.busy=false;renderGameView();}}
}
async function buyCard(id,button) {
  if(buying)return;buying=true;
  try{await withAction(button,"Đang mua…",async()=>{await api(`/api/shop/${id}/purchase`,{method:"POST"});toast("Đã thêm thẻ vào kho.");await refreshData();});}
  finally{buying=false;}
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
  try{const result=await api(`/api/games/${game.id}/resign`,{method:"POST"});if(version!==revision)return;state.game=result;await refreshData();}
  catch(error){toast(error.message,true);}
  finally{if(version===revision){state.busy=false;renderGameView();}}
}
async function leave() {
  if(state.busy)return;
  await withAction($("leave-game"),"Đang rời bàn…",async()=>{
    const game=state.game;
    if(game?.status==="WAITING")await api(`/api/games/${game.id}/cancel`,{method:"POST"});
    revision++;boardView.cancel();state.game=null;state.pendingCard=null;state.selectedSquare=null;go("home");await refreshData();
  });
}
function renderRoute() {
  boardView?.cancel();const route=routeName();
  const changed=document.querySelector(".page.is-active")?.dataset.page!==route;
  showRoute(route);if(changed)window.scrollTo({top:0,behavior:"instant"});
  if(route==="play")renderGameView();
  if(route==="profile")renderProfile(resumeGame);
  document.querySelectorAll("[data-route]").forEach(link=>{if(link.dataset.route===route)link.setAttribute("aria-current","page");else link.removeAttribute("aria-current");});
}
async function pollGame() {
  const game=state.game,version=revision;
  if(polling||document.hidden||state.busy||boardView.drag||!game||(game.type!=="PVP"&&!state.syncIssue)||!["WAITING","PLAYING"].includes(game.status))return;
  polling=true;
  try{
    const result=await api(`/api/games/${game.id}`);
    if(version!==revision||state.busy||boardView.drag||state.game?.id!==game.id)return;
    const reconnecting=state.syncIssue;state.syncIssue=false;
    if(JSON.stringify(result)===JSON.stringify(game)){if(reconnecting)renderGameView();return;}
    noteMove(game.board,result.board);state.game=result;state.selectedSquare=null;state.pendingCard=null;renderGameView();
    if(game.status==="WAITING"&&result.status==="PLAYING")toast("Bạn chơi đã vào phòng. Bắt đầu thôi!");
    if(!["PLAYING","WAITING"].includes(result.status))void refreshData();
  }catch{if(version===revision){state.syncIssue=true;renderGameView();}}
  finally{polling=false;}
}
function bind() {
  document.querySelectorAll("[data-auth-mode]").forEach(button=>button.addEventListener("click",()=>setAuthMode(button.dataset.authMode)));
  $("auth-form").addEventListener("submit",authenticate);$("guest-login").addEventListener("click",guestLogin);
  $("forgot-password").addEventListener("click",showForgot);$("back-to-login").addEventListener("click",showLogin);
  $("forgot-form").addEventListener("submit",requestPasswordReset);$("new-password-form").addEventListener("submit",confirmPasswordReset);
  $("sign-out").addEventListener("click",()=>showAuth("Bạn đã đăng xuất."));$("profile-button").addEventListener("click",()=>go("profile"));
  document.querySelectorAll("[data-create-game]").forEach(button=>button.addEventListener("click",()=>createGame(button.dataset.createGame,button)));
  $("quick-match").addEventListener("click",()=>quickMatch($("quick-match")));
  $("refresh-waiting").addEventListener("click",()=>withAction($("refresh-waiting"),"Đang tải…",refreshData));
  $("refresh-profile").addEventListener("click",()=>withAction($("refresh-profile"),"Đang tải…",refreshData));
  $("resign-game").addEventListener("click",resign);$("leave-game").addEventListener("click",leave);
  $("flip-board").addEventListener("click",()=>{boardView.flip();renderGameView();});
  $("cancel-card").addEventListener("click",()=>{state.pendingCard=null;renderGameView();});
  const sound=$("sound-toggle");
  function syncSound(){const on=localStorage.getItem("chess2.sound")==="on";sound.setAttribute("aria-pressed",String(on));sound.setAttribute("aria-label",on?"Tắt âm thanh":"Bật âm thanh");}
  syncSound();sound.addEventListener("click",()=>{localStorage.setItem("chess2.sound",localStorage.getItem("chess2.sound")==="on"?"off":"on");syncSound();playSound();});
  $("play-again").addEventListener("click",()=>createGame(state.game?.type||"PVE",$("play-again")));
  $("join-form").addEventListener("submit",event=>{
    event.preventDefault();let id=$("join-code").value.trim();
    try{if(id.includes("://"))id=new URL(id).searchParams.get("room")||"";}catch{id="";}
    if(!/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(id))return toast("Hãy nhập đúng mã phòng hoặc liên kết mời.",true);
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
