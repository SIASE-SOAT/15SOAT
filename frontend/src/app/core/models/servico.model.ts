export interface ServicoRequest {
  nome: string;
  descricao?: string;
  preco: number;
  tempoEstimadoMinutos?: number;
}

export interface InsumoRequest {
  pecaId: string;
  quantidade: number;
}

export interface InsumoResponse {
  pecaId: string;
  pecaCodigo: string;
  pecaNome: string;
  quantidade: number;
}

export interface ServicoResponse {
  id: string;
  nome: string;
  descricao?: string;
  preco: number;
  tempoEstimadoMinutos?: number;
  ativo: boolean;
  insumos: InsumoResponse[];
}
