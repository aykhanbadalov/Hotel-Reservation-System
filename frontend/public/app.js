/**
 * Guest UI: public room search; authenticated booking (dates only).
 */

async function fetchJson(url, options = {}, auth = 'none') {
    return hotelFetchJson(url, options, auth);
}

function qs(params) {
    const u = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v).trim() !== '') {
            u.set(k, v);
        }
    });
    const s = u.toString();
    return s ? `?${s}` : '';
}

function formatCurrency(value) {
    return `$${Number(value).toFixed(2)}`;
}

const FAVORITES_KEY = 'hotelOopFavorites';

function currentGuestKey() {
    const s = getGuestSession();
    if (!s || !s.guest) return null;
    return String(s.guest.id || s.guest.email || '');
}

function getFavoritesMap() {
    try {
        return JSON.parse(localStorage.getItem(FAVORITES_KEY) || '{}');
    } catch {
        return {};
    }
}

function saveFavoritesMap(map) {
    localStorage.setItem(FAVORITES_KEY, JSON.stringify(map));
}

function getFavoriteIds() {
    const key = currentGuestKey();
    if (!key) return [];
    const map = getFavoritesMap();
    return Array.isArray(map[key]) ? map[key] : [];
}

function isFavoriteRoom(roomId) {
    return getFavoriteIds().includes(Number(roomId));
}

function toggleFavoriteRoom(roomId) {
    const key = currentGuestKey();
    if (!key) return false;
    const id = Number(roomId);
    const map = getFavoritesMap();
    const list = Array.isArray(map[key]) ? map[key] : [];
    const idx = list.indexOf(id);
    if (idx >= 0) {
        list.splice(idx, 1);
    } else {
        list.push(id);
    }
    map[key] = list;
    saveFavoritesMap(map);
    return list.includes(id);
}

function favoriteHeartSvg(isActive) {
    if (isActive) {
        return `<svg viewBox="0 0 24 24" aria-hidden="true" class="h-5 w-5 fill-current"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09A6 6 0 0 1 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54z"/></svg>`;
    }
    return `<svg viewBox="0 0 24 24" aria-hidden="true" class="h-5 w-5 fill-none stroke-current stroke-2"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09A6 6 0 0 1 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54z"/></svg>`;
}

function setFavoriteButtonState(button, isActive) {
    button.className = isActive
        ? 'favBtn rounded-full border border-rose-200 bg-rose-50 p-2 text-rose-500 shadow-sm transition-transform duration-200 hover:scale-110 active:scale-95 dark:border-rose-900/60 dark:bg-rose-900/20 dark:text-rose-400'
        : 'favBtn rounded-full border border-slate-200 bg-white p-2 text-slate-400 shadow-sm transition-transform duration-200 hover:scale-110 hover:text-rose-500 active:scale-95 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-500 dark:hover:text-rose-400';
    button.innerHTML = favoriteHeartSvg(isActive);
    button.setAttribute('aria-pressed', isActive ? 'true' : 'false');
    button.setAttribute('title', isActive ? 'Remove from favorites' : 'Add to favorites');
}

function reservationStatusBadge(status) {
    if (status === 'CONFIRMED') {
        return 'rounded-full px-2 py-0.5 text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200';
    }
    if (status === 'PENDING') {
        return 'rounded-full px-2 py-0.5 text-xs font-semibold bg-amber-100 text-amber-900 dark:bg-amber-900/40 dark:text-amber-200';
    }
    return 'rounded-full px-2 py-0.5 text-xs font-semibold bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-200';
}

function initGuestPage() {
    const session = getGuestSession();
    if (!session || !session.token) {
        window.location.href = 'auth.html';
        return;
    }
    const form = document.getElementById('searchForm');
    const results = document.getElementById('results');
    const status = document.getElementById('status');
    if (!form || !results) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        status.textContent = 'Loading…';
        results.innerHTML = '';
        const fd = new FormData(form);
        const params = {
            q: fd.get('q'),
            type: fd.get('type'),
            minGuests: fd.get('minGuests'),
            checkIn: fd.get('checkIn'),
            checkOut: fd.get('checkOut'),
        };
        try {
            const rooms = await fetchJson(`${API}/rooms${qs(params)}`, {}, 'none');
            status.textContent = rooms.length ? `${rooms.length} room(s) found.` : 'Rooms not found';
            rooms.forEach((room) => results.appendChild(roomCard(room)));
        } catch (err) {
            status.textContent = 'Rooms not found';
            results.innerHTML = '';
        }
    });
}

