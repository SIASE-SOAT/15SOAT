import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PecaResponse } from '../../../core/models/peca.model';
import { PecaService } from '../../../core/services/peca.service';
import { PecaFormDialogComponent } from '../dialogs/peca-form-dialog.component';
import { EstoqueDialogComponent } from '../dialogs/estoque-dialog.component';
import { extractApiError } from '../../../core/utils/api-error.util';

@Component({
  selector: 'app-pecas-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './pecas-lista.component.html',
  styleUrl: './pecas-lista.component.scss',
})
export class PecasListaComponent implements OnInit {
  private readonly service = inject(PecaService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly pecas = signal<PecaResponse[]>([]);
  protected readonly colunas = ['codigo', 'nome', 'preco', 'estoque', 'acoes'];

  ngOnInit() { this.carregar(); }

  protected countCritico(): number {
    return this.pecas().filter(p => p.estoqueAbaixoMinimo).length;
  }

  private carregar() {
    this.loading.set(true);
    this.service.listar().subscribe({
      next: (data) => { this.pecas.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected abrirNovaPeca() {
    this.dialog.open(PecaFormDialogComponent, { width: '520px', data: {} })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.criar(result).subscribe({
          next: () => { this.snackBar.open('Peça criada!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: (err) => this.snackBar.open(extractApiError(err, 'Erro ao criar peça.'), 'Fechar', { duration: 5000 }),
        });
      });
  }

  protected editarPeca(p: PecaResponse) {
    this.dialog.open(PecaFormDialogComponent, { width: '520px', data: { peca: p } })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.atualizar(p.id, result).subscribe({
          next: () => { this.snackBar.open('Peça atualizada!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: (err) => this.snackBar.open(extractApiError(err, 'Erro ao atualizar peça.'), 'Fechar', { duration: 5000 }),
        });
      });
  }

  protected movimentarEstoque(p: PecaResponse) {
    this.dialog.open(EstoqueDialogComponent, { width: '420px', data: { peca: p } })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.movimentarEstoque(p.id, result).subscribe({
          next: () => { this.snackBar.open('Estoque atualizado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: (err) => this.snackBar.open(extractApiError(err, 'Erro ao movimentar estoque.'), 'Fechar', { duration: 5000 }),
        });
      });
  }
}
