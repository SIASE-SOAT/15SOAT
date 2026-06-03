package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.ItemPeca;
import br.com.fiap.siase.infrastructure.persistence.entity.ItemPecaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PecaMapper.class})
public interface ItemPecaMapper {
    @Mapping(target = "ordemDeServico", ignore = true)
    ItemPeca toDomain(ItemPecaEntity entity);

    @Mapping(target = "ordemDeServico", ignore = true)
    @Mapping(source = "peca.id", target = "peca.id")
    ItemPecaEntity toEntity(ItemPeca domain);
}


