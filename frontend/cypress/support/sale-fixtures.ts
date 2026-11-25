export const MOCK_SALE_LISTING = {
  id: 31,
  vehicleId: 12,
  titre: 'Dacia Duster 1.5 dCi Prestige',
  description: 'SUV polyvalent et robuste en parfait état. Première main, carnet d entretien complet chez le concessionnaire.',
  prixVente: 185000,
  prixNego: true,
  etat: 'USED' as const,
  kilometrage: 52000,
  premierProprietaire: true,
  dateMiseEnCirculation: '2022-03-15',
  numeroChassis: 'VF1HJD20260315001',
  nombreProprietaires: 1,
  puissance: 115,
  statut: 'PUBLISHED' as const,
  marque: 'Dacia',
  modele: 'Duster',
  annee: 2022,
  ville: 'Casablanca',
  carburant: 'DIESEL',
  transmission: 'MANUELLE',
  categorie: 'SUV',
  photoUrl: 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d',
  publishedAt: '2026-03-01T10:00:00Z',
};

export const MOCK_AGENCY_VEHICLES = [
  {
    id: 10,
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2022,
    ville: 'Casablanca',
    categorie: 'SUV',
    prixJour: 350,
    prixVente: 180000,
    matricule: '78945-A-1',
    statut: 'PUBLISHED',
    agenceId: 3,
  },
  {
    id: 11,
    marque: 'Renault',
    modele: 'Clio 5',
    annee: 2023,
    ville: 'Rabat',
    categorie: 'CITADINE',
    prixJour: 280,
    prixVente: 140000,
    matricule: '12345-B-2',
    statut: 'PUBLISHED',
    agenceId: 3,
  },
];

export const MOCK_BRANDS = [
  { id: 1, name: 'Dacia' },
  { id: 2, name: 'Peugeot' },
  { id: 3, name: 'Renault' },
  { id: 4, name: 'Volkswagen' },
];

export const MOCK_CATEGORIES = [
  { id: 1, name: 'CITADINE' },
  { id: 2, name: 'SUV' },
  { id: 3, name: 'BERLINE' },
];
