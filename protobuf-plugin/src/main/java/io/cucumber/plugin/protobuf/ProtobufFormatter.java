package io.cucumber.plugin.protobuf;

import io.cucumber.messages.Messages.Envelope;
import io.cucumber.messages.internal.com.google.protobuf.util.JsonFormat;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

// TODO: Move back to core
public final class ProtobufFormatter implements EventListener {
    private final OutputStream outputStream;
    private final Writer writer;
    private final JsonFormat.Printer jsonPrinter = JsonFormat.printer().omittingInsignificantWhitespace().includingDefaultValueFields();
    //    private final Map<TestCase, String> testCaseStartedIdByTestCase = new HashMap<>();
    private final ProtobufFormat format;

    public ProtobufFormatter(File file) throws FileNotFoundException {
        this.format = file.getPath().endsWith(".ndjson") ? ProtobufFormat.NDJSON : ProtobufFormat.PROTOBUF;
        this.outputStream = new FileOutputStream(file);
        this.writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(Envelope.class, this::writeMessage);
    }

    private void writeMessage(Envelope envelope) {
        write(envelope);
    }

    private void write(Envelope m) {
        try {
            switch (format) {
                case PROTOBUF:
                    m.writeDelimitedTo(outputStream);
                    break;
                case NDJSON:
                    String json = jsonPrinter.print(m);
                    writer.write(json);
                    writer.write("\n");
                    writer.flush();
                    break;
                default:
                    throw new IllegalStateException("Unsupported format: " + format.name());
            }
            if (m.hasTestRunFinished()) {
                outputStream.close();
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
