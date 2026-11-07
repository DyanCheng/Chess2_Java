import { state, playerSide } from './state.js';
import { pieceSvg, reducedMotion } from './board.js';

const $=id=>document.getElementById(id);
const el=(tag,cls,text)=>{ const n=document.createElement(tag); n.className=cls||''; if(text!==undefined)n.textContent=text; return n; };
export const effects={
  HEAL:['✚','Mạch sống','Hồi phục HP cho phe của bạn.','#85e2b3'],
  DOUBLE_DAMAGE:['ϟ','Sấm sét','Đánh thẳng vào HP của đối thủ.','#ff9d80'],
  FREEZE:['❄','Băng phong','Chọn quân đối thủ để khóa trong lượt kế tiếp.','#92d5ff'],
  INVULNERABLE:['◇','Hộ vệ','Chọn quân của bạn để bảo vệ khỏi bị bắt.','#f6d38b'],
  EXTRA_QUEEN:['♛','Triệu hồi','Đổi 80% HP hiện tại lấy một quân Hậu.','#bfa3ff'],
  SURRENDER:['⚑','Đình chiến','Đầu hàng và kết thúc ván đấu.','#efa4bb']
};
export function mountExperience() {
  const nav=document.querySelector('.main-nav');
  const profile=el('a','','Hồ sơ'); profile.href='#/profile'; profile.dataset.route='profile'; nav.append(profile);
  document.querySelector('.home-heading').insertAdjacentHTML('afterend',`<section class="lobby-hero"><div class="hero-copy"><p class="kicker">CHESS² / ARCANE CLUB</p><h2>Một bàn cờ.<br>Vạn cách <em>lật thế.</em></h2><p>Đọc vị đối thủ, tung thẻ đúng lúc.<br>Mỗi ván đấu là một câu chuyện mới.</p><div class="hero-actions"><button type="button" class="button primary" id="hero-play">♞ &nbsp; Thách đấu bot</button><button type="button" class="button secondary" data-create-game="PVP">Mời bạn chơi ↗</button></div><span class="hero-caption">CỜ ARCADE · THẺ CHIẾN THUẬT · 100 HP</span></div><div class="hero-art" aria-hidden="true"><div class="orbit orbit-one"></div><div class="orbit orbit-two"></div><div class="hero-checker"></div><div class="hero-knight">${pieceSvg('wN')}</div><div class="floating-card frost">❄<small>FREEZE</small></div><div class="floating-card thunder">ϟ<small>STRIKE</small></div><span class="art-spark spark-one">✧</span><span class="art-spark spark-two">✦</span></div></section>`);
  const icons=['♞','⚔','♜']; document.querySelectorAll('.mode-card').forEach((card,i)=>{card.dataset.mode=String(i); card.querySelector('.mode-icon').textContent=icons[i];});
  document.querySelector('[data-page="home"]').insertAdjacentHTML('beforeend',`<section class="panel invite-panel"><div><p class="kicker">CÙNG NHAU CHƠI HAY HƠN</p><h2>Bạn có lời mời?</h2><p class="muted">Dán liên kết hoặc mã phòng để ngồi vào quân Đen.</p></div><form id="join-form"><label class="field"><span>Liên kết hoặc mã phòng</span><input id="join-code" required placeholder="Dán link mời vào đây…" autocomplete="off"></label><button class="button primary" type="submit">Vào phòng</button></form></section>`);
  document.querySelector('[data-page="play"] .page-heading').insertAdjacentHTML('afterend',`<div id="room-invite" class="room-invite" hidden><div><strong id="room-message">Gửi lời mời cho bạn bè</strong><p>Phòng PvP công khai. Bạn bè có link có thể tham gia khi phòng còn trống.</p></div><input id="invite-url" readonly aria-label="Liên kết mời bạn"><button id="copy-invite" type="button" class="button primary">Sao chép link</button></div><div id="match-result" class="match-result" role="status" hidden><span class="result-mark">♛</span><div><p class="kicker">VÁN ĐẤU ĐÃ KHÉP LẠI</p><h2 id="result-title"></h2><p id="result-copy"></p></div><button id="play-again" class="button primary" type="button">Chơi ván mới</button><a class="button ghost" href="#/profile">Xem hồ sơ</a></div>`);
  const area=document.querySelector('.board-area');
  area.insertAdjacentHTML('beforeend',`<div class="board-toolbar"><span id="turn-indicator" role="status">Sẵn sàng</span><div><button id="flip-board" class="icon-button" type="button" aria-label="Xoay bàn cờ" title="Xoay bàn cờ">⇅</button><button id="sound-toggle" class="icon-button" type="button" aria-label="Bật âm thanh" aria-pressed="false" title="Âm thanh">♪</button></div></div><p class="board-tip">Kéo quân hoặc bấm hai ô để đi · Esc để hủy chọn thẻ</p>`);
  const panel=document.querySelector('.game-panel');
  panel.insertAdjacentHTML('beforeend',`<button id="cancel-card" type="button" class="text-link" hidden>Hủy chọn thẻ (Esc)</button><a class="button ghost shop-link" href="#/collection">✦ Ghé cửa hàng thẻ</a><section class="move-journal"><p class="kicker">DIỄN BIẾN TRONG PHIÊN</p><ol id="move-journal"><li>Hãy mở màn bằng nước đi của bạn.</li></ol></section><details class="arcade-rules"><summary>Luật cờ Arcane</summary><p>Thắng khi bắt Vua hoặc đưa HP đối thủ về 0. Tốt tự phong Hậu ở hàng cuối. Đây là biến thể arcade: chưa có chiếu bí, nhập thành và bắt tốt qua đường.</p><p>Thẻ chỉ dùng trong lượt của bạn. Thẻ được tiêu hao sau khi kích hoạt thành công.</p></details>`);
  $('app-view').insertAdjacentHTML('beforeend',`<section class="page" data-page="profile"><div class="page-heading"><div><p class="kicker">HÀNH TRÌNH CỦA BẠN</p><h1>Góc kỳ thủ</h1><p>Mỗi ván đấu để lại một dấu ấn.</p></div><button id="refresh-profile" type="button" class="button ghost">Làm mới hồ sơ</button></div><div class="profile-banner"><div id="profile-avatar" class="profile-avatar">♞</div><div><p class="kicker" id="profile-type"></p><h2 id="profile-name"></h2><p id="profile-email"></p></div><div class="profile-rating"><small>RATING</small><strong id="profile-rating"></strong><span id="profile-level"></span></div></div><div id="profile-stats" class="profile-stats"></div><section class="panel"><div class="section-title"><div><p class="kicker">20 VÁN GẦN NHẤT</p><h2>Lịch sử & ván đang chơi</h2></div></div><div id="match-history" class="history-list"></div></section></section>`);
  $('hero-play').addEventListener('click',()=>{document.querySelector('[data-create-game="PVE"]').scrollIntoView({behavior:reducedMotion()?'instant':'smooth',block:'center'});document.querySelector('[data-create-game="PVE"]').focus({preventScroll:true});});
  document.querySelector('.side-copy').textContent='Tung một lá bài. Đổi cả cục diện.';
}

