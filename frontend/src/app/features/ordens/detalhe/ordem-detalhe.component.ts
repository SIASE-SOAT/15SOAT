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
import { OrdemDeServicoResponse } from '../../../core/models/ordem-de-servico.model';
import { PagamentoResponse } from '../../../core/models/pagamento.model';
import { OrdemDeServicoService } from '../../../core/services/ordem-de-servico.service';
import { PagamentoService } from '../../../core/services/pagamento.service';
import { PagamentoFormDialogComponent } from '../dialogs/pagamento-form-dialog.component';


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

  protected podeAvancar() {
    const s = this.os()?.status;
    return s && !['ENTREGUE', 'CANCELADA'].includes(s);
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
      error: () => this.actionLoading.set(false),
    });
  }

  protected cancelar() {
    this.actionLoading.set(true);
    this.service.cancelar(this.id()).subscribe({
      next: (os) => { this.os.set(os); this.actionLoading.set(false); },
      error: () => this.actionLoading.set(false),
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
        error: () => {
          this.actionLoading.set(false);
          this.snackBar.open('Erro ao registrar pagamento.', 'Fechar', { duration: 3000 });
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
      error: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Erro ao confirmar pagamento.', 'Fechar', { duration: 3000 });
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
      error: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Erro ao cancelar pagamento.', 'Fechar', { duration: 3000 });
      },
    });
  }
}
