import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ServicoResponse } from '../../../core/models/servico.model';
import { ServicoService } from '../../../core/services/servico.service';
import { ServicoFormDialogComponent } from '../dialogs/servico-form-dialog.component';

@Component({
  selector: 'app-servicos-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    MatTableModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    MatChipsModule, MatTooltipModule, MatCardModule, MatDividerModule,
  ],
  templateUrl: './servicos-lista.component.html',
})
export class ServicosListaComponent implements OnInit {
  private readonly service = inject(ServicoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly servicos = signal<ServicoResponse[]>([]);
  protected readonly colunas = ['nome', 'preco', 'tempo', 'insumos', 'acoes'];

  ngOnInit() { this.carregar(); }

  private carregar() {
    this.loading.set(true);
    this.service.listar().subscribe({
      next: (data) => { this.servicos.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected abrirNovoServico() {
    this.dialog.open(ServicoFormDialogComponent, { width: '480px', data: {} })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.criar(result).subscribe({
          next: () => { this.snackBar.open('Serviço criado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao criar serviço.', 'Fechar', { duration: 3000 }),
        });
      });
  }

  protected editarServico(s: ServicoResponse) {
    this.dialog.open(ServicoFormDialogComponent, { width: '480px', data: { servico: s } })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.atualizar(s.id, result).subscribe({
          next: () => { this.snackBar.open('Serviço atualizado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao atualizar serviço.', 'Fechar', { duration: 3000 }),
        });
      });
  }
}
