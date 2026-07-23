import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HealthResponse {
  status: string;
  app: string;
}

@Injectable({
  providedIn: 'root'
})
export class Health {

  private http = inject(HttpClient);

  private apiUrl = '/api/health';

  getHealth(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(this.apiUrl);
  }
}
