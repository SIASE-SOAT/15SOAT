export interface VeiculoRequest {
  placa: string;
  marca: string;
  modelo: string;
  ano: number;
  cor?: string;
  clienteId: string;
}

export interface VeiculoResponse {
  id: string;
  placa: string;
  marca: string;
  modelo: string;
  ano: number;
  cor?: string;
  clienteId: string;
  clienteNome: string;
  ativo: boolean;
}
