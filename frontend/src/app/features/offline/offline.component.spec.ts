import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { OfflineComponent } from './offline.component';
import { NetworkService } from '../../core/services/network.service';

describe('OfflineComponent', () => {
  let component: OfflineComponent;
  let fixture: ComponentFixture<OfflineComponent>;
  let networkService: NetworkService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OfflineComponent, TranslateModule.forRoot()],
      providers: [NetworkService, provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(OfflineComponent);
    component = fixture.componentInstance;
    networkService = TestBed.inject(NetworkService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should navigate to home on retry() when connection is restored', () => {
    spyOn(networkService, 'checkConnection').and.returnValue(true);
    spyOn(router, 'navigate');

    component.retry();
    expect(networkService.checkConnection).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should stay on offline page on retry() when still offline', () => {
    spyOn(networkService, 'checkConnection').and.returnValue(false);
    spyOn(router, 'navigate');

    component.retry();
    expect(networkService.checkConnection).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
