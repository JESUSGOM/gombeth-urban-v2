import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ComunidadService } from './comunidad';

describe('ComunidadService', () => {

  let service: ComunidadService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(
      ComunidadService
    );
  });

  it('debe crear el servicio', () => {
    expect(service).toBeTruthy();
  });
});
