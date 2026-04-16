import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { PedidoCompraRequest, PedidoCompraResponse, StatusPedidoCompra } from '../models/pedido-compra.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PedidoCompraService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/pedidos-compra`;

  listar(status?: StatusPedidoCompra) {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<PedidoCompraResponse[]>(this.base, { params });
  }

  buscarPorId(id: string) {
    return this.http.get<PedidoCompraResponse>(`${this.base}/${id}`);
  }

  criar(body: PedidoCompraRequest) {
    return this.http.post<PedidoCompraResponse>(this.base, body);
  }

  aprovar(id: string) {
    return this.http.patch<PedidoCompraResponse>(`${this.base}/${id}/aprovar`, null);
  }

  receber(id: string, quantidade: number) {
    return this.http.patch<PedidoCompraResponse>(
      `${this.base}/${id}/receber`,
      null,
      { params: new HttpParams().set('quantidade', quantidade) }
    );
  }

  cancelar(id: string) {
    return this.http.patch<PedidoCompraResponse>(`${this.base}/${id}/cancelar`, null);
  }
}
