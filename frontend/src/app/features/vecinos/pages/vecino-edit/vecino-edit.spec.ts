import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { VecinoEdit } from './vecino-edit';

describe('VecinoEdit', () => {
  let component: VecinoEdit;
  let fixture: ComponentFixture<VecinoEdit>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VecinoEdit],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VecinoEdit);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
