/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package ro.cs.tao.wps.impl;

import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.schema.*;
import com.bc.wps.api.utils.WpsTypeConverter;
import ro.cs.tao.services.model.workflow.WorkflowInfo;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

class ExecuteResponseBuilder {

    private final ExecuteResponse executeResponse;
    private final WpsServerContext serverContext;

    ExecuteResponseBuilder(WpsServerContext serverContext) {
        this.serverContext = serverContext;
        executeResponse = new ExecuteResponse();
        executeResponse.setService("WPS");
        executeResponse.setVersion("1.0.0");
        executeResponse.setLang("en");
        withServiceInstance(serverContext.getRequestUrl());
    }

    ExecuteResponse build() {
        return executeResponse;
    }

    ExecuteResponseBuilder addProcessOutput(final String relativePath) {
        final String host = serverContext.getHostAddress();
        final int port = serverContext.getPort();
        final String hostPort = port != 80 ? host + ":" + port : host;
        final String url = "http://" + hostPort + "/files/download?fileName=" + relativePath;

        final OutputDataType outputDataType = new OutputDataType();
        outputDataType.setIdentifier(WpsTypeConverter.str2CodeType(relativePath));
        outputDataType.setTitle(WpsTypeConverter.str2LanguageStringType(relativePath));
        outputDataType.setReference(WpsTypeConverter.str2OutputReferenceType(url));
        ensureProcessOutputs().add(outputDataType);
        return this;
    }

    ExecuteResponseBuilder withProcessFailed(String failedMessage) {
        final ExceptionType e = new ExceptionType();
        e.setExceptionCode("NoApplicableCode");
        e.getExceptionText().add(failedMessage);

        final ExceptionReport exceptionReport = new ExceptionReport();
        exceptionReport.getException().add(e);

        final ProcessFailedType failedType = new ProcessFailedType();
        failedType.setExceptionReport(exceptionReport);

        ensureStatus().setProcessFailed(failedType);
        return this;
    }

    private ExecuteResponseBuilder withServiceInstance(String serviceUrl) {
        executeResponse.setServiceInstance(serviceUrl);
        return this;
    }

    ExecuteResponseBuilder withStatusLocation(String jobId) {
        executeResponse.setStatusLocation(getStatusUrl(jobId));
        return this;
    }

    ExecuteResponseBuilder withProcessBriefType(WorkflowInfo workflowInfo) {
        final ProcessBriefType process = new ProcessBriefType();
        process.setIdentifier(WpsTypeConverter.str2CodeType("" + workflowInfo.getId()));
        process.setTitle(WpsTypeConverter.str2LanguageStringType(workflowInfo.getName()));
        executeResponse.setProcess(process);
        return this;
    }

    ExecuteResponseBuilder withStatusCreationTime(XMLGregorianCalendar xmlNow) {
        ensureStatus().setCreationTime(xmlNow);
        return this;
    }

    ExecuteResponseBuilder withProcessAccepted(String acceptedMessage) {
        ensureStatus().setProcessAccepted(acceptedMessage);
        return this;
    }

    ExecuteResponseBuilder withProcessStarted(String startedMessage) {
        final ProcessStartedType processStarted = new ProcessStartedType();
        processStarted.setValue(startedMessage);
        ensureStatus().setProcessStarted(processStarted);
        return this;
    }

    ExecuteResponseBuilder withProcessPaused(String pausedMessage) {
        final ProcessStartedType processPaused = new ProcessStartedType();
        processPaused.setValue(pausedMessage);
        ensureStatus().setProcessPaused(processPaused);
        return this;
    }

    ExecuteResponseBuilder withProcessSucceeded(String succeededMessage) {
        ensureStatus().setProcessSucceeded(succeededMessage);
        return this;
    }

    private StatusType ensureStatus() {
        if (executeResponse.getStatus() == null) {
            executeResponse.setStatus(new StatusType());
        }
        return executeResponse.getStatus();
    }

    private List<OutputDataType> ensureProcessOutputs() {
        if (executeResponse.getProcessOutputs() == null) {
            executeResponse.setProcessOutputs(new ExecuteResponse.ProcessOutputs());
        }
        return executeResponse.getProcessOutputs().getOutput();
    }

    private String getStatusUrl(String jobId) {
        return serverContext.getRequestUrl() + "?Service=WPS&Request=GetStatus&JobId=" + jobId;
    }
}
