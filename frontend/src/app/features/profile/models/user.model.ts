export type DocumentStatus = 'pending' | 'validated' | 'rejected';
export type DocumentType = 'permis' | 'cin' | 'passeport';

export interface UserDocument {
  id: number;
  type: DocumentType;
  url: string;
  status: DocumentStatus;
  uploadedAt: string;
}

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: string;
  createdAt: string;
  documents: UserDocument[];
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone: string;
}
