import { TestBed } from '@angular/core/testing';

import { Comunidad } from './comunidad';

describe('Comunidad', () => {
  let service: Comunidad;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Comunidad);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