function isValidBookingDates(checkInRaw, checkOutRaw) {
    const iso4 = /^\d{4}-\d{2}-\d{2}$/;
    if (!iso4.test(checkInRaw) || !iso4.test(checkOutRaw)) {
        return { ok: false, message: 'Dates must use 4-digit year format (YYYY-MM-DD).' };
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const checkIn = new Date(`${checkInRaw}T00:00:00`);
    const checkOut = new Date(`${checkOutRaw}T00:00:00`);
    if (Number.isNaN(checkIn.getTime()) || Number.isNaN(checkOut.getTime())) {
        return { ok: false, message: 'Please provide valid check-in and check-out dates.' };
    }
    if (checkIn < today) {
        return { ok: false, message: 'Check-in cannot be before today.' };
    }
    if (checkOut <= checkIn) {
        return { ok: false, message: 'Check-out must be after check-in.' };
    }
    return { ok: true };
}

function roomCard(room) {
    const el = document.createElement('article');
    el.className = 'flex flex-col rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900';
    const typeLabel = room.roomType === 'SUITE' ? 'Suite' : 'Standard';
    const estimate = room.estimatedStayTotal != null
        ? `<p class="mt-3 text-sm text-teal-700 dark:text-teal-300">Estimated stay total: <strong>${formatCurrency(room.estimatedStayTotal)}</strong></p>`
        : '<p class="mt-2 text-xs text-slate-500">Select check-in/out in search to see priced totals.</p>';

    const activeFavorite = isFavoriteRoom(room.id);
    el.innerHTML = `
    <div class="flex items-start justify-between gap-2">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-teal-600 dark:text-teal-400">Room ${room.roomNumber}</p>
        <h3 class="text-lg font-semibold">${typeLabel} · up to ${room.maxGuests} guests</h3>
      </div>
      <div class="flex flex-col items-end gap-2">
        <span class="rounded-full border border-emerald-200 bg-emerald-100 px-3 py-1 text-xs font-bold text-emerald-800 shadow-sm dark:border-emerald-700/40 dark:bg-emerald-900/30 dark:text-emerald-400">${formatCurrency(room.baseRatePerNight)}/night base rate</span>
        <button type="button" class="favBtn" aria-label="Toggle favorite room"></button>
      </div>
    </div>
    <p class="mt-3 flex-1 text-sm text-slate-600 dark:text-slate-300">${room.description}</p>
    ${estimate}
    <form class="bookForm mt-4 space-y-3 border-t border-slate-100 pt-4 dark:border-slate-800" data-room-id="${room.id}">
      <p class="text-sm font-medium">Request this room</p>
      <p class="text-xs text-slate-500 dark:text-slate-400">You must be signed in. Your account name and email are sent automatically.</p>
      <div class="grid gap-3 sm:grid-cols-2">
        <label class="block text-xs font-medium text-slate-500 dark:text-slate-400">Check-in
          <input name="checkIn" type="date" required class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-950"/>
        </label>
        <label class="block text-xs font-medium text-slate-500 dark:text-slate-400">Check-out
          <input name="checkOut" type="date" required class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-950"/>
        </label>
      </div>
      <button type="submit" class="w-full rounded-lg bg-slate-900 py-2 text-sm font-semibold text-white hover:bg-slate-800 dark:bg-teal-600 dark:hover:bg-teal-500">Submit booking request</button>
      <p class="bookMsg text-xs text-slate-500"></p>
    </form>`;

    const searchForm = document.getElementById('searchForm');
    const bookForm = el.querySelector('.bookForm');
    const favBtn = el.querySelector('.favBtn');
    const ci = searchForm?.elements['checkIn']?.value;
    const co = searchForm?.elements['checkOut']?.value;
    if (ci) bookForm.elements['checkIn'].value = ci;
    if (co) bookForm.elements['checkOut'].value = co;
    const today = new Date().toISOString().slice(0, 10);
    bookForm.elements['checkIn'].setAttribute('min', today);
    bookForm.elements['checkOut'].setAttribute('min', today);
    bookForm.elements['checkIn'].setAttribute('max', '9999-12-31');
    bookForm.elements['checkOut'].setAttribute('max', '9999-12-31');
    setFavoriteButtonState(favBtn, activeFavorite);
    favBtn.addEventListener('click', () => {
        const nowFav = toggleFavoriteRoom(room.id);
        setFavoriteButtonState(favBtn, nowFav);
    });

    bookForm.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        if (!getGuestSession()?.token) {
            window.location.href = 'auth.html';
            return;
        }
        const msg = bookForm.querySelector('.bookMsg');
        msg.textContent = 'Submitting…';
        const body = {
            roomId: Number(bookForm.dataset.roomId),
            checkIn: bookForm.elements['checkIn'].value,
            checkOut: bookForm.elements['checkOut'].value,
        };
        const dateCheck = isValidBookingDates(body.checkIn, body.checkOut);
        if (!dateCheck.ok) {
            msg.textContent = dateCheck.message;
            msg.classList.add('text-red-600', 'dark:text-red-400');
            return;
        }
        try {
            const res = await fetchJson(`${API}/reservations`, {
                method: 'POST',
                body: JSON.stringify(body),
            }, 'guest');
            msg.textContent = `Request #${res.id} is PENDING. Total if approved: ${formatCurrency(res.totalPrice)}.`;
            msg.classList.remove('text-red-600', 'dark:text-red-400');
            msg.classList.add('text-teal-700', 'dark:text-teal-300');
        } catch (err) {
            msg.textContent = err.message;
            msg.classList.add('text-red-600', 'dark:text-red-400');
            if (err.message && err.message.toLowerCase().includes('session')) {
                window.location.href = 'auth.html';
            }
        }
    });

    return el;
}

