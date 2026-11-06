const { test, expect } = require("@playwright/test");
async function guest(page) {
  await page.goto("/");
  await page.locator("#guest-login").click();
  await expect(page.locator("#app-view")).toBeVisible();
}
async function game(page,type="LOCAL") {
  await page.locator('[data-create-game="'+type+'"]').last().click();
  await expect(page.locator("#board")).toBeVisible();
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
}
const square=(page,key)=>page.locator('#board .square[data-square="'+key+'"]');
const piece=(page,key)=>page.locator('#board .board-piece[data-square="'+key+'"]');
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
  await page.locator("#profile-button").click();
  await page.locator("#refresh-profile").click();
  await expect(page.locator("#profile-name")).toHaveText(name);
  await expect(page.locator("#match-history .history-row")).toHaveCount(1);
  await page.getByRole("button",{name:"Tiếp tục",exact:true}).click();
  await expect(square(page,"e4")).toHaveAttribute("aria-label","e4: wP");
  page.once("dialog",dialog=>dialog.accept());await page.locator("#resign-game").click();
  await expect(page.locator("#match-result")).toBeVisible();
  await page.locator("#profile-button").click();await page.locator("#refresh-profile").click();
  await expect(page.locator("#profile-stats")).toContainText("1");
  await expect(page.locator("#match-history")).toContainText("Thất bại");
  await page.reload();
  await expect(page.locator("#profile-name")).toHaveText(name);
  await expect(page.locator("#match-history")).toContainText("Thất bại");
  await page.screenshot({path:info.outputPath("profile.png"),fullPage:true,animations:"disabled"});
});
test("cards purchase, target cancellation, freeze and puzzle board",async({page},info)=>{
  await guest(page);
  await page.locator('[data-route="collection"]').click();
  const tile=page.locator("#shop-items .card-tile").filter({has:page.getByRole("heading",{name:"Freeze",exact:true})});
  await tile.getByRole("button").click();
  await expect(page.locator("#card-inventory")).toContainText("Freeze");
  await page.locator('[data-route="home"]').click();await game(page);
  const card=page.locator(".tactical-card").filter({hasText:"Freeze"});
  await card.click();await expect(page.locator("#cancel-card")).toBeVisible();
  await page.keyboard.press("Escape");await expect(page.locator("#cancel-card")).toBeHidden();
  await card.click();await square(page,"e7").click();
  await expect(square(page,"e7")).toHaveClass(/is-frozen/);
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await drag(page,"e2","e4");
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await square(page,"e7").click();await square(page,"e5").click();
  await expect(square(page,"e7")).toHaveAttribute("aria-label","e7: bP");
  await page.locator('[data-route="puzzles"]').click();
  const puzzle=page.locator(".puzzle-card").first();
  await expect(puzzle.locator(".mini-square")).toHaveCount(64);
  await puzzle.getByRole("button",{name:"e2: wR",exact:true}).click();
  await puzzle.getByRole("button",{name:"e8",exact:true}).click();
  await puzzle.getByRole("button",{name:"Giải",exact:true}).click();
  await expect(page.locator("#toast")).toContainText("Chính xác");
  await page.screenshot({path:info.outputPath("puzzle.png"),fullPage:true,animations:"disabled"});
});
test("friend invite synchronizes two players and forbids wrong-turn card",async({page,browser},info)=>{
  await guest(page);
  await game(page,"PVP");
  const invite=await page.locator("#invite-url").inputValue();
  const second=await browser.newContext(info.project.use);
  const friend=await second.newPage();
  try {
    await friend.goto(invite);await friend.locator("#guest-login").click();
    await expect(friend.locator("#game-status")).toHaveText("Đối thủ đang suy nghĩ…");
    await expect(page.locator("#game-status")).toHaveText("Đến lượt bạn",{timeout:8000});
    await drag(page,"e2","e4");
    await expect(friend.locator("#game-status")).toHaveText("Đến lượt bạn",{timeout:8000});
    await expect(square(friend,"e4")).toHaveAttribute("aria-label","e4: wP");
    const a=await square(friend,"a1").boundingBox(),b=await square(friend,"a8").boundingBox();
    expect(a.y).toBeLessThan(b.y);
    await drag(friend,"e7","e5");
    await expect(square(page,"e5")).toHaveAttribute("aria-label","e5: bP",{timeout:8000});
    await expect(page.locator("#game-status")).toHaveText("Đến lượt bạn");
    const forbidden=await friend.evaluate(async()=>{
      const {state}=await import("/assets/js/state.js");
      const cards=await fetch("/api/cards").then(r=>r.json());
      return (await fetch("/api/games/"+state.game.id+"/cards",{method:"POST",headers:{"Content-Type":"application/json",Authorization:"Bearer "+state.session.accessToken},body:JSON.stringify({cardId:cards[0].id,target:"OPPONENT"})})).status;
    });
    expect(forbidden).toBe(403);
    friend.once("dialog",d=>d.accept());await friend.locator("#resign-game").click();
    await expect(page.locator("#result-title")).toHaveText("Chiến thắng của bạn!",{timeout:8000});
  }finally{await second.close();}
});
test("protection follows the moving piece and a lost response recovers",async({page})=>{
  await guest(page);
  await page.locator('[data-route="collection"]').click();
  const tile=page.locator("#shop-items .card-tile").filter({has:page.getByRole("heading",{name:"Arsenal",exact:true})});
  await tile.getByRole("button").click();
  await expect(page.locator("#card-inventory")).toContainText("Arsenal");
  await page.locator('[data-route="home"]').click();await game(page,"PVE");
  await page.locator(".tactical-card").filter({hasText:"Arsenal"}).click();
  await square(page,"e2").click();
  await expect(square(page,"e2")).toHaveClass(/is-protected/);
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false");
  await page.route("**/api/games/*/moves",async route=>{
    await route.fetch();await route.abort("failed");
  },{times:1});
  await drag(page,"e2","e4");
  await expect(page.locator("#game-status")).toContainText("Đang kết nối lại");
  await expect(page.locator("#board")).toHaveAttribute("aria-busy","false",{timeout:8000});
  await expect(square(page,"e4")).toHaveClass(/is-protected/);
  await expect(square(page,"e2")).not.toHaveClass(/is-protected/);
  await expect(square(page,"e4")).toHaveAttribute("aria-label","e4: wP");
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
