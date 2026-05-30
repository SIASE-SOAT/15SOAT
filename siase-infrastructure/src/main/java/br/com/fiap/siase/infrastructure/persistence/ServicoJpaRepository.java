package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicoJpaRepository extends JpaRepository<Servico, UUID> {

    List<Servico> findByAtivoTrue();
}
