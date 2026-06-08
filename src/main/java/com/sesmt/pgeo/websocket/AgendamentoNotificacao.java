package com.sesmt.pgeo.websocket;

import com.sesmt.pgeo.model.Agendamento;
import lombok.Data;

import java.time.format.DateTimeFormatter;

/**
 * DTO enviado via WebSocket para todos os clientes conectados.
 *
 * Quando um agendamento é criado/editado/excluído:
 *   1. O Service chama NotificacaoService.broadcastAtualizacao()
 *   2. Spring envia este objeto serializado como JSON para /topic/agendamentos
 *   3. O JS no browser recebe e recarrega a tabela do dashboard
 *
 * Contém apenas o essencial — não expõe todos os campos da entidade.
 */
@Data
public class AgendamentoNotificacao {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Long id;
    private String acao;          // "CRIAR", "EDITAR", "EXCLUIR", "ASO_STATUS"
    private String nome;
    private String dataClinico;
    private String tipoExame;
    private String mensagem;      // ex: "João agendou Periódico para 15/06/2025"

    public static AgendamentoNotificacao de(Agendamento ag, String acao, String usuarioAtual) {
        AgendamentoNotificacao n = new AgendamentoNotificacao();
        n.setId(ag.getId());
        n.setAcao(acao);
        n.setNome(ag.getFuncionarioNome());
        n.setDataClinico(ag.getDataClinico() != null ? ag.getDataClinico().format(BR) : "—");
        n.setTipoExame(ag.getTipoExameDescricao());
        n.setMensagem(formatarMensagem(ag, acao, usuarioAtual));
        return n;
    }

    public static AgendamentoNotificacao exclusao(Long id, String nome, String usuarioAtual) {
        AgendamentoNotificacao n = new AgendamentoNotificacao();
        n.setId(id);
        n.setAcao("EXCLUIR");
        n.setNome(nome);
        n.setMensagem(usuarioAtual + " excluiu o agendamento de " + nome);
        return n;
    }

    private static String formatarMensagem(Agendamento ag, String acao, String usuario) {
        String data = ag.getDataClinico() != null ? ag.getDataClinico().format(BR) : "—";
        return switch (acao) {
            case "CRIAR"  -> usuario + " agendou " + ag.getTipoExameDescricao()
                             + " de " + ag.getFuncionarioNome() + " para " + data;
            case "EDITAR" -> usuario + " editou agendamento de " + ag.getFuncionarioNome();
            case "EXCLUIR"-> usuario + " excluiu agendamento de " + ag.getFuncionarioNome();
            default       -> "Agendamento atualizado por " + usuario;
        };
    }
}
