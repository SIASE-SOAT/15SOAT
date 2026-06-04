package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.infrastructure.persistence.entity.ClienteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    @Mapping(target = "veiculos", ignore = true)
    Cliente toDomain(ClienteEntity entity);

    @Mapping(target = "veiculos", ignore = true)
    ClienteEntity toEntity(Cliente domain);
}


