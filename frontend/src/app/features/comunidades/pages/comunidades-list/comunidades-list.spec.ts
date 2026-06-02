import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ComunidadesList } from './comunidades-list';

describe('ComunidadesList', () => {
  let component: ComunidadesList;
  let fixture: ComponentFixture<ComunidadesList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComunidadesList],
    }).compileComponents();

    fixture = TestBed.createComponent(ComunidadesList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
