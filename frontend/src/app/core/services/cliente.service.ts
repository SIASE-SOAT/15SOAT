import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ClienteRequest, ClienteResponse } from '../models/cliente.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/clientes`;

  listar() {
    return this.http.get<ClienteResponse[]>(this.base);
  }

  buscarPorId(id: string) {
    return this.http.get<ClienteResponse>(`${this.base}/${id}`);
  }

  buscarPorDocumento(documento: string) {
    return this.http.get<ClienteResponse>(`${this.base}/documento/${documento}`);
  }

  criar(body: ClienteRequest) {
    return this.http.post<ClienteResponse>(this.base, body);
  }

  atualizar(id: string, body: ClienteRequest) {
    return this.http.put<ClienteResponse>(`${this.base}/${id}`, body);
  }

  desativar(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
