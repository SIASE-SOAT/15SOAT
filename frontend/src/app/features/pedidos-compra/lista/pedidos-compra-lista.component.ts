import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PedidoCompraResponse } from '../../../core/models/pedido-compra.model';
import { PedidoCompraService } from '../../../core/services/pedido-compra.service';
import { PedidoCompraFormDialogComponent } from '../dialogs/pedido-compra-form-dialog.component';
import { ReceberPedidoDialogComponent } from '../dialogs/receber-pedido-dialog.component';

@Component({
  selector: 'app-pedidos-compra-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule],
  templateUrl: './pedidos-compra-lista.component.html',
})
export class PedidosCompraListaComponent implements OnInit {
  private readonly service = inject(PedidoCompraService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly pedidos = signal<PedidoCompraResponse[]>([]);
  protected readonly colunas = ['peca', 'qtd', 'qtdRecebida', 'status', 'data', 'acoes'];

  ngOnInit() { this.carregar(); }

  private carregar() {
    this.loading.set(true);
    this.service.listar().subscribe({
      next: (data) => { this.pedidos.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected abrirNovoPedido() {
    this.dialog.open(PedidoCompraFormDialogComponent, { width: '480px', data: {} })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.criar(result).subscribe({
          next: () => { this.snackBar.open('Pedido criado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao criar pedido.', 'Fechar', { duration: 3000 }),
        });
      });
  }

  protected aprovar(p: PedidoCompraResponse) {
    this.service.aprovar(p.id).subscribe({
      next: () => { this.snackBar.open('Pedido aprovado!', 'Fechar', { duration: 3000 }); this.carregar(); },
      error: () => this.snackBar.open('Erro ao aprovar pedido.', 'Fechar', { duration: 3000 }),
    });
  }

  protected receber(p: PedidoCompraResponse) {
    this.dialog.open(ReceberPedidoDialogComponent, { width: '380px', data: { pedido: p } })
      .afterClosed().subscribe((quantidade: number | undefined) => {
        if (!quantidade) return;
        this.service.receber(p.id, quantidade).subscribe({
          next: () => { this.snackBar.open('Recebimento registrado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao registrar recebimento.', 'Fechar', { duration: 3000 }),
        });
      });
  }

  protected cancelar(p: PedidoCompraResponse) {
    this.service.cancelar(p.id).subscribe({
      next: () => { this.snackBar.open('Pedido cancelado.', 'Fechar', { duration: 3000 }); this.carregar(); },
      error: () => this.snackBar.open('Erro ao cancelar pedido.', 'Fechar', { duration: 3000 }),
    });
  }
}