async function loadGuestHistory() {
    const rows = document.getElementById('historyRows');
    const status = document.getElementById('historyStatus');
    if (!rows || !status) return;
    const session = getGuestSession();
    if (!session || !session.token) {
        rows.innerHTML = '';
        status.textContent = 'Sign in to view your booking history.';
        return;
    }
    status.textContent = 'Loading your bookings...';
    rows.innerHTML = '';
    try {
        const reservations = await fetchJson(`${API}/reservations/me`, {}, 'guest');
        status.textContent = reservations.length
            ? `${reservations.length} booking request(s)`
            : 'No booking history yet.';
        if (!reservations.length) {
            rows.innerHTML = '<tr><td colspan="6" class="px-4 py-6 text-center text-slate-500">No booking history yet.</td></tr>';
            return;
        }
        rows.innerHTML = '';
        reservations.forEach((r) => {
            const tr = document.createElement('tr');
            tr.className = 'hover:bg-slate-50 dark:hover:bg-slate-800/40';
            const canCancel = r.status === 'PENDING';
            tr.innerHTML = `
        <td class="px-4 py-3 font-mono text-xs">${r.id}</td>
        <td class="px-4 py-3">${r.roomNumber}</td>
        <td class="px-4 py-3 text-xs">${r.checkIn} → ${r.checkOut}</td>
        <td class="px-4 py-3"><span class="${reservationStatusBadge(r.status)}">${r.status}</span></td>
        <td class="px-4 py-3 text-right font-semibold">${formatCurrency(r.totalPrice)}</td>
        <td class="px-4 py-3 text-right">${canCancel ? '<button type="button" data-cancel-id="' + r.id + '" class="rounded bg-rose-600 px-2 py-1 text-xs font-semibold text-white hover:bg-rose-500">Reject</button>' : ''}</td>`;
            rows.appendChild(tr);
        });
        rows.querySelectorAll('[data-cancel-id]').forEach((btn) => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-cancel-id');
                status.textContent = `Cancelling request #${id}...`;
                try {
                    await fetchJson(`${API}/reservations/${id}/cancel`, { method: 'POST', body: '{}' }, 'guest');
                    await loadGuestHistory();
                } catch (err) {
                    status.textContent = err.message;
                }
            });
        });
    } catch (err) {
        status.textContent = err.message;
        rows.innerHTML = '<tr><td colspan="6" class="px-4 py-6 text-center text-slate-500">Could not load booking history.</td></tr>';
    }
}

