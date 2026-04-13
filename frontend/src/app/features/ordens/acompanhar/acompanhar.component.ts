import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatStepperModule } from '@angular/material/stepper';
import { OrdemDeServicoService } from '../../../core/services/ordem-de-servico.service';
import { OrdemDeServicoResponse, StatusOS } from '../../../core/models/ordem-de-servico.model';

const STATUS_STEPS: StatusOS[] = [
  'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE',
];

@Component({
  selector: 'app-acompanhar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatStepperModule],
  templateUrl: './acompanhar.component.html',
})
export class AcompanharComponent implements OnInit {
  private readonly service = inject(OrdemDeServicoService);

  readonly numero = input.required<string>();

  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly os = signal<OrdemDeServicoResponse | null>(null);
  protected readonly STATUS_STEPS = STATUS_STEPS;

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
      AGUARDANDO_APROVACAO: 'Aprovação', EM_EXECUCAO: 'Execução',
      FINALIZADA: 'Finalizada', ENTREGUE: 'Entregue', CANCELADA: 'Cancelada',
    };
    return labels[status];
  }

  ngOnInit() {
    this.service.acompanharPorNumero(this.numero()).subscribe({
      next: (os) => { this.os.set(os); this.loading.set(false); },
      error: () => {
        this.error.set('OS não encontrada. Verifique o número e tente novamente.');
        this.loading.set(false);
      },
    });
  }
}
