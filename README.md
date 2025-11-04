# Scenario

Progetta e implementa una soluzione a **microservizi** per la gestione di un flusso di Fatturazione Elettronica, 
in cui più servizi collaborano per ricevere, validare, firmare e inviare una fattura.  

## Architettura:

![Architettura sistema](.\Architettura.png)

## Sequence Diagram

### Inserimento fattura interna
Si suppone l'utente sia loggato e abbia compilato la fattura attraverso un'interfaccia apposita.
L'utente è presente nel sistema.

![Sequence diagram](.\Sequence.png)

### Inserimento fattura esterna
Si suppone il sistema che invia la fattura per il cliente aruba
conosca il customer_id dell'utente

![Sequence diagram 1](.\Sequence-1.png)

### SdI notifica il risultato della spedizione della fattura
Si suppone venga mandato customer id, invoice number e stato

![Sequence diagram 2](.\Sequence-2.png)

### RECEIVER SERVER
Riceve fatture nuove e accende un thread virtuale per la gestione.
Ha una propria cache per associare customer, fattura a callback di risposta per mantenere il meccanismo asincrono.
Si occupa di creare un thread virtuale per l'invio a DBMANAGER usando la coda kafka INCOMING_INVOICE o SDI_NOTIFICATION.
Il thread attende che la medesima fattura arrivi sulla coda SAVED_INCOMING_INVOICE per chiudere il giro e chiamare la callback
notificando lo stato della fattura, che potrebbe anche essere INVALID_INVOICE.
In caso non riesca a salvare dopo un certo numero di tentativi, la fattura finisce in DLT.
In caso non riesca a chiamare la callback dopo un certo numero di tentativi, la fattura finisce in DLT.

##### FLUSSO di lavoro
Il servizio di ricezione ha 3 endpoint:

salvaFatturaInterna ->

    Necessita dei seguenti dati in ingresso: invoiceDto (contiene anche customerId e callback per la risposta)
    Avvia un virtual thread che, per ogni fattura in ingresso:
    Salva su cache locale customerId, invoiceNumber e riferimento a thread virtuale.  
    Mette fattura in stato INTERNAL_INVOICE_NEW
    Invia a kafka su topic INCOMING_INVOICE
    Viene risvegliato da receiver kafka quando la corrispondente fattura arriva su SAVED_INCOMING_INVOICE
    Ha un timeout oltre il quale manda la fattura in dlt
    Invia la risposta chiamando la callback
    Ha un timeout oltre il quale manda la fattura in dlt

salvaFatturaEsterna ->

    Necessita dei seguenti dati in ingresso: invoiceDto (contiene anche customerId e callback per la risposta)
    Avvia un virtual thread che, per ogni fattura in ingresso:
    Salva su cache locale customerId, invoiceNumber e riferimento a thread virtuale.  
    Mette fattura in stato EXTERNAL_INVOICE
    Invia a kafka su topic INCOMING_INVOICE
    Viene risvegliato da receiver kafka quando la corrispondente fattura arriva su SAVED_INCOMING_INVOICE
    Invia la risposta chiamando la callback
    Ha un timeout oltre il quale manda la fattura in dlt

notificaSdI ->

    Necessita dei seguenti dati in ingresso: customerId, invoiceNumber, stato
    Avvia un virtual thread che, per ogni notifica in ingresso:
    Salva su cache locale customerId, invoiceNumber e riferimento a thread virtuale.  
    Mette fattura in stato INTERNAL_INVOICE_DELIVERED, INTERNAL_INVOICE_DISCARDED o INTERNAL_INVOICE_NOT_DELIVERED
    Invia a kafka su topic DSI_NOTIFICATION
    Viene risvegliato da receiver kafka quando la corrispondente fattura arriva su SAVED_INCOMING_INVOICE
    Invia la risposta chiamando la callback se esiste.
    Ha un timeout oltre il quale manda il dato in dlt

### DBMANAGER SERVER
Dedicato allo storage delle fatture e dei clienti.
La validazione supponiamo sia la verifica dell'esistenza del customer passato.
Dopo la validazione ci sarebbe la firma, che potrebbe essere svolta da un microservizio diverso.

Accesso a REST per la ricerca di clienti e fatture e per la cancellazione di fatture e customers: 
per uso statistico e consultazione e gestione manuale.
Accesso comandato da kafka per creazione/update/cancellazione di fatture e customers.
Legge da INCOMING_INVOICE le fatture in entrata, scrive su SAVED_INCOMING_INVOICE le fatture in entrata salvate e 
scrive su OUTGOING_INVOICE le fatture valide e da spedire a SdI.
Legge da SENT_INVOICE per fare update della fattura in caso di successo o errore. 
Legger da DSI_NOTIFICATION per le ricevute delle fatture inviate a SdI e scrive su SAVED_INCOMING_INVOICE il risultato del salvataggio
Se non riesce a comunicare su sistemi esterni dopo un certo numero di tentativi, il dato va il DLT per gestione successiva

