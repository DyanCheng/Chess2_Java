const { test, expect } = require("@playwright/test");
async function guest(page) {
  await page.goto("/");
  await page.locator("#guest-login").click();
  await expect(page.locator("#app-view")).toBeVisible();
}
async function game(page,type="LOCAL") {
  if(type==="PVP")await page.locator("#quick-match").click();
  else await page.locator('[data-create-game="'+type+'"]').last().click();
  if(type==="PVP"){await expect(page.locator("#room-invite")).toBeVisible();return;}
  await expect(page.locator("#board")).toBeVisible();
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
}
const square=(page,key)=>page.locator('#board .square[data-square="'+key+'"]');
const piece=(page,key)=>page.locator('#board .board-piece[data-square="'+key+'"]');
test("first-time guide is optional and the starter deck is ready",async({page})=>{
  await guest(page);
  const guide=page.locator("#starter-guide");
  await expect(guide).toBeVisible();
  await expect(guide).toContainText("Hồi phục, Cường kích và Đóng băng");
  await page.locator("#starter-skip").click();
  await expect(guide).toBeHidden();
  await page.reload();
  await expect(guide).toBeHidden();
  await page.locator('[data-route="collection"]').click();
  await expect(page.locator("#deck-count")).toHaveText("3 / 3");
});
async function drag(page,from,to,{touch=false,cancel=false}={}) {
  await page.locator("#board").scrollIntoViewIfNeeded();
  const a=await square(page,from).boundingBox(), b=await square(page,to).boundingBox();
  const start={x:a.x+a.width/2,y:a.y+a.height/2}, end={x:b.x+b.width/2,y:b.y+b.height/2};
  async function tracksPointer() {
    await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
    const ghost=await page.locator(".pointer-piece").boundingBox();
    expect(Math.abs(ghost.x+ghost.width/2-end.x)).toBeLessThan(2);
    expect(Math.abs(ghost.y+ghost.height/2-end.y)).toBeLessThan(2);
  }
  if(touch) {
    const cdp=await page.context().newCDPSession(page);
    await cdp.send("Input.dispatchTouchEvent",{type:"touchStart",touchPoints:[start]});
    await cdp.send("Input.dispatchTouchEvent",{type:"touchMove",touchPoints:[end]});
    await expect(page.locator(".pointer-piece")).toHaveCount(1);
    await tracksPointer();
    await cdp.send("Input.dispatchTouchEvent",{type:cancel?"touchCancel":"touchEnd",touchPoints:[]});
    await cdp.detach();
  } else {
    await page.mouse.move(start.x,start.y);await page.mouse.down();
    await page.mouse.move(end.x,end.y,{steps:8});
    await expect(page.locator(".pointer-piece")).toHaveCount(1);
    await tracksPointer();
    if(cancel)await page.keyboard.press("Escape");
    await page.mouse.up();
  }
}
test("persistent square board, drag, click, cancellation and flip",async({page,isMobile},info)=>{
  const errors=[];page.on("pageerror",e=>errors.push(e.message));
  await guest(page);
  await page.screenshot({path:info.outputPath("lobby.png"),fullPage:true,animations:"disabled"});
  await game(page);
  const board=page.locator("#board"),box=await board.boundingBox(),cell=await square(page,"a1").boundingBox();
  expect(Math.abs(box.width-box.height)).toBeLessThan(1);
  expect(Math.abs(cell.width*8-box.width)).toBeLessThan(1);
  await expect(board.locator(".square")).toHaveCount(64);
  await expect(piece(page,"a2")).toHaveJSProperty("draggable",false);
  const handle=await square(page,"a2").elementHandle();
  await drag(page,"a2","a4",{touch:isMobile});
  await expect(square(page,"a4")).toHaveAttribute("aria-label","a4: wP");
  await expect(board).toHaveAttribute("aria-busy","false");
  expect(await handle.evaluate(el=>el.isConnected)).toBe(true);
  await drag(page,"a7","a5",{touch:isMobile,cancel:true});
  await expect(square(page,"a7")).toHaveAttribute("aria-label","a7: bP");
  await expect(page.locator(".pointer-piece")).toHaveCount(0);
  await square(page,"e7").click();await square(page,"e5").click();
  await expect(square(page,"e5")).toHaveAttribute("aria-label","e5: bP");
  await expect(board).toHaveAttribute("aria-busy","false");
  await drag(page,"b2","b5",{touch:isMobile});
  await expect(square(page,"b2")).toHaveAttribute("aria-label","b2: wP");
  await page.locator("#flip-board").click();
  expect((await square(page,"h8").boundingBox()).y).toBeGreaterThan((await square(page,"h1").boundingBox()).y);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:info.outputPath("board.png"),fullPage:true,animations:"disabled"});
  expect(errors).toEqual([]);
});
test("optimistic move under latency, duplicate protection and rollback",async({page})=>{
  await guest(page);await game(page);
  let calls=0,release;
  const gate=new Promise(resolve=>release=resolve);
  await page.route("**/api/games/*/moves",async route=>{
    calls++;await gate;await route.fulfill({status:409,contentType:"application/json",body:JSON.stringify({message:"Test rejection"})});
  });
  await drag(page,"e2","e4");
  await expect(piece(page,"e4")).toHaveCount(1);
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","true");
  await square(page,"d2").click();await square(page,"d4").click();
  expect(calls).toBe(1);release();
  await expect(square(page,"e2")).toHaveAttribute("aria-label","e2: wP");
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await page.unroute("**/api/games/*/moves");
  await drag(page,"e2","e4");
  await expect(square(page,"e4")).toHaveAttribute("aria-label","e4: wP");
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
});
test("bot, profile history, resume and result persist after reload",async({page},info)=>{
  await guest(page);
  const name=await page.locator("#header-name").textContent();
  await page.locator("#pve-difficulty").selectOption("HARD");await game(page,"PVE");
  await drag(page,"e2","e4");
  await expect(page.locator("#turn-indicator")).toHaveText("Đến lượt bạn");
  await expect(page.locator("#move-journal")).toContainText("Bot:");
  await page.reload();
  await expect(square(page,"e4")).toHaveAttribute("aria-label","e4: wP");
  await expect(page.locator("#game-title")).toContainText("Đấu bot");
  await page.screenshot({path:info.outputPath("move-log.png"),fullPage:true,animations:"disabled"});
  await page.locator("#profile-button").click();
  await page.locator("#refresh-profile").click();
  await expect(page.locator("#profile-name")).toHaveText(name);
  await expect(page.locator("#match-history .history-row")).toHaveCount(1);
  await page.getByRole("button",{name:"Tiếp tục",exact:true}).click();
  await expect(square(page,"e4")).toHaveAttribute("aria-label","e4: wP");
  await expect(page.locator("#move-journal")).toContainText("Bot:");
  await expect(page.locator("#move-journal .move-time")).toHaveCount(2);
  page.once("dialog",dialog=>dialog.accept());await page.locator("#resign-game").click();
  await expect(page.locator("#match-result")).toBeVisible();
  await expect(page.locator("#result-title")).toHaveText("Bạn đã thua");
  await expect(page.locator("#result-copy")).toContainText("Bạn đã đầu hàng.");
  await expect(page.locator("#result-copy")).toContainText("ELO -16");
  await page.screenshot({path:info.outputPath("match-result.png"),fullPage:true,animations:"disabled"});
  await page.locator("#profile-button").click();await page.locator("#refresh-profile").click();
  await expect(page.locator("#profile-stats")).toContainText("1");
  await expect(page.locator("#match-history")).toContainText("Thất bại");
  await page.reload();
  await expect(page.locator("#profile-name")).toHaveText(name);
  await expect(page.locator("#match-history")).toContainText("Thất bại");
  await page.screenshot({path:info.outputPath("profile.png"),fullPage:true,animations:"disabled"});
});
test("persistent journal exposes card, effect, capture and more than forty entries",async({page})=>{
  await guest(page);
  const playerId=await page.evaluate(async()=>(await import("/assets/js/state.js")).state.player.id);
  const occurredAt=new Date().toISOString();
  const logs=Array.from({length:42},(_,index)=>({sequence:index+1,actorUserId:playerId,actorSide:index%2?"BLACK":"WHITE",eventType:"MOVE",from:"e2",to:"e4",capturedPiece:null,effectCode:null,occurredAt}));
  logs[39]={...logs[39],eventType:"CARD",from:null,to:"e7",effectCode:"FREEZE"};
  logs[40]={...logs[40],actorUserId:null,eventType:"EFFECT",from:null,to:"d4",capturedPiece:"bQ",effectCode:"TRAP"};
  logs[41]={...logs[41],eventType:"RESIGN",from:null,to:null,effectCode:"RESIGN"};
  await page.route("**/api/games/*/logs",route=>route.fulfill({status:200,contentType:"application/json",body:JSON.stringify(logs)}));
  await game(page);
  await expect(page.locator(".move-journal summary")).toHaveText("Nhật ký ván đấu");
  await expect(page.locator("#move-journal .move-entry")).toHaveCount(42);
  await expect(page.locator("#move-journal")).toContainText("Bạn dùng Đóng băng");
  await expect(page.locator("#move-journal")).toContainText("Hiệu ứng Bẫy tại d4 · bắt Hậu Đen");
});
test("cards purchase, target cancellation, freeze and puzzle board",async({page},info)=>{
  await guest(page);
  await page.locator('[data-route="collection"]').click();
  await expect(page.locator("#shop-items .card-tile").filter({hasText:"Tốt xung phong"}).locator(".symbolic-card-art")).toHaveCount(1);
  await expect(page.locator("#card-inventory")).toContainText("Đóng băng");
  await expect(page.locator("#deck-count")).toHaveText("3 / 3");
  await expect(page.locator("#deck-slots")).toContainText("Đóng băng");
  const owned=page.locator("#card-inventory .card-tile").filter({has:page.getByRole("heading",{name:"Đóng băng",exact:true})});
  await owned.getByRole("button",{name:/Đang ở ô/}).click();
  await expect(page.locator("#deck-count")).toHaveText("2 / 3");
  await expect(owned.getByRole("button",{name:"Trang bị",exact:true})).toBeFocused();
  await owned.getByRole("button",{name:"Trang bị",exact:true}).click();
  await expect(page.locator("#deck-count")).toHaveText("3 / 3");
  await expect(owned.getByRole("button",{name:/Đang ở ô/})).toBeFocused();
  await page.screenshot({path:info.outputPath("inventory-deck.png"),fullPage:true,animations:"disabled"});
  await page.locator('[data-route="home"]').click();await game(page);
  const card=page.locator(".tactical-card").filter({hasText:"Đóng băng"});
  await card.click();await expect(page.locator("#cancel-card")).toBeVisible();
  await page.keyboard.press("Escape");await expect(page.locator("#cancel-card")).toBeHidden();
  await card.click();await square(page,"e7").click();
  await expect(square(page,"e7")).toHaveClass(/is-frozen/);
  await expect(page.locator("#move-journal")).toContainText("Bạn dùng Đóng băng");
  await expect(page.locator("#move-journal .move-time")).toHaveCount(1);
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await drag(page,"e2","e4");
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await expect(page.locator("#move-journal .move-time")).toHaveCount(2);
  await square(page,"e7").click();await square(page,"e5").click();
  await expect(square(page,"e7")).toHaveAttribute("aria-label","e7: bP");
  await page.locator("#leave-game").click();
  await page.locator('[data-route="puzzles"]').click();
  const puzzle=page.locator(".puzzle-card").first();
  await expect(puzzle.locator(".mini-square")).toHaveCount(64);
  await puzzle.getByRole("button",{name:"e2: wR",exact:true}).click();
  await puzzle.getByRole("button",{name:"e8",exact:true}).click();
  await puzzle.getByRole("button",{name:"Giải",exact:true}).click();
  await expect(page.locator("#toast")).toContainText("Chính xác");
  await page.screenshot({path:info.outputPath("puzzle.png"),fullPage:true,animations:"disabled"});
});
test("healing cannot be wasted at full HP",async({page})=>{
  await guest(page);
  await game(page,"LOCAL");
  const heal=page.locator('#game-cards .tactical-card').filter({hasText:"Hồi phục"});
  await expect(heal).toBeDisabled();await expect(heal).toContainText("HP đã đầy · chưa thể dùng");
  await square(page,"e2").click();await square(page,"e4").click();
  await expect(square(page,"e4")).toHaveAttribute("aria-label","e4: wP");await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await square(page,"d7").click();await square(page,"d5").click();
  await expect(square(page,"d5")).toHaveAttribute("aria-label","d5: bP");await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await square(page,"e4").click();await square(page,"d5").click();
  await expect(square(page,"d5")).toHaveAttribute("aria-label","d5: wP");await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await expect(page.locator("#black-hp-label")).toHaveText("95 / 100 HP");
  await expect(heal).toBeEnabled();await heal.click();
  await expect(page.locator("#black-hp-label")).toHaveText("100 / 100 HP");
  await expect(page.locator('#game-cards .tactical-card').filter({hasText:"Hồi phục"})).toHaveCount(0);
});
test("numbered PvP room shows both players and forbids wrong-turn card",async({page,browser},info)=>{
  const ownerSockets=[];page.on("websocket",socket=>ownerSockets.push(socket));
  await guest(page);
  await game(page,"PVP");
  await expect.poll(()=>ownerSockets.some(socket=>socket.url().endsWith("/ws/games"))).toBe(true);
  const rejectedSubscription=await page.evaluate(async()=>{
    const {state}=await import("/assets/js/state.js");
    return new Promise(resolve=>{
      const scheme=location.protocol==="https:"?"wss:":"ws:",socket=new WebSocket(`${scheme}//${location.host}/ws/games`,["chess2",`jwt.${state.session.accessToken}`]);
      const timer=setTimeout(()=>{socket.close();resolve(0);},3000);
      socket.addEventListener("open",()=>socket.send(JSON.stringify({type:"subscribe",gameId:crypto.randomUUID()})));
      socket.addEventListener("close",event=>{clearTimeout(timer);resolve(event.code);});
    });
  });
  expect(rejectedSubscription).toBe(1008);
  const code=(await page.locator("#room-code").textContent()).trim();
  expect(code).toMatch(/^\d{6}$/);
  await expect(page.locator("#room-message")).toHaveText("Phòng đang chờ đối thủ");
  await expect(page.locator("#room-owner")).toContainText("1000 ELO");
  await expect(page.locator("#room-countdown")).toHaveText(/^0[0-5]:\d{2}$/);
  await page.screenshot({path:info.outputPath("pvp-room-waiting.png"),animations:"disabled"});
  const owner=await page.evaluate(async()=>{const {state}=await import("/assets/js/state.js");return state.player.username;});
  const second=await browser.newContext(info.project.use);
  const friend=await second.newPage();
  const friendSockets=[];friend.on("websocket",socket=>friendSockets.push(socket));
  try {
    await friend.goto("/");await friend.locator('[data-auth-mode="register"]').click();
    const friendAccount="pvp"+Date.now().toString(36);
    await friend.locator("#username").fill(friendAccount);await friend.locator("#email").fill(friendAccount+"@example.com");await friend.locator("#password").fill("QaPassword123!");
    await friend.locator("#auth-submit").click();await expect(friend.locator("#app-view")).toBeVisible();
    await friend.locator("#join-code").fill(code);await friend.locator("#join-form").getByRole("button",{name:"Vào phòng"}).click();
    await expect(friend.locator("#board")).toBeVisible();
    await expect.poll(()=>friendSockets.some(socket=>socket.url().endsWith("/ws/games"))).toBe(true);
    await expect(page.locator("#board")).toBeVisible({timeout:8000});
    await expect(friend.locator("#game-status")).toHaveText("Đối thủ đang suy nghĩ…");
    await expect(page.locator("#game-status")).toHaveText("Đến lượt bạn",{timeout:8000});
    const friendName=await friend.evaluate(async()=>{const {state}=await import("/assets/js/state.js");return state.player.username;});
    await expect(page.locator("#black-name")).toContainText(friendName);
    await expect(page.locator("#black-rating")).toHaveText("1000 ELO");
    await expect(friend.locator("#white-name")).toContainText(owner);
    await expect(friend.locator("#white-rating")).toHaveText("1000 ELO");
    await expect(page.locator("#room-invite")).toBeHidden();
    await page.screenshot({path:info.outputPath("pvp-room-joined.png"),animations:"disabled"});
    await drag(page,"e2","e4");
    await expect(friend.locator("#game-status")).toHaveText("Đến lượt bạn",{timeout:8000});
    await expect(square(friend,"e4")).toHaveAttribute("aria-label","e4: wP");
    const a=await square(friend,"a1").boundingBox(),b=await square(friend,"a8").boundingBox();
    expect(a.y).toBeLessThan(b.y);
    await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
    await page.route("**/api/games/*/logs",async route=>{await new Promise(resolve=>setTimeout(resolve,3500));await route.continue();},{times:1});
    const opponentMoveAt=Date.now();
    await drag(friend,"e7","e5");
    await expect(square(page,"e5")).toHaveAttribute("aria-label","e5: bP",{timeout:1500});
    expect(Date.now()-opponentMoveAt).toBeLessThan(1500);
    await expect(page.locator("#game-status")).toHaveText("Đến lượt bạn");
    const forbidden=await friend.evaluate(async()=>{
      const {state}=await import("/assets/js/state.js");
      const cards=await fetch("/api/cards",{headers:{Authorization:"Bearer "+state.session.accessToken}}).then(r=>r.json());
      return (await fetch("/api/games/"+state.game.id+"/cards",{method:"POST",headers:{"Content-Type":"application/json",Authorization:"Bearer "+state.session.accessToken},body:JSON.stringify({cardId:cards[0].id,target:"OPPONENT"})})).status;
    });
    expect(forbidden).toBe(403);
    friend.once("dialog",d=>d.accept());await friend.locator("#resign-game").click();
    await expect(page.locator("#result-title")).toHaveText("Chiến thắng của bạn!",{timeout:8000});
    await expect(page.locator("#result-copy")).toContainText("Đối thủ đã đầu hàng.");
    await expect(page.locator("#result-copy")).toContainText("ELO +24");
    await page.screenshot({path:info.outputPath("pvp-result.png"),fullPage:true,animations:"disabled"});
  }finally{await second.close();}
});
test("protection follows the moving piece and a lost response recovers",async({page})=>{
  await guest(page);
  await page.locator('[data-route="collection"]').click();
  await page.getByText("Phòng thủ & hồi sinh",{exact:true}).click();
  const tile=page.locator("#shop-items .card-tile").filter({has:page.getByRole("heading",{name:"Hộ vệ",exact:true})});
  await tile.getByRole("button").click();
  await expect(page.locator("#card-inventory")).toContainText("Hộ vệ");
  const heal=page.locator("#card-inventory .card-tile").filter({has:page.getByRole("heading",{name:"Hồi phục",exact:true})});
  await heal.getByRole("button",{name:/Đang ở ô/}).click();
  const shield=page.locator("#card-inventory .card-tile").filter({has:page.getByRole("heading",{name:"Hộ vệ",exact:true})});
  await shield.getByRole("button",{name:"Trang bị",exact:true}).click();
  await page.locator('[data-route="home"]').click();await game(page,"PVE");
  await page.locator(".tactical-card").filter({hasText:"Hộ vệ"}).click();
  await square(page,"e2").click();
  await expect(square(page,"e2")).toHaveClass(/is-protected/);
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await page.route("**/api/games/*/moves",async route=>{
    await route.fetch();await route.abort("failed");
  },{times:1});
  await drag(page,"e2","e4");
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false",{timeout:8000});
  await expect(square(page,"e4")).toHaveClass(/is-protected/);
  await expect(square(page,"e2")).not.toHaveClass(/is-protected/);
  await expect(square(page,"e4")).toHaveAttribute("aria-label","e4: wP");
});

