import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ClienteResponse } from '../../../core/models/cliente.model';
import { VeiculoRequest, VeiculoResponse } from '../../../core/models/veiculo.model';
import { ClienteService } from '../../../core/services/cliente.service';

@Component({
  selector: 'app-veiculo-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  templateUrl: './veiculo-form-dialog.component.html',
  styleUrl: './veiculo-form-dialog.component.scss',
})
export class VeiculoFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly clienteService = inject(ClienteService);
  protected readonly dialogRef = inject(MatDialogRef<VeiculoFormDialogComponent>);
  protected readonly data: { veiculo?: VeiculoResponse } = inject(MAT_DIALOG_DATA) ?? {};

  protected readonly clientes = signal<ClienteResponse[]>([]);
  protected readonly currentYear = new Date().getFullYear();

  protected readonly form = this.fb.nonNullable.group({
    placa: [this.data.veiculo?.placa ?? '', Validators.required],
    marca: [this.data.veiculo?.marca ?? '', Validators.required],
    modelo: [this.data.veiculo?.modelo ?? '', Validators.required],
    ano: [this.data.veiculo?.ano ?? new Date().getFullYear(), [Validators.required, Validators.min(1900), Validators.max(new Date().getFullYear() + 1)]],
    cor: [this.data.veiculo?.cor ?? ''],
    clienteId: [this.data.veiculo?.clienteId ?? '', Validators.required],
  });

  ngOnInit() {
    this.clienteService.listar().subscribe(c => this.clientes.set(c));
  }

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialogRef.close(this.form.getRawValue() as VeiculoRequest);
  }
}
