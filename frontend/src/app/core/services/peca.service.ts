import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { EstoqueRequest, PecaRequest, PecaResponse, PecaUpdateRequest } from '../models/peca.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PecaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/pecas`;

  listar() {
    return this.http.get<PecaResponse[]>(this.base);
  }

  listarTodas() {
    return this.http.get<PecaResponse[]>(`${this.base}/todas`);
  }

  buscarPorId(id: string) {
    return this.http.get<PecaResponse>(`${this.base}/${id}`);
  }

  criar(body: PecaRequest) {
    return this.http.post<PecaResponse>(this.base, body);
  }

  atualizar(id: string, body: PecaUpdateRequest) {
    return this.http.put<PecaResponse>(`${this.base}/${id}`, body);
  }

  movimentarEstoque(id: string, body: EstoqueRequest) {
    return this.http.patch<PecaResponse>(`${this.base}/${id}/estoque`, body);
  }

  desativar(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
