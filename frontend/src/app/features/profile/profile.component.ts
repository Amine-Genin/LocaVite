import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { UserService } from './services/user.service';
import { AuthService } from '../../core/auth/auth.service';
import { DocumentStatus, DocumentType, User, UserDocument } from './models/user.model';
import { StatusBadgeComponent, BadgeTone } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule,
            MatInputModule, MatSnackBarModule, StatusBadgeComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  private readonly ALLOWED_TYPES = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png'];
  private readonly MAX_SIZE = 5 * 1024 * 1024; // 5 Mo, limite du backend

  user = signal<User | null>(null);
  loading = signal(true);
  saving = signal(false);
  dragOver = signal<DocumentType | null>(null);
  uploading = signal<DocumentType | null>(null);

  documentTypes: { value: DocumentType; label: string }[] = [
    { value: 'permis', label: 'Permis de conduire' },
    { value: 'cin', label: 'CIN' },
  ];

  form = this.fb.nonNullable.group({
    lastName: ['', Validators.required],
    firstName: ['', Validators.required],
    phone: ['', [Validators.required, Validators.pattern(/^(\+212|0)[5-7]\d{8}$/)]],
  });

  ngOnInit() {
    this.loadProfile();
  }

  loadProfile() {
    this.loading.set(true);
    this.userService.getProfile().subscribe({
      next: (u) => {
        this.user.set(u);
        this.form.patchValue(u);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast('Erreur de chargement du profil.');
      },
    });
  }

  save() {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.userService.updateProfile(this.form.getRawValue()).subscribe({
      next: (u) => {
        this.user.set(u);
        this.form.patchValue(u);
        this.saving.set(false);
        this.toast('Profil mis à jour.');
      },
      error: (err) => {
        this.saving.set(false);
        this.toast(this.errorMessage(err, 'Échec de la mise à jour.'));
      },
    });
  }

  logout() {
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
    const map: Record<string, string> = {
      pending: 'En attente',
      validated: 'Validé',
      rejected: 'Rejeté',
    };
    return map[key] || s;
  }

  onDragOver(e: DragEvent, type: DocumentType) {
    e.preventDefault();
    e.stopPropagation();
    if (e.dataTransfer) {
      e.dataTransfer.dropEffect = 'copy';
    }
    this.dragOver.set(type);
  }

  onDragLeave(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    this.dragOver.set(null);
  }

  onDrop(e: DragEvent, type: DocumentType) {
    e.preventDefault();
    e.stopPropagation();
    this.dragOver.set(null);
    const file = e.dataTransfer?.files?.[0];
    if (file) this.upload(type, file);
  }

  onFileSelected(e: Event, type: DocumentType) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.upload(type, file);
      input.value = '';
    }
  }

  private upload(type: DocumentType, file: File) {
    const fileType = (file.type || '').toLowerCase();
    const fileName = (file.name || '').toLowerCase();
    const isAllowed = this.ALLOWED_TYPES.includes(fileType) ||
      /\.(pdf|jpg|jpeg|png)$/i.test(fileName);

    if (!isAllowed) {
      this.toast('Format non autorisé. Formats acceptés : PDF, JPG, PNG.');
      return;
    }
    if (file.size > this.MAX_SIZE) {
      this.toast('Le fichier dépasse la taille maximale de 5 Mo.');
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
        this.toast('Document envoyé.');
        if (previous) this.userService.deleteDocument(previous.id).subscribe({ error: () => {} });
      },
      error: (err) => {
        this.uploading.set(null);
        this.toast(this.errorMessage(err, "Échec de l'envoi du document."));
      },
    });
  }

  private errorMessage(err: any, fallback: string): string {
    if (err?.error?.errors && typeof err.error.errors === 'object') {
      const messages = Object.values(err.error.errors).filter(Boolean);
      if (messages.length > 0) {
        return messages.join(', ');
      }
    }
    return err?.error?.message ?? fallback;
  }

  private toast(msg: string) {
    this.snackBar.open(msg, 'OK', { duration: 4000 });
  }
}
