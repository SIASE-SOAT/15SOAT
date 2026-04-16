import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrdemDeServicoService } from '../../core/services/ordem-de-servico.service';
import { PecaService } from '../../core/services/peca.service';
import { OrdemDeServicoResponse, StatusOS } from '../../core/models/ordem-de-servico.model';
import { PecaResponse } from '../../core/models/peca.model';

interface CardInfo {
  label: string;
  icon: string;
  count: number;
  color: string;
  status: StatusOS;
}

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly osService = inject(OrdemDeServicoService);
  private readonly pecaService = inject(PecaService);

  protected readonly loading = signal(true);
  protected readonly statusCards = signal<CardInfo[]>([]);
  protected readonly pecasCriticas = signal<PecaResponse[]>([]);
  protected readonly tempoMedioHoras = signal(0);
  protected readonly tempoMedioMinutosRestantes = signal(0);

  private readonly iconColorMap: Record<StatusOS, string> = {
    RECEBIDA:             '#6b7280',
    EM_DIAGNOSTICO:       '#7c3aed',
    AGUARDANDO_APROVACAO: '#d97706',
    EM_EXECUCAO:          '#2563eb',
    FINALIZADA:           '#16a34a',
    ENTREGUE:             '#0d9488',
    CANCELADA:            '#dc2626',
  };

  private readonly iconBgColorMap: Record<StatusOS, string> = {
    RECEBIDA:             '#f3f4f6',
    EM_DIAGNOSTICO:       '#ede9fe',
    AGUARDANDO_APROVACAO: '#fef3c7',
    EM_EXECUCAO:          '#dbeafe',
    FINALIZADA:           '#dcfce7',
    ENTREGUE:             '#ccfbf1',
    CANCELADA:            '#fee2e2',
  };

  protected iconColor(status: StatusOS): string {
    return this.iconColorMap[status];
  }

  protected iconBg(status: StatusOS): string {
    return this.iconBgColorMap[status] ?? '#f3f4f6';
  }

  private readonly statusConfig: Record<StatusOS, { label: string; icon: string; color: string }> = {
    RECEBIDA:             { label: 'Recebidas',           icon: 'inbox',             color: 'text-gray-500' },
    EM_DIAGNOSTICO:       { label: 'Em Diagnóstico',      icon: 'search',            color: 'text-purple-500' },
    AGUARDANDO_APROVACAO: { label: 'Aguard. Aprovação',   icon: 'pending_actions',   color: 'text-yellow-600' },
    EM_EXECUCAO:          { label: 'Em Execução',         icon: 'build',             color: 'text-blue-600' },
    FINALIZADA:           { label: 'Finalizadas',         icon: 'check_circle',      color: 'text-green-600' },
    ENTREGUE:             { label: 'Entregues',           icon: 'done_all',          color: 'text-green-800' },
    CANCELADA:            { label: 'Canceladas',          icon: 'cancel',            color: 'text-red-500' },
  };

  ngOnInit() {
    this.carregarDados();
  }

  private carregarDados() {
    this.loading.set(true);
    let pendentes = 3;
    const finalizar = () => { if (--pendentes === 0) this.loading.set(false); };

    this.osService.listar().subscribe({
      next: (ordens) => {
        this.calcularCards(ordens);
        finalizar();
      },
      error: finalizar,
    });

    this.osService.tempoMedioExecucao().subscribe({
      next: ({ tempoMedioMinutos }) => {
        this.tempoMedioHoras.set(Math.floor(tempoMedioMinutos / 60));
        this.tempoMedioMinutosRestantes.set(Math.round(tempoMedioMinutos % 60));
        finalizar();
      },
      error: finalizar,
    });

    this.pecaService.listar().subscribe({
      next: (pecas) => {
        this.pecasCriticas.set(pecas.filter(p => p.estoqueAbaixoMinimo));
        finalizar();
      },
      error: finalizar,
    });
  }

  private calcularCards(ordens: OrdemDeServicoResponse[]) {
    const contagem = Object.fromEntries(
      (Object.keys(this.statusConfig) as StatusOS[]).map(s => [s, 0])
    ) as Record<StatusOS, number>;

    ordens.forEach(o => contagem[o.status]++);

    this.statusCards.set(
      (Object.keys(this.statusConfig) as StatusOS[]).map(status => ({
        status,
        count: contagem[status],
        ...this.statusConfig[status],
      }))
    );
  }
}
