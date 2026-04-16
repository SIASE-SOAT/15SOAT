export type StatusPedidoCompra = 'PENDENTE' | 'APROVADO' | 'RECEBIDO' | 'CANCELADO';

export interface PedidoCompraRequest {
  pecaId: string;
  quantidadeSolicitada: number;
  observacoes?: string;
}

export interface PedidoCompraResponse {
  id: string;
  pecaId: string;
  pecaCodigo: string;
  pecaNome: string;
  quantidadeSolicitada: number;
  quantidadeRecebida: number;
  status: StatusPedidoCompra;
  statusDescricao: string;
  observacoes?: string;
  criadoEm: string;
}
