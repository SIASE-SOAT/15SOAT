package br.com.fiap.siase.infrastructure.persistence;

import br.com.fiap.siase.domain.model.Peca;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PecaJpaRepository extends JpaRepository<Peca, UUID> {

    List<Peca> findByAtivoTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Peca p WHERE p.id = :id")
    Optional<Peca> findByIdParaAtualizacao(UUID id);
}
