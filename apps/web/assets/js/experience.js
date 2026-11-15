import { state, playerSide } from './state.js';
import { pieceSvg, reducedMotion } from './board.js';
import { icon, cardArt, cardNames } from './visuals.js';

const $=id=>document.getElementById(id);
const el=(tag,cls,text)=>{ const n=document.createElement(tag); n.className=cls||''; if(text!==undefined)n.textContent=text; return n; };
function updateRoomClocks(){document.querySelectorAll('[data-expires-at]').forEach(node=>{const seconds=Math.max(0,Math.ceil((Date.parse(node.dataset.expiresAt)-Date.now())/1000));node.textContent=`${String(Math.floor(seconds/60)).padStart(2,'0')}:${String(seconds%60).padStart(2,'0')}`;});}
export const effects={
  HEAL:['✚','Mạch sống','Hồi phục HP cho phe của bạn.','#85e2b3'],
  DOUBLE_DAMAGE:['ϟ','Sấm sét','Đánh thẳng vào HP của đối thủ.','#ff9d80'],
  FREEZE:['❄','Băng phong','Chọn quân đối thủ để khóa trong lượt kế tiếp.','#92d5ff'],
  INVULNERABLE:['◇','Hộ vệ','Chọn quân của bạn để bảo vệ khỏi bị bắt.','#f6d38b'],
  PAWN_DASH:['♟','Tốt xung phong','Cho một Tốt tiến thêm một ô.','#bbd99a'],
  TRAP:['◇','Bẫy','Đặt bẫy phát nổ trên bàn cờ.','#ed9b8b'],
  REWIND:['↶','Quay ngược','Khôi phục bàn cờ ở lượt trước.','#d7b779'],
  PAWN_BACKWARD:['↟','Tốt lùi','Cho phép Tốt của bạn đi lùi.','#b7c0ae'],
  PORTAL:['◌','Cổng dịch chuyển','Hoán đổi vị trí hai quân cờ.','#bfa3ff'],
  EXTRA_QUEEN:['♛','Triệu hồi','Đổi 80% HP hiện tại lấy một quân Hậu.','#bfa3ff'],
  REVIVE:['✚','Hồi sinh','Đưa quân mạnh nhất đã mất trở lại.','#85e2b3'],
  HIDE_KING:['♜','Ẩn Vua','Đổi chỗ Vua với Xe gần nhất.','#d7b779'],
  TELEPORT:['⌁','Dịch chuyển','Đưa một quân đến ô trống ngẫu nhiên.','#92d5ff'],
  MOSQUITO:['✣','Muỗi','Khóa quân đối thủ và rút HP mỗi lượt.','#ed9b8b'],
  SURRENDER:['⚑','Đình chiến','Đầu hàng và kết thúc ván đấu.','#efa4bb']
};
export function mountExperience() {
  const nav=document.querySelector('.main-nav');
  const profile=el('a','','Hồ sơ'); profile.href='#/profile'; profile.dataset.route='profile'; nav.append(profile);
  document.querySelector('.home-heading').insertAdjacentHTML('afterend',`<section class="lobby-hero"><div class="hero-copy"><p class="kicker">CHESS² / ARCANE CLUB</p><h2>Một bàn cờ.<br>Vạn cách <em>lật thế.</em></h2><p>Đọc vị đối thủ, tung thẻ đúng lúc.<br>Mỗi ván đấu là một câu chuyện mới.</p><div class="hero-actions"><button type="button" class="button primary" id="hero-play">♞ &nbsp; Thách đấu bot</button></div><span class="hero-caption">CỜ ARCADE · THẺ CHIẾN THUẬT · 100 HP</span></div><div class="hero-art" aria-hidden="true"><div class="orbit orbit-one"></div><div class="orbit orbit-two"></div><div class="hero-checker"></div><div class="hero-knight">${pieceSvg('wN')}</div><div class="floating-card frost">❄<small>FREEZE</small></div><div class="floating-card thunder">ϟ<small>STRIKE</small></div><span class="art-spark spark-one">✧</span><span class="art-spark spark-two">✦</span></div></section>`);
  document.querySelector('.lobby-hero').insertAdjacentHTML('afterend',`<section id="starter-guide" class="starter-guide" aria-labelledby="starter-title" hidden><div class="starter-intro"><h2 id="starter-title">Deck khởi đầu đã sẵn sàng</h2><p>Vào trận đầu tiên với Hồi phục, Cường kích và Đóng băng.</p></div><ol class="starter-flow"><li><b>1</b><span><strong>Trang bị</strong><small>Ba thẻ đã nằm sẵn trong Deck.</small></span></li><li><b>2</b><span><strong>Tung thẻ</strong><small>Chỉ dùng thành công mới tiêu một thẻ.</small></span></li><li><b>3</b><span><strong>Bổ sung</strong><small>Giải Puzzle lấy Gold rồi mua thêm.</small></span></li></ol><div class="starter-actions"><button id="starter-play" class="button primary" type="button">Chơi trận đầu</button><button id="starter-skip" class="text-link" type="button">Tôi sẽ tự khám phá</button></div></section>`);
  const icons=['♞','⚔','♜']; document.querySelectorAll('.mode-card').forEach((card,i)=>{card.dataset.mode=String(i); card.querySelector('.mode-icon').textContent=icons[i];});
  document.querySelector('[data-page="home"]').insertAdjacentHTML('beforeend',`<section class="panel invite-panel"><div><p class="kicker">CÙNG NHAU CHƠI HAY HƠN</p><h2>Bạn có lời mời?</h2><p class="muted">Dán liên kết hoặc mã phòng để ngồi vào quân Đen.</p></div><form id="join-form"><label class="field"><span>Liên kết hoặc mã phòng</span><input id="join-code" required placeholder="Dán link mời vào đây…" autocomplete="off"></label><button class="button primary" type="submit">Vào phòng</button></form></section>`);
  document.querySelector('[data-page="play"] .page-heading').insertAdjacentHTML('afterend',`<div id="room-invite" class="room-invite" hidden><div class="room-waiting-copy"><span class="waiting-pulse" aria-hidden="true"></span><div><strong id="room-message">Đang mở phòng PvP</strong><p><span id="room-owner"></span> · tự đóng sau <b id="room-countdown">05:00</b></p></div></div><div class="room-code-block"><span>Mã phòng</span><strong id="room-code">------</strong></div><input id="invite-url" hidden readonly aria-label="Liên kết mời bạn"><button id="copy-invite" type="button" class="button primary">Sao chép lời mời</button></div><div id="match-result" class="match-result" role="status" hidden><span class="result-mark">♛</span><div><p class="kicker">VÁN ĐẤU ĐÃ KHÉP LẠI</p><h2 id="result-title"></h2><p id="result-copy"></p></div><button id="play-again" class="button primary" type="button">Chơi ván mới</button><a class="button ghost" href="#/profile">Xem hồ sơ</a></div>`);
  setInterval(updateRoomClocks,1000);
  const area=document.querySelector('.board-area');
  area.insertAdjacentHTML('beforeend',`<div class="board-toolbar"><span id="turn-indicator" role="status">Sẵn sàng</span><div><button id="flip-board" class="icon-button" type="button" aria-label="Xoay bàn cờ" title="Xoay bàn cờ">⇅</button><button id="sound-toggle" class="icon-button" type="button" aria-label="Bật âm thanh" aria-pressed="false" title="Âm thanh">♪</button></div></div><p class="board-tip">Kéo quân hoặc bấm hai ô để đi · Esc để hủy chọn thẻ</p>`);
  const panel=document.querySelector('.game-panel');
  panel.prepend($('room-invite'));
  panel.insertAdjacentHTML('beforeend',`<button id="cancel-card" type="button" class="text-link" hidden>Hủy chọn thẻ (Esc)</button><details class="move-journal"><summary>Nhật ký ván đấu</summary><ol id="move-journal"><li>Hãy mở màn bằng nước đi của bạn.</li></ol></details><details class="arcade-rules"><summary>Luật cờ Arcane</summary><p>Thắng khi bắt Vua hoặc đưa HP đối thủ về 0. Tốt tự phong Hậu ở hàng cuối. Đây là biến thể arcade: chưa có chiếu bí, nhập thành và bắt tốt qua đường.</p><p>Thẻ chỉ dùng trong lượt của bạn. Thẻ được tiêu hao sau khi kích hoạt thành công.</p></details>`);
  $('app-view').insertAdjacentHTML('beforeend',`<section class="page" data-page="profile"><div class="page-heading"><div><p class="kicker">HÀNH TRÌNH CỦA BẠN</p><h1>Góc kỳ thủ</h1><p>Mỗi ván đấu để lại một dấu ấn.</p></div><button id="refresh-profile" type="button" class="button ghost">Làm mới hồ sơ</button></div><div class="profile-banner"><div id="profile-avatar" class="profile-avatar">♞</div><div><p class="kicker" id="profile-type"></p><h2 id="profile-name"></h2><p id="profile-email"></p></div><div class="profile-rating"><small>RATING</small><strong id="profile-rating"></strong><span id="profile-level"></span></div></div><div id="profile-stats" class="profile-stats"></div><section class="panel"><div class="section-title"><div><p class="kicker">20 VÁN GẦN NHẤT</p><h2>Lịch sử & ván đang chơi</h2></div></div><div id="match-history" class="history-list"></div></section></section>`);
  $('hero-play').addEventListener('click',()=>{document.querySelector('[data-create-game="PVE"]').scrollIntoView({behavior:reducedMotion()?'instant':'smooth',block:'center'});document.querySelector('[data-create-game="PVE"]').focus({preventScroll:true});});
  document.querySelector('.side-copy').textContent='Tung một lá bài. Đổi cả cục diện.';
  mountClubDesign();
}

