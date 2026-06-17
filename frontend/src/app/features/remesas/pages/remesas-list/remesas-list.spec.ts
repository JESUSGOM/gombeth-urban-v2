import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RemesasList } from './remesas-list';

describe('RemesasList', () => {
  let component: RemesasList;
  let fixture: ComponentFixture<RemesasList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RemesasList],
    }).compileComponents();

    fixture = TestBed.createComponent(RemesasList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
