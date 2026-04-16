import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ClienteResponse } from '../../../core/models/cliente.model';
import { ClienteService } from '../../../core/services/cliente.service';
import { ClienteFormDialogComponent } from '../dialogs/cliente-form-dialog.component';

@Component({
  selector: 'app-clientes-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './clientes-lista.component.html',
  styleUrl: './clientes-lista.component.scss',
})
export class ClientesListaComponent implements OnInit {
  private readonly service = inject(ClienteService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly clientes = signal<ClienteResponse[]>([]);
  protected readonly colunas = ['nome', 'documento', 'tipo', 'email', 'telefone', 'acoes'];

  ngOnInit() { this.carregar(); }

  protected countTipo(tipo: string): number {
    return this.clientes().filter(c => c.tipoPessoa === tipo).length;
  }

  private carregar() {
    this.loading.set(true);
    this.service.listar().subscribe({
      next: (data) => { this.clientes.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected abrirNovoCliente() {
    this.dialog.open(ClienteFormDialogComponent, { width: '500px', data: {} })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.criar(result).subscribe({
          next: () => { this.snackBar.open('Cliente criado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao criar cliente.', 'Fechar', { duration: 3000 }),
        });
      });
  }

  protected editarCliente(c: ClienteResponse) {
    this.dialog.open(ClienteFormDialogComponent, { width: '500px', data: { cliente: c } })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.atualizar(c.id, result).subscribe({
          next: () => { this.snackBar.open('Cliente atualizado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao atualizar cliente.', 'Fechar', { duration: 3000 }),
        });
      });
  }
}
