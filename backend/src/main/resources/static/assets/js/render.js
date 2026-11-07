import { state, playerSide } from "./state.js";
import { ChessBoardView, coordinate, pieceSvg } from "./board.js";
import { effects, renderMatchChrome } from "./experience.js";
export let boardView;
const $ = (id) => document.getElementById(id); const node = (tag, className, text) => { const el = document.createElement(tag); if (className) el.className = className; if (text !== undefined) el.textContent = text; return el; }; const cardFor = (id) => state.cards.find((card) => card.id === id); const effectText = (card) => ({ HEAL: `Hồi ${card.effectValue} HP`, DOUBLE_DAMAGE: `Gây ${card.effectValue} sát thương`, FREEZE: "Khóa một quân đối thủ trong lượt kế", INVULNERABLE: `Bảo vệ một quân trong ${card.effectValue} lượt`, EXTRA_QUEEN: "Đổi 80% HP để gọi thêm Hậu", SURRENDER: "Kết thúc trận và chấp nhận thua" }[card?.effectType] || "Hiệu ứng đang được phát triển");
export function toast(message, error = false) { const el = $("toast"); el.textContent = message; el.classList.toggle("error", error); el.classList.add("show"); clearTimeout(toast.timer); toast.timer = setTimeout(() => el.classList.remove("show"), 3600); }
export function showRoute(route) { document.querySelectorAll("[data-page]").forEach((page) => page.classList.toggle("is-active", page.dataset.page === route)); document.querySelectorAll("[data-route]").forEach((link) => link.classList.toggle("is-active", link.dataset.route === route)); }
export function renderPlayer() { const p = state.player; if (!p) return; $("player-name").textContent = p.username; $("header-name").textContent = p.username; $("header-avatar").textContent = p.username[0].toUpperCase(); $("player-subtitle").textContent = p.accountType === "GUEST" ? "Tài khoản khách được lưu trên thiết bị này." : "Sẵn sàng cho một ván đấu mới?"; $("player-rating").textContent = p.rating; $("player-record").textContent = `${p.wins} thắng · ${p.losses} thua · ${p.draws} hòa`; $("player-level").textContent = p.level; $("player-xp").textContent = p.experience; $("dashboard-gold").textContent = p.gold; $("header-gold").textContent = p.gold; $("header-gems").textContent = p.gems; }
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
  const root=$("game-cards");
  const inventory=state.inventory.filter(item=>item.quantity>0&&cardFor(item.cardId));
  const key=JSON.stringify([inventory,state.pendingCard?.id,game?.id,game?.status,game?.currentTurn,state.busy,state.syncIssue]);
  if(root.dataset.renderKey!==key){
    root.dataset.renderKey=key;root.replaceChildren();
    if(!game||!inventory.length){root.className="list-state";root.append(node("p","empty-hand",game?"Kho thẻ đang trống. Ghé cửa hàng để chọn chiêu thức đầu tiên.":"Tạo một ván để dùng thẻ.")); if(game)root.firstChild.textContent="Kho thẻ đang trống. Ghé cửa hàng để chọn chiêu thức đầu tiên."; }
    else {
      root.className="action-stack";
      inventory.forEach(item=>{
        const card=cardFor(item.cardId),fx=effects[card.effectType]||effects.HEAL,targeted=["FREEZE","INVULNERABLE"].includes(card.effectType);
        const button=node("button","tactical-card"+(state.pendingCard?.id===card.id?" is-armed":""));
        button.type="button";button.dataset.cardId=card.id;button.style.setProperty("--card-accent",fx[3]);
        button.append(node("span","tactical-emblem",fx[0]));
        const info=node("span","tactical-info");info.append(node("strong","",card.name),node("small","",effectText(card)));
        button.append(info,node("span","card-count","×"+item.quantity));
        button.disabled=state.busy||state.syncIssue||game.status!=="PLAYING"||(!["BOTH",game.currentTurn].includes(playerSide()));
        button.setAttribute("aria-pressed",String(state.pendingCard?.id===card.id));
        button.addEventListener("click",()=>onUseCard(card,targeted));root.append(button);
      });
    }
  }
  renderMatchChrome();
}
function setHealth(side, value) { $(`${side}-hp`).style.width = `${Math.max(0, Math.min(100, value))}%`; $(`${side}-hp-label`).textContent = value; }
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