export function renderProfile(onResume) {
  const p=state.player; if(!p)return;
  $('profile-name').textContent=p.username; $('profile-avatar').textContent=p.username[0].toUpperCase();
  $('profile-email').textContent=p.accountType==='GUEST'?'Hồ sơ khách trên thiết bị này':p.email;
  $('profile-type').textContent=p.accountType==='GUEST'?'KỲ THỦ KHÁCH':'THÀNH VIÊN ARCANE CLUB';
  $('profile-rating').textContent=p.rating; $('profile-level').textContent=`Cấp ${p.level} · ${p.experience} XP`;
  const total=p.wins+p.losses+p.draws, stats=$('profile-stats'); stats.replaceChildren();
  [['Ván đã đấu',total],['Chiến thắng',p.wins],['Tỷ lệ thắng',total?`${Math.round(p.wins/total*100)}%`:'—'],['Gold trong ví',p.gold]].forEach(([label,value])=>{const box=el('article','stat-tile');box.append(el('span','',label),el('strong','',value));stats.append(box);});
  const root=$('match-history');root.replaceChildren();
  if(!state.history.length){root.append(el('p','empty-state','Chưa có ván đấu. Thách đấu bot để bắt đầu hành trình của bạn.'));return;}
  state.history.forEach(game=>{
    const row=el('article','history-row'), side=game.whitePlayerId===p.id?'WHITE':'BLACK';
    const active=['PLAYING','WAITING'].includes(game.status);
    const outcome=active?(game.status==='WAITING'?'Đang chờ':'Đang chơi'):game.status==='ABANDONED'?'Đã hủy':game.type==='LOCAL'?(game.winnerColor?`${game.winnerColor==='WHITE'?'Trắng':'Đen'} thắng`:'Hòa'):game.winnerColor?(game.winnerColor===side?'Chiến thắng':'Thất bại'):'Hòa';
    row.append(el('span','history-symbol',game.type==='PVE'?'♞':'⚔'));
    const info=el('div','history-info'); info.append(el('strong','',game.type==='PVE'?`Đấu bot · ${difficultyName(game.difficulty)}`:game.type==='PVP'?'Đấu bạn bè':'Cùng thiết bị'),el('small','',`Phòng ${game.id.slice(0,8)} · ${game.whiteHp} / ${game.blackHp} HP`));
    row.append(info,el('span','result-badge',outcome));const button=el('button','button ghost compact',active?'Tiếp tục':'Xem bàn');button.type='button';button.addEventListener('click',()=>onResume(game.id));row.append(button);root.append(row);
  });
}
export const difficultyName=level=>({EASY:'Tập sự',MEDIUM:'Chiến thuật',HARD:'Thử thách'}[level]||level);
export function renderMatchChrome() {
  const g=state.game, side=playerSide();
  $('room-invite').hidden=!g||g.type!=='PVP'||g.status!=='WAITING';
  if(g){const url=new URL(location.origin);url.searchParams.set('room',g.id);$('invite-url').value=url.href;}
  const ended=g&&!['PLAYING','WAITING'].includes(g.status);$('match-result').hidden=!ended;
  if(ended){$('result-title').textContent=g.status==='ABANDONED'?'Phòng đã đóng':!g.winnerColor?'Ván đấu hòa':side==='BOTH'?`${g.winnerColor==='WHITE'?'Trắng':'Đen'} chiến thắng`:g.winnerColor===side?'Chiến thắng của bạn!':'Một ván đấu đáng nhớ';$('result-copy').textContent='Xem lại bàn cờ, hoặc bắt đầu một thử thách mới.';}
  const status=!g?'Chọn một chế độ để bắt đầu':state.busy?(g.type==='PVE'?'Bot đang suy nghĩ…':'Đang xác nhận…'):g.status==='WAITING'?'Chờ bạn bè tham gia':ended?'Ván đã kết thúc':side==='BOTH'?`Lượt ${g.currentTurn==='WHITE'?'Trắng':'Đen'}`:side===g.currentTurn?'Đến lượt bạn':'Đối thủ đang suy nghĩ…';
  $('turn-indicator').textContent=status;$('turn-indicator').classList.toggle('thinking',!!state.busy||!!g&&g.status==='PLAYING'&&side!==g.currentTurn&&side!=='BOTH');
  $('game-status').textContent=status;$('cancel-card').hidden=!state.pendingCard;
  if(state.syncIssue)$('game-status').textContent="Đang kết nối lại… Bàn cờ sẽ tự đồng bộ.";
  const title=!g?'Bàn cờ':g.type==='PVE'?`Đấu bot · ${difficultyName(g.difficulty)}`:g.type==='PVP'?'Bạn bè & đối thủ':'Cùng một bàn cờ';$('game-title').textContent=title;
  const journal=$('move-journal');journal.replaceChildren();
  (state.moveLog.length?state.moveLog:['Hãy mở màn bằng nước đi của bạn.']).slice(-24).forEach(text=>journal.append(el('li','',text)));
}
export function decorateCards() {
  document.querySelectorAll('.card-tile').forEach(tile=>{
    const card=state.cards.find(c=>c.name===tile.querySelector('h3')?.textContent),fx=effects[card?.effectType];if(!fx)return;
    tile.style.setProperty('--card-accent',fx[3]);tile.classList.add('illustrated-card');
    if(!tile.querySelector('.card-emblem'))tile.prepend(el('div','card-emblem',fx[0]));
  });
}
let audioContext;
export function playSound(kind='move') {
  if(localStorage.getItem('chess2.sound')!=='on')return;
  try {
    audioContext ||= new (window.AudioContext||window.webkitAudioContext)();audioContext.resume();
    const osc=audioContext.createOscillator(),gain=audioContext.createGain(),now=audioContext.currentTime;
    osc.type='sine';osc.frequency.setValueAtTime(kind==='card'?660:330,now);osc.frequency.exponentialRampToValueAtTime(kind==='card'?880:150,now+.11);
    gain.gain.setValueAtTime(.0001,now);gain.gain.exponentialRampToValueAtTime(.07,now+.01);gain.gain.exponentialRampToValueAtTime(.0001,now+.16);osc.connect(gain);gain.connect(audioContext.destination);osc.start();osc.stop(now+.17);
  }catch { /* Audio is optional. */ }
}
export function cardBurst(card) {
  if(reducedMotion())return;
  const fx=effects[card.effectType]||effects.HEAL,burst=el('div','card-burst');burst.setAttribute('aria-hidden','true');burst.style.setProperty('--card-accent',fx[3]);burst.append(el('span','',fx[0]),el('strong','',card.name));document.body.append(burst);
  burst.animate([{opacity:0,transform:'translate(-50%, 20px) scale(.8)'},{opacity:1,transform:'translate(-50%, 0) scale(1)',offset:.2},{opacity:1,offset:.72},{opacity:0,transform:'translate(-50%, -35px) scale(.95)'}],{duration:1000}).onfinish=()=>burst.remove();
}
