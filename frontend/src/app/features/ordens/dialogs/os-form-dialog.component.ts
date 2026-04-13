import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { ClienteResponse } from '../../../core/models/cliente.model';
import { VeiculoResponse } from '../../../core/models/veiculo.model';
import { ServicoResponse } from '../../../core/models/servico.model';
import { PecaResponse } from '../../../core/models/peca.model';
import { OrdemDeServicoRequest } from '../../../core/models/ordem-de-servico.model';
import { ClienteService } from '../../../core/services/cliente.service';
import { VeiculoService } from '../../../core/services/veiculo.service';
import { ServicoService } from '../../../core/services/servico.service';
import { PecaService } from '../../../core/services/peca.service';

@Component({
  selector: 'app-os-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe, ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, MatDividerModule,
  ],
  templateUrl: './os-form-dialog.component.html',
})
export class OsFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly clienteService = inject(ClienteService);
  private readonly veiculoService = inject(VeiculoService);
  private readonly servicoService = inject(ServicoService);
  private readonly pecaService = inject(PecaService);
  protected readonly dialogRef = inject(MatDialogRef<OsFormDialogComponent>);
  protected readonly data: Record<string, never> = inject(MAT_DIALOG_DATA) ?? {};

  protected readonly clientes = signal<ClienteResponse[]>([]);
  protected readonly veiculos = signal<VeiculoResponse[]>([]);
  protected readonly servicos = signal<ServicoResponse[]>([]);
  protected readonly pecas = signal<PecaResponse[]>([]);
  protected readonly semServicos = signal(false);

  protected readonly form = this.fb.group({
    clienteId: ['', Validators.required],
    veiculoId: ['', Validators.required],
    observacoes: [''],
    itensServico: this.fb.array<FormGroup>([]),
    itensPeca: this.fb.array<FormGroup>([]),
  });

  get itensServico(): FormArray { return this.form.get('itensServico') as FormArray; }
  get itensPeca(): FormArray { return this.form.get('itensPeca') as FormArray; }

  ngOnInit() {
    this.clienteService.listar().subscribe(c => this.clientes.set(c));
    this.servicoService.listar().subscribe(s => this.servicos.set(s));
    this.pecaService.listar().subscribe(p => this.pecas.set(p));

    this.form.controls.clienteId.valueChanges.subscribe(clienteId => {
      this.form.controls.veiculoId.setValue('');
      this.veiculos.set([]);
      if (clienteId) {
        this.veiculoService.listarPorCliente(clienteId).subscribe(v => this.veiculos.set(v));
      }
    });

    this.adicionarServico();
  }

  protected adicionarServico() {
    this.itensServico.push(this.fb.group({ servicoId: ['', Validators.required], observacoes: [''] }));
    this.semServicos.set(false);
  }

  protected removerServico(i: number) { this.itensServico.removeAt(i); }

  protected adicionarPeca() {
    this.itensPeca.push(this.fb.group({
      pecaId: ['', Validators.required],
      quantidade: [1, [Validators.required, Validators.min(1)]],
    }));
  }

  protected removerPeca(i: number) { this.itensPeca.removeAt(i); }

  protected submit() {
    if (this.itensServico.length === 0) { this.semServicos.set(true); return; }
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.getRawValue() as {
      clienteId: string; veiculoId: string; observacoes: string;
      itensServico: { servicoId: string; observacoes: string }[];
      itensPeca: { pecaId: string; quantidade: number }[];
    };

    const payload: OrdemDeServicoRequest = {
      clienteId: v.clienteId,
      veiculoId: v.veiculoId,
      observacoes: v.observacoes || undefined,
      itensServico: v.itensServico.map(s => ({ servicoId: s.servicoId, observacoes: s.observacoes || undefined })),
      itensPeca: v.itensPeca.length ? v.itensPeca : undefined,
    };
    this.dialogRef.close(payload);
  }
}
