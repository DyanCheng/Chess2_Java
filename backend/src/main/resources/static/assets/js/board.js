// A persistent board: input never waits for rendering or a round-trip to the server.
export const coordinate = (row, col) => `${String.fromCharCode(97 + col)}${8 - row}`;
export const point = (square) => [8 - Number(square[1]), square.charCodeAt(0) - 97];
export const at = (board, square) => { const [r,c] = point(square); return board?.[r]?.[c]; };
export const reducedMotion = () => matchMedia('(prefers-reduced-motion: reduce)').matches;

// These are arcade move hints; the server remains authoritative.
export function destinations(game, from) {
  const board = game?.board, piece = at(board, from);
  if (!piece || game.frozenSquare === from) return [];
  const [r,c] = point(from), result = [];
  for (let y=0; y<8; y++) for (let x=0; x<8; x++) {
    const target = board[y][x], to = coordinate(y,x), dy=y-r, dx=x-c;
    if ((!dy && !dx) || target?.[0] === piece[0] || (target && game.invulnerableSquare === to)) continue;
    let valid = false;
    if (piece[1] === 'P') {
      const dir = piece[0] === 'w' ? -1 : 1, home = piece[0] === 'w' ? 6 : 1;
      valid = dx === 0 && !target && (dy === dir || (r === home && dy === dir*2 && !board[r+dir][c])) || Math.abs(dx) === 1 && dy === dir && !!target;
    } else if (piece[1] === 'N') valid = Math.abs(dx)*Math.abs(dy) === 2;
    else if (piece[1] === 'K') valid = Math.max(Math.abs(dx),Math.abs(dy)) === 1;
    else {
      valid = (piece[1] === 'R' && (!dx || !dy)) || (piece[1] === 'B' && Math.abs(dx) === Math.abs(dy)) || (piece[1] === 'Q' && (!dx || !dy || Math.abs(dx) === Math.abs(dy)));
      if (valid) { let yy=r+Math.sign(dy), xx=c+Math.sign(dx); while (yy!==y || xx!==x) { if (board[yy][xx]) { valid=false; break; } yy+=Math.sign(dy); xx+=Math.sign(dx); } }
    }
    if (valid) result.push(to);
  }
  return result;
}

const shapes = {
  P:'<circle cx="32" cy="16" r="8"/><path d="M26 25h12l-3 8 4 13H25l4-13z"/>',
  R:'<path d="M19 9h7v7h5V9h6v7h5V9h5v15l-7 6 2 16H24l2-16-7-6z"/>',
  N:'<path d="m22 45 3-10 12-8-9-1-7 5-7-6 11-15 7 1 3-6 10 10 5 17-7 13z"/><path d="m30 19 3-1"/>',
  B:'<path d="M32 5c-4 7-13 12-13 20 0 7 7 11 13 11s13-4 13-11C45 17 36 12 32 5Z"/><path d="m34 16-6 10m-2 9-3 11h18l-3-11"/>',
  Q:'<path d="m17 17 7 7 8-14 8 14 7-7-7 23H24z"/><circle cx="16" cy="14" r="3"/><circle cx="32" cy="8" r="3"/><circle cx="48" cy="14" r="3"/><path d="M24 40h16l2 6H22z"/>',
  K:'<path d="M32 5v13m-6-8h12M23 34c-12-9-7-18 1-16l8 7 8-7c8-2 13 7 1 16l-3 12H26z"/>'
};
export function pieceSvg(code) {
  const light = code[0] === 'w';
  return `<svg viewBox="0 0 64 64" aria-hidden="true" xmlns="http://www.w3.org/2000/svg"><g fill="${light?'#fff2d3':'#343546'}" stroke="${light?'#67533e':'#c5b5a0'}" stroke-width="2.4" stroke-linejoin="round" stroke-linecap="round">${shapes[code[1]] || ''}<path d="M23 46h18l5 7v4H18v-4z"/><path d="M22 52h20"/></g></svg>`;
}

