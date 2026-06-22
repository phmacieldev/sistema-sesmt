/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * modal-agendamento.js — Modal global de novo agendamento / edição.
 */
(function () {
    var _cache = { tipos: null, horarios: null };
    var _fpSangue  = null;
    var _fpClinico = null;

    function escH(s) {
        return String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }
    function fmtData(iso) {
        if (!iso) return "—";
        var p = iso.split("-");
        return p.length === 3 ? p[2] + "/" + p[1] + "/" + p[0] : iso;
    }
    function isWeekend(v) {
        if (!v) return false;
        var d = new Date(v + "T12:00:00");
        return d.getDay() === 0 || d.getDay() === 6;
    }
    function toast(msg, type) {
        if (window.showToast) window.showToast(msg, type);
    }
    function val(id, v) {
        var e = document.getElementById(id);
        if (e) e.value = v !== undefined ? (v || "") : "";
    }
    function hoje() {
        return new Date().toISOString().slice(0, 10);
    }

    function preencherSelect(id, items, valorAtual) {
        var sel = document.getElementById(id);
        if (!sel) return;
        sel.innerHTML = '<option value="">Selecione</option>'
            + items.map(function (v) {
                return '<option value="' + escH(v) + '"' + (v === valorAtual ? ' selected' : '') + '>' + escH(v) + '</option>';
            }).join("");
    }

    function carregarDados(cb) {
        if (_cache.tipos && _cache.horarios) { cb(); return; }
        Promise.all([
            fetch("/api/tipos-exame").then(function (r) { return r.json(); }),
            fetch("/api/horarios").then(function (r) { return r.json(); })
        ]).then(function (results) {
            _cache.tipos    = results[0];
            _cache.horarios = results[1];
            cb();
        }).catch(function () { toast("Erro ao carregar dados.", "danger"); });
    }

    function inicializarFlatpickr() {
        if (typeof flatpickr === "undefined") return;
        var fds = function (d) { return d.getDay() === 0 || d.getDay() === 6; };

        if (!_fpSangue) {
            var sangueEl = document.getElementById("mf-sangue");
            if (sangueEl) {
                _fpSangue = flatpickr(sangueEl, {
                    dateFormat: "Y-m-d",
                    minDate: "today",
                    disable: [fds],
                    locale: { firstDayOfWeek: 1 }
                });
            }
        }
        if (!_fpClinico) {
            var clinicoEl = document.getElementById("mf-clinico");
            if (clinicoEl) {
                _fpClinico = flatpickr(clinicoEl, {
                    dateFormat: "Y-m-d",
                    minDate: "today",
                    disable: [fds],
                    locale: { firstDayOfWeek: 1 }
                });
            }
        }
    }

    function setData(fpInstance, inputId, valor) {
        if (fpInstance) {
            fpInstance.setDate(valor || null);
        } else {
            val(inputId, valor || "");
        }
    }

    function abrirModal(modoEdicao, dados) {
        inicializarFlatpickr(); // garante inicialização mesmo se DOMContentLoaded já disparou
        carregarDados(function () {
            var form = document.getElementById("mf-form");
            if (!form) return;
            form.reset();
            if (_fpSangue)  _fpSangue.clear();
            if (_fpClinico) _fpClinico.clear();

            preencherSelect("mf-tipo",  _cache.tipos,    dados && dados.tipoExame || "");
            preencherSelect("mf-hora",  _cache.horarios, dados && dados.hora      || "");

            val("mf-id",       dados && dados.id        || "");
            val("mf-matricula",dados && dados.matricula  || "");
            val("mf-nome",     dados && dados.nome       || "");
            val("mf-setor",    dados && dados.setor      || "");
            val("mf-funcao",   dados && dados.funcao     || "");
            val("mf-obs",      dados && dados.observacoes|| "");

            setData(_fpSangue,  "mf-sangue",  dados && dados.dataSangue  || "");
            setData(_fpClinico, "mf-clinico", dados && dados.dataClinico || "");

            // fallback min para quando flatpickr não carregou
            // Em edição, libera datas passadas para exibir o valor existente
            if (_fpSangue)  _fpSangue.set("minDate",  modoEdicao ? null : "today");
            if (_fpClinico) _fpClinico.set("minDate", modoEdicao ? null : "today");

            if (!_fpClinico) {
                var clinicoEl = document.getElementById("mf-clinico");
                if (clinicoEl) clinicoEl.min = modoEdicao ? "" : hoje();
            }

            document.getElementById("mf-titulo").textContent = modoEdicao ? "Editar Agendamento" : "Novo Agendamento";
            var btnSalvar = document.getElementById("mf-btn-salvar");
            btnSalvar.querySelector(".btn-text").textContent = modoEdicao ? "Salvar Alterações" : "Agendar";
            btnSalvar.classList.remove("btn-loading");
            btnSalvar.disabled = false;
            document.getElementById("mf-aviso-sangue").classList.add("d-none");
            document.getElementById("mf-sugestoes").innerHTML = "";
            document.getElementById("mf-sugestoes-matricula").innerHTML = "";
            document.getElementById("modal-form-ag").classList.add("show");
        });
    }

    window.abrirModalFormNovo = function (matricula) {
        if (matricula) {
            fetch("/buscar_funcionario/" + encodeURIComponent(matricula))
                .then(function (r) { return r.json(); })
                .then(function (f) {
                    abrirModal(false, f.encontrado ? {
                        matricula: matricula,
                        nome:      f.nome,
                        setor:     f.setor,
                        funcao:    f.funcao
                    } : { matricula: matricula });
                })
                .catch(function () { abrirModal(false, { matricula: matricula }); });
        } else {
            abrirModal(false, null);
        }
    };

    window.abrirModalFormEditar = function (id) {
        fetch("/agendamento/" + id + "/json")
            .then(function (r) { return r.json(); })
            .then(function (ag) { abrirModal(true, ag); })
            .catch(function () { toast("Erro ao carregar agendamento.", "danger"); });
    };

    window.fecharModalForm = function () {
        var m = document.getElementById("modal-form-ag");
        if (m) m.classList.remove("show");
    };

    // Event delegation
    document.addEventListener("click", function (e) {
        var btnNovo = e.target.closest("[data-modal-novo]");
        if (btnNovo) {
            e.preventDefault();
            window.abrirModalFormNovo(btnNovo.dataset.matricula || null);
            return;
        }
        var btnEditar = e.target.closest("[data-modal-editar]");
        if (btnEditar) {
            e.preventDefault();
            var id = btnEditar.dataset.id || window._modalAgId;
            if (id) window.abrirModalFormEditar(id);
            return;
        }
        var btnFechar = e.target.closest("[data-modal-fechar]");
        if (btnFechar) {
            e.preventDefault();
            window.fecharModalForm();
        }
    });

    document.addEventListener("DOMContentLoaded", function () {
        inicializarFlatpickr();

        var overlay = document.getElementById("modal-form-ag");
        if (overlay) {
            overlay.addEventListener("click", function (e) {
                if (e.target === overlay) overlay.classList.remove("show");
            });
        }

        var btnConfirmCancelar = document.getElementById("mf-btn-confirm-cancelar");
        if (btnConfirmCancelar) {
            btnConfirmCancelar.addEventListener("click", function () {
                document.getElementById("mf-modal-confirm").classList.remove("show");
            });
        }

        // Modal de duplicado
        var btnDupCancelar = document.getElementById("mf-btn-dup-cancelar");
        if (btnDupCancelar) {
            btnDupCancelar.addEventListener("click", function () {
                document.getElementById("mf-modal-dup").classList.remove("show");
            });
        }

        var form = document.getElementById("mf-form");
        if (!form) return;

        // Autocomplete nome
        var inputNome = document.getElementById("mf-nome");
        var debNome;
        if (inputNome) {
            inputNome.addEventListener("input", function () {
                clearTimeout(debNome);
                var q   = this.value.trim();
                var box = document.getElementById("mf-sugestoes");
                if (q.length < 2) { if (box) { box.innerHTML = ""; box.style.display = "none"; } return; }
                debNome = setTimeout(function () {
                    fetch("/buscar_funcionarios_nome?q=" + encodeURIComponent(q))
                        .then(function (r) { return r.json(); })
                        .then(function (d) {
                            if (!box) return;
                            box.innerHTML = d.map(function (f) {
                                return '<div class="suggestion-item"'
                                    + ' data-matricula="' + escH(f.matricula) + '" data-nome="' + escH(f.nome) + '"'
                                    + ' data-setor="' + escH(f.setor) + '" data-funcao="' + escH(f.funcao) + '">'
                                    + escH(f.nome) + '<div class="suggestion-sub">' + escH(f.setor) + ' · ' + escH(f.funcao) + '</div></div>';
                            }).join("");
                            box.style.display = d.length ? "block" : "none";
                        });
                }, 200);
            });
        }

        // Autocomplete matrícula
        var inputMat = document.getElementById("mf-matricula");
        var debMat;
        if (inputMat) {
            inputMat.addEventListener("keypress", function (e) {
                if (!/\d/.test(e.key) && !e.ctrlKey && !e.metaKey) e.preventDefault();
            });
            inputMat.addEventListener("input", function () {
                this.value = this.value.replace(/\D/g, "");
                clearTimeout(debMat);
                var q   = this.value.trim();
                var box = document.getElementById("mf-sugestoes-matricula");
                if (q.length < 1) { if (box) { box.innerHTML = ""; box.style.display = "none"; } return; }
                debMat = setTimeout(function () {
                    fetch("/buscar_funcionarios_matricula?q=" + encodeURIComponent(q))
                        .then(function (r) { return r.json(); })
                        .then(function (d) {
                            if (!box) return;
                            box.innerHTML = d.map(function (f) {
                                return '<div class="suggestion-item"'
                                    + ' data-matricula="' + escH(f.matricula) + '" data-nome="' + escH(f.nome) + '"'
                                    + ' data-setor="' + escH(f.setor) + '" data-funcao="' + escH(f.funcao) + '">'
                                    + escH(f.matricula) + ' — ' + escH(f.nome)
                                    + '<div class="suggestion-sub">' + escH(f.setor) + ' · ' + escH(f.funcao) + '</div></div>';
                            }).join("");
                            box.style.display = d.length ? "block" : "none";
                        });
                }, 200);
            });
        }

        // Clique nas sugestões
        document.addEventListener("click", function (e) {
            var item = e.target.closest("#modal-form-ag .suggestion-item");
            if (item) {
                val("mf-matricula", item.dataset.matricula);
                val("mf-nome",      item.dataset.nome);
                val("mf-setor",     item.dataset.setor);
                val("mf-funcao",    item.dataset.funcao);
                ["mf-sugestoes", "mf-sugestoes-matricula"].forEach(function (id) {
                    var b = document.getElementById(id);
                    if (b) { b.innerHTML = ""; b.style.display = "none"; }
                });
                return;
            }
            if (!e.target.closest("#mf-nome")) {
                var b = document.getElementById("mf-sugestoes");
                if (b) { b.innerHTML = ""; b.style.display = "none"; }
            }
            if (!e.target.closest("#mf-matricula")) {
                var b2 = document.getElementById("mf-sugestoes-matricula");
                if (b2) { b2.innerHTML = ""; b2.style.display = "none"; }
            }
        });

        // Validação limite de sangue (flatpickr já impede fins de semana visualmente)
        var sangueEl = document.getElementById("mf-sangue");
        if (sangueEl) {
            sangueEl.addEventListener("change", function () {
                if (!this.value) return;
                fetch("/verificar_limite_sangue?data=" + this.value)
                    .then(function (r) { return r.json(); })
                    .then(function (resp) {
                        document.getElementById("mf-aviso-sangue").classList.toggle("d-none", !resp.atingido);
                    });
            });
        }

        // Submit
        form.addEventListener("submit", function (e) {
            e.preventDefault();
            var id      = document.getElementById("mf-id").value;
            var nome    = (document.getElementById("mf-nome").value    || "").trim();
            var tipo    = document.getElementById("mf-tipo").value;
            var clinico = (document.getElementById("mf-clinico").value || "").trim();
            var hora    = document.getElementById("mf-hora").value;
            var sangue  = (document.getElementById("mf-sangue").value  || "").trim();

            if (!nome || !tipo || !clinico || !hora) {
                toast("Preencha todos os campos obrigatórios.", "danger");
                return;
            }
            // Segurança extra caso flatpickr não tenha carregado
            if (clinico < hoje()) {
                toast("A data do exame clínico não pode ser anterior a hoje.", "danger");
                return;
            }
            if (isWeekend(clinico)) {
                toast("Exame clínico não pode ser em fim de semana.", "danger");
                return;
            }
            if (sangue && isWeekend(sangue)) {
                toast("Exame de sangue não pode ser em fim de semana.", "danger");
                return;
            }

            var txt = "Funcionário: " + nome
                + "\nExame: " + tipo
                + (sangue  ? "\nSangue: "  + fmtData(sangue)  : "")
                + "\nClínico: " + fmtData(clinico)
                + "\nHorário: " + hora;

            document.getElementById("mf-confirm-text").textContent = txt;
            document.getElementById("mf-modal-confirm").classList.add("show");

            document.getElementById("mf-btn-confirm").onclick = function () {
                var btnConfirm = document.getElementById("mf-btn-confirm");
                btnConfirm.classList.add("btn-loading");
                btnConfirm.disabled = true;

                var fd   = new FormData(form);
                var csrf = document.querySelector("[name=_csrf]");
                var url  = id ? "/editar_agendamento/" + id : "/agendar";

                fetch(url, {
                    method: "POST",
                    body: new URLSearchParams(fd),
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                        "X-CSRF-TOKEN": csrf ? csrf.value : ""
                    }
                })
                .then(function (r) { return r.json(); })
                .then(function (resp) {
                    btnConfirm.classList.remove("btn-loading");
                    btnConfirm.disabled = false;
                    document.getElementById("mf-modal-confirm").classList.remove("show");

                    if (resp.duplicado) {
                        var dupTexto = document.getElementById("mf-dup-texto");
                        if (dupTexto) dupTexto.textContent = resp.mensagem || "Já existe um agendamento para este funcionário.";
                        document.getElementById("mf-modal-dup").classList.add("show");

                        document.getElementById("mf-btn-dup-editar").onclick = function () {
                            document.getElementById("mf-modal-dup").classList.remove("show");
                            fetch("/agendamento/" + resp.id + "/json")
                                .then(function (r) { return r.json(); })
                                .then(function (ag) { abrirModal(true, ag); })
                                .catch(function () { toast("Erro ao carregar agendamento.", "danger"); });
                        };
                        return;
                    }
                    if (resp.erro) { toast(resp.mensagem, "danger"); return; }
                    window.fecharModalForm();
                    toast(id ? "Agendamento atualizado!" : "Agendamento realizado!", "success");
                    if (window.pgeoCalendar) window.pgeoCalendar.refetchEvents();
                    if (typeof carregarDashboard === "function") carregarDashboard();
                })
                .catch(function () {
                    btnConfirm.classList.remove("btn-loading");
                    btnConfirm.disabled = false;
                    document.getElementById("mf-modal-confirm").classList.remove("show");
                    toast("Erro ao comunicar com o servidor.", "danger");
                });
            };
        });
    });
})();
