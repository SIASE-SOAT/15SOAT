import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ClienteRequest, ClienteResponse } from '../../../core/models/cliente.model';

@Component({
  selector: 'app-cliente-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  templateUrl: './cliente-form-dialog.component.html',
  styleUrl: './cliente-form-dialog.component.scss',
})
export class ClienteFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly dialogRef = inject(MatDialogRef<ClienteFormDialogComponent>);
  protected readonly data: { cliente?: ClienteResponse } = inject(MAT_DIALOG_DATA) ?? {};

  protected readonly form = this.fb.nonNullable.group({
    nome: [this.data.cliente?.nome ?? '', Validators.required],
    tipoPessoa: [this.data.cliente?.tipoPessoa ?? 'PF', Validators.required],
    documento: [this.data.cliente?.documento ?? '', Validators.required],
    email: [this.data.cliente?.email ?? ''],
    telefone: [this.data.cliente?.telefone ?? ''],
    endereco: [this.data.cliente?.endereco ?? ''],
  });

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialogRef.close(this.form.getRawValue() as ClienteRequest);
  }
}
