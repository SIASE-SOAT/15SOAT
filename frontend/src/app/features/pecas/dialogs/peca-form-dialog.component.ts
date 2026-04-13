import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { PecaRequest, PecaResponse, PecaUpdateRequest } from '../../../core/models/peca.model';

@Component({
  selector: 'app-peca-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './peca-form-dialog.component.html',
})
export class PecaFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly dialogRef = inject(MatDialogRef<PecaFormDialogComponent>);
  protected readonly data: { peca?: PecaResponse } = inject(MAT_DIALOG_DATA) ?? {};

  protected readonly isEdit = !!this.data.peca;

  protected readonly form = this.fb.nonNullable.group({
    codigo: [this.data.peca?.codigo ?? '', Validators.required],
    nome: [this.data.peca?.nome ?? '', Validators.required],
    descricao: [this.data.peca?.descricao ?? ''],
    preco: [this.data.peca?.preco ?? 0, [Validators.required, Validators.min(0.01)]],
    quantidadeEstoque: [this.data.peca?.quantidadeEstoque ?? 0, [Validators.required, Validators.min(0)]],
    estoqueMinimo: [this.data.peca?.estoqueMinimo ?? 1, [Validators.required, Validators.min(0)]],
    unidadeMedida: [this.data.peca?.unidadeMedida ?? 'un', Validators.required],
  });

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    if (this.isEdit) {
      const payload: PecaUpdateRequest = {
        codigo: raw.codigo, nome: raw.nome,
        descricao: raw.descricao || undefined, preco: raw.preco,
        estoqueMinimo: raw.estoqueMinimo, unidadeMedida: raw.unidadeMedida,
      };
      this.dialogRef.close(payload);
    } else {
      const payload: PecaRequest = {
        ...raw, descricao: raw.descricao || undefined,
      };
      this.dialogRef.close(payload);
    }
  }
}
