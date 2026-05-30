package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.enums.StatusOS;
import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.domain.port.OrdemServicoRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrdemServicoRepositoryAdapter implements OrdemServicoRepositoryPort {

    private final OrdemServicoJpaRepository jpaRepository;

    public OrdemServicoRepositoryAdapter(OrdemServicoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OrdemDeServico save(OrdemDeServico ordemDeServico) {
        return jpaRepository.save(ordemDeServico);
    }

    @Override
    public Optional<OrdemDeServico> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<OrdemDeServico> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<OrdemDeServico> findByStatus(StatusOS status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public Optional<OrdemDeServico> findByNumero(String numero) {
        return jpaRepository.findByNumero(numero);
    }

    @Override
    public List<OrdemDeServico> findByClienteId(UUID clienteId) {
        return jpaRepository.findByClienteId(clienteId);
    }

    @Override
    public List<OrdemDeServico> findByVeiculoId(UUID veiculoId) {
        return jpaRepository.findByVeiculoId(veiculoId);
    }

    @Override
    public boolean existsByVeiculoIdAndStatusNotIn(UUID veiculoId, List<StatusOS> statuses) {
        return jpaRepository.existsByVeiculoIdAndStatusNotIn(veiculoId, statuses);
    }

    @Override
    public List<OrdemDeServico> findAllAtivasOrdered() {
        return jpaRepository.findAllAtivasOrdered();
    }
}
