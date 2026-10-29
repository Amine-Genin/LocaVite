import { TranslateService } from '@ngx-translate/core';

export const MOROCCAN_CITIES: string[] = [
  'Casablanca',
  'Rabat',
  'Marrakech',
  'Tanger',
  'Agadir',
  'Fès',
  'Meknès',
  'Oujda',
  'Kénitra',
  'Tétouan',
  'Safi',
  'Mohammédia',
  'El Jadida',
  'Béni Mellal',
  'Nador',
  'Dakhla',
  'Laâyoune',
  'Essaouira',
];

export function generateYearsList(startYear = 1990, endYear = 2026): number[] {
  const years: number[] = [];
  for (let y = endYear; y >= startYear; y--) {
    years.push(y);
  }
  return years;
}

export const FALLBACK_BRANDS = [
  { id: 1, name: 'Renault' },
  { id: 2, name: 'Dacia' },
  { id: 3, name: 'Peugeot' },
  { id: 4, name: 'Volkswagen' },
  { id: 5, name: 'Toyota' },
  { id: 6, name: 'Hyundai' },
  { id: 7, name: 'Mercedes-Benz' },
  { id: 8, name: 'BMW' },
];

export const FALLBACK_CATEGORIES = [
  { id: 1, name: 'Économique' },
  { id: 2, name: 'Berline' },
  { id: 3, name: 'SUV' },
  { id: 4, name: 'Compacte' },
  { id: 5, name: 'Luxe' },
  { id: 6, name: 'Utilitaire' },
];

