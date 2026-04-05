package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    List<Servico> findByAtivoTrue();
}
