export type OperacaoEstoque = 'ENTRADA' | 'SAIDA';

export interface PecaRequest {
  codigo: string;
  nome: string;
  descricao?: string;
  preco: number;
  quantidadeEstoque: number;
  estoqueMinimo: number;
  unidadeMedida: string;
}

export interface PecaUpdateRequest {
  codigo: string;
  nome: string;
  descricao?: string;
  preco: number;
  estoqueMinimo: number;
  unidadeMedida: string;
}

export interface EstoqueRequest {
  operacao: OperacaoEstoque;
  quantidade: number;
}

export interface PecaResponse {
  id: string;
  codigo: string;
  nome: string;
  descricao?: string;
  preco: number;
  quantidadeEstoque: number;
  estoqueMinimo: number;
  unidadeMedida: string;
  ativo: boolean;
  estoqueAbaixoMinimo: boolean;
}
