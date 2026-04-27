import { ChangeDetectionStrategy, Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../core/services/auth.service';
import { OrdemDeServicoService } from '../../core/services/ordem-de-servico.service';
import { OrdemDeServicoResponse, StatusOS } from '../../core/models/ordem-de-servico.model';
import { extractApiError } from '../../core/utils/api-error.util';

const STATUSES_MECANICO: StatusOS[] = ['APROVADO', 'EM_EXECUCAO', 'EM_DIAGNOSTICO', 'RECEBIDA'];

const STATUS_CONFIG: Record<string, { label: string; cor: string; bg: string }> = {
  APROVADO:        { label: 'Aprovado',       cor: '#065f46', bg: '#d1fae5' },
  EM_EXECUCAO:     { label: 'Em Execução',    cor: '#1e40af', bg: '#dbeafe' },
  EM_DIAGNOSTICO:  { label: 'Em Diagnóstico', cor: '#5b21b6', bg: '#ede9fe' },
  RECEBIDA:        { label: 'Recebida',        cor: '#374151', bg: '#f3f4f6' },
};

@Component({
  selector: 'app-mecanico',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatCardModule],
  templateUrl: './mecanico.component.html',
  styleUrl: './mecanico.component.scss',
})
export class MecanicoComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly osService = inject(OrdemDeServicoService);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly erro = signal('');
  protected readonly ordens = signal<OrdemDeServicoResponse[]>([]);

  protected readonly aprovadas = computed(() =>
    this.ordens().filter(o => o.status === 'APROVADO')
  );
  protected readonly emExecucao = computed(() =>
    this.ordens().filter(o => o.status === 'EM_EXECUCAO')
  );
  protected readonly pendentes = computed(() =>
    this.ordens().filter(o => o.status === 'EM_DIAGNOSTICO' || o.status === 'RECEBIDA')
  );

  protected readonly statusConfig = STATUS_CONFIG;

  ngOnInit() {
    if (this.authService.isAuthenticated()) {
      this.carregarOrdens();
    } else {
      this.authService.login({ username: 'mecanico', password: 'mecanico123' }).subscribe({
        next: () => this.carregarOrdens(),
        error: (err) => {
          this.loading.set(false);
          this.erro.set(extractApiError(err, 'Não foi possível autenticar o mecânico.'));
        },
      });
    }
  }

  private carregarOrdens() {
    this.osService.listar().subscribe({
      next: (todas) => {
        this.ordens.set(todas.filter(o => STATUSES_MECANICO.includes(o.status)));
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.erro.set(extractApiError(err, 'Não foi possível carregar as ordens.'));
      },
    });
  }

  protected verOS(id: string) {
    this.router.navigate(['/mecanico/ordens', id]);
  }
}
