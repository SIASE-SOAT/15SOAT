import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AgendamentoRequest, AgendamentoResponse, StatusAgendamento } from '../models/agendamento.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AgendamentoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/agendamentos`;

  listar(status?: StatusAgendamento) {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<AgendamentoResponse[]>(this.base, { params });
  }

  listarPorCliente(clienteId: string) {
    return this.http.get<AgendamentoResponse[]>(`${this.base}/cliente/${clienteId}`);
  }

  buscarPorId(id: string) {
    return this.http.get<AgendamentoResponse>(`${this.base}/${id}`);
  }

  criar(body: AgendamentoRequest) {
    return this.http.post<AgendamentoResponse>(this.base, body);
  }

  confirmar(id: string) {
    return this.http.patch<AgendamentoResponse>(`${this.base}/${id}/confirmar`, null);
  }

  cancelar(id: string) {
    return this.http.patch<AgendamentoResponse>(`${this.base}/${id}/cancelar`, null);
  }
}
