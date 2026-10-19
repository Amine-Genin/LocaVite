import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { UserService } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        UserService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(UserService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should get profile and map user + documents correctly', () => {
    service.getProfile().subscribe((user) => {
      expect(user.id).toBe(1);
      expect(user.firstName).toBe('Ali');
      expect(user.lastName).toBe('Alaoui');
      expect(user.email).toBe('ali@example.com');
      expect(user.phone).toBe('0612345678');
      expect(user.role).toBe('CLIENT');
      expect(user.documents.length).toBe(1);
      expect(user.documents[0].type).toBe('permis');
      expect(user.documents[0].status).toBe('validated');
    });

    const reqUser = httpTesting.expectOne('/api/users/me');
    expect(reqUser.request.method).toBe('GET');
    reqUser.flush({
      id: 1,
      nom: 'Alaoui',
      prenom: 'Ali',
      email: 'ali@example.com',
      telephone: '0612345678',
      roles: ['CLIENT'],
      createdAt: '2026-01-01T00:00:00',
    });

    const reqDocs = httpTesting.expectOne('/api/users/me/documents');
    expect(reqDocs.request.method).toBe('GET');
    reqDocs.flush([
      {
        id: 20,
        type: 'PERMIS',
        status: 'VALIDATED',
        url: 'http://minio/permis.pdf',
        uploadedAt: '2026-01-02T00:00:00',
      },
    ]);
  });

  it('should update profile sending nom, prenom, telephone in body', () => {
    service.updateProfile({
      firstName: 'Ali',
      lastName: 'Alaoui',
      phone: '0612345678',
    }).subscribe();

    const reqPut = httpTesting.expectOne('/api/users/me');
    expect(reqPut.request.method).toBe('PUT');
    expect(reqPut.request.body).toEqual({
      nom: 'Alaoui',
      prenom: 'Ali',
      telephone: '0612345678',
    });
    reqPut.flush({ id: 1 });

    // After PUT, getProfile is triggered
    const reqGet = httpTesting.expectOne('/api/users/me');
    reqGet.flush({
      id: 1,
      nom: 'Alaoui',
      prenom: 'Ali',
      email: 'ali@example.com',
      telephone: '0612345678',
      roles: ['CLIENT'],
      createdAt: '2026-01-01T00:00:00',
    });
    const reqDocs = httpTesting.expectOne('/api/users/me/documents');
    reqDocs.flush([]);
  });

  it('should upload document with FormData containing uppercase type', () => {
    const file = new File(['dummy'], 'cin.png', { type: 'image/png' });

    service.uploadDocument('cin', file).subscribe((doc) => {
      expect(doc.id).toBe(30);
      expect(doc.type).toBe('cin');
      expect(doc.status).toBe('pending');
    });

    const reqPost = httpTesting.expectOne('/api/users/me/documents');
    expect(reqPost.request.method).toBe('POST');
    expect(reqPost.request.body instanceof FormData).toBeTrue();
    reqPost.flush({
      id: 30,
      type: 'CIN',
      status: 'PENDING',
      url: 'http://minio/cin.png',
      uploadedAt: '2026-01-03T00:00:00',
    });
  });

  it('should delete document by id', () => {
    service.deleteDocument(30).subscribe();

    const reqDel = httpTesting.expectOne('/api/users/me/documents/30');
    expect(reqDel.request.method).toBe('DELETE');
    reqDel.flush(null);
  });
});

