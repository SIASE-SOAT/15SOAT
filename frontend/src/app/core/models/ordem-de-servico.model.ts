export type StatusOS =
  | 'RECEBIDA'
  | 'EM_DIAGNOSTICO'
  | 'AGUARDANDO_APROVACAO'
  | 'APROVADO'
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

export interface ClienteIdentificadoResponse {
  id: string;
  nome: string;
  documento: string;
  email?: string;
  telefone?: string;
}

export interface VeiculoIdentificadoResponse {
  id: string;
  placa: string;
  marca: string;
  modelo: string;
  ano: number;
  ativo: boolean;
}

export interface PreparacaoAberturaOrdemResponse {
  cliente: ClienteIdentificadoResponse;
  veiculos: VeiculoIdentificadoResponse[];
  veiculoSelecionado?: VeiculoIdentificadoResponse | null;
  prontoParaAbertura: boolean;
}

export interface ItemServicoResponse {
  id: string;
  servicoId: string;
  servicoNome: string;
  precoUnitario: number;
  tempoEstimadoMinutos?: number;
  observacoes?: string;
  dataInicioExecucao?: string;
  dataFimExecucao?: string;
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
