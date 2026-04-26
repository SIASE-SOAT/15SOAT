import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ItemServicoResponse, OrdemDeServicoResponse, StatusOS } from '../../../core/models/ordem-de-servico.model';
import { OrdemDeServicoService } from '../../../core/services/ordem-de-servico.service';
import { OsFormDialogComponent } from '../dialogs/os-form-dialog.component';

const STATUS_COLOR: Record<StatusOS, string> = {
  RECEBIDA:             'bg-gray-100 text-gray-700',
  EM_DIAGNOSTICO:       'bg-purple-100 text-purple-700',
  AGUARDANDO_APROVACAO: 'bg-yellow-100 text-yellow-700',
  EM_EXECUCAO:          'bg-blue-100 text-blue-700',
  FINALIZADA:           'bg-green-100 text-green-700',
  ENTREGUE:             'bg-green-200 text-green-900',
  CANCELADA:            'bg-red-100 text-red-700',
};

@Component({
  selector: 'app-ordens-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe, DatePipe, RouterLink, FormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatProgressSpinnerModule, MatSelectModule, MatFormFieldModule, MatTooltipModule,
  ],
  templateUrl: './ordens-lista.component.html',
  styleUrl: './ordens-lista.component.scss',
})
export class OrdensListaComponent implements OnInit {
  private readonly service = inject(OrdemDeServicoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly ordens = signal<OrdemDeServicoResponse[]>([]);
  protected filtroStatus: StatusOS | null = null;
  protected readonly colunas = ['numero', 'status', 'execucao', 'cliente', 'veiculo', 'total', 'data', 'acoes'];

  protected readonly statusList: { value: StatusOS; label: string }[] = [
    { value: 'RECEBIDA',             label: 'Recebida' },
    { value: 'EM_DIAGNOSTICO',       label: 'Em Diagnóstico' },
    { value: 'AGUARDANDO_APROVACAO', label: 'Aguardando Aprovação' },
    { value: 'EM_EXECUCAO',          label: 'Em Execução' },
    { value: 'FINALIZADA',           label: 'Finalizada' },
    { value: 'ENTREGUE',             label: 'Entregue' },
    { value: 'CANCELADA',            label: 'Cancelada' },
  ];

  protected statusColor(status: StatusOS) { return STATUS_COLOR[status]; }

  protected itemStatus(item: ItemServicoResponse): 'NAO_INICIADO' | 'EM_EXECUCAO' | 'FINALIZADO' {
    if (item.dataFimExecucao) return 'FINALIZADO';
    if (item.dataInicioExecucao) return 'EM_EXECUCAO';
    return 'NAO_INICIADO';
  }

  protected resumoExecucao(os: OrdemDeServicoResponse) {
    const total = os.itensServico.length;
    const finalizados = os.itensServico.filter(i => this.itemStatus(i) === 'FINALIZADO').length;
    const emExecucao = os.itensServico.filter(i => this.itemStatus(i) === 'EM_EXECUCAO').length;
    const naoIniciados = total - finalizados - emExecucao;
    return { total, finalizados, emExecucao, naoIniciados };
  }

  protected classeExecucaoOS(os: OrdemDeServicoResponse) {
    const { total, finalizados, emExecucao } = this.resumoExecucao(os);
    if (total === 0) return 'exec-sem-itens';
    if (finalizados === total) return 'exec-finalizado';
    if (emExecucao > 0) return 'exec-em-execucao';
    return 'exec-nao-iniciado';
  }

  protected labelExecucaoOS(os: OrdemDeServicoResponse) {
    const { total, finalizados, emExecucao } = this.resumoExecucao(os);
    if (total === 0) return 'Sem serviços';
    if (finalizados === total) return 'Finalizado';
    if (emExecucao > 0) return 'Em execução';
    return 'Não iniciado';
  }

  ngOnInit() { this.filtrar(); }

  protected filtrar() {
    this.loading.set(true);
    this.service.listar(this.filtroStatus ?? undefined).subscribe({
      next: (data) => { this.ordens.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected abrirNovaOS() {
    this.dialog.open(OsFormDialogComponent, { width: '880px', maxWidth: '95vw', data: {} })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.snackBar.open('OS criada com sucesso!', 'Fechar', { duration: 3000 });
        this.filtrar();
      });
  }
}
