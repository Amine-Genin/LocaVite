import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';

import { AvailabilityCalendarComponent } from './availability-calendar.component';
import { AvailabilityService } from '../services/availability.service';
import { AvailabilityResponse } from '../models/vehicle.model';

describe('AvailabilityCalendarComponent', () => {
  let component: AvailabilityCalendarComponent;
  let fixture: ComponentFixture<AvailabilityCalendarComponent>;
  let availabilityServiceSpy: jasmine.SpyObj<AvailabilityService>;

  const mockAvailability: AvailabilityResponse = {
    available: [
      { from: '2026-12-01', to: '2026-12-10' },
    ],
    unavailable: [
      { from: '2026-11-20', to: '2026-11-25', reason: 'RESERVED' },
    ],
  };

  beforeEach(async () => {
    availabilityServiceSpy = jasmine.createSpyObj('AvailabilityService', ['getAvailability']);
    availabilityServiceSpy.getAvailability.and.returnValue(of(mockAvailability));

    await TestBed.configureTestingModule({
      imports: [AvailabilityCalendarComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        { provide: AvailabilityService, useValue: availabilityServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AvailabilityCalendarComponent);
    component = fixture.componentInstance;
    component.availability = mockAvailability;
    fixture.detectChanges();
  });

  it('should create and initialize availability', () => {
    expect(component).toBeTruthy();
    expect(component.activeAvailability).toEqual(mockAvailability);
  });

  it('should mark unavailable days with calendar-date-unavailable', () => {
    const unavailableDate = new Date('2026-11-22T00:00:00');
    const className = component.dateClass(unavailableDate, 'month');
    expect(className).toBe('calendar-date-unavailable');
  });

  it('should block past dates and unavailable dates with dateFilter', () => {
    const pastDate = new Date('2020-01-01T00:00:00');
    expect(component.dateFilter(pastDate)).toBeFalse();

    const unavailableDate = new Date('2026-11-21T00:00:00');
    expect(component.dateFilter(unavailableDate)).toBeFalse();

    const futureAvailableDate = new Date('2026-12-05T00:00:00');
    expect(component.dateFilter(futureAvailableDate)).toBeTrue();
  });

  it('should allow range selection and emit dateRangeChange', () => {
    spyOn(component.dateRangeChange, 'emit');

    const startDate = new Date('2026-12-02T00:00:00');
    component.onDateSelected(startDate);
    expect(component.selectedRange.start).toEqual(startDate);
    expect(component.selectedRange.end).toBeNull();

    const endDate = new Date('2026-12-06T00:00:00');
    component.onDateSelected(endDate);
    expect(component.selectedRange.end).toEqual(endDate);
    expect(component.dateRangeChange.emit).toHaveBeenCalledWith({
      start: startDate,
      end: endDate,
    });
  });

  it('should show tooltip for unavailable dates', () => {
    const unavail = new Date('2026-11-20T00:00:00');
    component.evaluateStatus(unavail);

    expect(component.selectedStatus).toBe('UNAVAILABLE');
    expect(component.selectedTooltip).toContain('Indisponible (réservé du 2026-11-20 au 2026-11-25)');
  });
});
