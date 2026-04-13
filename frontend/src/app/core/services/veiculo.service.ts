import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { VeiculoRequest, VeiculoResponse } from '../models/veiculo.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class VeiculoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/veiculos`;

  listar() {
    return this.http.get<VeiculoResponse[]>(this.base);
  }

  listarPorCliente(clienteId: string) {
    return this.http.get<VeiculoResponse[]>(`${this.base}/cliente/${clienteId}`);
  }

  buscarPorId(id: string) {
    return this.http.get<VeiculoResponse>(`${this.base}/${id}`);
  }

  buscarPorPlaca(placa: string) {
    return this.http.get<VeiculoResponse>(`${this.base}/placa/${placa}`);
  }

  criar(body: VeiculoRequest) {
    return this.http.post<VeiculoResponse>(this.base, body);
  }

  atualizar(id: string, body: VeiculoRequest) {
    return this.http.put<VeiculoResponse>(`${this.base}/${id}`, body);
  }

  desativar(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
