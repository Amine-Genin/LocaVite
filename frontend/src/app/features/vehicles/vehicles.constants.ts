import { TranslateService } from '@ngx-translate/core';

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
    translate.instant('VEHICLES.DETAIL.RESERVE') === 'VEHICLES.DETAIL.RESERVE'
  ) {
    translate.setTranslation('fr', VEHICLE_FALLBACK_TRANSLATIONS, true);
    if (!translate.currentLang) {
      translate.use('fr');
    }
  }
}