function mountClubDesign() {
  document.querySelector('.brand-mark').innerHTML=pieceSvg('wN');
  document.querySelector('.brand small').textContent='ARCANE CLUB';
  const routes={home:['home','Sảnh'],play:['play','Chơi cờ'],collection:['cards','Bộ sưu tập'],puzzles:['puzzle','Puzzle'],leaderboard:['trophy','Xếp hạng'],profile:['user','Hồ sơ']};
  document.querySelectorAll('.main-nav a').forEach(link=>{
    const [symbol,label]=routes[link.dataset.route];
    link.innerHTML=icon(symbol)+`<span>${label}</span>`;
  });
  document.querySelectorAll('.kicker').forEach(label=>{if(!label.id)label.remove();});
  document.querySelector('.game-panel h2').textContent='Thẻ chiến thuật';
  document.querySelector('.side-copy').textContent='Chọn thẻ để xoay chuyển ván đấu.';
  document.querySelector('.game-panel').insertAdjacentHTML('afterbegin',`<span class="panel-emblem" aria-hidden="true">${icon('cards')}</span>`);
  for(const side of ['white','black']) {
    const strip=$(`${side}-hp`).closest('.player-strip');
    strip.dataset.side=side;
    strip.querySelector('.player-color').outerHTML=`<span class="player-portrait" aria-hidden="true">${pieceSvg(side==='white'?'wN':'bN')}</span>`;
    const name=strip.querySelector('span:not(.player-portrait)'),identity=el('span','player-identity'),main=el('strong','player-name',name.textContent),rating=el('small','player-elo','');main.id=`${side}-name`;rating.id=`${side}-rating`;identity.append(main,rating);name.replaceWith(identity);
    strip.querySelector('.health-track').setAttribute('aria-hidden','true');
  }
  $('flip-board').innerHTML=icon('flip')+'<span>Lật bàn</span>';
  $('sound-toggle').innerHTML=icon('sound')+'<span>Âm thanh</span>';
  $('resign-game').innerHTML=icon('flag')+'<span>Đầu hàng</span>';
  $('leave-game').textContent='Về sảnh';
  const tools=el('div','match-tools');
  tools.append($('flip-board'),$('sound-toggle'),$('resign-game'));
  document.querySelector('.board-area').append(tools);
  $('game-cards').insertAdjacentHTML('afterend','<div id="card-target" class="card-target" role="status" hidden><span id="target-art" class="card-art" aria-hidden="true"></span><div><strong id="target-name"></strong><p id="target-copy"></p></div></div>');
  $('card-target').append($('cancel-card'));
  $('cancel-card').textContent='Hủy chọn';
  document.querySelector('.result-mark').innerHTML=icon('trophy');
  const heroPlay=$('hero-play');heroPlay.innerHTML=icon('play')+'<span>Thách đấu bot</span>';
  document.querySelectorAll('.mode-icon').forEach((node,i)=>node.innerHTML=icon(['play','sword','home'][i]));
  document.querySelector('.auth-intro .feature-list').innerHTML=`<span>${icon('play')}Đấu bot & bạn bè</span><span>${icon('cards')}Thẻ chiến thuật</span><span>${icon('puzzle')}Puzzle</span>`;
  // Reuse the same illustrated cards in the lobby, never bake UI text into art.
  for(const [selector,type] of [['.floating-card.frost','FREEZE'],['.floating-card.thunder','DOUBLE_DAMAGE']]) {
    const card=document.querySelector(selector);card.replaceChildren(cardArt(type),el('small','',cardNames[type]));
  }
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
    const row=el('article','history-row'), side=game.type==='PVE'?game.playerSide:game.whitePlayerId===p.id?'WHITE':'BLACK';
    const active=['PLAYING','WAITING'].includes(game.status);
    const outcome=active?(game.status==='WAITING'?'Đang chờ':'Đang chơi'):game.status==='ABANDONED'?'Đã hủy':game.type==='LOCAL'?(game.winnerColor?`${game.winnerColor==='WHITE'?'Trắng':'Đen'} thắng`:'Hòa'):game.winnerColor?(game.winnerColor===side?'Chiến thắng':'Thất bại'):'Hòa';
    const symbol=el('span','history-symbol');symbol.innerHTML=game.type==='PVE'?pieceSvg('wN'):icon('sword');row.append(symbol);
    const info=el('div','history-info'); info.append(el('strong','',game.type==='PVE'?`Đấu bot · ${difficultyName(game.difficulty)}`:game.type==='PVP'?'Đấu bạn bè':'Cùng thiết bị'),el('small','',`${game.type==='PVP'&&game.roomCode?`Phòng #${game.roomCode}`:`Ván ${game.id.slice(0,8)}`} · ${game.whiteHp} / ${game.blackHp} HP`));
    row.append(info,el('span','result-badge',outcome));const button=el('button','button ghost compact',active?'Tiếp tục':'Xem bàn');button.type='button';button.addEventListener('click',()=>onResume(game.id));row.append(button);root.append(row);
  });
}
const onboardingKey=()=>`chess2.onboarding.v1.${state.player?.id||'unknown'}`;
export function renderOnboarding(){const guide=$('starter-guide');if(!guide||!state.player)return;const played=state.player.wins+state.player.losses+state.player.draws>0||state.history.length>0;guide.hidden=played||localStorage.getItem(onboardingKey())==='done';}
export function dismissOnboarding(){if(!state.player)return;localStorage.setItem(onboardingKey(),'done');renderOnboarding();}
export const difficultyName=level=>({EASY:'Tập sự',MEDIUM:'Chiến thuật',HARD:'Thử thách'}[level]||level);
function moveActor(log){
  if(log.actorUserId===state.player?.id)return 'Bạn';
  if(state.game?.type==='LOCAL')return log.actorSide==='WHITE'?'Trắng':'Đen';
  if(state.game?.type==='PVE'&&!log.actorUserId)return 'Bot';
  return log.actorSide==='WHITE'?'Trắng':'Đen';
}
function moveTime(value){return new Intl.DateTimeFormat('vi-VN',{hour:'2-digit',minute:'2-digit',second:'2-digit'}).format(new Date(value));}
function capturedPieceName(code){
  const names={P:'Tốt',N:'Mã',B:'Tượng',R:'Xe',Q:'Hậu',K:'Vua'},side=code?.[0]==='w'?'Trắng':'Đen';
  return code?`${names[code[1]]||'quân'} ${side}`:'';
}
function moveLogEntry(log){
  const captured=log.capturedPiece?` · bắt ${capturedPieceName(log.capturedPiece)}`:'';
  if(log.eventType==='MOVE')return `${moveActor(log)}: ${log.from} → ${log.to}${captured}`;
  if(log.eventType==='CARD')return `${moveActor(log)} dùng ${cardNames[log.effectCode]||log.effectCode}${log.to&&log.to!=='OPPONENT'?` · ${log.to}`:''}`;
  if(log.eventType==='EFFECT')return `Hiệu ứng ${cardNames[log.effectCode]||log.effectCode}${log.to?` tại ${log.to}`:''}${captured}`;
  if(log.eventType==='RESIGN')return `${moveActor(log)} đầu hàng`;
  return log.eventType;
}
export function renderMatchChrome() {
  const g=state.game, side=playerSide();
  const waiting=!!g&&g.type==='PVP'&&g.status==='WAITING';$('room-invite').hidden=!waiting;document.querySelector('.game-layout').classList.toggle('is-waiting',waiting);
  if(waiting){const url=new URL(location.origin);url.searchParams.set('room',g.roomCode||g.id);$('invite-url').value=url.href;$('room-code').textContent=g.roomCode||g.id.slice(0,6);$('room-message').textContent='Phòng đang chờ đối thủ';$('room-owner').textContent=`${g.whiteUsername} · ${g.whiteRating} ELO`;$('room-countdown').dataset.expiresAt=g.roomExpiresAt||'';updateRoomClocks();}
  const ended=g&&!['PLAYING','WAITING'].includes(g.status);$('match-result').hidden=!ended;
  if(ended){
    const local=side==='BOTH',won=!local&&g.winnerColor===side;
    $('result-title').textContent=g.status==='ABANDONED'?'Phòng đã đóng':!g.winnerColor?'Ván đấu hòa':local?`${g.winnerColor==='WHITE'?'Trắng':'Đen'} chiến thắng`:won?'Chiến thắng của bạn!':'Bạn đã thua';
    let reason=g.status==='ABANDONED'?'Phòng đã đóng trước khi có đối thủ.':!g.winnerColor?'Không bên nào giành chiến thắng.':g.status==='RESIGNED'?(local?'Ván đấu kết thúc bằng đầu hàng.':won?'Đối thủ đã đầu hàng.':'Bạn đã đầu hàng.'):(g.winnerColor==='WHITE'?g.blackHp:g.whiteHp)===0?(local?'Ván đấu kết thúc khi một bên hết HP.':won?'HP đối thủ đã về 0.':'HP của bạn đã về 0.'):(local?'Vua đã bị bắt.':won?'Bạn đã bắt được Vua đối thủ.':'Vua của bạn đã bị bắt.');
    if(!local&&g.status!=='ABANDONED'){
      const rating=g.whitePlayerId===state.player?.id?g.whiteRating:g.blackPlayerId===state.player?.id?g.blackRating:state.player?.rating;
      const delta=state.matchStartRating==null||rating==null?null:rating-state.matchStartRating;
      reason+=delta==null?` ELO hiện tại: ${rating}.`:` ELO ${delta>=0?'+':''}${delta} · hiện tại ${rating}.`;
    }
    $('result-copy').textContent=reason;
  }
  const status=!g?'Chọn một chế độ để bắt đầu':state.busy?(g.type==='PVE'?'Bot đang suy nghĩ…':'Đang xác nhận…'):g.status==='WAITING'?'Chờ bạn bè tham gia':ended?'Ván đã kết thúc':side==='BOTH'?`Lượt ${g.currentTurn==='WHITE'?'Trắng':'Đen'}`:side===g.currentTurn?'Đến lượt bạn':'Đối thủ đang suy nghĩ…';
  $('turn-indicator').textContent=status;$('turn-indicator').classList.toggle('thinking',!!state.busy||!!g&&g.status==='PLAYING'&&side!==g.currentTurn&&side!=='BOTH');
  $('game-status').textContent=status;$('cancel-card').hidden=!state.pendingCard;
  $('card-target').hidden=!state.pendingCard;
  if(state.pendingCard){$('target-art').dataset.effect=state.pendingCard.effectType;$('target-name').textContent=cardNames[state.pendingCard.effectType]||state.pendingCard.name;$('target-copy').textContent=state.pendingCard.effectType==='FREEZE'?'Chọn một quân đối thủ trên bàn cờ.':'Chọn một quân của bạn trên bàn cờ.';}
  const difficultyLabel=g?.type==='PVE'?({EASY:'Dễ',MEDIUM:'Trung bình',HARD:'Khó'}[g.difficulty]||'Bot'):'';
  const labels={WHITE:'Trắng',BLACK:'Đen'},ratings={WHITE:'',BLACK:''};
  if(g?.type==='PVP'){labels.WHITE=`${g.whiteUsername||'Kỳ thủ Trắng'}${side==='WHITE'?' · Bạn':''}`;labels.BLACK=g.blackUsername?`${g.blackUsername}${side==='BLACK'?' · Bạn':''}`:'Đang chờ đối thủ';ratings.WHITE=`${g.whiteRating} ELO`;ratings.BLACK=g.blackRating==null?'Phòng đang mở':`${g.blackRating} ELO`;}
  else if(g?.type==='PVE'){const bot=`Bot · ${difficultyLabel}`,player='Bạn';if(side==='BLACK'){labels.WHITE=bot;labels.BLACK=player;ratings.BLACK=`${state.player?.rating??g.whiteRating} ELO`;}else{labels.WHITE=player;labels.BLACK=bot;ratings.WHITE=`${state.player?.rating??g.whiteRating} ELO`;}}
  $('white-name').textContent=labels.WHITE;$('black-name').textContent=labels.BLACK;$('white-rating').textContent=ratings.WHITE;$('black-rating').textContent=ratings.BLACK;
  document.querySelectorAll('.player-strip').forEach(strip=>strip.classList.toggle('is-turn',g?.status==='PLAYING'&&strip.dataset.side.toUpperCase()===g.currentTurn));
  if(state.syncIssue)$('game-status').textContent="Đang kết nối lại… Bàn cờ sẽ tự đồng bộ.";
  $('leave-game').textContent=waiting?'Hủy phòng':'Về sảnh';
  const title=!g?'Bàn cờ':waiting?`Phòng #${g.roomCode}`:g.type==='PVE'?`Đấu bot · ${difficultyName(g.difficulty)}`:g.type==='PVP'?'Đấu bạn bè':'Cùng một bàn cờ';$('game-title').textContent=title;
  const journal=$('move-journal');journal.replaceChildren();
  if(state.moveLogStatus==='loading')journal.append(el('li','move-log-state','Đang tải nhật ký…'));
  else if(state.moveLogStatus==='error')journal.append(el('li','move-log-state','Chưa thể tải nhật ký. Hệ thống sẽ thử lại khi bàn cờ đồng bộ.'));
  else if(!state.moveLog.length)journal.append(el('li','move-log-state','Hãy mở màn bằng nước đi của bạn.'));
  else state.moveLog.forEach(log=>{const item=el('li','move-entry');item.append(el('span','move-copy',moveLogEntry(log)),el('time','move-time',moveTime(log.occurredAt)));item.querySelector('time').dateTime=log.occurredAt;journal.append(item);});
}
export function decorateCards() {
  document.querySelectorAll('.card-tile').forEach(tile=>{
    const card=state.cards.find(c=>c.id===tile.dataset.cardId),fx=effects[card?.effectType];if(!fx)return;
    tile.style.setProperty('--card-accent',fx[3]);tile.classList.add('illustrated-card');
    if(!tile.querySelector('.card-art'))tile.prepend(cardArt(card.effectType));
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
  const fx=effects[card.effectType]||effects.HEAL,burst=el('div','card-burst');burst.setAttribute('aria-hidden','true');burst.style.setProperty('--card-accent',fx[3]);burst.append(cardArt(card.effectType),el('strong','',cardNames[card.effectType]||card.name));document.body.append(burst);
  burst.animate([{opacity:0,transform:'translate(-50%, 20px) scale(.8)'},{opacity:1,transform:'translate(-50%, 0) scale(1)',offset:.2},{opacity:1,offset:.72},{opacity:0,transform:'translate(-50%, -35px) scale(.95)'}],{duration:1000}).onfinish=()=>burst.remove();
}
