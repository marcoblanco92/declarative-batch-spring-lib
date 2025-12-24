package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.marbl.declarative_batch.spring_declarative_batch.utils.ListenerUtils.validateListenerMatch;

@Data
@Slf4j
public class StepsConfig {

    public enum StepType {
        STEP,
        TASKLET
    }

    @NotBlank(message = "The Step name must be provided and cannot be blank")
    private String name;

    @NotNull(message = "The Step type must be specified (STEP or TASKLET)")
    private StepType type = StepType.STEP;

    private Integer chunk = 10;

    // --- Components ---
    @Valid
    private ComponentConfig reader;

    @Valid
    private ComponentConfig processor;

    @Valid
    private ComponentConfig writer;

    @Valid
    private String tasklet;

    @Valid
    private List<ListenerConfig> listeners;

    private RetryConfig retry;
    private SkipConfig skip;
    private TransactionConfig transaction;

    private String next;

    @Valid
    private List<StepConditionConfig> transitions;

    // --- Validation for next/transitions ---
    @AssertTrue(message = "Both 'next' and 'transitions' cannot be set simultaneously")
    public boolean isValidTransitions() {
        if (next != null && transitions != null && !transitions.isEmpty()) {
            log.warn("Step [{}] - Both 'next' [{}] and 'transitions' are set. Only one is allowed.", name, next);
            return false;
        }
        return true;
    }

    // --- Validation for STEP vs TASKLET ---
    @AssertTrue(message = "Invalid configuration: STEP requires reader, processor, and writer; TASKLET requires tasklet bean name only.")
    public boolean isValidStepTypeConfiguration() {
        if (type == StepType.STEP) {
            boolean valid = reader != null && processor != null && writer != null && tasklet == null;
            if (!valid) {
                log.error("Step [{}] - Type STEP requires reader, processor, and writer, and must not define a tasklet.", name);
            }
            return valid;
        }

        if (type == StepType.TASKLET) {
            boolean valid = tasklet != null && reader == null && processor == null && writer == null;
            if (!valid) {
                log.error("Step [{}] - Type TASKLET requires a tasklet bean name and must not define reader, processor, or writer.", name);
            }
            return valid;
        }

        return true;
    }

    // --- Validation for chunk usage ---
//    @AssertTrue(message = "Chunk property is only valid for STEP type and must be null for TASKLET.")
//    public boolean isValidChunkUsage() {
//        if (type == StepType.TASKLET && chunk != null) {
//            log.error("Step [{}] - 'chunk' is not applicable for TASKLET type.", name);
//            return false;
//        }
//        return true;
//    }

    // --- Listener validations (only for STEP type) ---
    @AssertTrue(message = "Processor listener name must match processor bean name when processor implements ItemProcessListener")
    public boolean isProcessorListenerValid() {
        return type != StepType.STEP || validateListenerMatch(name, processor, listeners, "ItemProcessListener", "Processor");
    }

    @AssertTrue(message = "Reader listener name must match reader bean name when reader implements ItemReadListener")
    public boolean isReaderListenerValid() {
        return type != StepType.STEP || validateListenerMatch(name, reader, listeners, "ItemReadListener", "Reader");
    }

    @AssertTrue(message = "Writer listener name must match writer bean name when writer implements ItemWriteListener")
    public boolean isWriterListenerValid() {
        return type != StepType.STEP || validateListenerMatch(name, writer, listeners, "ItemWriteListener", "Writer");
    }
}
