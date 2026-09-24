import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { RemesasList } from './remesas-list';

describe('RemesasList', () => {
  let component: RemesasList;
  let fixture: ComponentFixture<RemesasList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RemesasList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RemesasList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
