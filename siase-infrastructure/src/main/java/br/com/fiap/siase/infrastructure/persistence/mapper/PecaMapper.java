package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.Peca;
import br.com.fiap.siase.infrastructure.persistence.entity.PecaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PecaMapper {
    Peca toDomain(PecaEntity entity);
    PecaEntity toEntity(Peca domain);
}


