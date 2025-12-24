# declarative-batch-spring-lib

**declarative-batch-spring-lib** è una libreria per semplificare e standardizzare lo sviluppo di **batch Spring** in ambito enterprise. Basata su **Spring Batch 5+** e **Java 17+**, la libreria consente di creare job batch in modo **dichiarativo**, riducendo la complessità del codice e favorendo la riusabilità dei componenti.

---

## Perché dichiarativo?

Adottare un approccio dichiarativo porta diversi vantaggi:

- **Semplificazione**: la configurazione avviene tramite un file YAML o tramite annotazioni, senza necessità di codice Java complesso.
- **Standardizzazione**: tutti i processi batch possono seguire lo stesso modello, facilitando manutenzione ed evoluzione.
- **Flessibilità**: pur con una configurazione semplice, è possibile estendere o personalizzare il comportamento dei batch secondo necessità.

---

## Componenti principali

La libreria fornisce componenti pre-configurati per i batch:

- **Reader**:
    - `JdbcCursorItemReader`
    - `JdbcPagingItemReader`
    - `FlatFileItemReader`
- **Writer**:
    - `JdbcBatchItemWriter`
    - `FlatFileItemWriter`
- **Processor**:
    - `PassThroughItemProcessor`
- **Tasklet**: per operazioni puntuali all’interno di uno step
- **Listener**: supporto per listener a livello di job, step o componente
- **Gestione errori**: configurazione di skip, retry e tolleranza
- **Validazione**: possibilità di validare i parametri in input tramite un validator
- **Flow tra step**: gestione di transizioni lineari e condizionali tra gli step

---

## Vantaggi della libreria

- **Curva di apprendimento ridotta**: ideale anche per chi si avvicina per la prima volta a Spring Batch.
- **Adattabilità**: gli sviluppatori esperti possono implementare logiche personalizzate estendendo o sostituendo i componenti.
- **Modularità**: componenti facilmente sostituibili o estendibili secondo le necessità del progetto.

---

## Configurazione dichiarativa YAML

La libreria supporta la definizione dei job batch tramite file YAML, dove è possibile configurare **job, step, reader, processor, writer e listener** senza scrivere codice Java aggiuntivo.

### 1. `bulk.batch-properties`

Definisce le proprietà generali del batch:

```yaml
batch-properties:
  jdbc:
    platform: postgresql
    initialize-schema: never
    table-prefix: BATCH_
```

### 2. `bulk.datasources`

Questa sezione definisce le sorgenti dati (datasource) utilizzate dal batch. Ogni datasource può contenere i seguenti parametri:

- `main`: booleano che indica se il datasource è quello principale
- `url`: URL di connessione al database
- `username`: nome utente per la connessione
- `password`: password per la connessione
- `type`: tipo di database (es. `POSTGRES`)
- `driver-class-name`: driver JDBC da utilizzare

#### Esempio di configurazione

```yaml
datasources:
  b_declarative:
    main: true
    url: jdbc:postgresql://localhost:5432/b_declarative
    username: user
    password: pass
    type: POSTGRES
    driver-class-name: org.postgresql.Driver

  b_aux:
    url: jdbc:postgresql://localhost:5433/b_aux
    username: user
    password: pass
    type: POSTGRES
    driver-class-name: org.postgresql.Driver
```


### 3. `bulk.batch-job`

Questa sezione definisce il job batch e gli step che lo compongono. Ogni step può includere i seguenti parametri:

- `name`: nome dello step
- `type`: opzionale, indica se lo step è di tipo `TASKLET` o `CHUNK`
- `chunk`: dimensione del chunk per step di tipo chunk-oriented
- `reader`, `processor`, `writer`: componenti principali dello step
- `listeners`: eventuali listener associati allo step
- `transitions` / `next`: definizione della sequenza di esecuzione degli step, incluso il flusso condizionale

#### Esempio di configurazione

