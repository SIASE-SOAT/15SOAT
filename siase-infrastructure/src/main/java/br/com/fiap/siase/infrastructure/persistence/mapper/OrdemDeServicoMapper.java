package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.OrdemDeServico;
import br.com.fiap.siase.infrastructure.persistence.entity.OrdemDeServicoEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ClienteMapper.class, VeiculoMapper.class, ItemServicoMapper.class, ItemPecaMapper.class})
public interface OrdemDeServicoMapper {

    OrdemDeServico toDomain(OrdemDeServicoEntity entity);

    @Mapping(source = "cliente.id", target = "cliente.id")
    @Mapping(source = "veiculo.id", target = "veiculo.id")
    OrdemDeServicoEntity toEntity(OrdemDeServico domain);

    @AfterMapping
    default void setBackReferences(@MappingTarget OrdemDeServicoEntity entity) {
        if (entity.getItensServico() != null) {
            entity.getItensServico().forEach(item -> item.setOrdemDeServico(entity));
        }
        if (entity.getItensPeca() != null) {
            entity.getItensPeca().forEach(item -> item.setOrdemDeServico(entity));
        }
    }
}
