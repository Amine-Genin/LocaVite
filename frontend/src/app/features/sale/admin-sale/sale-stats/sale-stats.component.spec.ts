import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { SaleStatsComponent, SaleStatsDialogData } from './sale-stats.component';
import { SaleListing } from '../../models/sale-listing.model';

describe('SaleStatsComponent', () => {
  let component: SaleStatsComponent;
  let fixture: ComponentFixture<SaleStatsComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<SaleStatsComponent>>;

  const mockListing: SaleListing = {
    id: 10,
    vehicleId: 2,
    titre: 'Renault Clio 5',
    prixVente: 140000,
    etat: 'USED',
    kilometrage: 35000,
    marque: 'Renault',
    modele: 'Clio',
    annee: 2021,
    ville: 'Rabat',
    carburant: 'ESSENCE',
    categorie: 'CITADINE',
    photoUrl: null,
    publishedAt: new Date(Date.now() - 5 * 24 * 3600 * 1000).toISOString(),
  };

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    const dialogData: SaleStatsDialogData = {
      listing: mockListing,
      stats: {
        listingId: 10,
        viewsCount: 42,
        contactsCount: 5,
        visitsCount: 2,
        daysOnline: 5,
      },
    };

    await TestBed.configureTestingModule({
      imports: [SaleStatsComponent, TranslateModule.forRoot()],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: dialogRefSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SaleStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and display provided stats', () => {
    expect(component).toBeTruthy();
    expect(component.stats.viewsCount).toBe(42);
    expect(component.stats.contactsCount).toBe(5);
    expect(component.stats.visitsCount).toBe(2);
    expect(component.stats.daysOnline).toBe(5);
  });

  it('should close dialog when clicking close', () => {
    component.close();
    expect(dialogRefSpy.close).toHaveBeenCalled();
  });
});
