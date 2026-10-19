import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';

import {
  DocumentStatus,
  DocumentType,
  UpdateProfileRequest,
  User,
  UserDocument,
} from '../models/user.model';

const API_URL = '/api/users/me';

/** Contrat du backend (UserResponse). */
interface UserApi {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  roles: string[];
  createdAt: string;
}

/** Contrat du backend (DocumentResponse). */
interface DocumentApi {
  id: number;
  type: string;
  status: string;
  url: string;
  uploadedAt: string;
}

const toDocument = (d: DocumentApi): UserDocument => ({
  id: d.id,
  type: (d.type || '').toLowerCase() as DocumentType,
  status: (d.status || '').toLowerCase() as DocumentStatus,
  url: d.url,
  uploadedAt: d.uploadedAt,
});

const toUser = (u: UserApi, docs: DocumentApi[]): User => {
  // Le backend renvoie les documents du plus récent au plus ancien :
  // on conserve le premier rencontré pour chaque type.
  const latest = new Map<DocumentType, UserDocument>();
  for (const d of (docs || []).map(toDocument)) {
    if (!latest.has(d.type)) latest.set(d.type, d);
  }
  return {
    id: u.id,
    firstName: u.prenom || '',
    lastName: u.nom || '',
    email: u.email || '',
    phone: u.telephone || '',
    role: Array.isArray(u.roles) ? u.roles.join(', ') : (u.roles || ''),
    createdAt: u.createdAt,
    documents: [...latest.values()],
  };
};

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  getProfile(): Observable<User> {
    return forkJoin({
      user: this.http.get<UserApi>(API_URL),
      docs: this.http.get<DocumentApi[]>(`${API_URL}/documents`).pipe(
        catchError(() => of([] as DocumentApi[]))
      ),
    }).pipe(map(({ user, docs }) => toUser(user, docs)));
  }

  updateProfile(data: UpdateProfileRequest): Observable<User> {
    const body = { nom: data.lastName, prenom: data.firstName, telephone: data.phone };
    return this.http.put<UserApi>(API_URL, body).pipe(switchMap(() => this.getProfile()));
  }

  uploadDocument(type: DocumentType, file: File): Observable<UserDocument> {
    const formData = new FormData();
    formData.append('type', type.toUpperCase());
    formData.append('file', file);
    // Pas de Content-Type manuel : le navigateur ajoute le boundary multipart.
    return this.http.post<DocumentApi>(`${API_URL}/documents`, formData).pipe(map(toDocument));
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/documents/${id}`);
  }
}
