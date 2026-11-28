import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SeasonFormModalComponent, SeasonFormModalData } from './season-form-modal.component';
import { Season } from '../services/pricing-admin.service';

const EXISTING_SEASONS: Season[] = [
  {
    id: 1,
    nom: 'Haute saison été',
    dateDebut: '2026-07-01',
    dateFin: '2026-08-31',
    coefficient: 1.5,
    priorite: 3,
    isRecurring: true,
  },
];

describe('SeasonFormModalComponent', () => {
  let component: SeasonFormModalComponent;
  let fixture: ComponentFixture<SeasonFormModalComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<SeasonFormModalComponent>>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    const data: SeasonFormModalData = { existingSeasons: EXISTING_SEASONS };

    await TestBed.configureTestingModule({
      imports: [SeasonFormModalComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SeasonFormModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait bloquer un chevauchement côté client avec la même priorité', () => {
    component.form.patchValue({
      nom: 'Saison en conflit',
      dateDebut: new Date(2026, 7, 1),
      dateFin: new Date(2026, 8, 15),
      coefficient: 1.7,
      priorite: 3,
    });
    fixture.detectChanges();

    expect(component.form.invalid).toBeTrue();
    expect(component.overlapErrorMessage).toContain('Chevauchement détecté');
  });

  it('devrait autoriser un chevauchement si la priorité est distincte et soumettre le formulaire', () => {
    component.form.patchValue({
      nom: 'Pic Août',
      dateDebut: new Date(2026, 7, 1),
      dateFin: new Date(2026, 7, 20),
      coefficient: 1.9,
      priorite: 4,
    });
    fixture.detectChanges();

    expect(component.form.valid).toBeTrue();
    component.submit();

    expect(dialogRefSpy.close).toHaveBeenCalledWith(
      jasmine.objectContaining({
        nom: 'Pic Août',
        dateDebut: '2026-08-01',
        dateFin: '2026-08-20',
        coefficient: 1.9,
        priorite: 4,
      }),
    );
  });
});
