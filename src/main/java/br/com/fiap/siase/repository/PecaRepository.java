package br.com.fiap.siase.repository;

import br.com.fiap.siase.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PecaRepository extends JpaRepository<Peca, UUID> {

    Optional<Peca> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Peca> findByAtivoTrue();

    @Query("SELECT p FROM Peca p WHERE p.ativo = true AND p.quantidadeEstoque <= p.estoqueMinimo")
    List<Peca> findEstoqueBaixo();
}
