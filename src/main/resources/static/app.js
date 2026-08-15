/*
 * Frontend for the Spring Boot user API.
 * Served by Spring Boot itself from src/main/resources/static,
 * so it runs on the same origin as the API and needs no CORS setup.
 */

// Must match server.servlet.context-path in application.properties.
const API = "/api";
const ACCESS_KEY = "accessToken";
const REFRESH_KEY = "refreshToken";
const EMAIL_KEY = "userEmail";

// ---------------------------------------------------------------- helpers

const $ = (id) => document.getElementById(id);

function getAccessToken() {
    return localStorage.getItem(ACCESS_KEY);
}

function saveTokens(auth) {
    localStorage.setItem(ACCESS_KEY, auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
}

function clearTokens() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(EMAIL_KEY);
}

function log(line) {
    const el = $("log");
    if (!el) return;
    const time = new Date().toLocaleTimeString();
    el.textContent = `[${time}] ${line}\n` + el.textContent;
}

function showMsg(elId, text, kind) {
    const el = $(elId);
    el.textContent = text;
    el.className = "msg " + (kind || "");
}

/**
 * Pulls a readable message out of Spring's error response.
 * Validation failures carry an `errors` array; other failures carry `message`.
 */
function errorText(status, body) {
    if (body && Array.isArray(body.errors) && body.errors.length) {
        return body.errors.map((e) => e.defaultMessage).join(", ");
    }
    if (body && body.message) return body.message;
    if (status === 401) return "Not logged in, or token expired.";
    if (status === 403) return "You do not have permission for this action.";
    return "Request failed with status " + status;
}

// ---------------------------------------------------------------- api core

/**
 * fetch wrapper: attaches the bearer token, and on a 401 tries the
 * refresh-token endpoint once before giving up.
 */
async function api(path, options = {}, isRetry = false) {
    const headers = Object.assign({}, options.headers);

    if (options.body) headers["Content-Type"] = "application/json";

    const token = getAccessToken();
    if (token) headers["Authorization"] = "Bearer " + token;

    const res = await fetch(API + path, Object.assign({}, options, { headers }));
    const method = options.method || "GET";

    // 204 and empty bodies
    const raw = await res.text();
    let body = null;
    if (raw) {
        try {
            body = JSON.parse(raw);
        } catch (_) {
            body = raw;                      // plain text, e.g. "User deleted successfully"
        }
    }

    log(`${method} ${path} → ${res.status}`);

    if (res.status === 401 && !isRetry && localStorage.getItem(REFRESH_KEY)) {
        const refreshed = await tryRefresh();
        if (refreshed) return api(path, options, true);
    }

    if (!res.ok) {
        const err = new Error(errorText(res.status, body));
        err.status = res.status;
        throw err;
    }

    return body;
}

async function tryRefresh() {
    try {
        const res = await fetch(API + "/auth/refresh-token", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ refreshToken: localStorage.getItem(REFRESH_KEY) }),
        });
        log(`POST /auth/refresh-token → ${res.status}`);
        if (!res.ok) return false;
        saveTokens(await res.json());
        return true;
    } catch (_) {
        return false;
    }
}

// ---------------------------------------------------------------- api calls

const Api = {
    register: (name, email, password) =>
        api("/auth/register", { method: "POST", body: JSON.stringify({ name, email, password }) }),

    // NOTE: LoginRequest calls the field `username`, but it holds the email.
    login: (email, password) =>
        api("/auth/login", { method: "POST", body: JSON.stringify({ username: email, password }) }),

    refresh: () =>
        api("/auth/refresh-token", {
            method: "POST",
            body: JSON.stringify({ refreshToken: localStorage.getItem(REFRESH_KEY) }),
        }),

    listUsers: () => api("/users"),
    getUser: (id) => api("/users/" + id),
    createUser: (dto) => api("/users", { method: "POST", body: JSON.stringify(dto) }),
    updateUser: (id, dto) => api("/users/" + id, { method: "PUT", body: JSON.stringify(dto) }),
    deleteUser: (id) => api("/users/" + id, { method: "DELETE" }),
};

// ---------------------------------------------------------------- screens

function showApp() {
    $("auth-screen").classList.add("hidden");
    $("app-screen").classList.remove("hidden");
    $("who").textContent = localStorage.getItem(EMAIL_KEY) || "";
    loadUsers();
}

function showAuth() {
    $("app-screen").classList.add("hidden");
    $("auth-screen").classList.remove("hidden");
}

// ---------------------------------------------------------------- auth ui

document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
        tab.classList.add("active");

        const isLogin = tab.dataset.tab === "login";
        $("login-form").classList.toggle("hidden", !isLogin);
        $("register-form").classList.toggle("hidden", isLogin);
        showMsg("auth-msg", "");
    });
});

