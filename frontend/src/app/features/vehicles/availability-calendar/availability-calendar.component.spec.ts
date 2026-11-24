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

  const today = new Date();
  const formatIsoDate = (d: Date): string => d.toISOString().split('T')[0];

  const futureFrom = new Date(today.getFullYear(), today.getMonth() + 1, 1);
  const futureTo = new Date(today.getFullYear(), today.getMonth() + 1, 10);
  const unavailFrom = new Date(today.getFullYear(), today.getMonth() + 1, 15);
  const unavailTo = new Date(today.getFullYear(), today.getMonth() + 1, 20);

  const mockAvailability: AvailabilityResponse = {
    available: [
      { from: formatIsoDate(futureFrom), to: formatIsoDate(futureTo) },
    ],
    unavailable: [
      { from: formatIsoDate(unavailFrom), to: formatIsoDate(unavailTo), reason: 'RESERVED' },
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
    const unavailableDate = new Date(unavailFrom.getTime() + 24 * 60 * 60 * 1000);
    const className = component.dateClass(unavailableDate, 'month');
    expect(className).toBe('calendar-date-unavailable');
  });

  it('should block past dates and unavailable dates with dateFilter', () => {
    const pastDate = new Date('2020-01-01T00:00:00');
    expect(component.dateFilter(pastDate)).toBeFalse();

    const unavailableDate = new Date(unavailFrom.getTime() + 24 * 60 * 60 * 1000);
    expect(component.dateFilter(unavailableDate)).toBeFalse();

    const futureAvailableDate = new Date(futureFrom.getTime() + 24 * 60 * 60 * 1000);
    expect(component.dateFilter(futureAvailableDate)).toBeTrue();
  });

  it('should allow range selection and emit dateRangeChange', () => {
    spyOn(component.dateRangeChange, 'emit');

    const startDate = new Date(futureFrom.getTime() + 24 * 60 * 60 * 1000);
    component.onDateSelected(startDate);
    expect(component.selectedRange.start).toEqual(startDate);
    expect(component.selectedRange.end).toBeNull();

    const endDate = new Date(futureFrom.getTime() + 3 * 24 * 60 * 60 * 1000);
    component.onDateSelected(endDate);
    expect(component.selectedRange.end).toEqual(endDate);
    expect(component.dateRangeChange.emit).toHaveBeenCalledWith({
      start: startDate,
      end: endDate,
    });
  });

  it('should show tooltip for unavailable dates', () => {
    const unavail = new Date(unavailFrom.getTime() + 24 * 60 * 60 * 1000);
    component.evaluateStatus(unavail);

    expect(component.selectedStatus).toBe('UNAVAILABLE');
    expect(component.selectedTooltip).toContain(`Indisponible (réservé du ${formatIsoDate(unavailFrom)} au ${formatIsoDate(unavailTo)})`);
  });
});
