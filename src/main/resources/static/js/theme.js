/* PGEO Theme Manager */
(function () {
  const KEY = 'pgeo-theme';
  const DARK = 'dark';
  const LIGHT = 'light';

  function getPreferred() {
    const saved = localStorage.getItem(KEY);
    if (saved) return saved;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? DARK : LIGHT;
  }

  function apply(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(KEY, theme);
    document.querySelectorAll('.btn-theme').forEach(btn => {
      btn.textContent = theme === DARK ? '☀️' : '🌙';
      btn.title = theme === DARK ? 'Tema claro' : 'Tema escuro';
    });
  }

  function toggle() {
    const current = document.documentElement.getAttribute('data-theme') || LIGHT;
    apply(current === DARK ? LIGHT : DARK);
  }

  // Apply immediately (before paint) to prevent flash
  apply(getPreferred());

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.btn-theme').forEach(btn => {
      btn.addEventListener('click', toggle);
    });
  });

  // Mobile navbar toggle
  document.addEventListener('DOMContentLoaded', function () {
    const toggle = document.getElementById('navToggle');
    const menu   = document.getElementById('navLinks');
    if (toggle && menu) {
      toggle.addEventListener('click', () => menu.classList.toggle('open'));
      document.addEventListener('click', e => {
        if (!toggle.contains(e.target) && !menu.contains(e.target)) {
          menu.classList.remove('open');
        }
      });
    }

    // Auto-highlight active nav link
    const path = window.location.pathname;
    document.querySelectorAll('.nav-link').forEach(link => {
      const href = link.getAttribute('href');
      if (!href) return;
      if (href === '/' && path === '/') { link.classList.add('active'); return; }
      if (href !== '/' && path.startsWith(href)) link.classList.add('active');
    });
  });

  // Toast helper (global)
  window.showToast = function (msg, type = 'success', duration = 3000) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = msg;
    container.appendChild(toast);
    requestAnimationFrame(() => { requestAnimationFrame(() => toast.classList.add('show')); });
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, duration);
  };
})();
