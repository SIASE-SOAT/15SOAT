import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ServicoRequest, ServicoResponse } from '../../../core/models/servico.model';

@Component({
  selector: 'app-servico-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  templateUrl: './servico-form-dialog.component.html',
  styleUrl: './servico-form-dialog.component.scss',
})
export class ServicoFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly dialogRef = inject(MatDialogRef<ServicoFormDialogComponent>);
  protected readonly data: { servico?: ServicoResponse } = inject(MAT_DIALOG_DATA) ?? {};

  protected readonly form = this.fb.nonNullable.group({
    nome: [this.data.servico?.nome ?? '', Validators.required],
    descricao: [this.data.servico?.descricao ?? ''],
    preco: [this.data.servico?.preco ?? 0, [Validators.required, Validators.min(0.01)]],
    tempoEstimadoMinutos: [this.data.servico?.tempoEstimadoMinutos ?? (null as number | null)],
  });

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const payload: ServicoRequest = {
      nome: raw.nome,
      descricao: raw.descricao || undefined,
      preco: raw.preco,
      tempoEstimadoMinutos: raw.tempoEstimadoMinutos ?? undefined,
    };
    this.dialogRef.close(payload);
  }
}