```yaml
batch-job:
  name: nomeJob
  steps:
    - name: firstStep
      chunk: 10
      reader:
        name: readerCsv
        type: FlatFileItemReader
        config:
          resource: file:path/file.csv
          lineToSkip: 1
          fieldNames: [id, name, surname, email]
      processor:
        name: processor
        type: ItemProcessor
      writer:
        name: writerJdbc
        type: JdbcBatchItemWriter
        config:
          datasource: b_declarative
          sql: "INSERT INTO table (...) VALUES (...)"
      listeners:
        - type: StepExecutionListener
          name: firstStepListener
      next: secondStep
```

---

## Componenti per la Configurazione dei Batch

Oltre alla configurazione YAML, la libreria mette a disposizione alcune componenti utili per creare step, tasklet e listener in modo dichiarativo, tramite annotazioni e factory.

---

### 1. Creazione dinamica dei Job e degli Step

La libreria mette a disposizione i componenti principali per costruire dinamicamente job e step da YAML o annotazioni. Il cuore della logica risiede nella combinazione di:

- `createJob()`: costruisce il job principale.
- `createStepsFromSteplets()`: crea tutti gli step (tasklet o chunk-oriented) dallo YAML.
- `buildDynamicFlow()`: genera il flusso tra gli step, supportando transizioni condizionali e lineari.

---

#### 1.1 Creazione del Job

Il metodo `createJob()`:

1. Valida la configurazione del job (`jobConfig`) assicurandosi che contenga almeno uno step.
2. Crea dinamicamente gli step tramite `createStepsFromSteplets(jobConfig)`.
3. Costruisce il flusso dinamico con `buildDynamicFlow(jobConfig, stepsMap)` per gestire `next` e transizioni condizionali.
4. Configura listener e validator per il job se presenti.
5. Applica un `RunIdIncrementer` se necessario.
6. Costruisce infine il `Job` con tutti gli step e flussi associati.

```java
@Override
public Job createJob() {
    log.info("Creating job '{}'", jobConfig.getName());

    Map<String, Step> stepsMap = createStepsFromSteplets(jobConfig);
    Flow mainFlow = buildDynamicFlow(jobConfig, stepsMap);

    JobBuilder jobBuilder = new JobBuilder(jobConfig.getName(), jobRepository);
    attachJobListener(jobBuilder, jobConfig);
    attachJobValidator(jobBuilder, jobConfig);

    if (runIdIncrementer != null) {
        jobBuilder.incrementer(new DatabaseRunIdIncrementer(jobConfig, jobExplorer));
    }

    Job job = jobBuilder
            .start(mainFlow)
            .end()
            .build();

    log.info("Job '{}' created successfully with {} steps", jobConfig.getName(), stepsMap.size());
    return job;
}
```

#### 1.2 Creazione degli Step dai Steplet

Il metodo `createStepsFromSteplets(jobConfig)` gestisce la creazione di tutti gli step definiti nella configurazione YAML, supportando sia:

- **Tasklet steps**: step di tipo tasklet-oriented.
- **Chunk-oriented steps**: gestiti tramite `AbstractSteplet`.

##### Funzionalità principali:

1. **Validazione dei componenti**: controlla che reader, processor e writer siano correttamente configurati.
2. **Creazione dei componenti mancanti tramite factory**: se reader, processor o writer non sono forniti dallo Steplet, vengono creati automaticamente tramite le factory dedicate.
3. **Logging dettagliato**: registra la creazione di ogni step, indicando il tipo e la classe associata.
4. **Avviso per componenti annotati non utilizzati**: se uno steplet o un tasklet annotato con `@BulkBatchSteplet` o `@BulkBatchTasklet` non viene utilizzato nello YAML, viene generato un warning.

##### Esempio di implementazione

