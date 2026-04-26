import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ItemServicoResponse, OrdemDeServicoResponse, StatusOS } from '../../../core/models/ordem-de-servico.model';
import { PagamentoResponse } from '../../../core/models/pagamento.model';
import { OrdemDeServicoService } from '../../../core/services/ordem-de-servico.service';
import { PagamentoService } from '../../../core/services/pagamento.service';
import { PagamentoFormDialogComponent } from '../dialogs/pagamento-form-dialog.component';
import { AdicionarPecaDialogComponent } from '../dialogs/adicionar-peca-dialog.component';
import { AdicionarServicoDialogComponent } from '../dialogs/adicionar-servico-dialog.component';
import { extractApiError } from '../../../core/utils/api-error.util';


@Component({
  selector: 'app-ordem-detalhe',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe, DatePipe, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule, MatDividerModule, MatProgressSpinnerModule, MatTooltipModule,
  ],
  templateUrl: './ordem-detalhe.component.html',
  styleUrl: './ordem-detalhe.component.scss',
})
export class OrdemDetalheComponent implements OnInit {
  private readonly service = inject(OrdemDeServicoService);
  private readonly pagamentoService = inject(PagamentoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly id = input.required<string>();

  protected readonly loading = signal(true);
  protected readonly actionLoading = signal(false);
  protected readonly os = signal<OrdemDeServicoResponse | null>(null);
  protected readonly pagamento = signal<PagamentoResponse | null>(null);

  private static readonly PROXIMO_STATUS: Record<StatusOS, { label: string; icon: string } | null> = {
    RECEBIDA:             { label: 'Iniciar Diagnóstico',         icon: 'manage_search'      },
    EM_DIAGNOSTICO:       { label: 'Enviar para Aprovação',       icon: 'send'               },
    AGUARDANDO_APROVACAO: { label: 'Aprovar e Iniciar Execução',  icon: 'engineering'        },
    EM_EXECUCAO:          { label: 'Finalizar OS',                icon: 'task_alt'           },
    FINALIZADA:           { label: 'Confirmar Entrega',           icon: 'local_shipping'     },
    ENTREGUE:             null,
    CANCELADA:            null,
  };

  protected proximoStatusInfo() {
    const s = this.os()?.status;
    return s ? OrdemDetalheComponent.PROXIMO_STATUS[s] : null;
  }

  protected podeAvancar() {
    return !!this.proximoStatusInfo();
  }

  protected podeCancelar() {
    const s = this.os()?.status;
    return s && ['RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO'].includes(s);
  }

  protected podeRegistrarPagamento() {
    const s = this.os()?.status;
    const p = this.pagamento();
    return s === 'FINALIZADA' && (!p || p.status === 'CANCELADO');
  }

  protected podeConfirmarPagamento() {
    return this.pagamento()?.status === 'PENDENTE';
  }

  protected podeCancelarPagamento() {
    return this.pagamento()?.status === 'PENDENTE';
  }

  protected podeAdicionarPeca() {
    const s = this.os()?.status;
    return s && ['RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO'].includes(s);
  }

  protected podeAdicionarServico() {
    const s = this.os()?.status;
    return s && ['RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO'].includes(s);
  }

  protected podeIniciarItem() {
    return this.os()?.status === 'EM_EXECUCAO';
  }

  protected podeFinalizarItem() {
    return this.os()?.status === 'EM_EXECUCAO';
  }

  protected itemStatusLabel(item: ItemServicoResponse) {
    if (item.dataFimExecucao) return 'Finalizado';
    if (item.dataInicioExecucao) return 'Em execução';
    return 'Não iniciado';
  }

  protected itemStatusClass(item: ItemServicoResponse) {
    if (item.dataFimExecucao) return 'item-status-finalizado';
    if (item.dataInicioExecucao) return 'item-status-execucao';
    return 'item-status-nao-iniciado';
  }

  ngOnInit() {
    this.carregarOS();
  }

  private carregarOS() {
    this.service.buscarPorId(this.id()).subscribe({
      next: (os) => {
        this.os.set(os);
        this.loading.set(false);
        this.carregarPagamento();
      },
      error: () => this.loading.set(false),
    });
  }

  private carregarPagamento() {
    this.pagamentoService.buscarPorOS(this.id()).subscribe({
      next: (p) => this.pagamento.set(p),
      error: () => this.pagamento.set(null),
    });
  }

  protected avancar() {
    this.actionLoading.set(true);
    this.service.avancarStatus(this.id()).subscribe({
      next: (os) => { this.os.set(os); this.actionLoading.set(false); },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(extractApiError(err, 'Erro ao avançar status da OS.'), 'Fechar', { duration: 5000 });
      },
    });
  }

