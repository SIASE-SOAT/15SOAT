import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  ItemPecaRequest,
  ItemServicoRequest,
  OrdemDeServicoRequest,
  OrdemDeServicoResponse,
  PreparacaoAberturaOrdemResponse,
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

  prepararAbertura(documento: string, placa?: string) {
    let params = new HttpParams().set('documento', documento);
    if (placa?.trim()) {
      params = params.set('placa', placa.trim().toUpperCase());
    }
    return this.http.get<PreparacaoAberturaOrdemResponse>(`${this.base}/preparar-abertura`, { params });
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

  adicionarPeca(id: string, body: ItemPecaRequest) {
    return this.http.post<OrdemDeServicoResponse>(`${this.base}/${id}/items-peca`, body);
  }

  adicionarServico(id: string, body: ItemServicoRequest) {
    return this.http.post<OrdemDeServicoResponse>(`${this.base}/${id}/items-servico`, body);
  }

  iniciarExecucaoItem(id: string, itemId: string) {
    return this.http.patch<OrdemDeServicoResponse>(`${this.base}/${id}/itens-servico/${itemId}/iniciar`, null);
  }

  finalizarExecucaoItem(id: string, itemId: string) {
    return this.http.patch<OrdemDeServicoResponse>(`${this.base}/${id}/itens-servico/${itemId}/finalizar`, null);
  }
}