test("player can choose black and bot opens the game",async({page})=>{
  await guest(page);
  await page.locator("#pve-side").selectOption("BLACK");
  const created=page.waitForResponse(response=>response.url().endsWith("/api/games")&&response.request().method()==="POST");
  await page.locator('[data-create-game="PVE"]').last().click();
  const payload=await (await created).json();
  expect(payload.playerSide).toBe("BLACK");
  expect(payload.currentTurn).toBe("BLACK");
  await expect(page.locator("#black-name")).toHaveText("Bạn");
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
});

test("configured admin can manage access and inspect activity logs",async({page},testInfo)=>{
  const playerName="audit"+Date.now().toString(36);
  const registration=await page.request.post("/api/auth/register",{data:{username:playerName,email:playerName+"@example.com",password:"AuditPlayer123!"}});
  expect(registration.ok()).toBe(true);
  const registered=await registration.json();
  const game=await page.request.post("/api/games",{headers:{Authorization:"Bearer "+registered.accessToken},data:{type:"LOCAL"}});
  expect(game.ok()).toBe(true);
  await page.goto("/admin/");
  await page.locator("#username").fill("e2e_admin");
  await page.locator("#password").fill("E2eAdminPassword123!");
  await page.locator("#login-submit").click();
  await expect(page.locator("#portal-view")).toBeVisible();
  await expect(page.locator("#metric-strip .metric")).toHaveCount(6);
  await page.getByRole("button",{name:"Tài khoản",exact:true}).click();
  const row=page.locator("#user-rows tr").filter({hasText:"e2e_admin"}).first();
  const manageAccount=row.getByRole("button",{name:"Quản lý"});
  await manageAccount.click();
  await expect(page.locator("#dialog-title")).toBeFocused();
  await expect(page.locator("#toggle-lock")).toBeDisabled();
  await page.locator("#dialog-gold").fill("777");
  const saveWallet=page.locator("#save-wallet");
  await expect(saveWallet).toBeVisible();
  await saveWallet.scrollIntoViewIfNeeded();
  expect(await saveWallet.evaluate(element=>{const rect=element.getBoundingClientRect(),hit=document.elementFromPoint(rect.left+rect.width/2,rect.top+rect.height/2);return hit===element||element.contains(hit);})).toBe(true);
  await saveWallet.dispatchEvent("click");
  await expect(row).toContainText("777 G");
  await expect(manageAccount).toBeFocused();

  await page.locator("#user-search").fill(playerName);
  const playerRow=page.locator("#user-rows tr").filter({hasText:playerName});
  await playerRow.getByRole("button",{name:"Quản lý"}).click();
  await page.locator("#dialog-lock-reason").fill("Kiểm thử hành vi");
  const lockButton=page.locator("#toggle-lock");
  await lockButton.scrollIntoViewIfNeeded();
  expect(await lockButton.evaluate(element=>{const rect=element.getBoundingClientRect(),hit=document.elementFromPoint(rect.left+rect.width/2,rect.top+rect.height/2);return hit===element||element.contains(hit);})).toBe(true);
  await lockButton.dispatchEvent("click");
  await expect(playerRow).toContainText("Đã khóa");
  const lockedLogin=await page.request.post("/api/auth/login",{data:{username:playerName,password:"AuditPlayer123!"}});
  expect(lockedLogin.status()).toBe(423);

  await page.getByRole("button",{name:"Nhật ký",exact:true}).click();
  await page.locator("#log-search").fill(playerName);
  await expect(page.locator("#log-rows")).toContainText("Tạo ván đấu");
  await expect(page.locator("#log-rows")).toContainText("POST /api/games");

  await page.getByRole("button",{name:"Tài khoản",exact:true}).click();
  await playerRow.getByRole("button",{name:"Quản lý"}).click();
  await page.locator("#toggle-lock").dispatchEvent("click");
  await expect(playerRow).toContainText("Hoạt động");

  await page.route("**/actuator/health",route=>route.abort("failed"));
  await page.locator("#refresh-data").click();
  await expect(page.locator("#health-badge")).toHaveText("Dữ liệu trạng thái đã cũ");
  await page.unroute("**/actuator/health");
  await page.locator("#refresh-data").click();
  await expect(page.locator("#health-badge")).toHaveText("Hệ thống ổn định");
  await page.getByRole("button",{name:"Nhật ký",exact:true}).click();
  await page.screenshot({path:testInfo.outputPath("admin-logs.png"),fullPage:true,animations:"disabled"});
});

test("register, logout, login and reduced motion",async({page})=>{
  await page.emulateMedia({reducedMotion:"reduce"});await page.goto("/");
  await page.locator('[data-auth-mode="register"]').click();
  const username="qa"+Date.now().toString(36);
  await page.locator("#username").fill(username);await page.locator("#email").fill(username+"@example.com");await page.locator("#password").fill("QaPassword123!");
  await page.locator("#auth-submit").click();await expect(page.locator("#app-view")).toBeVisible();
  await page.locator("#sign-out").click();
  await page.locator('[data-auth-mode="login"]').click();await page.locator("#username").fill(username);await page.locator("#password").fill("QaPassword123!");
  await page.locator("#auth-submit").click();await expect(page.locator("#header-name")).toHaveText(username);
  expect(page.url()).not.toContain("password");
  await expect(page.locator(".hero-knight")).toHaveCSS("animation-name","none");
});