```java
private Map<String, Step> createStepsFromSteplets(BatchJobConfig jobConfig) {
    Map<String, Step> stepsMap = new HashMap<>();

    try {
        for (StepsConfig stepConfig : jobConfig.getSteps()) {
            Step step;

            if (stepConfig.getType() == StepsConfig.StepType.TASKLET) {
                // --- TASKLET handling ---
                Tasklet taskletBean = resolveTaskletBean(stepConfig);
                step = new StepBuilder(stepConfig.getName(), jobRepository)
                        .tasklet(taskletBean, transactionManager)
                        .build();

                log.info("Tasklet step '{}' created with tasklet bean '{}'",
                        stepConfig.getName(), taskletBean.getClass().getSimpleName());
            } else {
                // --- Chunk-oriented step via steplet ---
                AbstractSteplet<?, ?> steplet = resolveStepletBean(stepConfig);
                steplet.setConfig(stepConfig);
                step = steplet.buildStep();

                log.info("Chunk-oriented step '{}' created via steplet '{}'",
                        stepConfig.getName(), steplet.getClass().getSimpleName());
            }

            stepsMap.put(stepConfig.getName(), step);
        }
    } catch (Exception e) {
        log.error("Error while creating steps: {}", e.getMessage(), e);
        throw new BatchException("Error while building steps: " + e.getMessage(), e);
    }

    // --- Warn for unused annotated steplets ---
    Map<String, Object> annotatedSteplets = context.getBeansWithAnnotation(BulkBatchSteplet.class);
    for (Object bean : annotatedSteplets.values()) {
        BulkBatchSteplet ann = bean.getClass().getAnnotation(BulkBatchSteplet.class);
        if (!stepsMap.containsKey(ann.name())) {
            log.warn("Annotated steplet bean '{}' ({}) is not used in YAML configuration",
                    ann.name(), bean.getClass().getSimpleName());
        }
    }

    // --- Warn for unused annotated tasklets ---
    Map<String, Object> annotatedTasklets = context.getBeansWithAnnotation(BulkBatchTasklet.class);
    for (Object bean : annotatedTasklets.values()) {
        BulkBatchTasklet ann = bean.getClass().getAnnotation(BulkBatchTasklet.class);
        if (!stepsMap.containsKey(ann.name())) {
            log.warn("Annotated tasklet bean '{}' ({}) is not used in YAML configuration",
                    ann.name(), bean.getClass().getSimpleName());
        }
    }

    return stepsMap;
}
```

#### 1.3 Costruzione del flusso dinamico dei Job

Il metodo `buildDynamicFlow(jobConfig, stepsMap)` genera dinamicamente il flusso tra gli step del job, gestendo sia transizioni lineari (`next`) sia condizionali (`on-condition`). Questo permette di creare job complessi completamente configurabili tramite YAML, senza scrivere codice Java aggiuntivo.

##### Funzionalità principali:

1. **Partenza dal primo step**: il flusso inizia con il primo step definito nello YAML.
2. **Transizioni condizionali**: gestite tramite `on-condition`, permettono di definire percorsi diversi in base allo stato di uscita degli step.
3. **Transizioni lineari (`next`)**: collegano automaticamente step consecutivi in maniera sequenziale.
4. **Fallback globale**: tutti gli exit status non gestiti vengono instradati alla fine del flusso per garantire la terminazione corretta.
5. **Logging dettagliato**: ogni transizione aggiunta viene loggata per facilitare debug e monitoraggio.
6. **Validazione dei riferimenti**: se una transizione fa riferimento a step non esistenti, viene sollevata un’eccezione chiara.

##### Esempio di implementazione

