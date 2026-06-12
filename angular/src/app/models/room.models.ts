export type SensorStatus = 'OK' | 'WARNING' | 'CRITICAL';

export interface ScheduleEntryResponse {
  startTime: string;
  endTime: string;
  className: string;
  lecturerName: string;
}

export interface RoomResponse {
  roomId: string;
  roomName: string;
  occupied: boolean;
  currentClassName: string | null;
  occupiedUntil: string | null;
  schedule: ScheduleEntryResponse[];
  temperatureCelsius: number;
  temperatureStatus: SensorStatus;
  noiseDecibels: number;
  noiseStatus: SensorStatus;
  carbonDioxidePpm: number;
  airQualityStatus: SensorStatus;
  recommendation: string;
}

export interface SensorUpdateRequest {
  temperatureCelsius: number;
  noiseDecibels: number;
  carbonDioxidePpm: number;
  occupied: boolean;
}
