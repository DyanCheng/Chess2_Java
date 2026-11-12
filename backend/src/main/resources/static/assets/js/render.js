import { state, playerSide } from "./state.js";
import { ChessBoardView, coordinate, pieceSvg } from "./board.js";
import { effects, renderMatchChrome } from "./experience.js";
import { cardArt, cardNames } from "./visuals.js";
export let boardView;
const $ = (id) => document.getElementById(id); const node = (tag, className, text) => { const el = document.createElement(tag); if (className) el.className = className; if (text !== undefined) el.textContent = text; return el; }; const cardFor = (id) => state.cards.find((card) => card.id === id); const effectText = (card) => ({ HEAL: `Hồi ${card.effectValue} HP`, DOUBLE_DAMAGE: `Gây ${card.effectValue} sát thương`, FREEZE: "Khóa một quân đối thủ trong lượt kế", INVULNERABLE: `Bảo vệ một quân trong ${card.effectValue} lượt`, PAWN_DASH:"Cho một Tốt tiến thêm 1 ô",TRAP:`Đặt bẫy phát nổ sau ${card.effectValue} lượt`,REWIND:"Khôi phục bàn cờ ở lượt trước",PAWN_BACKWARD:"Tốt của bạn có thể đi lùi",PORTAL:"Hoán đổi vị trí hai quân không phải Vua", EXTRA_QUEEN: "Đổi 80% HP để gọi thêm Hậu",REVIVE:"Hồi sinh quân mạnh nhất đã bị bắt",HIDE_KING:"Đổi chỗ Vua với Xe gần nhất",TELEPORT:"Dịch chuyển một quân đến ô trống ngẫu nhiên",MOSQUITO:"Khóa quân đối thủ và rút 1 HP mỗi lượt", SURRENDER: "Kết thúc trận và chấp nhận thua" }[card?.effectType] || "Thẻ chiến thuật");
export function toast(message, error = false) { const el = $("toast"); el.textContent = message; el.classList.toggle("error", error); el.classList.add("show"); clearTimeout(toast.timer); toast.timer = setTimeout(() => el.classList.remove("show"), 3600); }
export function showRoute(route) { document.querySelectorAll("[data-page]").forEach((page) => page.classList.toggle("is-active", page.dataset.page === route)); document.querySelectorAll("[data-route]").forEach((link) => link.classList.toggle("is-active", link.dataset.route === route)); }
export function renderPlayer() { const p = state.player; if (!p) return; $("player-name").textContent = p.username; $("header-name").textContent = p.username; $("header-avatar").textContent = p.username[0].toUpperCase(); $("player-subtitle").textContent = p.accountType === "GUEST" ? "Tài khoản khách được lưu trên thiết bị này." : "Sẵn sàng cho một ván đấu mới?"; $("player-rating").textContent = p.rating; $("player-record").textContent = `${p.wins} thắng · ${p.losses} thua · ${p.draws} hòa`; $("player-level").textContent = p.level; $("player-xp").textContent = p.experience; $("dashboard-gold").textContent = p.gold; $("header-gold").textContent = p.gold; $("header-gems").textContent = p.gems; $("admin-nav").hidden=p.role!=="ADMIN"; }
export function renderBoard(onSquare) {
  if (!boardView) boardView = new ChessBoardView($("board"), { select: onSquare, move: (...args) => boardView.onMove?.(...args), cancel: () => { state.pendingCard=null;state.selectedSquare=null;renderGame(onSquare,boardView.onUseCard); } });
  boardView.render({ game:state.game, side:playerSide(), selected:state.selectedSquare?coordinate(state.selectedSquare.row,state.selectedSquare.col):null, lastMove:state.lastMove, busy:state.busy||state.syncIssue, pendingCard:state.pendingCard });
}
export function attachBoardDrag(onMove) { if(boardView)boardView.onMove=onMove; }
export function renderGame(onSquare, onUseCard) {
  const game=state.game;
  $("resign-game").hidden=!game||game.status!=="PLAYING";
  setHealth("white",game?.whiteHp??100);setHealth("black",game?.blackHp??100);
  renderBoard(onSquare);boardView.onUseCard=onUseCard;
  $("resign-game").disabled=state.busy;$("leave-game").disabled=state.busy;
  const whiteStrip=$("white-hp").closest(".player-strip"),blackStrip=$("black-hp").closest(".player-strip"),wrap=document.querySelector(".board-wrap"),area=wrap.parentElement;
  area.insertBefore(boardView.flipped?whiteStrip:blackStrip,wrap);
  area.insertBefore(boardView.flipped?blackStrip:whiteStrip,wrap.nextSibling);
  // The live turn label belongs to the player row, including after a board flip.
  (boardView.flipped?blackStrip:whiteStrip).append(document.querySelector('.board-toolbar'));
  const root=$("game-cards");
  const inventory=state.inventory.filter(item=>item.quantity>0&&cardFor(item.cardId));
  const key=JSON.stringify([inventory,state.pendingCard?.id,game?.id,game?.status,game?.currentTurn,state.busy,state.syncIssue]);
  if(root.dataset.renderKey!==key){
    root.dataset.renderKey=key;root.replaceChildren();
    if(!game||!inventory.length){root.className="list-state";root.append(node("p","empty-hand",game?"Kho thẻ đang trống. Ghé cửa hàng để chọn chiêu thức đầu tiên.":"Tạo một ván để dùng thẻ.")); if(game)root.firstChild.textContent="Kho thẻ đang trống. Ghé cửa hàng để chọn chiêu thức đầu tiên."; }
    else {
      root.className="action-stack";
      inventory.forEach(item=>{
        const card=cardFor(item.cardId),fx=effects[card.effectType]||effects.HEAL,targeted=["FREEZE","INVULNERABLE","PAWN_DASH","TRAP","PORTAL","TELEPORT","MOSQUITO"].includes(card.effectType);
        const button=node("button","tactical-card"+(state.pendingCard?.id===card.id?" is-armed":""));
        button.type="button";button.dataset.cardId=card.id;button.style.setProperty("--card-accent",fx[3]);
        button.append(cardArt(card.effectType));
        const info=node("span","tactical-info");info.append(node("strong","",cardNames[card.effectType]||card.name),node("small","card-original",card.name),node("small","card-effect",effectText(card)));
        button.append(info,node("span","card-count","×"+item.quantity));
        button.disabled=state.busy||state.syncIssue||game.status!=="PLAYING"||(!["BOTH",game.currentTurn].includes(playerSide()));
        button.setAttribute("aria-pressed",String(state.pendingCard?.id===card.id));
        button.addEventListener("click",()=>onUseCard(card,targeted));root.append(button);
      });
    }
  }
  renderMatchChrome();
}
function setHealth(side, value) { $(`${side}-hp`).style.width = `${Math.max(0, Math.min(100, value))}%`; $(`${side}-hp-label`).textContent = `${value} / 100 HP`; }
export function renderInventory() { const root = $("card-inventory"); root.replaceChildren(); if (!state.inventory.length) { root.textContent = "Chưa sở hữu thẻ. Hãy mua từ cửa hàng."; return; } state.inventory.forEach((item) => { const card = cardFor(item.cardId); if (card) root.append(cardTile(card, `Số lượng: ${item.quantity}`)); }); }
export function renderShop(onBuy) { const root = $("shop-items"); root.replaceChildren(); if (!state.shop.length) { root.textContent = "Cửa hàng chưa có thẻ."; return; } state.shop.filter((item) => item.active).forEach((item) => { const card = cardFor(item.cardId) || { name: item.name }; const tile = cardTile(card, effectText(card)); const button = node("button", "button primary compact", `Mua · ${item.price} Gold`); button.addEventListener("click", () => onBuy(item.id, button)); tile.append(button); root.append(tile); }); }
function cardTile(card, subtitle) { const tile = node("article", "card-tile"); tile.append(node("h3", "", card.name), node("p", "", subtitle)); return tile; }
export function renderPuzzles(onSubmit) {
  const root=$("puzzles");root.replaceChildren();
  if(!state.puzzles.length){root.textContent="Chưa có puzzle.";return;}
  state.puzzles.forEach((puzzle,index)=>{
    const item=node("article","puzzle-card"),form=node("form","puzzle-form");
    form.append(input("from","Từ (e2)"),input("to","Đến (e8)"));
    const submit=node("button","button primary compact","Giải");submit.type="submit";form.append(submit);
    const preview=node("div","puzzle-board");preview.setAttribute("aria-label","Bàn puzzle "+(index+1));
    const board=typeof puzzle.board==="string"?JSON.parse(puzzle.board):puzzle.board;
    let selected=null;
    board.forEach((row,r)=>row.forEach((code,c)=>{
      const square=coordinate(r,c),button=node("button",(r+c)%2?"mini-square":"mini-square light");
      button.type="button";button.setAttribute("aria-label",code?square+": "+code:square);
      if(code)button.innerHTML=pieceSvg(code);
      button.addEventListener("click",()=>{
        if(!selected||code?.[0]==="w"){
          if(code?.[0]!=="w")return;
          preview.querySelector(".selected")?.classList.remove("selected");button.classList.add("selected");
          selected=square;form.elements.from.value=square;form.elements.to.value="";
        }else{form.elements.to.value=square;submit.focus();}
      });preview.append(button);
    }));
    form.addEventListener("submit",event=>{event.preventDefault();onSubmit(puzzle.id,form.elements.from.value.toLowerCase(),form.elements.to.value.toLowerCase(),submit);});
    item.append(node("p","kicker",`PUZZLE ${index+1} · TRẮNG ĐI`),node("h3","","Tạo thế tấn công Vua"),preview,node("p","","Bấm quân Trắng rồi chọn ô đích."),node("p","",`Phần thưởng: ${puzzle.rewardGold} Gold`),form);root.append(item);
  });
}
function input(name, placeholder) { const el = document.createElement("input"); el.name = name; el.setAttribute("aria-label", name === "from" ? "Ô xuất phát" : "Ô đích"); el.placeholder = placeholder; el.maxLength = 2; el.pattern = "[a-h][1-8]"; el.required = true; return el; }
export function renderLeaderboard() { const root = $("leaderboard"); root.replaceChildren(); if (!state.leaderboard.length) { root.textContent = "Chưa có kỳ thủ."; return; } state.leaderboard.forEach((player, index) => { const row = node("div", "leader-row"); const info = node("div", "leader-player"); info.append(node("strong", "", player.username), node("small", "", `${player.wins} thắng · ${player.losses} thua · ${player.draws} hòa`)); row.append(node("span", "rank", `#${index + 1}`), info, node("span", "score", `${player.rating} ELO`)); root.append(row); }); }
export function renderWaiting(onJoin) { const root = $("waiting-games"); root.replaceChildren(); if (!state.waitingGames.length) { root.textContent = "Chưa có phòng đang chờ."; return; } state.waitingGames.forEach((game) => { const row = node("div", "waiting-row"); const text = node("div"); text.append(node("strong", "", "Phòng PvP"), node("small", "", "Chờ quân Đen tham gia")); const join = node("button", "button secondary compact", "Tham gia"); join.addEventListener("click", () => onJoin(game.id, join)); row.append(text, join); root.append(row); }); }

