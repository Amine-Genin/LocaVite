import { TranslateService } from '@ngx-translate/core';

export const MOROCCAN_CITIES = [
  'Casablanca',
  'Rabat',
  'Marrakech',
  'Tanger',
  'Agadir',
  'Fès',
  'Meknès',
  'Oujda',
  'Tétouan',
  'Essaouira',
  'Nador',
  'El Jadida',
  'Kenitra',
  'Béni Mellal',
  'Mohammédia',
];

export function generateYearsList(): number[] {
  const maxYear = 2026;
  const years: number[] = [];
  for (let y = maxYear; y >= 1990; y--) {
    years.push(y);
  }
  return years;
}

export const VEHICLE_FALLBACK_TRANSLATIONS = {
  COMMON: {
    CURRENCY_MAD: 'DH',
    RETRY: 'Réessayer',
    CANCEL: 'Annuler',
    SAVE: 'Enregistrer',
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
        SECTIONS: {
          IDENTITY: 'Identité du véhicule',
          SPECS: 'Caractéristiques techniques',
          PRICING: 'Tarification',
          LOCATION: 'Localisation',
          PHOTOS: 'Galerie photos',
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
          MATRICULE: 'Immatriculation / Matricule',
          MATRICULE_PLACEHOLDER: 'Ex: 12345-A-1',
          FUEL: 'Carburant',
          TRANSMISSION: 'Transmission',
          SEATS: 'Nombre de places (2 à 9)',
          DESCRIPTION: 'Description détaillée',
          DESCRIPTION_PLACEHOLDER:
            'Décrivez l’état, les équipements et les points forts du véhicule...',
          RENT_PRICE: 'Prix de location par jour (DH)',
          SALE_PRICE: 'Prix de vente (DH)',
          CITY: 'Ville de disponibilité',
          CITY_PLACEHOLDER: 'Sélectionner une ville',
        },
        PHOTOS: {
          TITLE: 'Photos du véhicule',
          SUBTITLE:
            'Ajoutez une ou plusieurs photos. Définissez la photo principale avec l’étoile.',
          DROP_TEXT: 'Glissez-déposez vos photos ici ou',
          BROWSE: 'Parcourir les fichiers',
          HINT: 'Formats : JPG, PNG, WEBP (Max 5 Mo par image)',
          PRIMARY_BADGE: 'Photo principale',
          SET_PRIMARY: 'Définir comme photo principale',
          MOVE_UP: 'Déplacer vers la gauche / avant',
          MOVE_DOWN: 'Déplacer vers la droite / après',
          DELETE: 'Supprimer',
        },
        ACTIONS: {
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
  },
};

export function setupFallbackTranslations(translate?: TranslateService | null): void {
  if (!translate) return;
  if (
    translate.instant('VEHICLES.DETAILS') === 'VEHICLES.DETAILS' ||
    translate.instant('VEHICLES.ADMIN.FORM.TITLE') === 'VEHICLES.ADMIN.FORM.TITLE'
  ) {
    translate.setTranslation('fr', VEHICLE_FALLBACK_TRANSLATIONS, true);
    if (!translate.currentLang) {
      translate.use('fr');
    }
  }
}
