import { Component, WritableSignal, computed, inject, input, linkedSignal, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, debounceTime, switchMap, tap } from 'rxjs';
import { RoomResponse, SensorStatus, SensorUpdateRequest } from '../../models/room.models';
import { RoomApiService } from '../../services/room-api.service';

@Component({
  selector: 'app-room-card',
  templateUrl: './room-card.component.html',
  styleUrl: './room-card.component.scss'
})
export class RoomCardComponent {
  readonly room = input.required<RoomResponse>();

  private readonly roomApiService = inject(RoomApiService);
  private readonly sensorChanges = new Subject<void>();

  protected readonly lastInteractionAt = signal(0);
  protected readonly displayedRoom = linkedSignal(() => this.room());
  protected readonly temperatureCelsius = this.createGuardedSensorSignal((serverRoom) => serverRoom.temperatureCelsius);
  protected readonly noiseDecibels = this.createGuardedSensorSignal((serverRoom) => serverRoom.noiseDecibels);
  protected readonly carbonDioxidePpm = this.createGuardedSensorSignal((serverRoom) => serverRoom.carbonDioxidePpm);
  protected readonly occupied = this.createGuardedSensorSignal((serverRoom) => serverRoom.occupied);

  protected readonly temperatureReadout = computed(() => this.temperatureCelsius().toFixed(1));
  protected readonly noiseReadout = computed(() => Math.round(this.noiseDecibels()).toString());
  protected readonly carbonDioxideReadout = computed(() => Math.round(this.carbonDioxidePpm()).toString());

  protected readonly temperatureFillPercentage = computed(() => ((this.temperatureCelsius() - 15) / 20) * 100);
  protected readonly noiseFillPercentage = computed(() => ((this.noiseDecibels() - 30) / 60) * 100);
  protected readonly carbonDioxideFillPercentage = computed(() => ((this.carbonDioxidePpm() - 400) / 1600) * 100);

  protected readonly worstStatus = computed<SensorStatus>(() => {
    const currentRoom = this.displayedRoom();
    const statuses = [currentRoom.temperatureStatus, currentRoom.noiseStatus, currentRoom.airQualityStatus];
    if (statuses.includes('CRITICAL')) {
      return 'CRITICAL';
    }
    if (statuses.includes('WARNING')) {
      return 'WARNING';
    }
    return 'OK';
  });

  constructor() {
    this.sensorChanges
      .pipe(
        debounceTime(300),
        switchMap(() => {
          const request: SensorUpdateRequest = {
            temperatureCelsius: this.temperatureCelsius(),
            noiseDecibels: this.noiseDecibels(),
            carbonDioxidePpm: this.carbonDioxidePpm(),
            occupied: this.occupied()
          };
          return this.roomApiService.updateSensors(this.room().roomId, request).pipe(
            tap((updatedRoom) => this.displayedRoom.set(updatedRoom)),
            catchError(() => EMPTY)
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe();
  }

  protected changeTemperature(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    this.temperatureCelsius.set(Number(inputElement.value));
    this.registerInteraction();
  }

  protected changeNoise(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    this.noiseDecibels.set(Number(inputElement.value));
    this.registerInteraction();
  }

  protected changeCarbonDioxide(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    this.carbonDioxidePpm.set(Number(inputElement.value));
    this.registerInteraction();
  }

  protected toggleOccupied(): void {
    this.occupied.update((currentValue) => !currentValue);
    this.registerInteraction();
  }

  protected statusClass(status: SensorStatus): string {
    if (status === 'OK') {
      return 'status-ok';
    }
    if (status === 'WARNING') {
      return 'status-warning';
    }
    return 'status-critical';
  }

  protected statusColorToken(status: SensorStatus): string {
    if (status === 'OK') {
      return 'var(--color-status-ok)';
    }
    if (status === 'WARNING') {
      return 'var(--color-status-warning)';
    }
    return 'var(--color-status-critical)';
  }

  private registerInteraction(): void {
    this.lastInteractionAt.set(Date.now());
    this.sensorChanges.next();
  }

  private createGuardedSensorSignal<Value>(selectValue: (serverRoom: RoomResponse) => Value): WritableSignal<Value> {
    return linkedSignal<RoomResponse, Value>({
      source: this.room,
      computation: (serverRoom, previous) => {
        const recentlyTouched = Date.now() - untracked(() => this.lastInteractionAt()) <= 5000;
        if (previous !== undefined && recentlyTouched) {
          return previous.value;
        }
        return selectValue(serverRoom);
      }
    });
  }
}
