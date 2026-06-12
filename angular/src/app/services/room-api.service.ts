import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RoomResponse, SensorUpdateRequest } from '../models/room.models';

@Injectable({ providedIn: 'root' })
export class RoomApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080';

  getRooms(): Observable<RoomResponse[]> {
    return this.httpClient.get<RoomResponse[]>(`${this.baseUrl}/api/rooms`);
  }

  getRoom(roomId: string): Observable<RoomResponse> {
    return this.httpClient.get<RoomResponse>(`${this.baseUrl}/api/rooms/${roomId}`);
  }

  updateSensors(roomId: string, request: SensorUpdateRequest): Observable<RoomResponse> {
    return this.httpClient.put<RoomResponse>(`${this.baseUrl}/api/rooms/${roomId}/sensors`, request);
  }
}
