/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.plugin.sources.snapshot;

import org.apache.inlong.agent.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Base64;

/**
 * PostgreSQL Snapshot
 */
public class PostgreSQLSnapshotBase implements SnapshotBase {

    public static final int BUFFER_SIZE = 1024;
    public static final int START_OFFSET = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLSnapshotBase.class);
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private File file;
    private byte[] offset;

    public PostgreSQLSnapshotBase(String filePath) {
        file = new File(filePath);
    }

    @Override
    public String getSnapshot() {
        load();
        return encoder.encodeToString(offset);
    }

    @Override
    public void close() {

    }

    /**
     * Load postgres offset from local file
     */
    private void load() {
        try {
            if (!file.exists()) {
                // if parentDir not exist, create first
                File parentDir = file.getParentFile();
                if (parentDir == null) {
                    LOGGER.info("no parent dir, file:{}", file.getAbsolutePath());
                    return;
                }
                if (!parentDir.exists()) {
                    boolean success = parentDir.mkdir();
                    LOGGER.info("create dir {} result {}", parentDir, success);
                }
                file.createNewFile();
            }
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream inputStream = new BufferedInputStream(fis);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int len;
            byte[] buf = new byte[BUFFER_SIZE];
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, START_OFFSET, len);
            }
            offset = outputStream.toByteArray();
            inputStream.close();
            outputStream.close();
        } catch (Throwable ex) {
            LOGGER.error("load PostgreSQL WAL log error", ex);
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
        }
    }

    /**
     * Save PostgreSQL offset to local file
     */
    public void save(String snapshot) {
        byte[] bytes = decoder.decode(snapshot);
        if (bytes.length != 0) {
            offset = bytes;
            try (OutputStream output = Files.newOutputStream(file.toPath())) {
                output.write(bytes);
            } catch (Throwable e) {
                LOGGER.error("save offset to file error", e);
                ThreadUtils.threadThrowableHandler(Thread.currentThread(), e);
            }
        }
    }
}
