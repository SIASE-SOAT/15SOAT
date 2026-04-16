import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { PagamentoRequest } from '../../../core/models/pagamento.model';

@Component({
  selector: 'app-pagamento-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  templateUrl: './pagamento-form-dialog.component.html',
})
export class PagamentoFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly dialogRef = inject(MatDialogRef<PagamentoFormDialogComponent>);
  protected readonly data: { total: number; osNumero: string } = inject(MAT_DIALOG_DATA);

  protected readonly formas = [
    { value: 'DINHEIRO',        label: 'Dinheiro' },
    { value: 'PIX',             label: 'PIX' },
    { value: 'CARTAO_DEBITO',   label: 'Cartão de Débito' },
    { value: 'CARTAO_CREDITO',  label: 'Cartão de Crédito' },
    { value: 'TRANSFERENCIA',   label: 'Transferência Bancária' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    formaPagamento: ['PIX', Validators.required],
    valor: [this.data.total, [Validators.required, Validators.min(0.01)]],
  });

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialogRef.close(this.form.getRawValue() as PagamentoRequest);
  }
}