#### FLUSSO di lavoro
Il DbManager riceve su INCOMING_INVOICE e su DSI_NOTIFICATION:

    Riceve su INCOMING_INVOICE
    Avvia un thread per ogni fattura in ingresso
    Verifica se customerId esiste 
    Se si salva la fattura e scrive su SAVED_INCOMING_INVOICE e su OUTGOING_INVOICE con stato INTERNAL_INVOICE_TOBE_SENT
        Attende una risposta su SENT_INVOICE per aggiornare lo stato a INTERNAL_INVOICE_SENT o INTERNAL_INVOICE_NOT_SENT 
    Se no salva la fattura e scrive su SAVED_INCOMING_INVOICE con stato INTERNAL_INVOICE_INVALID
    Il thread ha un timeout per mandare in DLT la fattura se non riesce a comunicare con qualche sistema esterno

    Riceve su DSI_NOTIFICATION
    Avvia un thread per ogni fattura in ingresso
    Verifica se customerId e invoiceNumber esistono
    Se si carica cambia lo stato della fattura in INTERNAL_INVOICE_DELIVERED, INTERNAL_INVOICE_DISCARDED o INTERNAL_INVOICE_NOT_DELIVERED 
    e scrive su SAVED_INCOMING_INVOICE  
    Se no scrive su SAVED_INCOMING_INVOICE con stato INTERNAL_INVOICE_INVALID 
    Il thread ha un timeout per mandare in DLT la notifica ricevuta se non riesce a comunicare con qualche sistema esterno


### SENDER SERVER
Legge da kafka OUTGOING_INVOICE per mandare a SdI e scrive su SENT_INVOICE il risultato della spedizione.
Se dopo un certo numero di tentativi non riesce a comunicare ad altri sistemi il risultato della propria operazione,
il dato va nella DLT per gestione successiva.
Supponiamo che l'invio a SdI sia via email, server e porta configurabili

### SCHEDULER SERVER
Ha un job che gira ogni x minuti configurabili per la lettura delle DLT di tutti i microservizi 
ed eventuale notifica dell'esistenza di problemi scrivendo un report che viene salvato come csv 
in una cartella configurata
Ha un job che gira ogni x minuti configurabili che interroga il dbmanagre per creare un semplice csv con:
stato fattura, numero fattura, username customer, email customer, data creazione e chi ha creato, 
data update e chi ha fatto update. Il file viene salvato in apposita cartella anch'essa configurabile  

### CONFIGURATION SERVER
Per la gestione di configurazioni specifiche per l'ambiente

### EUREKA SERVER
Per il discovery dei servizi

### GATEWAY SERVER
Punto di autenticazione/autorizzazione e ingresso dall'esterno

Qualsiasi microservizio ha il seguente meccanismo di gestione errori:
Counter errori
dopo tot numero di errori la fattura va in INVOICE_ERROR

---
## Comandi utili

#### Creazione DB
In folder docker-compose:
docker compose up  postgres -d
#### Creazione configuration server
In folder docker-compose:
docker compose up  xxx -d
#### Creazione eurekaserver
In folder docker-compose:
docker compose up  eurekaserver -d
#### Creazione kafka
In folder docker-compose:
docker compose up  kafka -d

#### Compilazione immagine docker
mvn compile jib:dockerBuild

#### Invio a repository docker
mvn jib:build

#### sevizi a supporto
docker compose up kafka kafka-ui postgres gateway backend grafana tempo prometheus minio alloy --build -d --force-recreate
#### microservizi
docker compose up eurekaserver dbmanager receiver sender gatewayserver  --build -d --force-recreate

docker compose down eurekaserver dbmanager receiver sender gatewayserver

#### Avvio mail server per test
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
http://localhost:8025/ per visualizzare le spedizioni effettuate

#### Accesso alla documentazione swagger
http://localhost:9020/swagger-ui/ o più in generale http://localhost:XXXX/swagger-ui/

#### Kafka-ui
http://localhost:9094/

### Test con gatling
Questo comando compilerà ed eseguirà tutte le simulazioni Gatling
che trova nel tuo progetto in src/test/scala.

    mvn gatling:test

Se vuoi eseguire una simulazione specifica, puoi usare l'opzione 

    -Dgatling.simulationClass:Shell Scriptmvn gatling:test -Dgatling.simulationClass=com.experis.receiver.simulations.YourSimulationName