$("login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    showMsg("auth-msg", "Logging in…");
    try {
        const email = $("login-email").value.trim();
        const auth = await Api.login(email, $("login-password").value);
        saveTokens(auth);
        localStorage.setItem(EMAIL_KEY, email);
        showMsg("auth-msg", "");
        showApp();
    } catch (err) {
        showMsg("auth-msg", err.message, "error");
    }
});

$("register-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    showMsg("auth-msg", "Creating account…");
    try {
        const user = await Api.register(
            $("reg-name").value.trim(),
            $("reg-email").value.trim(),
            $("reg-password").value
        );
        showMsg("auth-msg", `Account created for ${user.email}. Now log in.`, "ok");
        document.querySelector('.tab[data-tab="login"]').click();
        $("login-email").value = user.email;
    } catch (err) {
        showMsg("auth-msg", err.message, "error");
    }
});

$("logout-btn").addEventListener("click", () => {
    clearTokens();
    showAuth();
});

$("refresh-token-btn").addEventListener("click", async () => {
    try {
        saveTokens(await Api.refresh());
        showMsg("app-msg", "Token refreshed.", "ok");
    } catch (err) {
        showMsg("app-msg", err.message, "error");
    }
});

// ---------------------------------------------------------------- users ui

function renderUsers(users) {
    const body = $("users-body");
    body.innerHTML = "";

    if (!users.length) {
        body.innerHTML = '<tr><td colspan="4" class="empty">No users yet.</td></tr>';
        return;
    }

    users.forEach((u) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${u.id}</td>
            <td>${escapeHtml(u.name)}</td>
            <td>${escapeHtml(u.email)}</td>
            <td class="right">
                <div class="actions">
                    <button class="btn small" data-edit="${u.id}">Edit</button>
                    <button class="btn small danger" data-del="${u.id}">Delete</button>
                </div>
            </td>`;
        body.appendChild(tr);
    });

    body.querySelectorAll("[data-edit]").forEach((b) =>
        b.addEventListener("click", () => startEdit(b.dataset.edit))
    );
    body.querySelectorAll("[data-del]").forEach((b) =>
        b.addEventListener("click", () => removeUser(b.dataset.del))
    );
}

function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, (c) =>
        ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])
    );
}

async function loadUsers() {
    try {
        renderUsers(await Api.listUsers());
        showMsg("app-msg", "");
    } catch (err) {
        $("users-body").innerHTML = `<tr><td colspan="4" class="empty">${escapeHtml(err.message)}</td></tr>`;
        if (err.status === 401) showAuth();
    }
}

async function startEdit(id) {
    try {
        const u = await Api.getUser(id);
        $("user-id").value = u.id;
        $("user-name").value = u.name;
        $("user-email").value = u.email;
        $("user-password").value = "";
        $("form-title").textContent = "Edit user #" + u.id;
        $("cancel-edit").classList.remove("hidden");
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
        showMsg("app-msg", err.message, "error");
    }
}

function resetForm() {
    $("user-id").value = "";
    $("user-form").reset();
    $("form-title").textContent = "Add user";
    $("cancel-edit").classList.add("hidden");
}

$("cancel-edit").addEventListener("click", resetForm);

$("user-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    const id = $("user-id").value;
    const dto = {
        name: $("user-name").value.trim(),
        email: $("user-email").value.trim(),
        password: $("user-password").value,
    };

    try {
        if (id) {
            await Api.updateUser(id, dto);
            showMsg("app-msg", `User #${id} updated.`, "ok");
        } else {
            const created = await Api.createUser(dto);
            showMsg("app-msg", `User #${created.id} created.`, "ok");
        }
        resetForm();
        loadUsers();
    } catch (err) {
        showMsg("app-msg", err.message, "error");
    }
});

async function removeUser(id) {
    if (!confirm("Delete user #" + id + "?")) return;
    try {
        await Api.deleteUser(id);
        showMsg("app-msg", `User #${id} deleted.`, "ok");
        loadUsers();
    } catch (err) {
        // DELETE is restricted to ROLE_ADMIN in SecurityConfig
        showMsg("app-msg", err.message, "error");
    }
}

$("reload-btn").addEventListener("click", loadUsers);

$("find-btn").addEventListener("click", async () => {
    const id = $("find-id").value;
    if (!id) return loadUsers();
    try {
        renderUsers([await Api.getUser(id)]);
        showMsg("app-msg", "");
    } catch (err) {
        $("users-body").innerHTML = `<tr><td colspan="4" class="empty">${escapeHtml(err.message)}</td></tr>`;
    }
});

$("clear-log").addEventListener("click", () => ($("log").textContent = ""));

// ---------------------------------------------------------------- boot

if (getAccessToken()) {
    showApp();
} else {
    showAuth();
}
