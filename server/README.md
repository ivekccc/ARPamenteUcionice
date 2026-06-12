# Server — simulator pametnog okruženja

Spring Boot 3.5 aplikacija (Java 21) koja simulira senzore pametnih učionica. Stanje se drži u memoriji (`ConcurrentHashMap`), bez baze podataka. Pri pokretanju se učitavaju učionice 101, 102 i 103 sa podrazumevanim vrednostima senzora i rasporedom časova.

## Pokretanje

Potreban je samo Java 21 na putanji — Gradle wrapper je uključen u projekat.

```
./gradlew bootRun
```

Server sluša na adresi `http://localhost:8080`. CORS je dozvoljen za sve origine na `/api/**`.

## Build i testovi

```
./gradlew build
```

## REST API

| Metoda | Putanja | Opis |
|--------|---------|------|
| GET | `/api/rooms` | Lista svih učionica sa trenutnim stanjem |
| GET | `/api/rooms/{roomId}` | Jedna učionica (404 sa praznim telom za nepoznat `roomId`) |
| PUT | `/api/rooms/{roomId}/sensors` | Ažuriranje vrednosti senzora, vraća osveženo stanje |

Telo PUT zahteva (sva polja obavezna):

```json
{"temperatureCelsius": 23.5, "noiseDecibels": 42.0, "carbonDioxidePpm": 750, "occupied": true}
```

Dozvoljeni opsezi: temperatura 10–40 °C, buka 20–110 dB, CO2 300–3000 ppm. Vrednost van opsega vraća 400.

## Ponašanje

- Server računa statuse (`OK`, `WARNING`, `CRITICAL`) za temperaturu, buku i kvalitet vazduha, kao i preporuku na srpskom — klijenti ih samo prikazuju.
- Polja `currentClassName` i `occupiedUntil` se izvode iz rasporeda i trenutnog vremena na serveru; kada učionica nije zauzeta, oba su `null`.
- Na svaki GET server dodaje mali sinusni šum na vraćene vrednosti (temperatura ±0.2, buka ±1.5, CO2 ±15) da bi paneli delovali živo — sačuvano stanje se ne menja.

## Primeri

```
curl http://localhost:8080/api/rooms
curl http://localhost:8080/api/rooms/101
curl -X PUT http://localhost:8080/api/rooms/101/sensors \
  -H "Content-Type: application/json" \
  -d '{"temperatureCelsius": 23.5, "noiseDecibels": 42.0, "carbonDioxidePpm": 750, "occupied": true}'
```
