import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { VecinosList } from './vecinos-list';

describe('VecinosList', () => {
  let component: VecinosList;
  let fixture: ComponentFixture<VecinosList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VecinosList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VecinosList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
