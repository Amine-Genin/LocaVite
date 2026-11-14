import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

import { PaymentFailureComponent } from './payment-failure.component';

describe('PaymentFailureComponent', () => {
  let component: PaymentFailureComponent;
  let fixture: ComponentFixture<PaymentFailureComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PaymentFailureComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({
                reservationId: '88',
                paymentId: '42',
                error: 'Solde insuffisant',
              }),
            },
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(PaymentFailureComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait afficher le motif d’échec et permettre de réessayer le paiement', () => {
    expect(component).toBeTruthy();
    spyOn(router, 'navigate');
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('[data-cy="payment-failure-reason"]')?.textContent).toContain(
      'Solde insuffisant',
    );

    const retryBtn = el.querySelector('[data-cy="retry-payment-btn"]') as HTMLButtonElement;
    expect(retryBtn).toBeTruthy();
    retryBtn.click();

    expect(router.navigate).toHaveBeenCalledWith(['/payment', 88]);
  });
});