export function renderAdmin(onAction){
  const root=$("admin-overview");if(!root)return;root.replaceChildren();
  if(state.player?.role!=="ADMIN"||!state.admin){root.className="admin-dashboard list-state";root.textContent="Chỉ tài khoản ADMIN mới truy cập được.";return;}
  root.className="admin-dashboard";const summary=node("div","admin-summary");
  [["Người dùng",state.admin.users],["Ván đấu",state.admin.games],["Đang chơi",state.admin.activeGames],["Thẻ",state.admin.cards]].forEach(([label,value])=>{const tile=node("article","stat-tile");tile.append(node("span","",label),node("strong","",String(value)));summary.append(tile);});root.append(summary);
  const users=node("section","panel admin-section");users.append(node("h2","","Tài khoản & tài nguyên"));
  state.admin.recentUsers.forEach(user=>{const row=node("form","admin-row");row.addEventListener("submit",e=>e.preventDefault());const identity=node("div","admin-identity");identity.append(node("strong","",user.username),node("small","",`${user.email} · ${user.accountType}`));
    const gold=document.createElement("input");gold.type="number";gold.min="0";gold.value=user.gold;gold.setAttribute("aria-label",`Gold của ${user.username}`);
    const gems=document.createElement("input");gems.type="number";gems.min="0";gems.value=user.gems;gems.setAttribute("aria-label",`Gem của ${user.username}`);
    const role=document.createElement("select");role.innerHTML='<option value="PLAYER">PLAYER</option><option value="ADMIN">ADMIN</option>';role.value=user.role;role.setAttribute("aria-label",`Quyền của ${user.username}`);
    const save=node("button","button primary compact","Lưu ví");save.type="button";save.addEventListener("click",()=>onAction({path:`/api/admin/users/${user.id}/wallet`},{gold:Number(gold.value),gems:Number(gems.value)},save));
    const setRole=node("button","button secondary compact","Lưu quyền");setRole.type="button";setRole.addEventListener("click",()=>onAction({path:`/api/admin/users/${user.id}/role`},{role:role.value},setRole));
    const cardSelect=document.createElement("select");state.cards.forEach(card=>{const option=document.createElement("option");option.value=card.id;option.textContent=card.name;cardSelect.append(option);});cardSelect.setAttribute("aria-label",`Thẻ cấp cho ${user.username}`);
    const cardQuantity=document.createElement("input");cardQuantity.type="number";cardQuantity.min="1";cardQuantity.value="1";cardQuantity.setAttribute("aria-label",`Số thẻ cấp cho ${user.username}`);
    const grant=node("button","button ghost compact","Cấp thẻ");grant.type="button";grant.addEventListener("click",()=>{if(cardSelect.value)onAction({path:`/api/admin/users/${user.id}/cards`,method:"POST"},{cardId:cardSelect.value,quantity:Number(cardQuantity.value)},grant);});
    row.append(identity,gold,gems,role,save,setRole,cardSelect,cardQuantity,grant);users.append(row);
  });root.append(users);
  const shop=node("section","panel admin-section");shop.append(node("h2","","Cửa hàng"));state.admin.shop.forEach(item=>{const row=node("form","admin-row shop-admin-row");row.addEventListener("submit",e=>e.preventDefault());row.append(node("strong","",item.name));const price=document.createElement("input");price.type="number";price.min="0";price.value=item.price;const active=document.createElement("input");active.type="checkbox";active.checked=item.active;active.setAttribute("aria-label",`Bật bán ${item.name}`);const save=node("button","button primary compact","Cập nhật");save.type="button";save.addEventListener("click",()=>onAction({path:`/api/admin/shop/${item.id}`},{price:Number(price.value),active:active.checked},save));row.append(price,active,save);shop.append(row);});root.append(shop);
}
