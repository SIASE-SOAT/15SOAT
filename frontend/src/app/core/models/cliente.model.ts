export type TipoPessoa = 'PF' | 'PJ';

export interface ClienteRequest {
  nome: string;
  tipoPessoa: TipoPessoa;
  documento: string;
  email?: string;
  telefone?: string;
  endereco?: string;
}

export interface ClienteResponse {
  id: string;
  nome: string;
  tipoPessoa: TipoPessoa;
  documento: string;
  email?: string;
  telefone?: string;
  endereco?: string;
  ativo: boolean;
  criadoEm: string;
}