```java
private Flow buildDynamicFlow(BatchJobConfig jobConfig, Map<String, Step> stepsMap) {
    FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("flow-" + jobConfig.getName());

    // Start from first step
    Step firstStep = stepsMap.get(jobConfig.getSteps().get(0).getName());
    flowBuilder.start(firstStep);

    for (StepsConfig stepConfig : jobConfig.getSteps()) {
        Step currentStep = stepsMap.get(stepConfig.getName());

        // --- Conditional transitions ---
        if (stepConfig.getTransitions() != null && !stepConfig.getTransitions().isEmpty()) {
            for (StepConditionConfig transition : stepConfig.getTransitions()) {
                Step toStep = stepsMap.get(transition.getToStep());
                Step fromStep = stepsMap.get(transition.getFrom());
                if (toStep == null || fromStep == null) {
                    log.error("Transition references unknown step: {} -> {}", transition.getFrom(), transition.getToStep());
                    throw new IllegalArgumentException("Transition references unknown step: " +
                            transition.getFrom() + " -> " + transition.getToStep());
                }

                log.info("Adding conditional transition: {} --[{}]--> {}",
                        fromStep.getName(), transition.getOnCondition(), toStep.getName());

                flowBuilder.from(fromStep)
                        .on(transition.getOnCondition())
                        .to(toStep);

                if (transition.isEnded()) {
                    flowBuilder.end();
                }
            }
        }

        // --- Linear 'next' transitions ---
        if (stepConfig.getNext() != null && !stepConfig.getNext().isEmpty()) {
            Step nextStep = stepsMap.get(stepConfig.getNext());
            if (nextStep == null) {
                log.error("Next step '{}' not found for step '{}'", stepConfig.getNext(), currentStep.getName());
                throw new IllegalArgumentException("Next step not found: " + stepConfig.getNext());
            }

            log.info("Adding linear transition: {} --*--> {}", currentStep.getName(), nextStep.getName());

            flowBuilder.from(currentStep)
                    .on("*")
                    .to(nextStep);
        }
    }

    // --- Global fallback for all unhandled exit statuses ---
    for (StepsConfig stepConfig : jobConfig.getSteps()) {
        Step currentStep = stepsMap.get(stepConfig.getName());
        flowBuilder.from(currentStep)
                .on("*")
                .end();
    }

    return flowBuilder.end();
}
```

---

### 2. Creazione degli Step tramite `StepFactory`

La **`StepFactory`** è il componente centrale che costruisce uno step Spring Batch a partire dalla configurazione dichiarativa (`StepsConfig`) e dai componenti forniti (reader, processor, writer). Questo approccio consente di centralizzare la logica di costruzione e di applicare comportamenti comuni come logging e fault tolerance.

#### Funzionalità principali:

1. **Validazione dei componenti**: verifica che reader, processor e writer siano correttamente configurati prima di costruire lo step.
2. **Creazione o riuso dei componenti tipizzati**: se i componenti non vengono passati dallo Steplet, la factory li crea tramite readerFactory, processorFactory e writerFactory.
3. **Costruzione dello step chunk-oriented**: utilizza `StepBuilder` e `SimpleStepBuilder` per definire chunk, transaction manager, reader, processor e writer.
4. **Attacco dei listener comuni**: applica listener di logging standard e listener definiti nel YAML.
5. **Configurazione della tolleranza agli errori**: supporta retry, skip e transazioni se definiti nella configurazione dello step.

#### Esempio di metodo `createStep`

```java
public <I, O> Step createStep(StepsConfig config,
                              ItemReader<I> reader,
                              ItemProcessor<I, O> processor,
                              ItemWriter<O> writer) throws Exception {

    log.info("Creating step '{}'", config.getName());

    // --- Validate components passed from Steplet ---
    validateReader(reader, config);
    validateProcessor(processor, config);
    validateWriter(writer, config);

    // --- Build or reuse typed components ---
    ItemReader<I> finalReader = reader != null
            ? reader
            : readerFactory.createReader(config.getReader(), config.getChunk());

    ItemProcessor<I, O> finalProcessor = processor != null
            ? processor
            : processorFactory.createProcessor(config.getProcessor());

    ItemWriter<O> finalWriter = writer != null
            ? writer
            : writerFactory.createWriter(config.getWriter());

    // --- Build chunk step ---
    StepBuilder stepBuilder = new StepBuilder(config.getName(), jobRepository);
    SimpleStepBuilder<I, O> chunkStep = stepBuilder
            .<I, O>chunk(config.getChunk(), transactionManager)
            .reader(finalReader)
            .processor(finalProcessor)
            .writer(finalWriter);

    // --- Attach common logging listener ---
    chunkStep.listener((StepExecutionListener) loggingStepListener);
    chunkStep.listener((ChunkListener) loggingStepListener);

    // --- Attach additional listeners from YAML config ---
    attachStepListeners(chunkStep, config);

    // --- Configure fault tolerance if defined ---
    if (config.getRetry() != null || config.getSkip() != null || config.getTransaction() != null) {
        chunkStep = configureFaultTolerance(chunkStep, config);
    }

    Step step = chunkStep.build();
    log.info("Step '{}' created successfully", config.getName());
    return step;
}
```

