import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PecaResponse } from '../../../core/models/peca.model';
import { PedidoCompraRequest } from '../../../core/models/pedido-compra.model';
import { PecaService } from '../../../core/services/peca.service';

@Component({
  selector: 'app-pedido-compra-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  templateUrl: './pedido-compra-form-dialog.component.html',
  styleUrl: './pedido-compra-form-dialog.component.scss',
})
export class PedidoCompraFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly pecaService = inject(PecaService);
  protected readonly dialogRef = inject(MatDialogRef<PedidoCompraFormDialogComponent>);
  protected readonly data: { pecaId?: string } = inject(MAT_DIALOG_DATA) ?? {};

  protected readonly pecas = signal<PecaResponse[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    pecaId: [this.data.pecaId ?? '', Validators.required],
    quantidadeSolicitada: [1, [Validators.required, Validators.min(1)]],
    observacoes: [''],
  });

  ngOnInit() {
    this.pecaService.listarTodas().subscribe(p => this.pecas.set(p));
  }

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const payload: PedidoCompraRequest = {
      pecaId: raw.pecaId,
      quantidadeSolicitada: raw.quantidadeSolicitada,
      observacoes: raw.observacoes || undefined,
    };
    this.dialogRef.close(payload);
  }
}
