import { state, playerSide } from "./state.js";
import { ChessBoardView, coordinate, pieceSvg } from "./board.js";
import { effects, renderMatchChrome } from "./experience.js";
import { cardArt, cardNames } from "./visuals.js";
export let boardView;
const $ = (id) => document.getElementById(id); const node = (tag, className, text) => { const el = document.createElement(tag); if (className) el.className = className; if (text !== undefined) el.textContent = text; return el; }; const cardFor = (id) => state.cards.find((card) => card.id === id); const effectText = (card) => ({ HEAL: `Hồi ${card.effectValue} HP`, DOUBLE_DAMAGE: `Gây ${card.effectValue} sát thương`, FREEZE: "Khóa một quân đối thủ trong lượt kế", INVULNERABLE: `Bảo vệ một quân trong ${card.effectValue} lượt`, PAWN_DASH:"Cho một Tốt tiến thêm 1 ô",TRAP:`Đặt bẫy phát nổ sau ${card.effectValue} lượt`,REWIND:"Khôi phục bàn cờ ở lượt trước",PAWN_BACKWARD:"Tốt của bạn có thể đi lùi",PORTAL:"Hoán đổi vị trí hai quân không phải Vua", EXTRA_QUEEN: "Đổi 80% HP để gọi thêm Hậu",REVIVE:"Hồi sinh quân mạnh nhất đã bị bắt",HIDE_KING:"Đổi chỗ Vua với Xe gần nhất",TELEPORT:"Dịch chuyển một quân đến ô trống ngẫu nhiên",MOSQUITO:"Khóa quân đối thủ và rút 1 HP mỗi lượt", SURRENDER: "Kết thúc trận và chấp nhận thua" }[card?.effectType] || "Thẻ chiến thuật");
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
  // The live turn label belongs to the player row, including after a board flip.
  (boardView.flipped?blackStrip:whiteStrip).append(document.querySelector('.board-toolbar'));
  const root=$("game-cards");
  const deckIds=new Set(game?.deckCardIds||[]);
  const inventory=state.inventory.filter(item=>item.quantity>0&&deckIds.has(item.cardId)&&cardFor(item.cardId));
  const key=JSON.stringify([inventory,state.pendingCard?.id,game?.id,game?.status,game?.currentTurn,game?.whiteHp,game?.blackHp,state.busy,state.syncIssue]);
  if(root.dataset.renderKey!==key){
    root.dataset.renderKey=key;root.replaceChildren();
    if(!game||!inventory.length){root.className="list-state";root.append(node("p","empty-hand",game?"Deck của ván này đang trống. Trang bị thẻ trước khi bắt đầu ván kế tiếp.":"Tạo một ván để dùng thẻ."));}
    else {
      root.className="action-stack";
      inventory.forEach(item=>{
        const card=cardFor(item.cardId),fx=effects[card.effectType]||effects.HEAL,targeted=["FREEZE","INVULNERABLE","PAWN_DASH","TRAP","PORTAL","TELEPORT","MOSQUITO"].includes(card.effectType);
        const side=playerSide(),ownHp=side==="BLACK"||side==="BOTH"&&game.currentTurn==="BLACK"?game.blackHp:game.whiteHp,healBlocked=card.effectType==="HEAL"&&ownHp>=100;
        const button=node("button","tactical-card"+(state.pendingCard?.id===card.id?" is-armed":""));
        button.type="button";button.dataset.cardId=card.id;button.style.setProperty("--card-accent",fx[3]);
        button.append(cardArt(card.effectType));
        const info=node("span","tactical-info");info.append(node("strong","",cardNames[card.effectType]||card.name),node("small","card-original",card.name),node("small","card-effect",healBlocked?"HP đã đầy · chưa thể dùng":effectText(card)));
        button.append(info,node("span","card-count","×"+item.quantity));
        button.disabled=healBlocked||state.busy||state.syncIssue||game.status!=="PLAYING"||(!["BOTH",game.currentTurn].includes(side));
        if(healBlocked)button.title="HP đã đầy";
        button.setAttribute("aria-pressed",String(state.pendingCard?.id===card.id));
        button.addEventListener("click",()=>onUseCard(card,targeted));root.append(button);
      });
    }
  }
  renderMatchChrome();
}
function setHealth(side, value) { $(`${side}-hp`).style.width = `${Math.max(0, Math.min(100, value))}%`; $(`${side}-hp-label`).textContent = `${value} / 100 HP`; }
export function renderInventory(onDeck) {
  const root=$("card-inventory"),slots=$("deck-slots"),equipped=state.inventory.filter(item=>item.deckSlot).sort((a,b)=>a.deckSlot-b.deckSlot);
  $("deck-count").textContent=`${equipped.length} / 3`;
  slots.replaceChildren();
  for(let slot=1;slot<=3;slot++){
    const item=equipped.find(entry=>entry.deckSlot===slot),card=item&&cardFor(item.cardId),tile=node("article",`deck-slot${card?" is-filled":""}`);
    tile.append(node("span","deck-slot-number",String(slot)));
    if(card){
      tile.append(cardArt(card.effectType));
      const copy=node("div","deck-slot-copy");copy.append(node("strong","",cardNames[card.effectType]||card.name),node("small","",`Còn ${item.quantity} thẻ`));tile.append(copy);
      const remove=node("button","text-link","Gỡ");remove.type="button";remove.setAttribute("aria-label",`Gỡ ${card.name} khỏi Deck`);remove.addEventListener("click",()=>onDeck(card.id));tile.append(remove);
    }else tile.append(node("span","deck-empty","Ô trống"));
    slots.append(tile);
  }
  root.replaceChildren();
  if(!state.inventory.length){root.textContent="Chưa sở hữu thẻ. Hãy mua từ cửa hàng.";return;}
  state.inventory.forEach(item=>{
    const card=cardFor(item.cardId);if(!card)return;
    const tile=cardTile(card,`Số lượng: ${item.quantity}`),button=node("button",item.deckSlot?"button secondary compact":"button primary compact",item.deckSlot?`Đang ở ô ${item.deckSlot}`:"Trang bị");
    tile.dataset.cardId=card.id;
    button.type="button";button.disabled=!item.deckSlot&&equipped.length>=3;button.addEventListener("click",()=>onDeck(card.id));tile.classList.toggle("is-equipped",!!item.deckSlot);tile.append(button);root.append(tile);
  });
}
export function renderShop(onBuy) {
  const root=$("shop-items");root.replaceChildren();root.className="shop-groups";
  if(!state.shop.length){root.className="list-state";root.textContent="Cửa hàng chưa có thẻ.";return;}
  const groups=[
    ["Bổ sung Deck khởi đầu","Ba hiệu ứng dễ dùng để làm quen trận đấu.",["HEAL","DOUBLE_DAMAGE","FREEZE"]],
    ["Phòng thủ & hồi sinh","Giữ quân chủ lực và sửa sai sau giao tranh.",["INVULNERABLE","REVIVE","HIDE_KING"]],
    ["Điều khiển bàn cờ","Dịch chuyển, đặt bẫy và thay đổi luật di chuyển.",["PAWN_DASH","TRAP","REWIND","PAWN_BACKWARD","PORTAL","TELEPORT","MOSQUITO"]],
    ["Mạo hiểm","Hiệu ứng mạnh đi kèm đánh đổi rõ ràng.",["EXTRA_QUEEN","SURRENDER"]]
  ];
  groups.forEach(([title,description,effects],index)=>{
    const items=state.shop.filter(item=>item.active&&effects.includes(cardFor(item.cardId)?.effectType));if(!items.length)return;
    const details=node("details","shop-group");details.open=index===0;
    const summary=node("summary","shop-group-summary"),copy=node("span","");copy.append(node("strong","",title),node("small","",description));summary.append(copy,node("b","",String(items.length)));
    const grid=node("div","card-grid shop-group-grid");
    items.forEach(item=>{
      const card=cardFor(item.cardId)||{name:item.name},owned=state.inventory.find(entry=>entry.cardId===item.cardId),tile=cardTile(card,effectText(card));
      if(owned)tile.append(node("small","owned-count",`Đang có ${owned.quantity}`));
      const missing=Math.max(0,item.price-(state.player?.gold||0)),button=node("button","button primary compact",missing?`Thiếu ${missing} Gold`:`Mua · ${item.price} Gold`);
      button.disabled=missing>0;button.addEventListener("click",()=>onBuy(item.id,button));tile.append(button);grid.append(tile);
    });
    details.append(summary,grid);root.append(details);
  });
}
function cardTile(card, subtitle) { const tile = node("article", "card-tile"); if(card.id)tile.dataset.cardId=card.id; if(card.effectType)tile.append(cardArt(card.effectType)); tile.append(node("h3", "", cardNames[card.effectType]||card.name), node("p", "", subtitle)); return tile; }
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
export function renderWaiting(onJoin) { const root = $("waiting-games"); root.replaceChildren(); $("waiting-panel").hidden=!state.waitingGames.length; if (!state.waitingGames.length) return; state.waitingGames.forEach((game) => { const row = node("div", "waiting-row"),text=node("div","waiting-room-copy"),title=node("strong","",`#${game.roomCode} · ${game.whiteUsername}`),meta=node("small","",`${game.whiteRating} ELO · còn `),expiry=node("span","room-expiry","05:00"),own=game.whitePlayerId===state.player?.id;expiry.dataset.expiresAt=game.roomExpiresAt;meta.append(expiry);text.append(title,meta);const join=node("button",own?"button ghost compact":"button secondary compact",own?"Mở phòng":"Tham gia");join.addEventListener("click",()=>onJoin(game.id,join));row.append(text,join);root.append(row); }); }
