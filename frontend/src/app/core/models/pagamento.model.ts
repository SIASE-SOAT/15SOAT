export type FormaPagamento =
  | 'DINHEIRO'
  | 'CARTAO_DEBITO'
  | 'CARTAO_CREDITO'
  | 'PIX'
  | 'TRANSFERENCIA';

export type StatusPagamento = 'PENDENTE' | 'PAGO' | 'CANCELADO';

export interface PagamentoRequest {
  formaPagamento: FormaPagamento;
  valor: number;
}

export interface PagamentoResponse {
  id: string;
  ordemDeServicoId: string;
  osNumero: string;
  clienteNome: string;
  formaPagamento: FormaPagamento;
  formaPagamentoDescricao: string;
  valor: number;
  status: StatusPagamento;
  statusDescricao: string;
  dataPagamento?: string;
}
