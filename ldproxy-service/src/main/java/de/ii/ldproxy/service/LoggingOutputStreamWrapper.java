/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author zahnen
 */
public class LoggingOutputStreamWrapper extends OutputStream {

    private final OutputStream outputStream;
    private final Logger logger;

    public LoggingOutputStreamWrapper(OutputStream outputStream, Logger logger) {
        super();
        this.outputStream = outputStream;
        this.logger = logger;
    }

    @Override
    public void write(int i) throws IOException {
        outputStream.write(i);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug(new String(bytes, StandardCharsets.UTF_8));
        }
        outputStream.write(bytes);
    }

    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug(new String(bytes, i, i1, StandardCharsets.UTF_8));
        }
        outputStream.write(bytes, i, i1);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
