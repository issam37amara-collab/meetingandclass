// ── auth.js — shared JWT utilities ──────────────────────────────────────────
const AUTH_API = 'http://localhost:8080';

function getToken()    { return sessionStorage.getItem('jwt'); }
function getRole()     { return sessionStorage.getItem('role'); }
function getFullName() { return sessionStorage.getItem('fullName'); }
function getEmail()    { return sessionStorage.getItem('email'); }

function requireAuth(allowedRoles) {
  const token = getToken();
  const role  = getRole();
  if (!token || !role) { window.location.href = 'login.html'; return false; }
  if (allowedRoles && !allowedRoles.includes(role)) {
    window.location.href = 'unauthorized.html'; return false;
  }
  return true;
}

function logout() {
  sessionStorage.clear();
  window.location.href = 'login.html';
}

// Route the current user to their role-specific dashboard.
const ROLE_DASHBOARDS = {
  SUPER_ADMIN:          'super-admin-dashboard.html',
  DIRECTOR_OF_STUDIES:  'dos-dashboard.html',
  DIRECTOR_OF_INSTITUTE:'director-dashboard.html',
  HEAD_OF_DEPARTMENT:   'hod-dashboard.html',
  TEACHER:              'teacher-dashboard.html'
};
function goToDashboard() {
  const role = getRole();
  if (!role) { window.location.href = 'login.html'; return; }
  window.location.href = ROLE_DASHBOARDS[role] || 'login.html';
}

async function apiGet(path) {
  const res = await fetch(`${AUTH_API}${path}`, {
    headers: { 'Authorization': `Bearer ${getToken()}` }
  });
  if (res.status === 401) { logout(); return null; }
  return res.json();
}

async function apiPost(path, body) {
  const res = await fetch(`${AUTH_API}${path}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${getToken()}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (res.status === 401) { logout(); return null; }
  return res.json();
}

async function apiPut(path, body) {
  const res = await fetch(`${AUTH_API}${path}`, {
    method: 'PUT',
    headers: { 'Authorization': `Bearer ${getToken()}`, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 401) { logout(); return null; }
  return res.json();
}
