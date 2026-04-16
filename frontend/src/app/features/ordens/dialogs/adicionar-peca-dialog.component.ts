import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PecaResponse } from '../../../core/models/peca.model';
import { ItemPecaRequest } from '../../../core/models/ordem-de-servico.model';
import { PecaService } from '../../../core/services/peca.service';

@Component({
  selector: 'app-adicionar-peca-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe, ReactiveFormsModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  templateUrl: './adicionar-peca-dialog.component.html',
})
export class AdicionarPecaDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly pecaService = inject(PecaService);
  protected readonly dialogRef = inject(MatDialogRef<AdicionarPecaDialogComponent>);
  protected readonly data = inject<{ orderId: string }>(MAT_DIALOG_DATA);

  protected readonly pecas = signal<PecaResponse[]>([]);
  protected readonly loading = signal(true);

  protected readonly form = this.fb.group({
    pecaId: ['', Validators.required],
    quantidade: [1, Validators.required],
  });

  // toSignal converte o Observable do form em signal reativo
  // (necessário com provideZonelessChangeDetection)
  private readonly formValue = toSignal(this.form.valueChanges, {
    initialValue: this.form.value,
  });

  protected readonly pecaSelecionada = computed(() => {
    const id = this.formValue().pecaId;
    return id ? this.pecas().find(p => p.id === id) : null;
  });

  protected readonly estoqueDisponivel = computed(() => {
    return this.pecaSelecionada()?.quantidadeEstoque ?? 0;
  });

  protected readonly quantidadeValida = computed(() => {
    const quantidade = this.parseQuantidade();
    const estoque = this.estoqueDisponivel();
    return quantidade > 0 && quantidade <= estoque;
  });

  protected readonly totalEstimado = computed(() => {
    const peca = this.pecaSelecionada();
    if (!peca || !this.quantidadeValida()) return 0;
    return peca.preco * this.parseQuantidade();
  });

  private parseQuantidade(): number {
    const value = this.formValue().quantidade;
    const num = typeof value === 'string' ? parseInt(value, 10) : (value ?? 0);
    return isNaN(num) ? 0 : num;
  }

  ngOnInit() {
    this.pecaService.listar().subscribe({
      next: (pecas) => {
        // Filtrar apenas peças ativas
        this.pecas.set(pecas.filter(p => p.ativo));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected submit() {
    if (this.form.invalid || !this.quantidadeValida()) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const payload: ItemPecaRequest = {
      pecaId: v.pecaId!,
      quantidade: this.parseQuantidade(),
    };

    this.dialogRef.close(payload);
  }
}
