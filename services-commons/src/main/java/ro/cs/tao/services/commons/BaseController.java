/*
 * Copyright (C) 2017 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.commons;

import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author Cosmin Cara
 */
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:63343"})
public class BaseController {

    private ExecutorService executorService;

    protected void asyncExecute(Runnable runnable, Consumer<Exception> callback) {
        if (this.executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.submit(() -> {
            Exception exception = null;
            try {
                runnable.run();
            } catch (Exception ex) {
                exception = ex;
            } finally {
                if (callback != null) {
                    callback.accept(exception);
                }
            }
        });
    }
}
