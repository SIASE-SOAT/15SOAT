package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.PedidoCompra;
import br.com.fiap.siase.infrastructure.persistence.entity.PedidoCompraEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PecaMapper.class})
public interface PedidoCompraMapper {
    PedidoCompra toDomain(PedidoCompraEntity entity);

    @Mapping(source = "peca.id", target = "peca.id")
    PedidoCompraEntity toEntity(PedidoCompra domain);
}


