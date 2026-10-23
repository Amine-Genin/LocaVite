import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { UserService } from './services/user.service';
import { AuthService } from '../../core/auth/auth.service';
import { DocumentStatus, DocumentType, User, UserDocument } from './models/user.model';
import { StatusBadgeComponent, BadgeTone } from '../../shared/components/status-badge/status-badge.component';
import { LanguageSwitcherComponent } from '../../shared/components/language-switcher/language-switcher.component';
import { LocalizedDatePipe } from '../../core/i18n/localized-date.pipe';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    DatePipe,
    LocalizedDatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    TranslateModule,
    StatusBadgeComponent,
    LanguageSwitcherComponent,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);
  private translate = inject(TranslateService, { optional: true });

  private readonly ALLOWED_TYPES = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png'];
  private readonly MAX_SIZE = 5 * 1024 * 1024; // 5 Mo, limite du backend

  user = signal<User | null>(null);
  loading = signal(true);
  saving = signal(false);
  dragOver = signal<DocumentType | null>(null);
  uploading = signal<DocumentType | null>(null);

  documentTypes: { value: DocumentType; labelKey: string; fallbackLabel: string }[] = [
    { value: 'permis', labelKey: 'PROFILE.DOC_PERMIS', fallbackLabel: 'Permis de conduire' },
    { value: 'cin', labelKey: 'PROFILE.DOC_CIN', fallbackLabel: 'CIN' },
  ];

  form = this.fb.nonNullable.group({
    lastName: ['', Validators.required],
    firstName: ['', Validators.required],
    phone: ['', [Validators.required, Validators.pattern(/^(\+212|0)[5-7]\d{8}$/)]],
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.userService.getProfile().subscribe({
      next: (u) => {
        this.user.set(u);
        this.form.patchValue(u);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast(this.t('PROFILE.LOAD_ERROR_TOAST', 'Erreur de chargement du profil.'));
      },
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.userService.updateProfile(this.form.getRawValue()).subscribe({
      next: (u) => {
        this.user.set(u);
        this.form.patchValue(u);
        this.saving.set(false);
        this.toast(this.t('PROFILE.UPDATE_SUCCESS', 'Profil mis à jour.'));
      },
      error: (err) => {
        this.saving.set(false);
        this.toast(this.errorMessage(err, this.t('PROFILE.UPDATE_ERROR', 'Échec de la mise à jour.')));
      },
    });
  }

  logout(): void {
    this.authService.logout();
  }

  docOf(type: DocumentType): UserDocument | undefined {
    return this.user()?.documents.find((d) => d.type === type);
  }

  statusTone(s: DocumentStatus | string): BadgeTone {
    const key = (s || '').toLowerCase();
    const map: Record<string, BadgeTone> = {
      pending: 'warning',
      validated: 'success',
      rejected: 'error',
    };
    return map[key] || 'info';
  }

  statusLabel(s: DocumentStatus | string): string {
    const key = (s || '').toLowerCase();
    const labelKeyMap: Record<string, string> = {
      pending: 'PROFILE.STATUS_PENDING',
      validated: 'PROFILE.STATUS_VALIDATED',
      rejected: 'PROFILE.STATUS_REJECTED',
    };
    const fallbackMap: Record<string, string> = {
      pending: 'En attente',
      validated: 'Validé',
      rejected: 'Rejeté',
    };
    if (labelKeyMap[key]) {
      return this.t(labelKeyMap[key], fallbackMap[key]);
    }
    return s;
  }

  onDragOver(e: DragEvent, type: DocumentType): void {
    e.preventDefault();
    e.stopPropagation();
    if (e.dataTransfer) {
      e.dataTransfer.dropEffect = 'copy';
    }
    this.dragOver.set(type);
  }

  onDragLeave(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.dragOver.set(null);
  }

  onDrop(e: DragEvent, type: DocumentType): void {
    e.preventDefault();
    e.stopPropagation();
    this.dragOver.set(null);
    const file = e.dataTransfer?.files?.[0];
    if (file) this.upload(type, file);
  }

  onFileSelected(e: Event, type: DocumentType): void {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.upload(type, file);
      input.value = '';
    }
  }

  private upload(type: DocumentType, file: File): void {
    const fileType = (file.type || '').toLowerCase();
    const fileName = (file.name || '').toLowerCase();
    const isAllowed = this.ALLOWED_TYPES.includes(fileType) ||
      /\.(pdf|jpg|jpeg|png)$/i.test(fileName);

    if (!isAllowed) {
      this.toast(this.t('PROFILE.FORMAT_ERROR', 'Format non autorisé. Formats acceptés : PDF, JPG, PNG.'));
      return;
    }
    if (file.size > this.MAX_SIZE) {
      this.toast(this.t('PROFILE.SIZE_ERROR', 'Le fichier dépasse la taille maximale de 5 Mo.'));
      return;
    }

    this.uploading.set(type);
    const previous = this.docOf(type);
    this.userService.uploadDocument(type, file).subscribe({
      next: (doc) => {
        const u = this.user();
        if (u) {
          this.user.set({ ...u, documents: [...u.documents.filter((d) => d.type !== type), doc] });
        }
        this.uploading.set(null);
        this.toast(this.t('PROFILE.UPLOAD_SUCCESS', 'Document envoyé.'));
        if (previous) this.userService.deleteDocument(previous.id).subscribe({ error: () => {} });
      },
      error: (err) => {
        this.uploading.set(null);
        this.toast(this.errorMessage(err, this.t('PROFILE.UPLOAD_ERROR', "Échec de l'envoi du document.")));
      },
    });
  }

  private errorMessage(err: unknown, fallback: string): string {
    const errorObj = err as { error?: { errors?: Record<string, string>; message?: string } };
    if (errorObj?.error?.errors && typeof errorObj.error.errors === 'object') {
      const messages = Object.values(errorObj.error.errors).filter(Boolean);
      if (messages.length > 0) {
        return messages.join(', ');
      }
    }
    return errorObj?.error?.message ?? fallback;
  }

  private toast(msg: string): void {
    const okLabel = this.t('COMMON.OK', 'OK');
    this.snackBar.open(msg, okLabel, { duration: 4000 });
  }

  private t(key: string, fallback: string): string {
    if (this.translate) {
      const val = this.translate.instant(key);
      if (val && val !== key) {
        return val;
      }
    }
    return fallback;
  }
}