export const VEHICLE_FALLBACK_TRANSLATIONS = {
  COMMON: {
    CURRENCY_MAD: 'DH',
    RETRY: 'Réessayer',
    CLOSE: 'Fermer',
    CANCEL: 'Annuler',
    SAVE: 'Enregistrer',
    SEND: 'Envoyer',
  },
  VEHICLES: {
    TITLE: 'Véhicules disponibles',
    SUBTITLE: 'Parcourez notre sélection de véhicules vérifiés partout au Maroc',
    RESULTS_COUNT: '{{count}} véhicule(s) disponible(s)',
    SEARCH_CITY_PLACEHOLDER: 'Filtrer par ville (ex. Casablanca, Marrakech…)',
    ALL_CITIES: 'Toutes les villes',
    ALL_CATEGORIES: 'Toutes les catégories',
    RESET_FILTERS: 'Réinitialiser les filtres',
    LOADING: 'Chargement des véhicules…',
    EMPTY_TITLE: 'Aucun véhicule disponible',
    EMPTY_MESSAGE:
      'Aucun résultat ne correspond à votre recherche. Essayez de modifier vos critères.',
    ERROR_MESSAGE: 'Impossible de charger la liste des véhicules.',
    PER_DAY: '/ jour',
    YEAR: 'Année',
    CITY: 'Ville',
    CATEGORY: 'Catégorie',
    DETAILS: 'Détails',
    ADD_FAVORITE: 'Ajouter aux favoris',
    REMOVE_FAVORITE: 'Retirer des favoris',
    ADMIN: {
      FORM: {
        TITLE: 'Ajouter un véhicule',
        SUBTITLE: 'Complétez les informations pour ajouter un véhicule au catalogue LocaVite',
        BACK_TO_LIST: 'Retour à la liste des véhicules',
        PAGE_TITLE: 'Ajouter un véhicule',
        SECTIONS: {
          IDENTITY: 'Identité du véhicule',
          FEATURES: 'Caractéristiques techniques',
          SPECS: 'Caractéristiques techniques',
          PRICING: 'Tarification & Prix',
          LOCATION: 'Localisation',
          PHOTOS: 'Photos du véhicule',
        },
        FIELDS: {
          BRAND: 'Marque',
          BRAND_PLACEHOLDER: 'Rechercher une marque (ex: Dacia, Renault...)',
          MODEL: 'Modèle',
          MODEL_PLACEHOLDER: 'Ex: Duster, Clio 5...',
          YEAR: 'Année',
          YEAR_PLACEHOLDER: 'Sélectionner une année',
          CATEGORY: 'Catégorie',
          CATEGORY_PLACEHOLDER: 'Sélectionner une catégorie',
          LICENSE_PLATE: 'Immatriculation / Matricule',
          MATRICULE: 'Immatriculation / Matricule',
          MATRICULE_PLACEHOLDER: 'Ex: 12345-A-1',
          FUEL: 'Carburant',
          TRANSMISSION: 'Transmission',
          SEATS: 'Nombre de places',
          DESCRIPTION: 'Description détaillée',
          DESCRIPTION_PLACEHOLDER:
            'Décrivez l’état, les équipements et les points forts du véhicule...',
          PRICE_PER_DAY: 'Prix par jour',
          RENT_PRICE: 'Prix de location par jour (DH)',
          SALE_PRICE: 'Prix de vente (DH)',
          CITY: 'Ville de disponibilité',
          CITY_PLACEHOLDER: 'Sélectionner une ville',
        },
        RADIO: {
          ESSENCE: 'Essence',
          DIESEL: 'Diesel',
          HYBRIDE: 'Hybride',
          ELECTRIQUE: 'Électrique',
          MANUELLE: 'Manuelle',
          AUTOMATIQUE: 'Automatique',
        },
        VALIDATION: {
          REQUIRED: 'Ce champ est obligatoire.',
          SEATS_RANGE: 'Le nombre de places doit être compris entre 2 et 9.',
          POSITIVE: 'Le montant doit être supérieur à 0.',
        },
        PHOTOS: {
          TITLE: 'Photos du véhicule',
          SUBTITLE:
            'Ajoutez une ou plusieurs photos. Définissez la photo principale avec l’étoile.',
          DROP_TEXT: 'Glissez-déposez vos photos ici ou',
          BROWSE: 'Parcourir les fichiers',
          HINT: 'Formats : JPG, PNG, WEBP (Max 5 Mo par image)',
          PRIMARY_BADGE: 'Photo principale',
          PRIMARY_HINT: 'Cliquez sur l’étoile pour définir la photo principale.',
          SET_PRIMARY: 'Définir comme photo principale',
          MOVE_UP: 'Déplacer vers la gauche / avant',
          MOVE_DOWN: 'Déplacer vers la droite / après',
          DELETE: 'Supprimer',
        },
        ACTIONS: {
          CANCEL: 'Annuler',
          SAVE_DRAFT: 'Enregistrer en brouillon',
          PUBLISH: 'Publier le véhicule',
          SUBMITTING: 'Enregistrement en cours...',
        },
        ERRORS: {
          REQUIRED: 'Ce champ est obligatoire.',
          MIN_YEAR: 'L’année doit être supérieure ou égale à 1990.',
          SEATS_RANGE: 'Le nombre de places doit être compris entre 2 et 9.',
          PRICE_POSITIVE: 'Le prix doit être strictement supérieur à 0.',
          SAVE_ERROR: 'Une erreur est survenue lors de l’enregistrement du véhicule.',
        },
        SUCCESS: {
          CREATED_PUBLISHED: 'Le véhicule a été publié avec succès !',
          CREATED_DRAFT: 'Le brouillon a été enregistré avec succès !',
        },
      },
    },
    DETAIL: {
      BACK: 'Retour au catalogue',
      RESERVE: 'Réserver ce véhicule',
      CONTACT_AGENCY: "Contacter l'agence",
      FAVORITE_ADD: 'Ajouter aux favoris',
      FAVORITE_REMOVE: 'Retirer des favoris',
      SALE_PRICE: 'Prix de vente',
      RENT_PRICE: 'Prix de location',
      PER_DAY: 'par jour',
      LOCATION: 'Emplacement',
      AGENCY: 'Agence',
      DEFAULT_AGENCY: 'LocaVite Agence Principale',
      DESCRIPTION: 'Description',
      NO_DESCRIPTION: 'Aucune description fournie pour ce véhicule.',
      TABS: {
        SPECS: 'Caractéristiques',
        AVAILABILITY: 'Disponibilité',
        AGENCY: 'Agence & Contact',
      },
      SPECS: {
        BRAND: 'Marque',
        MODEL: 'Modèle',
        YEAR: 'Année',
        CATEGORY: 'Catégorie',
        FUEL: 'Carburant',
        TRANSMISSION: 'Transmission',
        SEATS: 'Nombre de places',
        MATRICULE: 'Immatriculation',
        STATUS: 'Statut',
      },
      CALENDAR: {
        TITLE: 'Calendrier de disponibilité',
        SUBTITLE: 'Consultez les dates libres et réservées en temps réel',
        AVAILABLE: 'Disponible',
        UNAVAILABLE: 'Réservé / Indisponible',
        SELECTED_DATE: 'Date sélectionnée',
        STATUS_AVAILABLE: 'Ce véhicule est disponible à cette date',
        STATUS_UNAVAILABLE: 'Ce véhicule est indisponible ou déjà réservé à cette date',
        MONTH_VIEW: 'Vue mensuelle',
      },
      CONTACT_DIALOG: {
        TITLE: "Contacter l'agence",
        SUBTITLE: 'Remplissez le formulaire ou contactez-nous directement sur WhatsApp',
        WHATSAPP_BTN: 'Échanger sur WhatsApp',
        NAME: 'Votre nom complet',
        NAME_REQUIRED: 'Le nom est obligatoire',
        EMAIL: 'Votre e-mail',
        EMAIL_REQUIRED: "L'e-mail est obligatoire",
        EMAIL_INVALID: 'Format e-mail invalide',
        PHONE: 'Votre numéro de téléphone',
        PHONE_REQUIRED: 'Le téléphone est obligatoire',
        MESSAGE: 'Votre message',
        MESSAGE_REQUIRED: 'Le message est obligatoire',
        SEND: 'Envoyer ma demande',
        SENDING: 'Envoi…',
        SUCCESS: 'Votre message a bien été transmis à notre agence ! Nous vous répondrons sous 2h.',
        CALL_US: 'Ou appelez directement notre agence',
      },
      SIMILAR_TITLE: 'Véhicules similaires',
      SIMILAR_SUBTITLE: 'D’autres modèles de la même catégorie qui pourraient vous intéresser',
      NOT_FOUND_TITLE: 'Véhicule introuvable',
      NOT_FOUND_MSG: 'Le véhicule demandé n’existe pas ou a été désactivé.',
    },
  },
};

export function setupFallbackTranslations(translate?: TranslateService | null): void {
  if (!translate) return;
  if (
    translate.instant('VEHICLES.DETAILS') === 'VEHICLES.DETAILS' ||
    translate.instant('VEHICLES.ADMIN.FORM.TITLE') === 'VEHICLES.ADMIN.FORM.TITLE' ||
    translate.instant('VEHICLES.DETAIL.RESERVE') === 'VEHICLES.DETAIL.RESERVE'
  ) {
    translate.setTranslation('fr', VEHICLE_FALLBACK_TRANSLATIONS, true);
    if (!translate.currentLang) {
      translate.use('fr');
    }
  }
}
