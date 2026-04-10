/**
 * Shared API helpers: guest/admin session headers for protected endpoints.
 */
const API = '/api';
const STORAGE_GUEST = 'hotelOopGuest';
const STORAGE_ADMIN = 'hotelOopAdmin';

function guestAuthHeaders() {
    try {
        const raw = localStorage.getItem(STORAGE_GUEST);
        if (!raw) {
            return {};
        }
        const { token } = JSON.parse(raw);
        return token ? { 'X-Session-Token': token } : {};
    } catch {
        return {};
    }
}

function adminAuthHeaders() {
    try {
        const raw = localStorage.getItem(STORAGE_ADMIN);
        if (!raw) {
            return {};
        }
        const { token } = JSON.parse(raw);
        return token ? { 'X-Admin-Token': token } : {};
    } catch {
        return {};
    }
}

function saveGuestSession(token, guest) {
    localStorage.setItem(STORAGE_GUEST, JSON.stringify({ token, guest }));
}

function saveAdminSession(token) {
    localStorage.setItem(STORAGE_ADMIN, JSON.stringify({ token }));
}

function clearGuestSession() {
    localStorage.removeItem(STORAGE_GUEST);
}

function clearAdminSession() {
    localStorage.removeItem(STORAGE_ADMIN);
}

function getGuestSession() {
    try {
        const raw = localStorage.getItem(STORAGE_GUEST);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

/**
 * @param {'none'|'guest'|'admin'} auth
 */
async function hotelFetchJson(url, options = {}, auth = 'none') {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (auth === 'guest') {
        Object.assign(headers, guestAuthHeaders());
    } else if (auth === 'admin') {
        Object.assign(headers, adminAuthHeaders());
    }
    const res = await fetch(url, { ...options, headers });
    const text = await res.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = { message: text };
        }
    }
    if (!res.ok) {
        const msg = (data && data.message) ? data.message : res.statusText;
        throw new Error(msg);
    }
    return data;
}

function requireGuestSessionOrRedirect() {
    const s = getGuestSession();
    if (!s || !s.token) {
        window.location.href = 'auth.html';
        return null;
    }
    return s;
}

// Explicitly publish helpers for pages that rely on shared globals.
const root = (typeof window !== 'undefined') ? window : globalThis;
root.API = API;
root.hotelFetchJson = hotelFetchJson;
root.saveGuestSession = saveGuestSession;
root.saveAdminSession = saveAdminSession;
root.clearGuestSession = clearGuestSession;
root.clearAdminSession = clearAdminSession;
root.getGuestSession = getGuestSession;
root.requireGuestSessionOrRedirect = requireGuestSessionOrRedirect;
