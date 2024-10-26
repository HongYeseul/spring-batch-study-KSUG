package com.example.batch_sample.jobs.task04.writer;

import org.springframework.batch.item.file.FlatFileHeaderCallback;

import java.io.IOException;
import java.io.Writer;

import static com.example.batch_sample.jobs.task04.FlatFileItemJobConfig.DELIMITER_FOR_WRITER;

public class CustomerHeader implements FlatFileHeaderCallback {
    @Override
    public void writeHeader(Writer writer) throws IOException {
        writer.write("ID" + DELIMITER_FOR_WRITER + "AGE");
    }
}
