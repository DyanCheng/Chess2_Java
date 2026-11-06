const SESSION_KEY = "chess2.session.v2";
const GUEST_KEY = "chess2.guest-key.v1";

export const state = { session: null, player: null, game: null, selectedSquare: null, lastMove: [], cards: [], inventory: [], shop: [], puzzles: [], leaderboard: [], waitingGames: [] };

export function restoreSession() { try { const stored = JSON.parse(localStorage.getItem(SESSION_KEY)); if (stored?.accessToken && stored?.player) state.session = stored; } catch { localStorage.removeItem(SESSION_KEY); } return state.session; }
export function saveSession(session) { state.session = session; state.player = session.player; localStorage.setItem(SESSION_KEY, JSON.stringify(session)); }
export function clearSession() { state.session = null; state.player = null; state.game = null; state.selectedSquare = null; localStorage.removeItem(SESSION_KEY); }
export function stableGuestKey() { let key = localStorage.getItem(GUEST_KEY); if (!key) { key = crypto.randomUUID(); localStorage.setItem(GUEST_KEY, key); } return key; }
