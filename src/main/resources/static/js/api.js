/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * api.js — Utilitário global para chamadas fetch com CSRF e tratamento de sessão.
 */
(function () {
    function getCsrfHeaders() {
        var token  = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) {
            var h = {};
            h[header.content] = token.content;
            return h;
        }
        return {};
    }

    function handleSessionExpiry(response) {
        if (response.status === 401 || response.status === 403) {
            if (window.showToast) {
                window.showToast("Sessão expirada. Redirecionando para login...", "danger");
            }
            setTimeout(function () {
                window.location.href = "/login?expired=true";
            }, 1500);
            throw new Error("SESSION_EXPIRED");
        }
        return response;
    }

    window.pgeoFetch = function (url, options) {
        options = options || {};
        options.headers = options.headers || {};

        var csrf = getCsrfHeaders();
        for (var key in csrf) {
            if (csrf.hasOwnProperty(key)) options.headers[key] = csrf[key];
        }

        return fetch(url, options).then(handleSessionExpiry);
    };

    window.pgeoPost = function (url, body) {
        var csrf = getCsrfHeaders();
        var headers = { "Content-Type": "application/x-www-form-urlencoded" };
        for (var key in csrf) {
            if (csrf.hasOwnProperty(key)) headers[key] = csrf[key];
        }
        return fetch(url, { method: "POST", headers: headers, body: body })
            .then(handleSessionExpiry);
    };

    window.pgeoPostJson = function (url, data) {
        var csrf = getCsrfHeaders();
        var headers = { "Content-Type": "application/json" };
        for (var key in csrf) {
            if (csrf.hasOwnProperty(key)) headers[key] = csrf[key];
        }
        return fetch(url, { method: "POST", headers: headers, body: JSON.stringify(data) })
            .then(handleSessionExpiry);
    };
})();
