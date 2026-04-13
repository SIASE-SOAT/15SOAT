export type StatusAgendamento = 'AGENDADO' | 'CONFIRMADO' | 'CANCELADO' | 'REALIZADO';

export interface AgendamentoRequest {
  clienteId: string;
  veiculoId: string;
  dataHora: string;
  descricaoServicos?: string;
}

export interface AgendamentoResponse {
  id: string;
  clienteId: string;
  clienteNome: string;
  clienteEmail?: string;
  veiculoId: string;
  veiculoPlaca: string;
  veiculoModelo: string;
  dataHora: string;
  descricaoServicos?: string;
  status: StatusAgendamento;
  statusDescricao: string;
  ordemDeServicoId?: string;
}
