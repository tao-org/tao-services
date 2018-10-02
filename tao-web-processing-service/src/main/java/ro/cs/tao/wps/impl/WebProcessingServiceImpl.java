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
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.ProcessDescriptionType;
import com.bc.wps.api.utils.InputDescriptionTypeBuilder;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.orchestration.impl.OrchestrationServiceImpl;
import ro.cs.tao.wps.operations.GetCapabilitiesOperation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//@Service("webProcessingService")
public class WebProcessingServiceImpl implements WpsServiceInstance /*, WebProcessingService */ {

    private OrchestratorService orchestratorService = new OrchestrationServiceImpl();

    private Logger logger = Logger.getLogger(this.getClass().getName());

    //    @Override
//    public List<WorkflowInfo> getCapabilities() {
//        return workflowService.getPublicWorkflows();
//    }
//
//    @Override
    public Map<String, List<Parameter>> describeProcess(long workflowId) {
//        return workflowService.getWorkflowParameters(workflowId);
        return null;
    }
//
//    @Override
//    public long execute(long workflowId, Map<String, Map<String, String>> parameters) {
//        return orchestratorService.startWorkflow(workflowId, parameters);
//    }

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
