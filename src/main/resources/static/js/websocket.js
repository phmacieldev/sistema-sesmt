/**
 * websocket.js — Notificações em tempo real sem interromper o usuário
 *
 * Estratégia:
 *   - Notificações chegam via WebSocket
 *   - A tabela NÃO é recarregada automaticamente
 *   - Um banner aparece no topo: "X atualização(ões) pendente(s) — Atualizar agora"
 *   - O usuário clica quando terminar o que está fazendo
 *   - Exceção: ASO_STATUS (marcar enviado/recebido) aplica inline sem recarregar
 *
 * Detecção de "usuário ocupado":
 *   - Input focado em qualquer campo
 *   - Modal aberto
 *   - Mouse se movendo (último movimento < 3s)
 *
 * Nesses casos o banner fica guardado mas não pisca nem distrai.
 */

(function () {

    // ── Estado ──────────────────────────────────────────────────────
    let pendentes = [];          // notificações ainda não aplicadas
    let bannerVisivel = false;
    let ultimoMovimento = Date.now();

    // ── Conexão WebSocket ────────────────────────────────────────────
    const socket = new SockJS('/ws');
    const stomp  = Stomp.over(socket);
    stomp.debug  = null; // silencia logs no console

    stomp.connect({}, function () {
        console.log('[PGEO] WebSocket conectado ✓');

        stomp.subscribe('/topic/agendamentos', function (msg) {
            const notif = JSON.parse(msg.body);
            receberNotificacao(notif);
        });

    }, function (erro) {
        // Desconectou — tenta reconectar silenciosamente após 10s
        console.warn('[PGEO] WebSocket desconectado. Reconectando em 10s...');
        setTimeout(conectar, 10000);
    });

    // ── Recebimento ──────────────────────────────────────────────────

    function receberNotificacao(notif) {

        // ASO_STATUS: aplica diretamente na linha da tabela (não exige reload)
        // O usuário que marcou já viu o resultado imediato — outros precisam ver
        if (notif.acao === 'ASO_STATUS' || notif.acao === 'ASO_ENVIADO' || notif.acao === 'ASO_RECEBIDO') {
            exibirToastLeve(notif);
            return;
        }

        // Acumula a notificação na fila de pendentes
        pendentes.push(notif);

        // Atualiza ou exibe o banner
        atualizarBanner();

        // Toast leve apenas para informar — não recarrega nada
        exibirToastLeve(notif);
    }

    // ── Banner "Atualizar agora" ──────────────────────────────────────

    function atualizarBanner() {
        let banner = document.getElementById('pgeo-banner-atualizacao');

        if (!banner) {
            banner = document.createElement('div');
            banner.id = 'pgeo-banner-atualizacao';
            banner.style.cssText = `
                position: fixed;
                top: 0; left: 0; right: 0;
                z-index: 10000;
                background: #2980b9;
                color: white;
                padding: 10px 20px;
                display: flex;
                align-items: center;
                justify-content: space-between;
                font-family: Arial, sans-serif;
                font-size: 14px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.25);
                transform: translateY(-100%);
                transition: transform 0.3s ease;
            `;

            banner.innerHTML = `
                <span id="pgeo-banner-texto">🔄 Novas atualizações disponíveis</span>
                <div style="display:flex; gap:10px; align-items:center;">
                    <button id="pgeo-btn-atualizar" style="
                        background: white; color: #2980b9; border: none;
                        padding: 6px 16px; border-radius: 6px; cursor: pointer;
                        font-weight: bold; font-size: 13px;
                    ">Atualizar agora</button>
                    <button id="pgeo-btn-ignorar" style="
                        background: transparent; color: rgba(255,255,255,0.8);
                        border: 1px solid rgba(255,255,255,0.4);
                        padding: 6px 12px; border-radius: 6px; cursor: pointer;
                        font-size: 13px;
                    ">Ignorar</button>
                </div>
            `;

            document.body.prepend(banner);

            document.getElementById('pgeo-btn-atualizar').addEventListener('click', function () {
                aplicarAtualizacoes();
            });

            document.getElementById('pgeo-btn-ignorar').addEventListener('click', function () {
                fecharBanner();
                pendentes = []; // descarta as notificações pendentes
            });
        }

        // Atualiza o texto com a contagem
        const qtd = pendentes.length;
        const texto = qtd === 1
            ? `🔄 1 atualização pendente`
            : `🔄 ${qtd} atualizações pendentes`;
        document.getElementById('pgeo-banner-texto').textContent = texto;

        // Exibe o banner (animação)
        if (!bannerVisivel) {
            requestAnimationFrame(() => {
                banner.style.transform = 'translateY(0)';
                bannerVisivel = true;
            });
        }
    }

    function fecharBanner() {
        const banner = document.getElementById('pgeo-banner-atualizacao');
        if (banner) {
            banner.style.transform = 'translateY(-100%)';
            setTimeout(() => {
                banner.remove();
                bannerVisivel = false;
            }, 300);
        }
    }

    // ── Aplicar atualizações (quando o usuário clicar) ────────────────

    function aplicarAtualizacoes() {
        fecharBanner();

        // Processa as notificações acumuladas
        const copia = [...pendentes];
        pendentes = [];

        // Verifica se alguma foi EXCLUIR — remove as linhas visualmente
        copia.forEach(notif => {
            if (notif.acao === 'EXCLUIR' && notif.id) {
                const linha = document.querySelector(`tr[data-id="${notif.id}"]`);
                if (linha) {
                    linha.style.transition = 'opacity 0.4s';
                    linha.style.opacity = '0';
                    setTimeout(() => linha.remove(), 400);
                }
            }
        });

        // Recarrega a tabela com os filtros atuais (mantém os filtros do usuário)
        if (typeof carregarDados === 'function') {
            carregarDados();
        }

        // Toast de confirmação
        exibirToastLeve({ acao: 'OK', mensagem: 'Tabela atualizada com sucesso.' });
    }

    // ── Toast leve (só informa, não interfere) ────────────────────────

    function exibirToastLeve(notif) {
        // Remove toasts anteriores do mesmo tipo para não acumular
        const anterior = document.getElementById('pgeo-toast-leve');
        if (anterior) anterior.remove();

        const icones = {
            CRIAR: '📅', EDITAR: '✏️', EXCLUIR: '🗑️',
            ASO_STATUS: '📋', ASO_ENVIADO: '📤', ASO_RECEBIDO: '✅', OK: '✓'
        };
        const icone = icones[notif.acao] || '🔔';

        const toast = document.createElement('div');
        toast.id = 'pgeo-toast-leve';
        toast.style.cssText = `
            position: fixed;
            bottom: 20px; right: 20px;
            z-index: 9998;
            background: rgba(44, 62, 80, 0.92);
            color: white;
            padding: 10px 16px;
            border-radius: 8px;
            font-family: Arial, sans-serif;
            font-size: 13px;
            max-width: 320px;
            box-shadow: 0 3px 10px rgba(0,0,0,0.25);
            pointer-events: none;
            opacity: 0;
            transform: translateY(10px);
            transition: all 0.25s ease;
        `;
        toast.innerHTML = `${icone} ${notif.mensagem || notif.acao}`;
        document.body.appendChild(toast);

        requestAnimationFrame(() => {
            toast.style.opacity = '1';
            toast.style.transform = 'translateY(0)';
        });

        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 250);
        }, 3500);
    }

    // ── Detecção de atividade do usuário ─────────────────────────────
    // (reservado para uso futuro — ex: pausar reconexão se inativo)

    document.addEventListener('mousemove', () => { ultimoMovimento = Date.now(); });
    document.addEventListener('keydown',   () => { ultimoMovimento = Date.now(); });

    function usuarioOcupado() {
        const campoFocado = document.activeElement &&
            ['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName);
        const modalAberto = document.querySelector('.modal[style*="block"]') !== null;
        return campoFocado || modalAberto;
    }

    // Expõe para debug no console do browser
    window.PGEO_WS = { pendentes, aplicarAtualizacoes, fecharBanner };

})();
