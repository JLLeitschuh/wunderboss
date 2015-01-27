/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss.web.async;

import io.undertow.util.Headers;
import org.projectodd.wunderboss.ThreadPool;
import org.projectodd.wunderboss.WunderBoss;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executor;

public class ServletHttpChannel extends OutputStreamHttpChannel {

    public ServletHttpChannel(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final OnOpen onOpen,
                              final OnError onError,
                              final OnClose onClose){
        super(onOpen, onError, onClose);
        this.response = response;
        this.asyncContext = request.startAsync();
    }

    @Override
    protected String getResponseCharset() {
        return this.response.getCharacterEncoding();
    }

    @Override
    protected void setContentLength(int length) {
        this.response.setIntHeader(Headers.CONTENT_LENGTH_STRING,
                                   length);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return this.response.getOutputStream();
    }

    @Override
    protected Executor getExecutor() {
        if (this.executor == null){
            this.executor = WunderBoss.findOrCreateComponent(ThreadPool.class,
                                                             "http-stream-worker",
                                                             null);
        }

        return this.executor;
    }


    protected void enqueue(PendingSend pending) {
        //TODO: be async in 9.x, sync in 8.x (due to https://issues.jboss.org/browse/WFLY-3715)
        //super.enqueue(pending); // async
        send(pending); // sync
    }

    @Override
    public void close() throws IOException {
        this.asyncContext.complete();
        super.close();
    }

    private final HttpServletResponse response;
    private final AsyncContext asyncContext;
    private Executor executor;
}
