import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { UploadedPhotoItem } from '../models/vehicle.model';
import { setupFallbackTranslations } from '../vehicles.constants';

@Component({
  selector: 'app-photo-uploader',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule, TranslateModule],
  templateUrl: './photo-uploader.component.html',
  styleUrl: './photo-uploader.component.scss',
})
export class PhotoUploaderComponent {
  private readonly translate = inject(TranslateService, { optional: true });

  @Input() photos: UploadedPhotoItem[] = [];
  @Output() photosChange = new EventEmitter<UploadedPhotoItem[]>();

  @ViewChild('fileInput') fileInputRef?: ElementRef<HTMLInputElement>;

  isDragging = false;
  errorMessage: string | null = null;

  constructor() {
    setupFallbackTranslations(this.translate);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    this.errorMessage = null;

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.handleFileList(event.dataTransfer.files);
    }
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.errorMessage = null;
    if (input.files && input.files.length > 0) {
      this.handleFileList(input.files);
      input.value = ''; // Reset input to allow selecting same files if needed
    }
  }

  triggerFileInput(): void {
    this.fileInputRef?.nativeElement.click();
  }

  handleFileList(fileList: FileList): void {
    const validFiles: File[] = [];
    for (let i = 0; i < fileList.length; i++) {
      const file = fileList.item(i);
      if (!file) continue;

      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Seuls les fichiers image (JPG, PNG, WEBP) sont acceptés.';
        continue;
      }

      if (file.size > 5 * 1024 * 1024) {
        this.errorMessage = `Le fichier ${file.name} dépasse la limite de 5 Mo.`;
        continue;
      }

      validFiles.push(file);
    }

    if (validFiles.length === 0) return;

    const newItems: UploadedPhotoItem[] = validFiles.map((file, idx) => ({
      file,
      name: file.name,
      size: file.size,
      previewUrl: URL.createObjectURL(file),
      isPrimary: this.photos.length === 0 && idx === 0,
      ordre: this.photos.length + idx,
    }));

    const updated = [...this.photos, ...newItems];

    // Ensure exactly one photo is marked primary
    if (!updated.some((p) => p.isPrimary) && updated.length > 0) {
      updated[0].isPrimary = true;
    }

    this.photos = updated;
    this.photosChange.emit(this.photos);
  }

  setPrimary(index: number): void {
    if (index < 0 || index >= this.photos.length) return;
    this.photos = this.photos.map((item, idx) => ({
      ...item,
      isPrimary: idx === index,
    }));
    this.photosChange.emit(this.photos);
  }

  move(index: number, direction: 'up' | 'down'): void {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= this.photos.length) return;

    const updated = [...this.photos];
    const [moved] = updated.splice(index, 1);
    updated.splice(targetIndex, 0, moved);

    // Update ordre field
    this.photos = updated.map((item, idx) => ({ ...item, ordre: idx }));
    this.photosChange.emit(this.photos);
  }

  remove(index: number): void {
    if (index < 0 || index >= this.photos.length) return;
    const removedItem = this.photos[index];
    if (removedItem.previewUrl.startsWith('blob:')) {
      try {
        URL.revokeObjectURL(removedItem.previewUrl);
      } catch {
        // ignore
      }
    }

    const updated = this.photos.filter((_, idx) => idx !== index);
    if (removedItem.isPrimary && updated.length > 0) {
      updated[0].isPrimary = true;
    }

    this.photos = updated.map((item, idx) => ({ ...item, ordre: idx }));
    this.photosChange.emit(this.photos);
  }
}
