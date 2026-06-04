package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.infrastructure.persistence.entity.VeiculoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ClienteMapper.class)
public interface VeiculoMapper {
    Veiculo toDomain(VeiculoEntity entity);

    @Mapping(source = "cliente.id", target = "cliente.id")
    VeiculoEntity toEntity(Veiculo domain);
}


