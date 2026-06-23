/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * modal-atestado.js — Modal global de novo atestado / edição.
 * Disponível em todas as páginas via footer.
 */
(function () {
    var _tiposCache = null;

    function escH(s) {
        return String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }
    function val(id, v) {
        var e = document.getElementById(id);
        if (e) e.value = v !== undefined ? (v || "") : "";
    }
    function toast(msg, type) {
        if (window.showToast) window.showToast(msg, type);
    }

    function carregarTipos(cb) {
        if (_tiposCache) { cb(); return; }
        fetch("/atestados/api/tipos")
            .then(function (r) { return r.json(); })
            .then(function (data) { _tiposCache = data; cb(); })
            .catch(function () { toast("Erro ao carregar tipos de atestado.", "danger"); });
    }

    function preencherTipos(valorAtual) {
        var sel = document.getElementById("mat-tipo");
        if (!sel || !_tiposCache) return;
        sel.innerHTML = '<option value="">Selecione</option>' +
            _tiposCache.map(function (t) {
                return '<option value="' + escH(t.value) + '"' +
                    (t.value === valorAtual ? ' selected' : '') + '>' +
                    escH(t.label) + '</option>';
            }).join("");
    }

    function abrirModalAtestado(modoEdicao, dados) {
        carregarTipos(function () {
            var form = document.getElementById("mat-form");
            if (!form) return;
            form.reset();

            preencherTipos(dados && dados.tipo || "");
            val("mat-id",              dados && dados.id              || "");
            val("mat-funcionario-id",  dados && dados.funcionarioId   || "");
            val("mat-funcionario-nome",dados && dados.funcionarioNome || "");
            val("mat-data",            dados && dados.dataAfastamento || "");
            val("mat-dias",            dados && dados.diasAfastamento || "");
            val("mat-cid",             dados && dados.cid             || "");
            val("mat-medico-nome",     dados && dados.medicoNome      || "");
            val("mat-medico-crm",      dados && dados.medicoCrm       || "");

            var sugestoes = document.getElementById("mat-sugestoes");
            if (sugestoes) { sugestoes.innerHTML = ""; sugestoes.style.display = "none"; }

            document.getElementById("mat-titulo").textContent = modoEdicao ? "Editar Atestado" : "Lançar Atestado";
            var btnSalvar = document.getElementById("mat-btn-salvar");
            btnSalvar.querySelector(".btn-text").textContent = modoEdicao ? "Salvar Alterações" : "Lançar";
            btnSalvar.classList.remove("btn-loading");
            btnSalvar.disabled = false;
            document.getElementById("modal-form-atestado").classList.add("show");
        });
    }

    window.abrirModalAtestadoNovo = function (funcionarioId, funcionarioNome) {
        abrirModalAtestado(false, funcionarioId
            ? { funcionarioId: funcionarioId, funcionarioNome: funcionarioNome || "" }
            : null);
    };

    window.abrirModalAtestadoEditar = function (id) {
        fetch("/atestados/" + id + "/json")
            .then(function (r) { return r.json(); })
            .then(function (dados) { abrirModalAtestado(true, dados); })
            .catch(function () { toast("Erro ao carregar atestado.", "danger"); });
    };

    window.fecharModalAtestado = function () {
        var m = document.getElementById("modal-form-atestado");
        if (m) m.classList.remove("show");
    };

    // Event delegation
    document.addEventListener("click", function (e) {
        var btnNovo = e.target.closest("[data-atestado-novo]");
        if (btnNovo) {
            e.preventDefault();
            window.abrirModalAtestadoNovo(
                btnNovo.dataset.funcionarioId || null,
                btnNovo.dataset.funcionarioNome || ""
            );
            return;
        }
        var btnEditar = e.target.closest("[data-atestado-editar]");
        if (btnEditar) {
            e.preventDefault();
            var id = btnEditar.dataset.id;
            if (id) window.abrirModalAtestadoEditar(id);
            return;
        }
        var btnFechar = e.target.closest("[data-atestado-fechar]");
        if (btnFechar) {
            e.preventDefault();
            window.fecharModalAtestado();
        }
    });

    document.addEventListener("DOMContentLoaded", function () {
        // Fechar ao clicar fora
        var overlay = document.getElementById("modal-form-atestado");
        if (overlay) {
            overlay.addEventListener("click", function (e) {
                if (e.target === overlay) overlay.classList.remove("show");
            });
        }

        var form = document.getElementById("mat-form");
        if (!form) return;

        // Autocomplete funcionário
        var inputNome = document.getElementById("mat-funcionario-nome");
        var hiddenId  = document.getElementById("mat-funcionario-id");
        var sugestoes = document.getElementById("mat-sugestoes");
        var deb;

        if (inputNome) {
            inputNome.addEventListener("input", function () {
                clearTimeout(deb);
                var q = this.value.trim();
                if (q.length < 2) {
                    if (sugestoes) { sugestoes.innerHTML = ""; sugestoes.style.display = "none"; }
                    return;
                }
                deb = setTimeout(function () {
                    fetch("/buscar_funcionarios_nome?q=" + encodeURIComponent(q))
                        .then(function (r) { return r.json(); })
                        .then(function (lista) {
                            if (!sugestoes) return;
                            sugestoes.innerHTML = lista.map(function (f) {
                                return '<div class="suggestion-item"'
                                    + ' data-id="' + escH(f.id) + '" data-nome="' + escH(f.nome) + '">'
                                    + escH(f.nome)
                                    + '<div class="suggestion-sub">'
                                    + escH(f.setor || "") + " · " + escH(f.matricula || "")
                                    + '</div></div>';
                            }).join("");
                            sugestoes.style.display = lista.length ? "block" : "none";
                        });
                }, 200);
            });
        }

        // Clique nas sugestões
        document.addEventListener("click", function (e) {
            var item = e.target.closest("#modal-form-atestado .suggestion-item");
            if (item) {
                if (hiddenId)  hiddenId.value  = item.dataset.id;
                if (inputNome) inputNome.value  = item.dataset.nome;
                if (sugestoes) { sugestoes.innerHTML = ""; sugestoes.style.display = "none"; }
                return;
            }
            if (inputNome && !e.target.closest("#mat-funcionario-nome")) {
                if (sugestoes) { sugestoes.innerHTML = ""; sugestoes.style.display = "none"; }
            }
        });

        // Submit
        form.addEventListener("submit", function (e) {
            e.preventDefault();
            var id     = document.getElementById("mat-id").value;
            var funcId = document.getElementById("mat-funcionario-id").value;
            var data   = document.getElementById("mat-data").value;
            var dias   = document.getElementById("mat-dias").value;
            var tipo   = document.getElementById("mat-tipo").value;

            if (!funcId || !data || !dias || !tipo) {
                toast("Preencha todos os campos obrigatórios.", "danger");
                return;
            }

            var btnSalvar = document.getElementById("mat-btn-salvar");
            btnSalvar.classList.add("btn-loading");
            btnSalvar.disabled = true;

            var url = id ? "/atestados/" + id + "/editar/modal" : "/atestados/novo/modal";
            var fd  = new FormData(form);

            pgeoPost(url, new URLSearchParams(fd))
            .then(function (r) { return r.json(); })
            .then(function (resp) {
                btnSalvar.classList.remove("btn-loading");
                btnSalvar.disabled = false;
                if (!resp.ok) { toast(resp.mensagem || "Erro ao salvar.", "danger"); return; }
                window.fecharModalAtestado();
                toast(id ? "Atestado atualizado!" : "Atestado lançado!", "success");
                recarregarPagina();
            })
            .catch(function () {
                btnSalvar.classList.remove("btn-loading");
                btnSalvar.disabled = false;
                toast("Erro ao comunicar com o servidor.", "danger");
            });
        });
    });

    function recarregarPagina() {
        // Tenta atualizar só a tabela via AJAX; se não encontrar, faz reload
        var wrap = document.getElementById("tabelaWrap");
        if (!wrap) { setTimeout(function () { window.location.reload(); }, 600); return; }
        fetch(window.location.href)
            .then(function (r) { return r.text(); })
            .then(function (html) {
                var tmp = document.createElement("div");
                tmp.innerHTML = html;
                // Atualiza tabela
                var novaTabela = tmp.querySelector("#tabelaWrap");
                if (novaTabela) wrap.innerHTML = novaTabela.innerHTML;
                // Atualiza resumo semanal (stats)
                var novoResumo = tmp.querySelector("#resumoSemana");
                var resumo     = document.getElementById("resumoSemana");
                if (novoResumo && resumo) resumo.innerHTML = novoResumo.innerHTML;
                // Atualiza totais por setor/tipo
                var novosTotais = tmp.querySelector("#totaisWrap");
                var totais      = document.getElementById("totaisWrap");
                if (novosTotais && totais) totais.innerHTML = novosTotais.innerHTML;
            })
            .catch(function () { window.location.reload(); });
    }
})();
