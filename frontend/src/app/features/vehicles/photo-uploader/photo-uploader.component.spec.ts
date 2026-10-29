import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PhotoUploaderComponent } from './photo-uploader.component';
import { UploadedPhotoItem } from '../models/vehicle.model';
import { TranslateModule } from '@ngx-translate/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('PhotoUploaderComponent', () => {
  let component: PhotoUploaderComponent;
  let fixture: ComponentFixture<PhotoUploaderComponent>;

  beforeEach(async () => {
    // Stub URL.createObjectURL and URL.revokeObjectURL for tests
    if (typeof URL.createObjectURL === 'undefined') {
      URL.createObjectURL = () => 'blob:mock-url';
    } else {
      spyOn(URL, 'createObjectURL').and.returnValue('blob:mock-url');
    }
    if (typeof URL.revokeObjectURL === 'undefined') {
      URL.revokeObjectURL = () => {};
    } else {
      spyOn(URL, 'revokeObjectURL').and.stub();
    }

    await TestBed.configureTestingModule({
      imports: [PhotoUploaderComponent, TranslateModule.forRoot(), NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PhotoUploaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the photo uploader component', () => {
    expect(component).toBeTruthy();
  });

  it('should upload 3 photos and generate previews', () => {
    const file1 = new File(['dummy content 1'], 'car1.jpg', { type: 'image/jpeg' });
    const file2 = new File(['dummy content 2'], 'car2.png', { type: 'image/png' });
    const file3 = new File(['dummy content 3'], 'car3.webp', { type: 'image/webp' });

    const emitSpy = spyOn(component.photosChange, 'emit');

    // Simulate FileList
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file1);
    dataTransfer.items.add(file2);
    dataTransfer.items.add(file3);

    component.handleFileList(dataTransfer.files);
    fixture.detectChanges();

    expect(component.photos.length).toBe(3);
    expect(component.photos[0].name).toBe('car1.jpg');
    expect(component.photos[0].isPrimary).toBeTrue();
    expect(component.photos[1].name).toBe('car2.png');
    expect(component.photos[1].isPrimary).toBeFalse();
    expect(component.photos[2].name).toBe('car3.webp');
    expect(emitSpy).toHaveBeenCalledWith(component.photos);

    const previewCards = fixture.nativeElement.querySelectorAll('[data-cy="photo-preview"]');
    expect(previewCards.length).toBe(3);
  });

  it('should reject non-image files with an error message', () => {
    const file = new File(['pdf content'], 'doc.pdf', { type: 'application/pdf' });
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);

    component.handleFileList(dataTransfer.files);
    fixture.detectChanges();

    expect(component.photos.length).toBe(0);
    expect(component.errorMessage).toContain('Seuls les fichiers image');
  });

  it('should reject files larger than 5MB', () => {
    // 6 MB file
    const largeBlob = new Blob([new Uint8Array(6 * 1024 * 1024)]);
    const file = new File([largeBlob], 'huge.jpg', { type: 'image/jpeg' });
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);

    component.handleFileList(dataTransfer.files);
    fixture.detectChanges();

    expect(component.photos.length).toBe(0);
    expect(component.errorMessage).toContain('dépasse la limite de 5 Mo');
  });

  it('should allow user to change primary photo with setPrimary', () => {
    component.photos = [
      { previewUrl: 'blob:1', name: 'p1.jpg', size: 100, isPrimary: true, ordre: 0 },
      { previewUrl: 'blob:2', name: 'p2.jpg', size: 100, isPrimary: false, ordre: 1 },
      { previewUrl: 'blob:3', name: 'p3.jpg', size: 100, isPrimary: false, ordre: 2 },
    ];
    fixture.detectChanges();

    component.setPrimary(1);
    fixture.detectChanges();

    expect(component.photos[0].isPrimary).toBeFalse();
    expect(component.photos[1].isPrimary).toBeTrue();
    expect(component.photos[2].isPrimary).toBeFalse();
  });

  it('should allow user to reorder photos (move)', () => {
    component.photos = [
      { previewUrl: 'blob:1', name: 'p1.jpg', size: 100, isPrimary: true, ordre: 0 },
      { previewUrl: 'blob:2', name: 'p2.jpg', size: 100, isPrimary: false, ordre: 1 },
    ];
    fixture.detectChanges();

    component.move(0, 'down');
    fixture.detectChanges();

    expect(component.photos[0].name).toBe('p2.jpg');
    expect(component.photos[0].ordre).toBe(0);
    expect(component.photos[1].name).toBe('p1.jpg');
    expect(component.photos[1].ordre).toBe(1);
  });

  it('should allow user to remove photo and reassign primary if removed photo was primary', () => {
    component.photos = [
      { previewUrl: 'blob:1', name: 'p1.jpg', size: 100, isPrimary: true, ordre: 0 },
      { previewUrl: 'blob:2', name: 'p2.jpg', size: 100, isPrimary: false, ordre: 1 },
    ];
    fixture.detectChanges();

    component.remove(0);
    fixture.detectChanges();

    expect(component.photos.length).toBe(1);
    expect(component.photos[0].name).toBe('p2.jpg');
    expect(component.photos[0].isPrimary).toBeTrue();
  });

  it('should update isDragging on dragover and dragleave', () => {
    const dragEvent = new DragEvent('dragover');
    component.onDragOver(dragEvent);
    expect(component.isDragging).toBeTrue();

    const leaveEvent = new DragEvent('dragleave');
    component.onDragLeave(leaveEvent);
    expect(component.isDragging).toBeFalse();
  });
});

