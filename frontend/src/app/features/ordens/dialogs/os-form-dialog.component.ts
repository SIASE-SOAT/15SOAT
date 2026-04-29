import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ServicoResponse } from '../../../core/models/servico.model';
import { PecaResponse } from '../../../core/models/peca.model';
import {
  ClienteIdentificadoResponse,
  OrdemDeServicoRequest,
  VeiculoIdentificadoResponse,
} from '../../../core/models/ordem-de-servico.model';
import { ServicoService } from '../../../core/services/servico.service';
import { PecaService } from '../../../core/services/peca.service';
import { OrdemDeServicoService } from '../../../core/services/ordem-de-servico.service';

@Component({
  selector: 'app-os-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe, ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    MatDividerModule, MatProgressSpinnerModule, MatTooltipModule,
  ],
  templateUrl: './os-form-dialog.component.html',
  styleUrl: './os-form-dialog.component.scss',
})
export class OsFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly servicoService = inject(ServicoService);
  private readonly pecaService = inject(PecaService);
  private readonly osService = inject(OrdemDeServicoService);
  protected readonly dialogRef = inject(MatDialogRef<OsFormDialogComponent>);
  protected readonly data: Record<string, never> = inject(MAT_DIALOG_DATA) ?? {};

  protected readonly clienteIdentificado = signal<ClienteIdentificadoResponse | null>(null);
  protected readonly veiculos = signal<VeiculoIdentificadoResponse[]>([]);
  protected readonly servicos = signal<ServicoResponse[]>([]);
  protected readonly pecas = signal<PecaResponse[]>([]);
  protected readonly semServicos = signal(false);
  protected readonly carregando = signal(true);
  protected readonly preparandoAbertura = signal(false);
  protected readonly enviando = signal(false);
  protected readonly apiError = signal<string | null>(null);
  protected readonly itensServicoCount = signal(0);
  protected readonly itensPecaCount = signal(0);

  protected readonly form = this.fb.group({
    documento: ['', Validators.required],
    placa: [''],
    clienteId: [''],
    veiculoId: ['', Validators.required],
    observacoes: [''],
    itensServico: this.fb.array<FormGroup>([]),
    itensPeca: this.fb.array<FormGroup>([]),
  });

  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.value });

  protected readonly veiculoSelecionado = computed(() => {
    const id = this.formValue().veiculoId;
    return id ? this.veiculos().find(v => v.id === id) : null;
  });

  protected readonly buscaPreparada = computed(() => this.clienteIdentificado() !== null);

  protected readonly totalEstimado = computed(() => {
    const fv = this.formValue();
    const servicos = this.servicos();
    const pecas = this.pecas();

    const totalServicos = (fv.itensServico ?? []).reduce((acc, item) => {
      const s = servicos.find(sv => sv.id === item?.servicoId);
      return acc + (s?.preco ?? 0);
    }, 0);

    const totalPecas = (fv.itensPeca ?? []).reduce((acc, item) => {
      const p = pecas.find(pc => pc.id === item?.pecaId);
      const qtd = Number(item?.quantidade) || 0;
      return acc + (p?.preco ?? 0) * qtd;
    }, 0);

    return totalServicos + totalPecas;
  });

  get itensServico(): FormArray { return this.form.get('itensServico') as FormArray; }
  get itensPeca(): FormArray { return this.form.get('itensPeca') as FormArray; }

  ngOnInit() {
    let pendentes = 2;
    const done = () => { if (--pendentes === 0) this.carregando.set(false); };

    this.servicoService.listar().subscribe(s => { this.servicos.set(s); done(); });
    this.pecaService.listar().subscribe(p => { this.pecas.set(p); done(); });

    this.form.controls.documento.valueChanges.subscribe(() => this.resetPreparacao());
    this.form.controls.placa.valueChanges.subscribe(() => this.resetPreparacao(false));

    this.form.controls.veiculoId.valueChanges.subscribe(veiculoId => {
      if (veiculoId) {
        this.apiError.set(null);
      }
    });

    this.form.valueChanges.subscribe(() => this.apiError.set(null));

    this.adicionarServico();
  }

  protected adicionarServico() {
    this.itensServico.push(this.fb.group({ servicoId: ['', Validators.required], observacoes: [''] }));
    this.itensServicoCount.set(this.itensServico.length);
    this.semServicos.set(false);
  }

  protected removerServico(i: number) {
    this.itensServico.removeAt(i);
    this.itensServicoCount.set(this.itensServico.length);
  }

  protected adicionarPeca() {
    this.itensPeca.push(this.fb.group({
      pecaId: ['', Validators.required],
      quantidade: [1, [Validators.required, Validators.min(1)]],
    }));
    this.itensPecaCount.set(this.itensPeca.length);
  }

  protected removerPeca(i: number) {
    this.itensPeca.removeAt(i);
    this.itensPecaCount.set(this.itensPeca.length);
  }

  protected precoServico(servicoId: string | null | undefined): number {
    if (!servicoId) return 0;
    return this.servicos().find(s => s.id === servicoId)?.preco ?? 0;
  }

  protected estoqueDisponivelPeca(pecaId: string | null | undefined): number {
    if (!pecaId) return 0;
    return this.pecas().find(p => p.id === pecaId)?.quantidadeEstoque ?? 0;
  }

  protected prepararAbertura() {
    const documento = this.form.controls.documento.value?.trim();
    const placa = this.form.controls.placa.value?.trim().toUpperCase();

    if (!documento) {
      this.form.controls.documento.markAsTouched();
      return;
    }

    this.preparandoAbertura.set(true);
    this.apiError.set(null);

    this.osService.prepararAbertura(documento, placa || undefined).subscribe({
      next: (response) => {
        this.clienteIdentificado.set(response.cliente);
        this.veiculos.set(response.veiculos);
        this.form.patchValue({
          documento,
          placa: placa ?? '',
          clienteId: response.cliente.id,
          veiculoId: response.veiculoSelecionado?.id ?? '',
        }, { emitEvent: false });
        this.preparandoAbertura.set(false);
      },
      error: (err) => {
        this.clienteIdentificado.set(null);
        this.veiculos.set([]);
        this.form.patchValue({ clienteId: '', veiculoId: '' }, { emitEvent: false });
        this.apiError.set(
          err.error?.message
            ?? err.message
            ?? 'Erro inesperado ao preparar a abertura da ordem de servico.',
        );
        this.preparandoAbertura.set(false);
      },
    });
  }

  protected submit() {
    if (this.itensServico.length === 0) { this.semServicos.set(true); return; }
    if (!this.clienteIdentificado()) {
      this.apiError.set('Identifique o cliente e o veiculo antes de criar a OS.');
      return;
    }
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.getRawValue() as {
      documento: string; placa: string; clienteId: string; veiculoId: string; observacoes: string;
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

    this.enviando.set(true);
    this.apiError.set(null);

    this.osService.criar(payload).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => {
        const msg = err.error?.message
          ?? err.message
          ?? 'Erro inesperado ao criar a ordem de servico.';
        this.apiError.set(msg);
        this.enviando.set(false);
      },
    });
  }

  private resetPreparacao(limparPlaca = false) {
    if (!this.clienteIdentificado() && this.veiculos().length === 0 && !this.form.controls.clienteId.value) {
      return;
    }

    this.clienteIdentificado.set(null);
    this.veiculos.set([]);
    this.form.patchValue({
      clienteId: '',
      veiculoId: '',
      ...(limparPlaca ? { placa: '' } : {}),
    }, { emitEvent: false });
  }
}
