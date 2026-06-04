package br.com.fiap.siase.infrastructure.persistence.mapper;

import br.com.fiap.siase.domain.model.Agendamento;
import br.com.fiap.siase.infrastructure.persistence.entity.AgendamentoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ClienteMapper.class, VeiculoMapper.class})
public interface AgendamentoMapper {
    @Mapping(target = "ordemDeServico", ignore = true)
    Agendamento toDomain(AgendamentoEntity entity);

    @Mapping(source = "cliente.id", target = "cliente.id")
    @Mapping(source = "veiculo.id", target = "veiculo.id")
    @Mapping(source = "ordemDeServico.id", target = "ordemDeServico.id")
    AgendamentoEntity toEntity(Agendamento domain);
}


