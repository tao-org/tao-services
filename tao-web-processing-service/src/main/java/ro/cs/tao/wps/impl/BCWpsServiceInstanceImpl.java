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

package ro.cs.tao.wps.impl;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.exceptions.InvalidParameterValueException;
import com.bc.wps.api.exceptions.NoApplicableCodeException;
import com.bc.wps.api.exceptions.OptionNotSupportedException;
import com.bc.wps.api.exceptions.WpsRuntimeException;
import com.bc.wps.api.exceptions.WpsServiceException;
import com.bc.wps.api.exceptions.XmlSchemaFaultException;
import com.bc.wps.api.schema.Capabilities;
import com.bc.wps.api.schema.CodeType;
import com.bc.wps.api.schema.ComplexDataCombinationType;
import com.bc.wps.api.schema.ComplexDataCombinationsType;
import com.bc.wps.api.schema.ComplexDataDescriptionType;
import com.bc.wps.api.schema.DataInputsType;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.InputType;
import com.bc.wps.api.schema.LanguageStringType;
import com.bc.wps.api.schema.OutputDescriptionType;
import com.bc.wps.api.schema.ProcessDescriptionType;
import com.bc.wps.api.schema.ResponseDocumentType;
import com.bc.wps.api.schema.ResponseFormType;
import com.bc.wps.api.schema.SupportedComplexDataType;
import com.bc.wps.api.schema.ValueType;
import com.bc.wps.api.utils.InputDescriptionTypeBuilder;
import org.apache.commons.lang.StringUtils;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.wps.operations.GetCapabilitiesOperation;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BCWpsServiceInstanceImpl implements WpsServiceInstance {

    private WebProcessingService taoWpsImpl = new WebProcessingServiceImpl();

    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    public Capabilities getCapabilities(WpsRequestContext context) throws WpsServiceException {
        try {
            final GetCapabilitiesOperation operation = new GetCapabilitiesOperation(context);
            return operation.getCapabilities();
        } catch (IOException | URISyntaxException | PersistenceException exception) {
            logger.log(Level.SEVERE, "Unable to perform GetCapabilities operation successfully", exception);
            throw new WpsServiceException(exception);
        }
    }

    @Override
    public List<ProcessDescriptionType> describeProcess(WpsRequestContext wpsRequestContext, String processIdentifier) throws WpsServiceException {
        final ProcessDescriptionType processDescription = new ProcessDescriptionType();
        processDescription.setProcessVersion("na");
        processDescription.setStatusSupported(true);
        processDescription.setStoreSupported(true);

        final CodeType identifier = new CodeType();
        identifier.setValue(processIdentifier);
        processDescription.setIdentifier(identifier);

        final WebProcessingService.ProcessInfo processInfo = taoWpsImpl.describeProcess(Long.parseLong(processIdentifier));

        final LanguageStringType title = new LanguageStringType();
        title.setValue(processInfo.getWorkflowInfo().getName());
        processDescription.setTitle(title);


        final ProcessDescriptionType.DataInputs dataInputs = getDataInputs(processInfo.getParameters());
        processDescription.setDataInputs(dataInputs);

        final ProcessDescriptionType.ProcessOutputs processOutputs = getProcessOutputs(processInfo.getOutputs());
        processDescription.setProcessOutputs(processOutputs);

        return Collections.singletonList(processDescription);
    }

    @Override
    public ExecuteResponse doExecute(WpsRequestContext context, Execute executeRequest) throws WpsServiceException {
        final String suffix = " because TAO WPS service allows only asynchronous execution.";

        final ResponseFormType responseForm = executeRequest.getResponseForm();
        if (responseForm == null) {
            throw new OptionNotSupportedException("Execute needs a ResponseForm element" + suffix, "ResponseForm missed in element Execute");
        }
        final ResponseDocumentType responseDocument = responseForm.getResponseDocument();
        if (responseDocument == null) {
            throw new OptionNotSupportedException("ResponseForm element needs a ResponseDocument element" + suffix, "ResponseDocument missed in element ResponseForm");
        }
        if (!responseDocument.isStoreExecuteResponse()) {
            throw new OptionNotSupportedException("ResponseDocument needs attribute storeExecuteResponse=\"true\"" + suffix, "storeExecuteResponse=\"false\" or attribute is missed");
        }
        if (!responseDocument.isStatus()) {
            throw new OptionNotSupportedException("ResponseDocument needs attribute status=\"true\"" + suffix, "status=\"false\" or attribute is missed");
        }
        final CodeType identifier = executeRequest.getIdentifier();
        if (identifier == null) {
            throw new XmlSchemaFaultException("Identifier", "Execute");
        }
        final String identifierValue = identifier.getValue();
        if (StringUtils.isBlank(identifierValue)) {
            throw new InvalidParameterValueException("Invalid value", null, "Identifier");
        }
        final long workflowId = Long.parseLong(identifierValue);

        final Map<String, Map<String, String>> parameters = new HashMap<>();
        final DataInputsType dataInputs = executeRequest.getDataInputs();
        if (dataInputs != null) {
            final List<InputType> inputParams = dataInputs.getInput();

            for (InputType input : inputParams) {
                final String iIdentifier = input.getIdentifier().getValue();
                final String[] strings = iIdentifier.split("~");
                final String parameterGroupName = strings[0];
                final Map<String, String> parameterGroup;
                if (parameters.containsKey(parameterGroupName)) {
                    parameterGroup = parameters.get(parameterGroupName);
                } else {
                    parameterGroup = parameters.put(parameterGroupName, new HashMap<>());
                }
                final String parameterName = strings[1];
                final String value = input.getData().getLiteralData().getValue();
                parameterGroup.put(parameterName, value);
            }
        }

        final long executionJobId;
        try {
            executionJobId = taoWpsImpl.execute(workflowId, parameters);
        } catch (ExecutionException e) {
            throw new NoApplicableCodeException("Unable to start workflow execution: " + e.getMessage(), e);
        }

        return getStatus(context, Long.toString(executionJobId));
    }

    @Override
    public ExecuteResponse getStatus(WpsRequestContext context, String jobId) throws WpsServiceException {
        final long jobIdL = Long.parseLong(jobId);
        final ExecutionJob jobById = taoWpsImpl.getStatus(jobIdL);
        if (jobById == null) {
            return null;
        }

        final WpsServerContext serverContext = context.getServerContext();

        final long workflowId = jobById.getWorkflowId();
        final WebProcessingService.ProcessInfo processInfo = taoWpsImpl.describeProcess(workflowId);

        ExecuteResponseBuilder responseBuilder = new ExecuteResponseBuilder(serverContext)
                .withStatusLocation(jobId)
                .withProcessBriefType(processInfo.getWorkflowInfo())
                .withStatusCreationTime(getXmlNow());

        final ExecutionStatus status = jobById.getExecutionStatus();
        if (ExecutionStatus.UNDETERMINED.equals(status)
            || ExecutionStatus.QUEUED_ACTIVE.equals(status)) {
            responseBuilder.withProcessAccepted("TAO status " + status.friendlyName());
        } else if (ExecutionStatus.RUNNING.equals(status)) {
            responseBuilder.withProcessStarted("TAO status " + status.friendlyName());
        } else if (ExecutionStatus.SUSPENDED.equals(status)) {
            responseBuilder.withProcessPaused("TAO status " + status.friendlyName());
        } else if (ExecutionStatus.DONE.equals(status)) {
            responseBuilder.withProcessSucceeded("TAO status " + status.friendlyName());
            responseBuilder.withStatusCreationTime(getXmlWithTime(jobById.getEndTime()));

            try {
                final List<FileObject> jobResult = taoWpsImpl.getJobResult(jobIdL);
                for (FileObject fileObject : jobResult) {
                    responseBuilder.addProcessOutput(fileObject.getRelativePath());
                }
            } catch (IOException e) {
                throw new NoApplicableCodeException("Unable to collect processing outputs: " + e.getMessage(), e);
            }
        } else if (ExecutionStatus.FAILED.equals(status)
                   || ExecutionStatus.CANCELLED.equals(status)) {
            responseBuilder.withProcessFailed("TAO status " + status.friendlyName());
        }

        return responseBuilder.build();
    }

    @Override
    public void dispose() {
        throw new RuntimeException("not implemented");
    }

    protected ProcessDescriptionType.ProcessOutputs getProcessOutputs(List<TargetDescriptor> workflowOutputs) {
        final ProcessDescriptionType.ProcessOutputs processOutputs = new ProcessDescriptionType.ProcessOutputs();
        for (TargetDescriptor workflowOutput : workflowOutputs) {

            final ComplexDataDescriptionType format = new ComplexDataDescriptionType();
            format.setMimeType("application/octet-stream");

            final ComplexDataCombinationType aDefault = new ComplexDataCombinationType();
            aDefault.setFormat(format);

            final ComplexDataCombinationsType supported = new ComplexDataCombinationsType();
            supported.getFormat().add(format);

            final SupportedComplexDataType complexOutput = new SupportedComplexDataType();
            complexOutput.setDefault(aDefault);
            complexOutput.setSupported(supported);

            final CodeType outputIdentifier = new CodeType();
            outputIdentifier.setValue(workflowOutput.getId());

            final LanguageStringType outputTitle = new LanguageStringType();
            outputTitle.setValue(workflowOutput.getDataDescriptor().getLocation());


            final OutputDescriptionType outputDescription = new OutputDescriptionType();

            outputDescription.setIdentifier(outputIdentifier);
            outputDescription.setTitle(outputTitle);
            outputDescription.setComplexOutput(complexOutput);

            processOutputs.getOutput().add(outputDescription);
        }
        return processOutputs;
    }

    protected ProcessDescriptionType.DataInputs getDataInputs(final Map<String, List<Parameter>> parameters) {

        final ProcessDescriptionType.DataInputs dataInputs = new ProcessDescriptionType.DataInputs();

        for (Map.Entry<String, List<Parameter>> mapEntry : parameters.entrySet()) {
            final String groupName = mapEntry.getKey();
            final List<Parameter> groupParameterList = mapEntry.getValue();
            for (Parameter groupParameter : groupParameterList) {
                final String parameterName = groupParameter.getName();
                final String parameterType = groupParameter.getType();
                final String[] valueSet = groupParameter.getValueSet();
                InputDescriptionTypeBuilder builder = InputDescriptionTypeBuilder.create()
                        .withIdentifier(groupName + "~" + parameterName)
                        .withTitle("Param '" + parameterName + "' of group '" + groupName + "'.")
                        .withAbstract("The parameter '" + parameterName + "' of parametergroup '" + groupName + "'.")
                        .withDataType(parameterType);

                if (valueSet != null) {
                    final ArrayList<Object> strings = new ArrayList<>();
                    for (Object s : valueSet) {
                        final ValueType valueType = new ValueType();
                        valueType.setValue((String) s);
                        strings.add(valueType);
                    }
                    builder = builder.withAllowedValues(strings);
                }
                dataInputs.getInput().add(builder.build());
            }
        }
        return dataInputs;
    }

    // package local for test purposes only
    void setTaoWpsImpl(WebProcessingService taoWpsImpl) {
        this.taoWpsImpl = taoWpsImpl;
    }

    // package local for test purposes only
    XMLGregorianCalendar getXmlWithTime(LocalDateTime time) {
        final ZonedDateTime zdt = time.atZone(ZoneOffset.UTC);
        final GregorianCalendar gregorianCalendar = GregorianCalendar.from(zdt);
        return getXmlGregorianCalendar(gregorianCalendar);
    }

    private XMLGregorianCalendar getXmlNow() {
        return getXmlGregorianCalendar(new GregorianCalendar());
    }

    private XMLGregorianCalendar getXmlGregorianCalendar(GregorianCalendar gregorianCalendar) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException exception) {
            throw new WpsRuntimeException("Unable to create new Gregorian Calendar.", exception);
        }
    }
}
