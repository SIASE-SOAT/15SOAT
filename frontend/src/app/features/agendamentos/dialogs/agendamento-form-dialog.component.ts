import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { ClienteResponse } from '../../../core/models/cliente.model';
import { VeiculoResponse } from '../../../core/models/veiculo.model';
import { AgendamentoRequest } from '../../../core/models/agendamento.model';
import { ClienteService } from '../../../core/services/cliente.service';
import { VeiculoService } from '../../../core/services/veiculo.service';

@Component({
  selector: 'app-agendamento-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  templateUrl: './agendamento-form-dialog.component.html',
})
export class AgendamentoFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly clienteService = inject(ClienteService);
  private readonly veiculoService = inject(VeiculoService);
  protected readonly dialogRef = inject(MatDialogRef<AgendamentoFormDialogComponent>);
  protected readonly data: Record<string, never> = inject(MAT_DIALOG_DATA) ?? {};

  protected readonly clientes = signal<ClienteResponse[]>([]);
  protected readonly veiculos = signal<VeiculoResponse[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    clienteId: ['', Validators.required],
    veiculoId: ['', Validators.required],
    dataHora: ['', Validators.required],
    descricaoServicos: [''],
  });

  ngOnInit() {
    this.clienteService.listar().subscribe(c => this.clientes.set(c));

    this.form.controls.clienteId.valueChanges.subscribe(clienteId => {
      this.form.controls.veiculoId.setValue('');
      this.veiculos.set([]);
      if (clienteId) {
        this.veiculoService.listarPorCliente(clienteId).subscribe(v => this.veiculos.set(v));
      }
    });
  }

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const payload: AgendamentoRequest = {
      clienteId: raw.clienteId,
      veiculoId: raw.veiculoId,
      dataHora: new Date(raw.dataHora).toISOString(),
      descricaoServicos: raw.descricaoServicos || undefined,
    };
    this.dialogRef.close(payload);
  }
}
