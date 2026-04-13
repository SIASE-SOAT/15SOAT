import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { VeiculoResponse } from '../../../core/models/veiculo.model';
import { VeiculoService } from '../../../core/services/veiculo.service';
import { VeiculoFormDialogComponent } from '../dialogs/veiculo-form-dialog.component';

@Component({
  selector: 'app-veiculos-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './veiculos-lista.component.html',
})
export class VeiculosListaComponent implements OnInit {
  private readonly service = inject(VeiculoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly veiculos = signal<VeiculoResponse[]>([]);
  protected readonly colunas = ['placa', 'veiculo', 'cor', 'cliente', 'acoes'];

  ngOnInit() { this.carregar(); }

  private carregar() {
    this.loading.set(true);
    this.service.listar().subscribe({
      next: (data) => { this.veiculos.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected abrirNovoVeiculo() {
    this.dialog.open(VeiculoFormDialogComponent, { width: '520px', data: {} })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.criar(result).subscribe({
          next: () => { this.snackBar.open('Veículo criado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao criar veículo.', 'Fechar', { duration: 3000 }),
        });
      });
  }

  protected editarVeiculo(v: VeiculoResponse) {
    this.dialog.open(VeiculoFormDialogComponent, { width: '520px', data: { veiculo: v } })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.atualizar(v.id, result).subscribe({
          next: () => { this.snackBar.open('Veículo atualizado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao atualizar veículo.', 'Fechar', { duration: 3000 }),
        });
      });
  }
}
