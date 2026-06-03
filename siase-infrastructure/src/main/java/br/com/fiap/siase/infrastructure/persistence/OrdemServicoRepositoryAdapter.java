package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import br.com.fiap.siase.infrastructure.persistence.entity.OrdemDeServicoEntity;
import br.com.fiap.siase.infrastructure.persistence.mapper.OrdemDeServicoMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class OrdemServicoRepositoryAdapter implements OrdemServicoRepositoryPort {

    private final OrdemServicoJpaRepository jpaRepository;
    private final OrdemDeServicoMapper mapper;

    public OrdemServicoRepositoryAdapter(OrdemServicoJpaRepository jpaRepository, OrdemDeServicoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public OrdemDeServico save(OrdemDeServico ordemDeServico) {
        OrdemDeServicoEntity entity = mapper.toEntity(ordemDeServico);
        OrdemDeServicoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrdemDeServico> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServico> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServico> findByStatus(StatusOS status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrdemDeServico> findByNumero(String numero) {
        return jpaRepository.findByNumero(numero).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServico> findByClienteId(UUID clienteId) {
        return jpaRepository.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServico> findByVeiculoId(UUID veiculoId) {
        return jpaRepository.findByVeiculoId(veiculoId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByVeiculoIdAndStatusNotIn(UUID veiculoId, List<StatusOS> statuses) {
        return jpaRepository.existsByVeiculoIdAndStatusNotIn(veiculoId, statuses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServico> findAllAtivasOrdered() {
        return jpaRepository.findAllAtivasOrdered().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> calcularTempoMedioExecucaoMinutos() {
        return Optional.ofNullable(jpaRepository.calcularTempoMedioExecucaoMinutos());
    }
}
