import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';

import { AdminSaleComponent } from './admin-sale.component';
import { SaleListingService } from '../services/sale-listing.service';
import { SaleListing, SaleListingPage } from '../models/sale-listing.model';

describe('AdminSaleComponent', () => {
  let component: AdminSaleComponent;
  let fixture: ComponentFixture<AdminSaleComponent>;
  let saleServiceSpy: jasmine.SpyObj<SaleListingService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const mockListings: SaleListing[] = [
    {
      id: 1,
      vehicleId: 101,
      titre: 'Dacia Duster 2022',
      prixVente: 185000,
      etat: 'USED',
      kilometrage: 45000,
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2022,
      ville: 'Casablanca',
      carburant: 'DIESEL',
      categorie: 'SUV',
      photoUrl: null,
      statut: 'PUBLISHED',
      publishedAt: new Date(Date.now() - 3 * 24 * 3600 * 1000).toISOString(),
    },
    {
      id: 2,
      vehicleId: 102,
      titre: 'Peugeot 208 Neuve',
      prixVente: 210000,
      etat: 'NEW',
      kilometrage: 0,
      marque: 'Peugeot',
      modele: '208',
      annee: 2024,
      ville: 'Rabat',
      carburant: 'ESSENCE',
      categorie: 'CITADINE',
      photoUrl: null,
      statut: 'DRAFT',
      publishedAt: null,
    },
  ];

  const mockPage: SaleListingPage = {
    content: mockListings,
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 10,
  };

  beforeEach(async () => {
    saleServiceSpy = jasmine.createSpyObj('SaleListingService', [
      'getMyListings',
      'publish',
      'unpublish',
      'markSold',
      'delete',
    ]);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    dialogSpy.open.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);

    saleServiceSpy.getMyListings.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [AdminSaleComponent, TranslateModule.forRoot()],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: SaleListingService, useValue: saleServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogSpy },
      ],
    })
      .overrideComponent(AdminSaleComponent, {
        set: { providers: [{ provide: MatDialog, useValue: dialogSpy }] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(AdminSaleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait afficher les annonces dans le tableau', () => {
    expect(component).toBeTruthy();
    expect(component.listings().length).toBe(2);
    expect(component.filteredListings().length).toBe(2);
    expect(saleServiceSpy.getMyListings).toHaveBeenCalledWith(0, 10, undefined);
  });

  it('devrait filtrer les annonces par état (NEW / USED)', () => {
    component.onConditionFilterChange('NEW');
    expect(component.filteredListings().length).toBe(1);
    expect(component.filteredListings()[0].marque).toBe('Peugeot');

    component.onConditionFilterChange('');
    expect(component.filteredListings().length).toBe(2);
  });

  it('devrait filtrer les annonces par terme de recherche', () => {
    component.onSearchChange('duster');
    expect(component.filteredListings().length).toBe(1);
    expect(component.filteredListings()[0].marque).toBe('Dacia');
  });

  it('action "Publier" : devrait appeler API et mettre à jour le statut', () => {
    const draft = mockListings[1];
    const published = { ...draft, statut: 'PUBLISHED' as const };
    saleServiceSpy.publish.and.returnValue(of(published));

    component.publish(draft);

    expect(saleServiceSpy.publish).toHaveBeenCalledWith(draft.id);
    expect(snackBarSpy.open).toHaveBeenCalledWith('Annonce publiée avec succès', 'OK', jasmine.any(Object));
    expect(component.listings().find((l) => l.id === draft.id)?.statut).toBe('PUBLISHED');
  });

  it('action "Marquer comme vendu" : devrait appeler API et rafraîchir', () => {
    const pub = mockListings[0];
    const sold = { ...pub, statut: 'SOLD' as const };
    saleServiceSpy.markSold.and.returnValue(of(sold));

    component.markSold(pub);

    expect(saleServiceSpy.markSold).toHaveBeenCalledWith(pub.id);
    expect(snackBarSpy.open).toHaveBeenCalledWith('Véhicule marqué comme vendu', 'OK', jasmine.any(Object));
    expect(component.listings().find((l) => l.id === pub.id)?.statut).toBe('SOLD');
  });

  it('devrait ouvrir la modale de statistiques', () => {
    component.openStats(mockListings[0]);
    expect(dialogSpy.open).toHaveBeenCalled();
  });

  it('devrait exporter les annonces en fichier CSV', () => {
    spyOn(document, 'createElement').and.callThrough();
    component.exportCsv();
    expect(snackBarSpy.open).not.toHaveBeenCalledWith('Aucune annonce à exporter', 'OK', jasmine.any(Object));
  });
});
