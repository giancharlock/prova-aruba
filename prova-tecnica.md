## Prova Tecnica – Sviluppatore Senior JAVA

### Scenario

Progetta e implementa una soluzione a **microservizi** per la gestione di un flusso di Fatturazione Elettronica, 
in cui più servizi collaborano per ricevere, validare, firmare e inviare una fattura.  
La soluzione deve essere pensata per ambienti ad alto carico e scalabilità, con attenzione alla robustezza e alla gestione delle sessioni utente.

---

### Requisiti Tecnici

1. **API Gateway**  
   - Implementa un API Gateway (Spring Cloud Gateway o simile) che gestisca l’autenticazione e la sessione utente, memorizzando le sessioni su Redis.
   - Il Gateway deve esporre endpoint REST per l’inserimento e la consultazione delle fatture.

2. **Microservizi e Kafka/SAGA**  
   - Progetta almeno 3 microservizi distinti (ad esempio: Ricezione, Validazione, Invio).
   - Lo scambio tra microservizi deve avvenire tramite Kafka, implementando il protocollo SAGA per la gestione delle transazioni distribuite e dei rollback.
   - Ogni microservizio deve essere autonomo e documentato.

3. **Gestione Errori e Retry**  
   - Implementa una gestione degli errori robusta, con politiche di retry configurabili (es. exponential backoff, dead letter topic).
   - Gli errori devono essere tracciati e gestiti in modo da non perdere dati o generare duplicati.

4. **Alto Carico e Concorrenza**  
   - Predisponi la soluzione per gestire un alto numero di chiamate concorrenti (es. thread pool, configurazione dei consumer Kafka, test di carico).
   - Documenta le scelte fatte per la scalabilità e la resilienza.

5. **Scheduler/CronJob**  
   - Implementa almeno una operazione schedulata (Spring Scheduler/CronJob) per la pulizia di fatture in errore o la generazione di report periodici.

---

### Consegna

- **Codice sorgente** (preferibilmente su repository Git)
- **README** con spiegazione architetturale, scelte progettuali e istruzioni di avvio
- **Diagramma** (anche semplice) che illustri il flusso tra i microservizi e il protocollo SAGA
- **Commenti** e **documentazione** nei punti chiave del codice

---

#### Indicazioni pratiche

La prova tecnica non richiede una soluzione completa e pronta per la produzione.  
Puoi tralasciare test automatici, configurazioni avanzate di sicurezza, deployment, e la documentazione dettagliata delle API.  
Concentrati sull’architettura, il flusso principale, le scelte tecniche e la spiegazione dei passaggi chiave.  
Se necessario, puoi simulare alcune integrazioni (Kafka, Redis) o lasciare parti come pseudocodice/commenti.

### Note per il colloquio

Durante la presentazione, il candidato dovrà:
- Spiegare le scelte architetturali e di dedbmanager
- Motivare le soluzioni adottate per la gestione delle sessioni, degli errori e della concorrenza
- Illustrare come il protocollo SAGA è stato implementato e gestito con Kafka
- Descrivere come la soluzione può essere estesa o adattata per altri flussi documentali

---

**Obiettivo:**  
Valutare la capacità del candidato di progettare, 
implementare e spiegare una soluzione enterprise in Java/Spring, 
con attenzione a scalabilità, robustezza e best practice.

