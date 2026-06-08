/* agenda.js — idêntico ao projeto Flask original.
   O JavaScript não precisa mudar: os endpoints /agenda_events_json,
   /mover_agendamento etc. são expostos pelo Java com as mesmas URLs. */

document.addEventListener('DOMContentLoaded', function() {

    var calendarEl = document.getElementById('calendar');
    if (!calendarEl) return;

    var calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'pt-br',
        height: "auto",
        editable: true,
        eventDurationEditable: false,
        defaultTimedEventDuration: "00:30:00",
        forceEventDuration: true,
        slotEventOverlap: false,
        allDaySlot: false,

        eventAllow: function(dropInfo) {
            let hora = dropInfo.start.getHours();
            let minuto = dropInfo.start.getMinutes();
            let horario = hora + ":" + minuto.toString().padStart(2, "0");
            return !(horario >= "11:00" && horario < "13:30");
        },

        eventDidMount: function(info) {
            let d = info.event.extendedProps;
            info.el.title = d.nome + "\nSetor: " + d.setor + "\nFunção: " + d.funcao + "\nExame: " + d.tipo;
        },

        headerToolbar: {
            left:   'prev,next today',
            center: 'title',
            right:  'dayGridMonth,timeGridWeek,timeGridDay'
        },

        buttonText: { today: 'Hoje', month: 'Mês', week: 'Semana', day: 'Dia' },

        slotLabelFormat: [{ hour: '2-digit', minute: '2-digit', hour12: false }],
        slotDuration: "00:30:00",
        slotMinTime: "08:00:00",
        slotMaxTime: "16:00:00",
        slotLabelInterval: "00:30:00",

        businessHours: [
            { daysOfWeek: [1,2,3,4,5], startTime: '08:00', endTime: '11:00' },
            { daysOfWeek: [1,2,3,4,5], startTime: '13:30', endTime: '15:30' }
        ],
        weekends: false,

        events: function(fetchInfo, successCallback, failureCallback) {
            fetch('/agenda_events_json')
                .then(r => r.json())
                .then(data => successCallback(data))
                .catch(err => failureCallback(err));
        },

        eventClick: function(info) {
            var d = info.event.extendedProps;
            abrirModal({
                id:    info.event.id,
                nome:  d.nome,
                setor: d.setor,
                funcao: d.funcao,
                exame: d.tipo,
                data:  info.event.start.toLocaleDateString('pt-BR'),
                hora:  info.event.start.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})
            });
        },

        eventDrop: function(info) {
            fetch("/mover_agendamento", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    id:   info.event.id,
                    data: info.event.start.toISOString().slice(0, 10),
                    hora: info.event.start.toTimeString().slice(0, 5)
                })
            })
            .then(r => r.json())
            .then(data => {
                if (data.status !== "ok") { alert("Erro ao mover agendamento"); info.revert(); }
                else calendar.refetchEvents();
            });
        },

        datesSet: function() { calendar.refetchEvents(); }
    });

    calendar.render();
});

// Desaparece alertas após 3s
setTimeout(function() {
    document.querySelectorAll(".alerta").forEach(el => el.classList.add("sumir"));
}, 3000);

function abrirModal(ag) {
    document.getElementById("modal-nome").innerText  = ag.nome;
    document.getElementById("modal-setor").innerText = ag.setor;
    document.getElementById("modal-funcao").innerText= ag.funcao;
    document.getElementById("modal-exame").innerText = ag.exame;
    document.getElementById("modal-data").innerText  = ag.data;
    document.getElementById("modal-hora").innerText  = ag.hora;
    document.getElementById("btn-editar-modal").href = "/editar_agendamento/" + ag.id;
    document.getElementById("modal-agendamento").style.display = "block";
}

function fecharModal() {
    document.getElementById("modal-agendamento").style.display = "none";
}

window.onclick = function(e) {
    let modal = document.getElementById("modal-agendamento");
    if (modal && e.target === modal) modal.style.display = "none";
};
