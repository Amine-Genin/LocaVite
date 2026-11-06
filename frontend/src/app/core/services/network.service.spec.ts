import { TestBed } from '@angular/core/testing';
import { NetworkService } from './network.service';

describe('NetworkService', () => {
  let service: NetworkService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NetworkService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize isOnline signal with current navigator state', () => {
    expect(service.isOnline()).toBe(navigator.onLine);
  });

  it('should update isOnline on online event', () => {
    service.isOnline.set(false);
    window.dispatchEvent(new Event('online'));
    expect(service.isOnline()).toBeTrue();
  });

  it('should update isOnline on offline event', () => {
    service.isOnline.set(true);
    window.dispatchEvent(new Event('offline'));
    expect(service.isOnline()).toBeFalse();
  });

  it('should refresh status using checkConnection()', () => {
    const status = service.checkConnection();
    expect(typeof status).toBe('boolean');
    expect(service.isOnline()).toBe(status);
  });
});
