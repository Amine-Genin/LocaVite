import { TranslateService } from '@ngx-translate/core';

export const VEHICLE_FALLBACK_TRANSLATIONS = {
  COMMON: {
    CURRENCY_MAD: 'DH',
    RETRY: 'Réessayer',
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
    FILTERS: {
      TITLE: 'Filtres et tri', OPEN: 'Filtres', CLOSE: 'Fermer', CITY: 'Ville',
      CATEGORY: 'Catégorie', BRAND: 'Marque', FUEL: 'Carburant', TRANSMISSION: 'Transmission',
      START_DATE: 'Date de départ', END_DATE: 'Date de retour', PRICE: 'Prix par jour',
      MIN_PRICE: 'Min', MAX_PRICE: 'Max', SORT: 'Trier par', ALL: 'Tous',
      DEFAULT_SORT: 'Pertinence', PRICE_ASC: 'Prix croissant', PRICE_DESC: 'Prix décroissant',
      YEAR: 'Année récente', POPULARITY: 'Popularité',
      FUELS: { ESSENCE: 'Essence', DIESEL: 'Diesel', HYBRIDE: 'Hybride', ELECTRIQUE: 'Électrique' },
      TRANSMISSIONS: { MANUELLE: 'Manuelle', AUTOMATIQUE: 'Automatique' },
    },
  },
};

export function setupFallbackTranslations(translate?: TranslateService | null): void {
  if (!translate) return;
  if (translate.instant('VEHICLES.DETAILS') === 'VEHICLES.DETAILS') {
    translate.setTranslation('fr', VEHICLE_FALLBACK_TRANSLATIONS, true);
    if (!translate.currentLang) {
      translate.use('fr');
    }
  }
}