  protected cancelar() {
    this.actionLoading.set(true);
    this.service.cancelar(this.id()).subscribe({
      next: (os) => { this.os.set(os); this.actionLoading.set(false); },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(extractApiError(err, 'Erro ao cancelar OS.'), 'Fechar', { duration: 5000 });
      },
    });
  }

  protected registrarPagamento() {
    const os = this.os();
    if (!os) return;

    this.dialog.open(PagamentoFormDialogComponent, {
      width: '420px',
      data: { total: os.total, osNumero: os.numero },
    }).afterClosed().subscribe(result => {
      if (!result) return;
      this.actionLoading.set(true);
      this.pagamentoService.registrar(this.id(), result).subscribe({
        next: (p) => {
          this.pagamento.set(p);
          this.actionLoading.set(false);
          this.snackBar.open('Pagamento registrado!', 'Fechar', { duration: 3000 });
        },
        error: (err) => {
          this.actionLoading.set(false);
          this.snackBar.open(extractApiError(err, 'Erro ao registrar pagamento.'), 'Fechar', { duration: 5000 });
        },
      });
    });
  }

  protected confirmarPagamento() {
    const p = this.pagamento();
    if (!p) return;
    this.actionLoading.set(true);
    this.pagamentoService.confirmar(p.id).subscribe({
      next: (updated) => {
        this.pagamento.set(updated);
        this.actionLoading.set(false);
        this.snackBar.open('Pagamento confirmado!', 'Fechar', { duration: 3000 });
        this.carregarOS();
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(extractApiError(err, 'Erro ao confirmar pagamento.'), 'Fechar', { duration: 5000 });
      },
    });
  }

  protected cancelarPagamento() {
    const p = this.pagamento();
    if (!p) return;
    this.actionLoading.set(true);
    this.pagamentoService.cancelar(p.id).subscribe({
      next: (updated) => {
        this.pagamento.set(updated);
        this.actionLoading.set(false);
        this.snackBar.open('Pagamento cancelado.', 'Fechar', { duration: 3000 });
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(extractApiError(err, 'Erro ao cancelar pagamento.'), 'Fechar', { duration: 5000 });
      },
    });
  }

  protected abrirDialogAdicionarPeca() {
    this.dialog.open(AdicionarPecaDialogComponent, {
      width: '480px',
      data: { orderId: this.id() },
    }).afterClosed().subscribe(result => {
      if (!result) return;
      this.actionLoading.set(true);
      this.service.adicionarPeca(this.id(), result).subscribe({
        next: (os) => {
          this.os.set(os);
          this.actionLoading.set(false);
          this.snackBar.open('Peça adicionada!', 'Fechar', { duration: 3000 });
        },
        error: (err) => {
          this.actionLoading.set(false);
          this.snackBar.open(extractApiError(err, 'Erro ao adicionar peça.'), 'Fechar', { duration: 5000 });
        },
      });
    });
  }

  protected abrirDialogAdicionarServico() {
    this.dialog.open(AdicionarServicoDialogComponent, {
      width: '480px',
      data: { orderId: this.id() },
    }).afterClosed().subscribe(result => {
      if (!result) return;
      this.actionLoading.set(true);
      this.service.adicionarServico(this.id(), result).subscribe({
        next: (os) => {
          this.os.set(os);
          this.actionLoading.set(false);
          this.snackBar.open('Serviço adicionado!', 'Fechar', { duration: 3000 });
        },
        error: (err) => {
          this.actionLoading.set(false);
          this.snackBar.open(extractApiError(err, 'Erro ao adicionar serviço.'), 'Fechar', { duration: 5000 });
        },
      });
    });
  }

  protected iniciarExecucaoItem(itemId: string) {
    this.actionLoading.set(true);
    this.service.iniciarExecucaoItem(this.id(), itemId).subscribe({
      next: (os) => {
        this.os.set(os);
        this.actionLoading.set(false);
        this.snackBar.open('Execução iniciada!', 'Fechar', { duration: 3000 });
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(extractApiError(err, 'Erro ao iniciar execução do serviço.'), 'Fechar', { duration: 5000 });
      },
    });
  }

  protected finalizarExecucaoItem(itemId: string) {
    this.actionLoading.set(true);
    this.service.finalizarExecucaoItem(this.id(), itemId).subscribe({
      next: (os) => {
        this.os.set(os);
        this.actionLoading.set(false);
        this.snackBar.open('Execução finalizada!', 'Fechar', { duration: 3000 });
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(extractApiError(err, 'Erro ao finalizar execução do serviço.'), 'Fechar', { duration: 5000 });
      },
    });
  }
}
