package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.Pagamento;
import br.com.fiap.siase.infrastructure.persistence.entity.PagamentoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrdemDeServicoMapper.class})
public interface PagamentoMapper {
    Pagamento toDomain(PagamentoEntity entity);

    @Mapping(source = "ordemDeServico.id", target = "ordemDeServico.id")
    PagamentoEntity toEntity(Pagamento domain);
}


