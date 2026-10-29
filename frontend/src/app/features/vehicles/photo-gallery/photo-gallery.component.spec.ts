import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PhotoGalleryComponent } from './photo-gallery.component';

describe('PhotoGalleryComponent', () => {
  let component: PhotoGalleryComponent;
  let fixture: ComponentFixture<PhotoGalleryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PhotoGalleryComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PhotoGalleryComponent);
    component = fixture.componentInstance;
  });

  it('should create the photo gallery component', () => {
    expect(component).toBeTruthy();
  });

  it('should display default photo when photos input is empty', () => {
    component.photos = [];
    fixture.detectChanges();

    expect(component.totalPhotos).toBe(1);
    expect(component.currentPhotoUrl).toBe(component.defaultPhoto);
  });

  it('should navigate through photos with next and prev', () => {
    component.photos = [
      { id: 1, vehicleId: 1, url: 'https://example.com/p1.jpg', isPrimary: true },
      { id: 2, vehicleId: 1, url: 'https://example.com/p2.jpg', isPrimary: false },
      { id: 3, vehicleId: 1, url: 'https://example.com/p3.jpg', isPrimary: false },
    ];
    fixture.detectChanges();

    expect(component.currentIndex).toBe(0);
    expect(component.currentPhotoUrl).toBe('https://example.com/p1.jpg');

    component.next();
    expect(component.currentIndex).toBe(1);
    expect(component.currentPhotoUrl).toBe('https://example.com/p2.jpg');

    component.next();
    expect(component.currentIndex).toBe(2);

    // Should wrap around to 0
    component.next();
    expect(component.currentIndex).toBe(0);

    // Prev should wrap to end
    component.prev();
    expect(component.currentIndex).toBe(2);
  });

  it('should select photo by index', () => {
    component.photos = ['https://example.com/a.jpg', 'https://example.com/b.jpg'];
    fixture.detectChanges();

    component.select(1);
    expect(component.currentIndex).toBe(1);
    expect(component.currentPhotoUrl).toBe('https://example.com/b.jpg');
  });

  it('should handle mobile touch swipe left (next) and right (prev)', () => {
    component.photos = [
      'https://example.com/1.jpg',
      'https://example.com/2.jpg',
      'https://example.com/3.jpg',
    ];
    fixture.detectChanges();

    // Swipe left (next)
    const touchStartLeft = {
      touches: [{ clientX: 200, clientY: 100 }],
    } as unknown as TouchEvent;
    const touchEndLeft = {
      changedTouches: [{ clientX: 120, clientY: 100 }], // deltaX = -80
    } as unknown as TouchEvent;

    component.onTouchStart(touchStartLeft);
    component.onTouchEnd(touchEndLeft);
    expect(component.currentIndex).toBe(1);

    // Swipe right (prev)
    const touchStartRight = {
      touches: [{ clientX: 100, clientY: 100 }],
    } as unknown as TouchEvent;
    const touchEndRight = {
      changedTouches: [{ clientX: 180, clientY: 100 }], // deltaX = +80
    } as unknown as TouchEvent;

    component.onTouchStart(touchStartRight);
    component.onTouchEnd(touchEndRight);
    expect(component.currentIndex).toBe(0);
  });

  it('should respond to keyboard ArrowLeft and ArrowRight keys', () => {
    component.photos = ['https://example.com/1.jpg', 'https://example.com/2.jpg'];
    fixture.detectChanges();

    component.onKeyDown(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    expect(component.currentIndex).toBe(1);

    component.onKeyDown(new KeyboardEvent('keydown', { key: 'ArrowLeft' }));
    expect(component.currentIndex).toBe(0);
  });

  it('should fallback to default photo on image error', () => {
    const dummyImg = document.createElement('img');
    dummyImg.src = 'https://broken.example.com/image.jpg';
    component.onImageError({ target: dummyImg } as unknown as Event);
    expect(dummyImg.src).toContain('data:image/svg+xml');
  });
});
