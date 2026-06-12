# AR Pametne Učionice — Android aplikacija

Android aplikacija koja preko ARCore prepoznaje marker na vratima učionice i iznad njega prikazuje panel sa živim podacima sa servera (temperatura, buka, CO₂, zauzetost, preporuka).

## Pokretanje

1. Otvori folder `android/` u Android Studiju (File → Open).
2. Sačekaj da se Gradle sinhronizacija završi (projekat koristi Gradle 8.13, AGP 8.13.0, Kotlin 2.1.20).
3. Poveži telefon koji podržava ARCore (uključen USB debugging) i pokreni konfiguraciju `app`.
4. Telefon i računar na kome radi server moraju biti na istoj WiFi mreži.

## Adresa servera

Na početnom ekranu, u polje "Adresa servera" upiši adresu računara na kome radi Spring Boot server, na primer `http://192.168.0.49:8080`. Adresa se pamti automatski. Server se pokreće sa `cd server && ./gradlew bootRun`.

## Štampanje markera

1. Odštampaj fajl `app/src/main/assets/markers/ucionica_101.png` na A4 papiru, tako da širina odštampane slike bude oko 21 cm (cela širina A4 strane).
2. Zalepi marker na vrata učionice u visini očiju.
3. U aplikaciji pritisni "Pokreni AR pregled" i uperi kameru u marker — panel sa podacima se prikazuje iznad markera.

## Zamena i dodavanje markera

Markeri se učitavaju iz foldera `app/src/main/assets/markers/` — svaki PNG fajl postaje jedan marker. Ime fajla određuje učionicu: deo posle poslednje donje crte je `roomId`, na primer `ucionica_101.png` → učionica `101`. Za novu učionicu dovoljno je ubaciti novi PNG (na primer `ucionica_102.png`) i ponovo instalirati aplikaciju. Slika treba da bude bogata detaljima i kontrastom; slike slabog kvaliteta ARCore preskače pri učitavanju.
