import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AvailabilityCalendarComponent } from './availability-calendar.component';
import { TranslateModule } from '@ngx-translate/core';

describe('AvailabilityCalendarComponent', () => {
  let component: AvailabilityCalendarComponent;
  let fixture: ComponentFixture<AvailabilityCalendarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AvailabilityCalendarComponent, TranslateModule.forRoot()],
    }).compileComponents();

    fixture = TestBed.createComponent(AvailabilityCalendarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the availability calendar component', () => {
    expect(component).toBeTruthy();
  });

  it('should mark dates as available or unavailable correctly', () => {
    component.availability = {
      available: [{ from: '2026-11-01', to: '2026-11-05' }],
      unavailable: [{ from: '2026-11-10', to: '2026-11-15', reason: 'RESERVED' }],
    };

    const dateAvailable = new Date(2026, 10, 2); // 2026-11-02
    const dateUnavailable = new Date(2026, 10, 12); // 2026-11-12

    expect(component.dateClass(dateAvailable, 'month')).toBe('calendar-date-available');
    expect(component.dateClass(dateUnavailable, 'month')).toBe('calendar-date-unavailable');
    expect(component.dateClass(dateAvailable, 'year')).toBe('');
  });

  it('should update selected date and status on date selection', () => {
    component.availability = {
      available: [{ from: '2026-11-01', to: '2026-11-05' }],
      unavailable: [{ from: '2026-11-10', to: '2026-11-15' }],
    };

    const unavailDate = new Date(2026, 10, 12);
    component.onDateSelected(unavailDate);
    expect(component.selectedStatus).toBe('UNAVAILABLE');

    const availDate = new Date(2026, 10, 3);
    component.onDateSelected(availDate);
    expect(component.selectedStatus).toBe('AVAILABLE');
  });
});

