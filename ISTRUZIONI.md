# Pressione — App Android per il monitoraggio di pressione e frequenza cardiaca

App personale per registrare pressione arteriosa e pulsazioni, con **riconoscimento automatico dei valori fotografando il display del misuratore** (OCR integrato che funziona **offline**, senza account né cloud).

## Funzioni

- **Foto misurazione**: scatti una foto al display; l'app legge massima, minima, pulsazioni e indicatore di battito irregolare. Prima di salvare puoi verificare e correggere tutto (compresa data e ora).
- **Inserimento manuale** come alternativa alla foto.
- **Storico** completo, con modifica ed eliminazione di ogni voce.
- **Grafici**: andamento di massima, minima e pulsazioni (7 giorni / 30 giorni / 3 mesi / 1 anno / tutto); medie giornaliere, settimanali e mensili; confronto tra periodi (7/30/90 giorni).
- **Avvisi informativi** con soglie personali configurabili (pressione alta/bassa, pulsazioni, variazioni significative). Nessuna diagnosi: gli avvisi non sostituiscono il parere medico.
- **Dati 100% locali**: nessun account, nessun cloud. I dati sono salvati nella memoria del telefono.
- **Esportazione CSV** (per Excel) e **backup completo** in JSON, con importazione/ripristino.

## Requisiti

- Smartphone Android 8.0 o successivo (minSdk 26).
- Per generare l'APK: [Android Studio](https://developer.android.com/studio) (gratuito) su PC Windows/Mac/Linux. La prima sincronizzazione scarica le dipendenze (serve internet solo sul PC, una volta).

## Generare l'APK con Android Studio (consigliato)

1. Installa e apri **Android Studio** (consigliato l'ultima versione stabile).
2. **File → Open** e seleziona la cartella `pressione`.
3. Attendi la **sincronizzazione Gradle** (scarica le dipendenze; al primo avvio può richiedere qualche minuto; Android Studio provvede a scaricare tutto il necessario, incluso Gradle).
4. Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
5. Al termine, una notifica mostra il percorso dell'APK:
   `app/build/outputs/apk/debug/app-debug.apk`
   (in alternativa: click destro su `app` → *Locate*).

## Installare l'APK sul telefono

1. Copia `app-debug.apk` sul telefono (cavo USB, e-mail, WhatsApp a te stesso, ecc.).
2. Tocca il file dal telefono: se richiesto, autorizza **"Installa app sconosciute"** per l'app con cui apri il file (è normale per gli APK non provenienti dal Play Store).
3. Completa l'installazione. L'app si chiama **Pressione**.

## Alternativa: compilare da riga di comando (senza Android Studio)

Serve un **JDK 17** installato (per esempio [Adoptium Temurin](https://adoptium.net)). Nella cartella del progetto:

```
gradlew.bat assembleDebug
```

L'APK viene creato in `app\build\outputs\apk\debug\app-debug.apk`.

## Consigli per il riconoscimento automatico (OCR)

- Photographa il display **vicino, in orizzontale e ben illuminato**, evitando riflessi e ombre.
- Dopo la foto controlla **sempre** i valori proposti prima di salvare: se qualcosa non torna, correggi a mano (i campi restano modificabili).
- Se i valori non vengono letti correttamente, puoi rifare la foto oppure usarne una dalla galleria e inserire i valori manualmente.

## Privacy e dati

- Nessun permesso di rete richiesto: l'app funziona completamente offline.
- I dati sono in `misurazioni.json` e `impostazioni.json` nella memoria interna dell'app. Fai regolarmente un **backup** (Impostazioni → Esporta backup completo) e conservalo in un posto sicuro.
- Esportando l'APK in versione "debug" non è richiesta alcuna firma particolare per l'uso personale.

## Avvertenza sanitaria

Gli avvisi dell'app sono **puramente informativi** e non costituiscono diagnosi o consiglio medico. Non usare l'app per modificare una terapia. Per valori molto elevati (es. 180/120 o più) o in presenza di sintomi importanti (mal di testa forte, dolore al petto, difficoltà a respirare, disturbi alla vista) segui le indicazioni sanitarie appropriate.
