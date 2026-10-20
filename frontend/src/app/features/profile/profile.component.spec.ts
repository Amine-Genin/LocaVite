import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { UserService } from './services/user.service';
import { AuthService } from '../../core/auth/auth.service';
import { User } from './models/user.model';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let snackBarSpy: jasmine.Spy;

  const mockUser: User = {
    id: 1,
    firstName: 'Ahmed',
    lastName: 'Benali',
    email: 'ahmed.benali@example.com',
    phone: '0612345678',
    role: 'CLIENT',
    createdAt: '2026-01-15T10:00:00',
    documents: [
      {
        id: 10,
        type: 'permis',
        status: 'validated',
        url: 'http://minio/permis.pdf',
        uploadedAt: '2026-01-16T10:00:00',
      },
      {
        id: 11,
        type: 'cin',
        status: 'pending',
        url: 'http://minio/cin.pdf',
        uploadedAt: '2026-01-16T11:00:00',
      },
    ],
  };

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', [
      'getProfile',
      'updateProfile',
      'uploadDocument',
      'deleteDocument',
    ]);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['logout']);

    userServiceSpy.getProfile.and.returnValue(of(mockUser));
    userServiceSpy.updateProfile.and.returnValue(
      of({ ...mockUser, firstName: 'Karim', phone: '0698765432' }),
    );
    userServiceSpy.uploadDocument.and.returnValue(
      of({
        id: 12,
        type: 'permis',
        status: 'pending',
        url: 'http://minio/new-permis.pdf',
        uploadedAt: '2026-01-17T12:00:00',
      }),
    );
    userServiceSpy.deleteDocument.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideNoopAnimations(),
        { provide: UserService, useValue: userServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    snackBarSpy = spyOn((component as any).snackBar, 'open').and.callThrough();
    fixture.detectChanges();
  });

  it('should load and display user profile on init', () => {
    expect(userServiceSpy.getProfile).toHaveBeenCalled();
    expect(component.user()).toEqual(mockUser);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Benali');
    expect(compiled.textContent).toContain('Ahmed');
    expect(compiled.textContent).toContain('ahmed.benali@example.com');
    expect(compiled.textContent).toContain('0612345678');
    expect(compiled.textContent).toContain('CLIENT');
  });

  it('should initialize the form with user profile values', () => {
    expect(component.form.value).toEqual({
      lastName: 'Benali',
      firstName: 'Ahmed',
      phone: '0612345678',
    });
  });

  it('should validate Moroccan phone format', () => {
    const phoneControl = component.form.controls.phone;
    phoneControl.setValue('12345');
    expect(phoneControl.invalid).toBeTrue();

    phoneControl.setValue('0612345678');
    expect(phoneControl.valid).toBeTrue();

    phoneControl.setValue('+212612345678');
    expect(phoneControl.valid).toBeTrue();
  });

  it('should submit form and show success toast on update', () => {
    component.form.setValue({
      lastName: 'Benali',
      firstName: 'Karim',
      phone: '0698765432',
    });

    component.save();

    expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({
      lastName: 'Benali',
      firstName: 'Karim',
      phone: '0698765432',
    });
    expect(snackBarSpy).toHaveBeenCalledWith('Profil mis à jour.', 'OK', { duration: 4000 });
  });

  it('should show error toast on update failure', () => {
    userServiceSpy.updateProfile.and.returnValue(
      throwError(() => ({ error: { message: 'Numéro de téléphone déjà utilisé' } })),
    );

    component.save();

    expect(snackBarSpy).toHaveBeenCalledWith(
      'Numéro de téléphone déjà utilisé',
      'OK',
      { duration: 4000 },
    );
  });

  it('should display status badge with correct labels', () => {
    expect(component.statusLabel('pending')).toBe('En attente');
    expect(component.statusLabel('validated')).toBe('Validé');
    expect(component.statusLabel('rejected')).toBe('Rejeté');

    expect(component.statusTone('pending')).toBe('warning');
    expect(component.statusTone('validated')).toBe('success');
    expect(component.statusTone('rejected')).toBe('error');
  });

  it('should upload document and show success message', () => {
    const file = new File(['content'], 'permis.pdf', { type: 'application/pdf' });
    const event = {
      target: { files: [file], value: 'fake' },
    } as unknown as Event;

    component.onFileSelected(event, 'permis');

    expect(userServiceSpy.uploadDocument).toHaveBeenCalledWith('permis', file);
    expect(snackBarSpy).toHaveBeenCalledWith('Document envoyé.', 'OK', { duration: 4000 });
  });

  it('should reject file upload when size exceeds 5 Mo', () => {
    const bigFile = new File([''], 'big.pdf', { type: 'application/pdf' });
    Object.defineProperty(bigFile, 'size', { value: 6 * 1024 * 1024 });

    const event = {
      target: { files: [bigFile], value: 'fake' },
    } as unknown as Event;

    component.onFileSelected(event, 'permis');

    expect(userServiceSpy.uploadDocument).not.toHaveBeenCalled();
    expect(snackBarSpy).toHaveBeenCalledWith(
      'Le fichier dépasse la taille maximale de 5 Mo.',
      'OK',
      { duration: 4000 },
    );
  });

  it('should call authService.logout on logout button click', () => {
    component.logout();
    expect(authServiceSpy.logout).toHaveBeenCalled();
  });
});

