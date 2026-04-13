import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  OrdemDeServicoRequest,
  OrdemDeServicoResponse,
  StatusOS,
  TempoMedioResponse,
} from '../models/ordem-de-servico.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OrdemDeServicoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/ordens`;

  listar(status?: StatusOS) {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<OrdemDeServicoResponse[]>(this.base, { params });
  }

  buscarPorId(id: string) {
    return this.http.get<OrdemDeServicoResponse>(`${this.base}/${id}`);
  }

  acompanharPorNumero(numero: string) {
    return this.http.get<OrdemDeServicoResponse>(`${this.base}/acompanhar/${numero}`);
  }

  criar(body: OrdemDeServicoRequest) {
    return this.http.post<OrdemDeServicoResponse>(this.base, body);
  }

  avancarStatus(id: string) {
    return this.http.patch<OrdemDeServicoResponse>(`${this.base}/${id}/avancar`, null);
  }

  cancelar(id: string) {
    return this.http.patch<OrdemDeServicoResponse>(`${this.base}/${id}/cancelar`, null);
  }

  tempoMedioExecucao() {
    return this.http.get<TempoMedioResponse>(`${this.base}/monitoramento/tempo-medio`);
  }
}