async function loadFavoritesPage() {
    const rows = document.getElementById('favoritesRows');
    const status = document.getElementById('favoritesStatus');
    if (!rows || !status) return;
    const ids = getFavoriteIds();
    if (!ids.length) {
        status.textContent = 'No favorite rooms yet.';
        rows.innerHTML = '<tr><td colspan="5" class="px-4 py-6 text-center text-slate-500">No favorites yet.</td></tr>';
        return;
    }
    rows.innerHTML = '';
    status.textContent = 'Loading favorite rooms...';
    try {
        const rooms = await fetchJson(`${API}/rooms`, {}, 'none');
        const favorites = rooms.filter((r) => ids.includes(Number(r.id)));
        status.textContent = favorites.length ? `${favorites.length} favorite room(s)` : 'No favorite rooms found.';
        if (!favorites.length) {
            rows.innerHTML = '<tr><td colspan="5" class="px-4 py-6 text-center text-slate-500">No favorites yet.</td></tr>';
            return;
        }
        favorites.forEach((room) => {
            const tr = document.createElement('tr');
            tr.className = 'hover:bg-slate-50 dark:hover:bg-slate-800/40';
            tr.innerHTML = `
        <td class="px-4 py-3 font-medium">${room.roomNumber}</td>
        <td class="px-4 py-3">${room.roomType}</td>
        <td class="px-4 py-3">${room.maxGuests}</td>
        <td class="px-4 py-3 text-right font-semibold">${formatCurrency(room.baseRatePerNight)}</td>
        <td class="px-4 py-3 text-right"><button type="button" data-unfav="${room.id}" class="rounded bg-rose-600 px-2 py-1 text-xs font-semibold text-white hover:bg-rose-500">Remove</button></td>`;
            rows.appendChild(tr);
        });
        rows.querySelectorAll('[data-unfav]').forEach((btn) => {
            btn.addEventListener('click', async () => {
                toggleFavoriteRoom(btn.getAttribute('data-unfav'));
                await loadFavoritesPage();
            });
        });
    } catch (err) {
        status.textContent = err.message;
    }
}

async function loadAdminTable() {
    const body = document.getElementById('resBody');
    const status = document.getElementById('adminStatus');
    if (!body) return;

    status.textContent = 'Loading reservations…';
    body.innerHTML = '';
    try {
        const rows = await fetchJson(`${API}/reservations`, {}, 'admin');
        status.textContent = `${rows.length} reservation(s) (all statuses).`;
        const statReservations = document.getElementById('statReservations');
        if (statReservations) {
            statReservations.textContent = String(rows.length);
        }
        if (rows.length === 0) {
            body.innerHTML = '<tr><td colspan="7" class="px-4 py-6 text-center text-slate-500">No reservations yet.</td></tr>';
            return;
        }
        rows.forEach((r) => {
            const tr = document.createElement('tr');
            tr.className = 'hover:bg-slate-50 dark:hover:bg-slate-800/40';
            const statusClass = reservationStatusBadge(r.status);
            const canRemove = r.status === 'CONFIRMED';
            tr.innerHTML = `
        <td class="px-4 py-3 font-mono text-xs">${r.id}</td>
        <td class="px-4 py-3">
          <div class="font-medium">${r.guestName}</div>
          <div class="text-xs text-slate-500 dark:text-slate-400">${r.guestEmail}</div>
        </td>
        <td class="px-4 py-3">${r.roomNumber}</td>
        <td class="px-4 py-3 text-xs">${r.checkIn} → ${r.checkOut}</td>
        <td class="px-4 py-3"><span class="${statusClass}">${r.status}</span></td>
        <td class="px-4 py-3 text-right font-semibold">${formatCurrency(r.totalPrice)}</td>
        <td class="px-4 py-3 text-right">${canRemove ? `<button type="button" data-del-res="${r.id}" class="rounded bg-rose-600 px-2 py-1 text-xs font-semibold text-white hover:bg-rose-500">Remove</button>` : ''}</td>`;
            body.appendChild(tr);
        });
        body.querySelectorAll('[data-del-res]').forEach((btn) => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-del-res');
                if (!confirm(`Remove confirmed reservation #${id}?`)) return;
                try {
                    await fetchJson(`${API}/admin/reservations/${id}`, { method: 'DELETE' }, 'admin');
                    await loadAdminTable();
                } catch (err) {
                    status.textContent = err.message;
                }
            });
        });
    } catch (err) {
        status.textContent = err.message;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('searchForm')) {
        initGuestPage();
    }
});
