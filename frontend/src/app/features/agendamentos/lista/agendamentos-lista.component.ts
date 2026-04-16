import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AgendamentoResponse } from '../../../core/models/agendamento.model';
import { AgendamentoService } from '../../../core/services/agendamento.service';
import { AgendamentoFormDialogComponent } from '../dialogs/agendamento-form-dialog.component';

@Component({
  selector: 'app-agendamentos-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule],
  templateUrl: './agendamentos-lista.component.html',
})
export class AgendamentosListaComponent implements OnInit {
  private readonly service = inject(AgendamentoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly agendamentos = signal<AgendamentoResponse[]>([]);
  protected readonly colunas = ['dataHora', 'cliente', 'veiculo', 'status', 'acoes'];

  ngOnInit() { this.carregar(); }

  private carregar() {
    this.loading.set(true);
    this.service.listar().subscribe({
      next: (data) => { this.agendamentos.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected abrirNovoAgendamento() {
    this.dialog.open(AgendamentoFormDialogComponent, { width: '500px', data: {} })
      .afterClosed().subscribe(result => {
        if (!result) return;
        this.service.criar(result).subscribe({
          next: () => { this.snackBar.open('Agendamento criado!', 'Fechar', { duration: 3000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao criar agendamento.', 'Fechar', { duration: 3000 }),
        });
      });
  }

  protected confirmar(a: AgendamentoResponse) {
    this.service.confirmar(a.id).subscribe({
      next: () => { this.snackBar.open('Agendamento confirmado!', 'Fechar', { duration: 3000 }); this.carregar(); },
      error: () => this.snackBar.open('Erro ao confirmar agendamento.', 'Fechar', { duration: 3000 }),
    });
  }

  protected cancelar(a: AgendamentoResponse) {
    this.service.cancelar(a.id).subscribe({
      next: () => { this.snackBar.open('Agendamento cancelado.', 'Fechar', { duration: 3000 }); this.carregar(); },
      error: () => this.snackBar.open('Erro ao cancelar agendamento.', 'Fechar', { duration: 3000 }),
    });
  }
}
