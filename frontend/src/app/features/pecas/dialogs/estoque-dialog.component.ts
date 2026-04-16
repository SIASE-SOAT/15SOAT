import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { EstoqueRequest, PecaResponse } from '../../../core/models/peca.model';

@Component({
  selector: 'app-estoque-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  templateUrl: './estoque-dialog.component.html',
})
export class EstoqueDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly dialogRef = inject(MatDialogRef<EstoqueDialogComponent>);
  protected readonly data: { peca: PecaResponse } = inject(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    operacao: ['ENTRADA' as const, Validators.required],
    quantidade: [1, [Validators.required, Validators.min(1)]],
  });

  protected submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialogRef.close(this.form.getRawValue() as EstoqueRequest);
  }
}
