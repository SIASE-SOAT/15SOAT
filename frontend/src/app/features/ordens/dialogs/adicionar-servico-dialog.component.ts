import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ServicoResponse } from '../../../core/models/servico.model';
import { ItemServicoRequest } from '../../../core/models/ordem-de-servico.model';
import { ServicoService } from '../../../core/services/servico.service';

@Component({
  selector: 'app-adicionar-servico-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe, ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Adicionar Serviço</h2>

    <mat-dialog-content>
      <form [formGroup]="form" novalidate class="dialog-form">

        @if (loading()) {
          <div class="dialog-loader">
            <mat-spinner diameter="40" />
          </div>
        } @else {

          <mat-form-field appearance="outline">
            <mat-label>Serviço</mat-label>
            <mat-select formControlName="servicoId">
              @for (s of servicos(); track s.id) {
                <mat-option [value]="s.id">
                  {{ s.nome }}
                  @if (s.tempoEstimadoMinutos) {
                    <span class="opt-meta">({{ s.tempoEstimadoMinutos }} min)</span>
                  }
                </mat-option>
              }
              @if (servicos().length === 0) {
                <mat-option disabled>Nenhum serviço disponível</mat-option>
              }
            </mat-select>
            @if (form.controls.servicoId.hasError('required') && form.controls.servicoId.touched) {
              <mat-error>Obrigatório</mat-error>
            }
          </mat-form-field>

          @if (servicoSelecionado(); as s) {
            <div class="preview-card">
              <div class="preview-row">
                <span class="preview-label">Preço</span>
                <span class="preview-value">R$ {{ s.preco | number:'1.2-2' }}</span>
              </div>
              @if (s.tempoEstimadoMinutos) {
                <div class="preview-row">
                  <span class="preview-label">Tempo estimado</span>
                  <span class="preview-value">{{ s.tempoEstimadoMinutos }} min</span>
                </div>
              }
              @if (s.descricao) {
                <p class="preview-desc">{{ s.descricao }}</p>
              }
            </div>
          }

          <mat-form-field appearance="outline">
            <mat-label>Observações (opcional)</mat-label>
            <textarea matInput formControlName="observacoes" rows="2"></textarea>
          </mat-form-field>

        }
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Cancelar</button>
      <button
        mat-flat-button
        color="primary"
        (click)="submit()"
        [disabled]="form.invalid || loading()"
      >
        Adicionar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 12px; min-width: 400px; padding-top: 8px; }
    .dialog-loader { display: flex; justify-content: center; padding: 32px 0; }
    mat-form-field { width: 100%; }
    .opt-meta { color: #888; font-size: 0.85em; margin-left: 4px; }
    .preview-card {
      background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;
      padding: 12px 16px; display: flex; flex-direction: column; gap: 6px;
    }
    .preview-row { display: flex; justify-content: space-between; font-size: 0.9rem; }
    .preview-label { color: #64748b; }
    .preview-value { font-weight: 600; }
    .preview-desc { font-size: 0.85rem; color: #64748b; margin: 4px 0 0; }
    mat-dialog-actions { gap: 8px; padding-top: 8px; }
  `],
})
export class AdicionarServicoDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly servicoService = inject(ServicoService);
  protected readonly dialogRef = inject(MatDialogRef<AdicionarServicoDialogComponent>);
  protected readonly data = inject<{ orderId: string }>(MAT_DIALOG_DATA);

  protected readonly servicos = signal<ServicoResponse[]>([]);
  protected readonly loading = signal(true);

  protected readonly form = this.fb.group({
    servicoId: ['', Validators.required],
    observacoes: [''],
  });

  private readonly formValue = toSignal(this.form.valueChanges, {
    initialValue: this.form.value,
  });

  protected readonly servicoSelecionado = computed(() => {
    const id = this.formValue().servicoId;
    return id ? this.servicos().find(s => s.id === id) : null;
  });

  ngOnInit() {
    this.servicoService.listar().subscribe({
      next: (servicos) => {
        this.servicos.set(servicos.filter(s => s.ativo));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const payload: ItemServicoRequest = {
      servicoId: v.servicoId!,
      observacoes: v.observacoes || undefined,
    };

    this.dialogRef.close(payload);
  }
}
