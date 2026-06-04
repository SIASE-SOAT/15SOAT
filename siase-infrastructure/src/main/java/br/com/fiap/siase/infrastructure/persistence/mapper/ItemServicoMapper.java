package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.ItemServico;
import br.com.fiap.siase.infrastructure.persistence.entity.ItemServicoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ServicoMapper.class})
public interface ItemServicoMapper {
    @Mapping(target = "ordemDeServico", ignore = true)
    ItemServico toDomain(ItemServicoEntity entity);

    @Mapping(target = "ordemDeServico", ignore = true)
    @Mapping(source = "servico.id", target = "servico.id")
    ItemServicoEntity toEntity(ItemServico domain);
}