export class ChessBoardView {
  constructor(root, callbacks) {
    this.root=root; this.callbacks=callbacks; this.tokens=new Map(); this.squares=new Map(); this.drag=null; this.flipped=false;
    root.replaceChildren(); root.className='chess-board premium-board';
    for(let r=0;r<8;r++) for(let c=0;c<8;c++) {
      const square=document.createElement('button'); square.type='button'; square.className=`square ${(r+c)%2===0?'is-light':''}`;
      square.dataset.square=coordinate(r,c); square.draggable=false;
      square.addEventListener('click',e=> { if(e.detail && performance.now()<this.suppressUntil) return; this.callbacks.select(r,c); });
      root.append(square); this.squares.set(coordinate(r,c),square);
    }
    this.layer=document.createElement('div'); this.layer.className='piece-layer'; this.layer.setAttribute('aria-hidden','true'); root.append(this.layer);
    root.addEventListener('dragstart',e=>e.preventDefault());
    root.addEventListener('pointerdown',e=>this.down(e));
    root.addEventListener('pointermove',e=>this.move(e));
    root.addEventListener('pointerup',e=>this.up(e));
    root.addEventListener('pointercancel',()=>this.cancel());
    root.addEventListener('lostpointercapture',()=>this.cancel());
    window.addEventListener('blur',()=>this.cancel());
    document.addEventListener('keydown',e=>{ if(e.key==='Escape') { this.cancel(); this.callbacks.cancel(); } });
    this.resizeObserver=new ResizeObserver(()=>this.cancel()); this.resizeObserver.observe(root);
  }
  position(square) { const [r,c]=point(square); return [this.flipped?7-c:c,this.flipped?7-r:r]; }
  selectable(square) {
    const {game,busy,side,pendingCard}=this.model || {}, p=at(game?.board,square);
    return !busy && !pendingCard && game?.status==='PLAYING' && (side==='BOTH'||game.currentTurn===side) && p?.[0]===(game.currentTurn==='WHITE'?'w':'b') && game.frozenSquare!==square;
  }
  render(model, {animate=true}={}) {
    this.model=model;
    // Polling must not dislodge the piece currently held by the player.
    if(this.drag) return;
    const {game,selected,lastMove=[],busy}=model, board=game?.board;
    const hints=new Set(selected?destinations(game,selected):[]);
    const wanted=new Map();
    this.squares.forEach((el,key)=> {
      const code=at(board,key), [x,y]=this.position(key);
      el.style.gridColumn=String(x+1); el.style.gridRow=String(y+1);
      el.setAttribute('aria-label',code?`${key}: ${code}`:key); el.setAttribute('aria-pressed',String(key===selected));
      el.classList.toggle('is-selected',key===selected); el.classList.toggle('is-last',lastMove.includes(key));
      el.classList.toggle('is-hint',hints.has(key)); el.classList.toggle('is-capture-hint',hints.has(key)&&!!code);
      el.classList.toggle('is-frozen',key===game?.frozenSquare); el.classList.toggle('is-protected',key===game?.invulnerableSquare);
      el.classList.toggle('can-drag',this.selectable(key));
      el.dataset.file=y===7?key[0]:''; el.dataset.rank=x===0?key[1]:'';
      if(code) wanted.set(key,code);
    });
    this.root.setAttribute('aria-busy',String(!!busy));
    const displaced=[...this.tokens].filter(([key,t])=>wanted.get(key)!==t.code);
    const next=new Map();
    wanted.forEach((code,key)=> {
      let token=this.tokens.get(key)?.code===code?this.tokens.get(key):null;
      if(!token) {
        const candidate=displaced.findIndex(([,t])=>t.code===code);
        if(candidate>=0) token=displaced.splice(candidate,1)[0][1];
      }
      if(!token) { const el=document.createElement('span'); el.className=`piece board-piece piece-${code[0]==='w'?'white':'black'}`; el.draggable=false; el.innerHTML=pieceSvg(code); this.layer.append(el); token={code,el}; }
      const [x,y]=this.position(key), transform=`translate(${x*100}%, ${y*100}%)`;
      token.el.style.transition=animate&&!reducedMotion()?'transform 190ms cubic-bezier(.2,.75,.25,1)':'none'; token.el.style.transform=transform; token.el.dataset.square=key;
      token.el.classList.remove('held'); next.set(key,token);
    });
    this.tokens.forEach(t=>{ if(![...next.values()].includes(t)) { if(animate&&!reducedMotion()) { const anim=t.el.animate([{opacity:1},{opacity:0,scale:'.55'}],{duration:150}); anim.onfinish=()=>t.el.remove(); } else t.el.remove(); } });
    this.tokens=next;
  }
  down(e) {
    if(e.button!==0 || !e.isPrimary || this.drag) return;
    const square=e.target.closest('[data-square]')?.dataset.square; if(!square || !this.selectable(square)) return;
    const token=this.tokens.get(square); if(!token) return;
    this.drag={id:e.pointerId,from:square,token,startX:e.clientX,startY:e.clientY,x:e.clientX,y:e.clientY,active:false};
    // Capture on the source button so a tap still produces its normal click.
    this.squares.get(square).setPointerCapture(e.pointerId);
  }
  move(e) {
    const d=this.drag; if(!d || e.pointerId!==d.id) return;
    d.x=e.clientX; d.y=e.clientY;
    if(!d.active && Math.hypot(d.x-d.startX,d.y-d.startY)<5) return;
    e.preventDefault();
    if(!d.active) {
      d.active=true; d.token.el.classList.add('held'); this.root.classList.add('is-dragging');
      d.ghost=d.token.el.cloneNode(true); d.ghost.className='pointer-piece';
      const size=this.layer.getBoundingClientRect().width/8;
      d.ghost.style.cssText=`width:${size}px;height:${size}px`; document.body.append(d.ghost);
    }
    if(!this.frame) this.frame=requestAnimationFrame(()=>{ this.frame=0; if(this.drag?.ghost) this.drag.ghost.style.transform=`translate3d(${this.drag.x-this.drag.ghost.offsetWidth/2}px,${this.drag.y-this.drag.ghost.offsetHeight/2}px,0)`; });
    const target=this.hit(e.clientX,e.clientY);
    if(d.target!==target) { this.root.querySelector('.is-drop-target')?.classList.remove('is-drop-target'); this.squares.get(target)?.classList.add('is-drop-target'); d.target=target; }
  }
  hit(x,y) { const el=document.elementFromPoint(x,y)?.closest('.square'); return el && this.root.contains(el)?el.dataset.square:null; }
  up(e) {
    const d=this.drag; if(!d || e.pointerId!==d.id) return;
    const target=this.hit(e.clientX,e.clientY), active=d.active;
    this.cancel();
    if(active) {
      this.suppressUntil=performance.now()+40;
      if(target&&target!==d.from) {
        if(destinations(this.model.game,d.from).includes(target)) {
          const [x,y]=this.position(target);
          d.token.el.style.transition='none';d.token.el.style.transform=`translate(${x*100}%, ${y*100}%)`;
        }
        this.callbacks.move(d.from,target);
      } else this.render(this.model);
    }
  }
  cancel() {
    const d=this.drag; if(!d) return; this.drag=null;
    cancelAnimationFrame(this.frame); this.frame=0; d.ghost?.remove(); d.token.el.classList.remove('held');
    this.root.classList.remove('is-dragging'); this.root.querySelector('.is-drop-target')?.classList.remove('is-drop-target');
    const source=this.squares.get(d.from); if(source.hasPointerCapture(d.id)) source.releasePointerCapture(d.id);
  }
  flip() {
    this.cancel(); this.flipped=!this.flipped; this.render(this.model,{animate:false});
    // Commit the new orientation before a surrounding UI render restores move transitions.
    this.layer.getBoundingClientRect();
  }
  effect(kind, target) {
    const el=this.squares.get(target)||this.root;
    if(reducedMotion()) return;
    const color=kind==='FREEZE'?'#9fdaff':kind==='DOUBLE_DAMAGE'?'#fa836b':'#f7d691';
    el.animate([{boxShadow:`inset 0 0 0 0 ${color}`},{boxShadow:`inset 0 0 60px 12px ${color}`},{boxShadow:`inset 0 0 0 0 ${color}`}],{duration:620});
  }
}
