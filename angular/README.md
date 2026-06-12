# Kontrolna tabla — Pametni kampus

Veb aplikacija za upravljanje senzorima pametnih učionica tokom demonstracije uživo. Prikazuje karticu za svaku učionicu sa kliznicima za temperaturu, buku i CO₂, prekidačem zauzetosti, rasporedom časova i preporukom.

## Preduslovi

- Node.js 22 i npm 10
- Pokrenut server na adresi `http://localhost:8080` (iz foldera `server/` komandom `./gradlew bootRun`)

## Pokretanje

```
npm install
npm start
```

Aplikacija se otvara na adresi `http://localhost:4200` i očekuje server na adresi `http://localhost:8080`.

## Build

```
npm run build
```

## Ponašanje

- Stanje učionica se osvežava sa servera na svakih 5 sekundi.
- Pomeranje kliznika ili promena zauzetosti šalje `PUT /api/rooms/{roomId}/sensors` posle 300 ms zatišja.
- Vrednost koju je korisnik dirao u poslednjih 5 sekundi se ne prepisuje podacima sa servera.
- Tačka u zaglavlju pokazuje status veze sa serverom: zelena kada poslednje osvežavanje uspe, crvena u suprotnom.
