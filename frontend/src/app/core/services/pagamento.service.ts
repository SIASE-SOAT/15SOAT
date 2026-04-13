import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { PagamentoRequest, PagamentoResponse } from '../models/pagamento.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PagamentoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  registrar(osId: string, body: PagamentoRequest) {
    return this.http.post<PagamentoResponse>(`${this.apiUrl}/ordens/${osId}/pagamento`, body);
  }

  buscarPorOS(osId: string) {
    return this.http.get<PagamentoResponse>(`${this.apiUrl}/ordens/${osId}/pagamento`);
  }

  confirmar(id: string) {
    return this.http.patch<PagamentoResponse>(`${this.apiUrl}/pagamentos/${id}/confirmar`, null);
  }

  cancelar(id: string) {
    return this.http.patch<PagamentoResponse>(`${this.apiUrl}/pagamentos/${id}/cancelar`, null);
  }
}
