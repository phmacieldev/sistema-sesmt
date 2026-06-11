package com.sesmt.pgeo.service;

import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.model.enums.TipoAtestado;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AtestadoServiceTest {

    private final AtestadoService service = new AtestadoService();

    // ── semanaInicio ──────────────────────────────────────────────────

    @Test
    void semanaInicio_quandoTerca_retornaPropriaTerca() {
        LocalDate terca = proximaTerca();
        assertThat(service.semanaInicio(terca).getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(service.semanaInicio(terca)).isEqualTo(terca);
    }

    @Test
    void semanaInicio_quandoSegunda_retornaTercaAnterior() {
        LocalDate terca  = proximaTerca();
        LocalDate segunda = terca.plusDays(6); // segunda seguinte
        assertThat(service.semanaInicio(segunda)).isEqualTo(terca);
    }

    @Test
    void semanaInicio_quandoDomingo_retornaTercaAnterior() {
        LocalDate terca   = proximaTerca();
        LocalDate domingo = terca.plusDays(5);
        assertThat(service.semanaInicio(domingo)).isEqualTo(terca);
    }

    @Test
    void semanaInicio_quandoQuarta_retornaTercaImediata() {
        LocalDate terca   = proximaTerca();
        LocalDate quarta  = terca.plusDays(1);
        assertThat(service.semanaInicio(quarta)).isEqualTo(terca);
    }

    // ── semanaFim ─────────────────────────────────────────────────────

    @Test
    void semanaFim_retornaSegundaAposTerca() {
        LocalDate terca = proximaTerca();
        LocalDate fim   = service.semanaFim(terca);
        assertThat(fim).isEqualTo(terca.plusDays(6));
        assertThat(fim.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void semanaInicioEFim_cobrem7Dias() {
        LocalDate terca = proximaTerca();
        LocalDate inicio = service.semanaInicio(terca);
        LocalDate fim    = service.semanaFim(inicio);
        assertThat(fim.toEpochDay() - inicio.toEpochDay()).isEqualTo(6);
    }

    // ── totalPorSetor ─────────────────────────────────────────────────

    @Test
    void totalPorSetor_agrupa_corretamente() {
        List<MedicalLeave> atestados = List.of(
            atestado("UTI",         5),
            atestado("UTI",         3),
            atestado("Enfermagem",  2)
        );
        Map<String, Integer> resultado = service.totalPorSetor(atestados);

        assertThat(resultado).containsEntry("UTI", 8);
        assertThat(resultado).containsEntry("Enfermagem", 2);
    }

    @Test
    void totalPorSetor_listaVazia_retornaMapaVazio() {
        assertThat(service.totalPorSetor(List.of())).isEmpty();
    }

    // ── totalPorTipo ──────────────────────────────────────────────────

    @Test
    void totalPorTipo_contaPorTipo() {
        List<MedicalLeave> atestados = List.of(
            atestadoTipo(TipoAtestado.DOENCA),
            atestadoTipo(TipoAtestado.DOENCA),
            atestadoTipo(TipoAtestado.ACIDENTE_TRABALHO)
        );
        Map<String, Long> resultado = service.totalPorTipo(atestados);

        assertThat(resultado.get("Doença")).isEqualTo(2L);
        assertThat(resultado.get("Acidente de Trabalho")).isEqualTo(1L);
    }

    // ── resumoPor60Dias ───────────────────────────────────────────────

    @Test
    void resumoPor60Dias_agrupaPorFuncionario() {
        Funcionario func = funcionario(1L, "João");
        MedicalLeave ml1 = atestadoFuncionario(func, 10);
        MedicalLeave ml2 = atestadoFuncionario(func, 8);

        List<AtestadoService.ResumoFuncionario> resumos = service.resumoPor60Dias(List.of(ml1, ml2));

        assertThat(resumos).hasSize(1);
        assertThat(resumos.get(0).getTotalDias()).isEqualTo(18);
        assertThat(resumos.get(0).getTotalAtestados()).isEqualTo(2);
    }

    @Test
    void resumoPor60Dias_marcaRiscoInss_quando15DiasOuMais() {
        Funcionario func = funcionario(2L, "Maria");
        MedicalLeave ml  = atestadoFuncionario(func, 15);

        List<AtestadoService.ResumoFuncionario> resumos = service.resumoPor60Dias(List.of(ml));

        assertThat(resumos.get(0).isRiscoInss()).isTrue();
    }

    @Test
    void resumoPor60Dias_naoMarcaRiscoInss_quandoMenos15Dias() {
        Funcionario func = funcionario(3L, "Pedro");
        MedicalLeave ml  = atestadoFuncionario(func, 14);

        List<AtestadoService.ResumoFuncionario> resumos = service.resumoPor60Dias(List.of(ml));

        assertThat(resumos.get(0).isRiscoInss()).isFalse();
    }

    @Test
    void resumoPor60Dias_ordenaPorDiasDesc() {
        Funcionario f1 = funcionario(1L, "Ana");
        Funcionario f2 = funcionario(2L, "Bruno");

        List<MedicalLeave> atestados = List.of(
            atestadoFuncionario(f1, 5),
            atestadoFuncionario(f2, 20)
        );

        List<AtestadoService.ResumoFuncionario> resumos = service.resumoPor60Dias(atestados);

        assertThat(resumos.get(0).getTotalDias()).isGreaterThanOrEqualTo(resumos.get(1).getTotalDias());
    }

    // ── helpers ───────────────────────────────────────────────────────

    private LocalDate proximaTerca() {
        LocalDate d = LocalDate.now().with(DayOfWeek.TUESDAY);
        return d.isBefore(LocalDate.now()) ? d.plusWeeks(1) : d;
    }

    private MedicalLeave atestado(String setor, int dias) {
        Funcionario f = new Funcionario();
        f.setId((long) setor.hashCode());
        f.setNome("Func " + setor);
        f.setSetor(setor);

        MedicalLeave ml = new MedicalLeave();
        ml.setFuncionario(f);
        ml.setDiasAfastamento(dias);
        ml.setDataAfastamento(LocalDate.now());
        ml.setTipo(TipoAtestado.DOENCA);
        return ml;
    }

    private MedicalLeave atestadoTipo(TipoAtestado tipo) {
        Funcionario f = new Funcionario();
        f.setId(1L);
        f.setNome("Func");

        MedicalLeave ml = new MedicalLeave();
        ml.setFuncionario(f);
        ml.setDiasAfastamento(1);
        ml.setDataAfastamento(LocalDate.now());
        ml.setTipo(tipo);
        return ml;
    }

    private MedicalLeave atestadoFuncionario(Funcionario func, int dias) {
        MedicalLeave ml = new MedicalLeave();
        ml.setFuncionario(func);
        ml.setDiasAfastamento(dias);
        ml.setDataAfastamento(LocalDate.now());
        ml.setTipo(TipoAtestado.DOENCA);
        return ml;
    }

    private Funcionario funcionario(Long id, String nome) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setNome(nome);
        f.setSetor("Setor");
        return f;
    }
}
