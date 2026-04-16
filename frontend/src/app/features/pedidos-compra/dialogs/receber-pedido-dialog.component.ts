import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { PedidoCompraResponse } from '../../../core/models/pedido-compra.model';

@Component({
  selector: 'app-receber-pedido-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './receber-pedido-dialog.component.html',
})
export class ReceberPedidoDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly dialogRef = inject(MatDialogRef<ReceberPedidoDialogComponent>);
  protected readonly data: { pedido: PedidoCompraResponse } = inject(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    quantidade: [
      this.data.pedido.quantidadeSolicitada,
      [Validators.required, Validators.min(1), Validators.max(this.data.pedido.quantidadeSolicitada)],
    ],
  });

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialogRef.close(this.form.controls.quantidade.value);
  }
}