---

### 3. Creazione degli Step tramite `AbstractSteplet`

Gli **Steplet** sono componenti che estendono la classe `AbstractSteplet` per costruire automaticamente uno step Spring Batch utilizzando la `StepFactory`. Questo approccio permette di mappare direttamente la configurazione YAML sui componenti del batch senza scrivere codice aggiuntivo per la creazione dello step.

#### Funzionalità principali di `AbstractSteplet`:

- **Costruzione automatica dello step**: utilizza la `StepFactory` per creare lo step completo con reader, processor e writer.
- **Gestione della configurazione**: legge la configurazione dello step dal file YAML tramite `StepsConfig`.
- **Logging integrato**: registra la costruzione dello step e eventuali errori, facilitando il debug.
- **Validazione**: verifica che la configurazione dello step sia presente, altrimenti genera un’eccezione.

#### Esempio di implementazione

```java
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSteplet<I, O> implements StepComponent<I, O> {

    private final StepFactory stepFactory;

    @Setter
    private StepsConfig config;

    /**
     * Build the Spring Batch Step using StepFactory.
     */
    public Step buildStep() throws Exception {
        StepsConfig cfg = getConfig();
        log.info("Building step '{}' via AbstractSteplet '{}'", cfg.getName(), this.getClass().getSimpleName());

        Step step = stepFactory.createStep(
                cfg,
                reader(),
                processor(),
                writer()
        );

        log.info("Step '{}' built successfully via AbstractSteplet '{}'", cfg.getName(), this.getClass().getSimpleName());
        return step;
    }

    /**
     * Get StepsConfig, ensuring it is set.
     */
    protected StepsConfig getConfig() {
        if (config == null) {
            log.error("StepsConfig not set in AbstractSteplet '{}'", this.getClass().getSimpleName());
            throw new IllegalStateException("StepsConfig not set");
        }
        log.debug("Retrieved StepsConfig for step '{}'", config.getName());
        return config;
    }
}
```

---

### 4. Annotazioni

Le annotazioni semplificano l’integrazione dei componenti nello Spring Context e nel batch:

#### `@BulkBatchSteplet`

Annotazione utilizzata per marcare un **Steplet**, cioè uno step chunk-oriented.

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BulkBatchSteplet {
    String name();
}
```
#### `@BulkBatchTasklet`

Annotazione utilizzata per marcare un Tasklet, cioè uno step di tipo tasklet-oriented.

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BulkBatchTasklet {
    String name();
}
```

#### `@BulkBatchReader`

Annotazione utilizzata per marcare un Reader.

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BulkBatchReader {
  String name();
}
```

#### `@BulkBatchProcessor`

Annotazione utilizzata per marcare un Processor.

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BulkBatchProcessor {
  String name();
}
```

#### `@BulkBatchWriter`

Annotazione utilizzata per marcare un Writer.

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BulkBatchWriter {
  String name();
}
```

#### `@BulkBatchListener`

Annotazione utilizzata per marcare un Listener.

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BulkBatchListener {
  String name();
}
```

#### `@BulkBatchValidator`

Annotazione utilizzata per marcare l'input param Validator.

```java
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BulkBatchValidator {
}
```

---

### License
© 2025 – Tutti i diritti riservati.

Il presente software e il relativo codice sorgente sono di esclusiva proprietà dell'autore.

All rights reserved.

Nessuna parte di questo software può essere riprodotta, distribuita, modificata, pubblicata o utilizzata, in alcuna forma e con alcun mezzo, senza la preventiva autorizzazione scritta dell'autore.
