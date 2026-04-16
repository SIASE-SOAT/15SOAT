import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { InsumoRequest, ServicoRequest, ServicoResponse } from '../models/servico.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ServicoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/servicos`;

  listar() {
    return this.http.get<ServicoResponse[]>(this.base);
  }

  buscarPorId(id: string) {
    return this.http.get<ServicoResponse>(`${this.base}/${id}`);
  }

  criar(body: ServicoRequest) {
    return this.http.post<ServicoResponse>(this.base, body);
  }

  atualizar(id: string, body: ServicoRequest) {
    return this.http.put<ServicoResponse>(`${this.base}/${id}`, body);
  }

  vincularInsumo(servicoId: string, body: InsumoRequest) {
    return this.http.post<ServicoResponse>(`${this.base}/${servicoId}/insumos`, body);
  }

  atualizarInsumo(servicoId: string, pecaId: string, body: InsumoRequest) {
    return this.http.put<ServicoResponse>(`${this.base}/${servicoId}/insumos/${pecaId}`, body);
  }

  removerInsumo(servicoId: string, pecaId: string) {
    return this.http.delete<void>(`${this.base}/${servicoId}/insumos/${pecaId}`);
  }

  desativar(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
