export type StatusOS =
  | 'RECEBIDA'
  | 'EM_DIAGNOSTICO'
  | 'AGUARDANDO_APROVACAO'
  | 'EM_EXECUCAO'
  | 'FINALIZADA'
  | 'ENTREGUE'
  | 'CANCELADA';

export interface ItemServicoRequest {
  servicoId: string;
  observacoes?: string;
}

export interface ItemPecaRequest {
  pecaId: string;
  quantidade: number;
}

export interface OrdemDeServicoRequest {
  clienteId: string;
  veiculoId: string;
  observacoes?: string;
  itensServico: ItemServicoRequest[];
  itensPeca?: ItemPecaRequest[];
}

export interface ItemServicoResponse {
  servicoId: string;
  servicoNome: string;
  precoUnitario: number;
  tempoEstimadoMinutos?: number;
  observacoes?: string;
}

export interface ItemPecaResponse {
  pecaId: string;
  pecaNome: string;
  pecaCodigo: string;
  quantidade: number;
  precoUnitario: number;
}

export interface OrdemDeServicoResponse {
  id: string;
  numero: string;
  status: StatusOS;
  statusDescricao: string;
  clienteId: string;
  clienteNome: string;
  veiculoId: string;
  veiculoPlaca: string;
  veiculoModelo: string;
  observacoes?: string;
  itensServico: ItemServicoResponse[];
  itensPeca: ItemPecaResponse[];
  totalServicos: number;
  totalPecas: number;
  total: number;
  dataAbertura: string;
  dataFechamento?: string;
}

export interface TempoMedioResponse {
  tempoMedioMinutos: number;
  tempoMedioHoras: number;
  descricao: string;
}
