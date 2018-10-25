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
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.exceptions.WpsServiceException;
import com.bc.wps.api.schema.Capabilities;
import com.bc.wps.api.schema.CodeType;
import com.bc.wps.api.schema.ComplexDataCombinationType;
import com.bc.wps.api.schema.ComplexDataCombinationsType;
import com.bc.wps.api.schema.ComplexDataDescriptionType;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.LanguageStringType;
import com.bc.wps.api.schema.OutputDescriptionType;
import com.bc.wps.api.schema.ProcessDescriptionType;
import com.bc.wps.api.schema.SupportedComplexDataType;
import com.bc.wps.api.schema.ValueType;
import com.bc.wps.api.utils.InputDescriptionTypeBuilder;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.entity.impl.WorkflowServiceImpl;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.orchestration.impl.OrchestrationServiceImpl;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.wps.operations.GetCapabilitiesOperation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BCWpsServiceInstanceImpl implements WpsServiceInstance {

    private WorkflowService workflowService = new WorkflowServiceImpl();

    private OrchestratorService orchestratorService = new OrchestrationServiceImpl();

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

        final CodeType identifier = new CodeType();
        identifier.setValue(processIdentifier);
        processDescription.setIdentifier(identifier);

        final LanguageStringType title = new LanguageStringType();
        title.setValue("TAO Workflow ... " + processIdentifier);
        processDescription.setTitle(title);

        final ProcessDescriptionType.DataInputs dataInputs = getDataInputs(processIdentifier);
        processDescription.setDataInputs(dataInputs);

        final ProcessDescriptionType.ProcessOutputs processOutputs = getProcessOutputs(processIdentifier);
        processDescription.setProcessOutputs(processOutputs);

        return Collections.singletonList(processDescription);

//        final ProcessDescriptionType.ProcessOutputs outputs = new ProcessDescriptionType.ProcessOutputs();
//        description.setProcessOutputs(outputs);
//
//        } catch (PersistenceException e) {
//            throw new WpsServiceException("Unable to describe process for process identifier '"+processIdentifier+"'", e);
//        }
    }

    protected ProcessDescriptionType.ProcessOutputs getProcessOutputs(String processIdentifier) {
        final List<TargetDescriptor> workflowOutputs = orchestratorService.getWorkflowOutputs(Long.parseLong(processIdentifier));
//        final List<TargetDescriptor> workflowOutputs = workflowService.getWorkflowOutputs(Long.parseLong(processIdentifier));
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

    protected ProcessDescriptionType.DataInputs getDataInputs(String processIdentifier) {
        final Map<String, List<Parameter>> parameters = orchestratorService.getWorkflowParameters(Long.parseLong(processIdentifier));

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
                    for (Object s: valueSet) {
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

/*
    public List<ProcessDescriptionType> describeProcess(WpsRequestContext wpsRequestContext, String processIdentifier) throws WpsServiceException {
        final Map<String, List<Parameter>> parameters = orchestratorService.getWorkflowParameters(Long.parseLong(processIdentifier));

        final ProcessDescriptionType.DataInputs dataInputs = new ProcessDescriptionType.DataInputs();

        for (Map.Entry<String, List<Parameter>> mapEntry : parameters.entrySet()) {
            final String groupName = mapEntry.getKey();
            final List groupParameterList = mapEntry.getValue();
//            final List<Parameter> groupParameterList = mapEntry.getValue();
            for (int i = 0; i < groupParameterList.size(); i++) {
                Map groupParameter = (Map) groupParameterList.get(i);

//            for (Parameter groupParameter : groupParameterList) {
                final String parameterName = (String) groupParameter.get("name");
                final String parameterType = (String) groupParameter.get("type");
                final List valueSet = (List) groupParameter.get("valueSet");
//                final String parameterName = groupParameter.getName();
//                final String parameterType = groupParameter.getType();
//                final String[] valueSet = groupParameter.getValueSet();
                InputDescriptionTypeBuilder builder = InputDescriptionTypeBuilder.create()
                        .withIdentifier(groupName + "~" + parameterName)
                        .withTitle("Param '" + parameterName + "' of group '" + groupName + "'.")
                        .withAbstract("The parameter '" + parameterName + "' of parametergroup '" + groupName + "'.")
                        .withDataType(parameterType);

                if (valueSet != null) {
                    builder = builder.withAllowedValues(valueSet);
                }
                dataInputs.getInput().add(builder.build());
            }
        }
        final ProcessDescriptionType processDescription = new ProcessDescriptionType();
        processDescription.setDataInputs(dataInputs);
        return Collections.singletonList(processDescription);

//        final ProcessDescriptionType.ProcessOutputs outputs = new ProcessDescriptionType.ProcessOutputs();
//        description.setProcessOutputs(outputs);
//
//        } catch (PersistenceException e) {
//            throw new WpsServiceException("Unable to describe process for process identifier '"+processIdentifier+"'", e);
//        }
    }
*/

//    @Override
    public long execute(long workflowId, Map<String, Map<String, String>> parameters) {
        long result = -1;
        try {
            WorkflowDescriptor descriptor = workflowService.findById(workflowId);
            if (descriptor != null) {
                String jobName = descriptor.getName() + " via WPS on " + LocalDateTime.now().format(DateTimeFormatter.ISO_TIME);
                result = orchestratorService.startWorkflow(workflowId, jobName, parameters);
            }
        } catch (PersistenceException e) {
            Logger.getLogger(WebProcessingService.class.getName()).severe(e.getMessage());
        }
        return result;
    }

    @Override
    public ExecuteResponse doExecute(WpsRequestContext context, Execute executeRequest) throws WpsServiceException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ExecuteResponse getStatus(WpsRequestContext context, String jobId) throws WpsServiceException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void dispose() {
        throw new RuntimeException("not implemented");
    }

}
