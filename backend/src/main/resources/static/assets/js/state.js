const SESSION_KEY = "chess2.session.v2";
const GUEST_KEY = "chess2.guest-key.v1";

export const state = { session: null, player: null, game: null, selectedSquare: null, pendingCard: null, lastMove: [], cards: [], inventory: [], shop: [], puzzles: [], leaderboard: [], waitingGames: [], history: [], busy: false, moveLog: [] };

export function restoreSession() { try { const stored = JSON.parse(localStorage.getItem(SESSION_KEY)); if (stored?.accessToken && stored?.player) state.session = stored; } catch { localStorage.removeItem(SESSION_KEY); } return state.session; }
export function saveSession(session) { state.session = session; state.player = session.player; localStorage.setItem(SESSION_KEY, JSON.stringify(session)); }
export function clearSession() { state.session = null; state.player = null; state.game = null; state.selectedSquare = null; state.pendingCard = null; state.lastMove = []; state.moveLog = []; state.history = []; state.inventory = []; state.busy = false; localStorage.removeItem(SESSION_KEY); }
export function playerSide() { if (!state.game) return null; if (state.game.type === 'LOCAL') return 'BOTH'; return state.game.whitePlayerId === state.player?.id ? 'WHITE' : state.game.blackPlayerId === state.player?.id ? 'BLACK' : null; }
export function stableGuestKey() { let key = localStorage.getItem(GUEST_KEY); if (!key) { key = crypto.randomUUID(); localStorage.setItem(GUEST_KEY, key); } return key; }
