import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { OrdemDeServicoService } from '../../../core/services/ordem-de-servico.service';
import { OrdemDeServicoResponse, StatusOS } from '../../../core/models/ordem-de-servico.model';
import { extractApiError } from '../../../core/utils/api-error.util';

const STATUS_STEPS: StatusOS[] = [
  'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'APROVADO', 'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE',
];
const FINALIZADOS = new Set<StatusOS>(['FINALIZADA', 'ENTREGUE', 'CANCELADA']);

@Component({
  selector: 'app-acompanhar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './acompanhar.component.html',
  styleUrl: './acompanhar.component.scss',
})
export class AcompanharComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly service = inject(OrdemDeServicoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly buscaForm = new FormGroup({
    numero: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected readonly loading = signal(false);
  protected readonly actionLoading = signal(false);
  protected readonly error = signal('');
  protected readonly os = signal<OrdemDeServicoResponse | null>(null);
  protected readonly minhasOrdens = signal<OrdemDeServicoResponse[]>([]);
  protected readonly loadingMinhas = signal(false);
  protected readonly STATUS_STEPS = STATUS_STEPS;
  protected readonly ordensEmAndamento = computed(() =>
    this.minhasOrdens().filter(ordem => !FINALIZADOS.has(ordem.status))
  );
  protected readonly ordensFinalizadas = computed(() =>
    this.minhasOrdens().filter(ordem => FINALIZADOS.has(ordem.status))
  );

  protected stepIndex() {
    const idx = STATUS_STEPS.indexOf(this.os()?.status as StatusOS);
    return idx >= 0 ? idx : 0;
  }

  protected isCompleted(step: StatusOS) {
    return STATUS_STEPS.indexOf(step) < this.stepIndex();
  }

  protected labelFor(status: StatusOS): string {
    const labels: Record<StatusOS, string> = {
      RECEBIDA: 'Recebida', EM_DIAGNOSTICO: 'Diagnóstico',
      AGUARDANDO_APROVACAO: 'Aguardando', APROVADO: 'Aprovado',
      EM_EXECUCAO: 'Execução', FINALIZADA: 'Finalizada',
      ENTREGUE: 'Entregue', CANCELADA: 'Cancelada',
    };
    return labels[status];
  }

  protected podeAprovarOuRecusar() {
    return this.os()?.status === 'AGUARDANDO_APROVACAO';
  }

  ngOnInit() {
    const numeroRota = this.route.snapshot.paramMap.get('numero');
    const numeroQuery = this.route.snapshot.queryParamMap.get('numero');
    const numeroInicial = (numeroRota || numeroQuery || '').trim().toUpperCase();

    if (numeroInicial) {
      this.buscaForm.controls.numero.setValue(numeroInicial);
      this.buscar();
    }

    if (this.auth.token()) {
      this.carregarMinhasOrdens();
    }
  }

  private carregarMinhasOrdens() {
    this.loadingMinhas.set(true);
    this.service.minhasOrdens()
      .pipe(finalize(() => this.loadingMinhas.set(false)))
      .subscribe({
        next: ordens => this.minhasOrdens.set(ordens),
        error: () => this.minhasOrdens.set([]),
      });
  }

  protected selecionar(ordem: OrdemDeServicoResponse) {
    this.os.set(ordem);
    void this.router.navigate(['/acompanhar', ordem.numero], { replaceUrl: true });
  }

  protected buscar() {
    const numero = this.buscaForm.controls.numero.value.trim().toUpperCase();
    if (!numero) {
      this.error.set('Informe o número da OS para consultar.');
      this.os.set(null);
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.service.acompanharPorNumero(numero).subscribe({
      next: (os) => {
        this.os.set(os);
        this.loading.set(false);
        void this.router.navigate(['/acompanhar', numero], { replaceUrl: true });
      },
      error: () => {
        this.error.set('OS não encontrada. Verifique o número e tente novamente.');
        this.os.set(null);
        this.loading.set(false);
      },
    });
  }

  protected aprovarOrcamento() {
    const numero = this.os()?.numero;
    if (!numero) return;

    this.actionLoading.set(true);
    this.service.aprovarOrcamentoPorNumero(numero).subscribe({
      next: (os) => {
        this.os.set(os);
        this.actionLoading.set(false);
        this.snackBar.open('Orçamento aprovado com sucesso!', 'Fechar', { duration: 4000 });
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(extractApiError(err, 'Não foi possível aprovar o orçamento.'), 'Fechar', { duration: 5000 });
      },
    });
  }

  protected recusarOrcamento() {
    const numero = this.os()?.numero;
    if (!numero) return;

    this.actionLoading.set(true);
    this.service.recusarOrcamentoPorNumero(numero).subscribe({
      next: (os) => {
        this.os.set(os);
        this.actionLoading.set(false);
        this.snackBar.open('Orçamento recusado. A OS foi cancelada.', 'Fechar', { duration: 4500 });
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.snackBar.open(extractApiError(err, 'Não foi possível recusar o orçamento.'), 'Fechar', { duration: 5000 });
      },
    });
  }
}
