# 🎨 Frontend

Le frontend de LocaVite sera développé avec **Angular 18** et organisé par domaines fonctionnels afin de séparer les briques transverses des fonctionnalités métier.

## 🗂️ Structure prévue

```text
frontend/
├── core/
│   ├── auth/           # Services et modèles d'authentification
│   ├── interceptors/   # Intercepteurs HTTP
│   └── guards/         # Guards de navigation et d'autorisation
├── shared/
│   ├── components/     # Composants réutilisables
│   └── pipes/          # Pipes partagés
├── features/
│   ├── auth/           # Connexion et gestion du compte
│   ├── catalogue/      # Recherche et consultation des véhicules
│   ├── reservation/    # Parcours de réservation
│   ├── vente/          # Parcours de vente
│   └── admin/          # Administration
└── assets/
    └── i18n/
        ├── fr/         # Français
        ├── ar/         # Arabe
        └── en/         # Anglais
```

L'application sera conçue pour être responsive et accessible, avec une prise en charge initiale du français, de l'arabe et de l'anglais.
