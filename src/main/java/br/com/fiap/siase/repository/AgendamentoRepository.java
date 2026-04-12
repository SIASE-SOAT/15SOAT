package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Agendamento;
import br.com.fiap.siase.model.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {

    List<Agendamento> findByClienteId(UUID clienteId);

    List<Agendamento> findByStatus(StatusAgendamento status);

    List<Agendamento> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);
}
