import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, interval, startWith, switchMap, tap } from 'rxjs';
import { RoomResponse } from './models/room.models';
import { RoomApiService } from './services/room-api.service';
import { RoomCardComponent } from './components/room-card/room-card.component';

@Component({
  selector: 'app-root',
  imports: [RoomCardComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly roomApiService = inject(RoomApiService);

  protected readonly rooms = signal<RoomResponse[]>([]);
  protected readonly connectionHealthy = signal(false);
  protected readonly currentTime = signal(this.formatCurrentTime());

  constructor() {
    interval(5000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.roomApiService.getRooms().pipe(
            tap((roomList) => {
              this.rooms.set(roomList);
              this.connectionHealthy.set(true);
            }),
            catchError(() => {
              this.connectionHealthy.set(false);
              return EMPTY;
            })
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe();

    interval(1000)
      .pipe(
        tap(() => this.currentTime.set(this.formatCurrentTime())),
        takeUntilDestroyed()
      )
      .subscribe();
  }

  private formatCurrentTime(): string {
    return new Date().toLocaleTimeString('sr-RS', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  }
}
