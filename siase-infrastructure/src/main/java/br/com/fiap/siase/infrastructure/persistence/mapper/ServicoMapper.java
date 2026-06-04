package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.Servico;
import br.com.fiap.siase.infrastructure.persistence.entity.ServicoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServicoMapper {
    @Mapping(target = "insumos", ignore = true)
    Servico toDomain(ServicoEntity entity);

    @Mapping(target = "insumos", ignore = true)
    ServicoEntity toEntity(Servico domain);
}


