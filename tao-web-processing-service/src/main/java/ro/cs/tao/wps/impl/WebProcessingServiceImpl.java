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
import com.bc.wps.api.schema.ContactType;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.OnlineResourceType;
import com.bc.wps.api.schema.ProcessDescriptionType;
import com.bc.wps.api.schema.ResponsiblePartySubsetType;
import com.bc.wps.api.schema.ServiceProvider;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.services.entity.impl.WorkflowServiceImpl;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.orchestration.impl.OrchestrationServiceImpl;
import ro.cs.tao.wps.operations.GetCapabilitiesOperation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//@Service("webProcessingService")
public class WebProcessingServiceImpl implements WpsServiceInstance /*, WebProcessingService */{

//    @Autowired
    private WorkflowService workflowService = new WorkflowServiceImpl();

//    @Autowired
    private OrchestratorService orchestratorService = new OrchestrationServiceImpl();

    private Logger logger = Logger.getLogger(this.getClass().getName());

//    @Override
//    public List<WorkflowInfo> getCapabilities() {
//        return workflowService.getPublicWorkflows();
//    }
//
//    @Override
    public Map<String, List<Parameter>> describeProcess(long workflowId) {
        return workflowService.getWorkflowParameters(workflowId);
    }
//
//    @Override
//    public long execute(long workflowId, Map<String, Map<String, String>> parameters) {
//        return orchestratorService.startWorkflow(workflowId, parameters);
//    }

    @Override
    public Capabilities getCapabilities(WpsRequestContext context) throws WpsServiceException {
//        final OnlineResourceType onlineResourceType = new OnlineResourceType();
//        onlineResourceType.setHref("http://do.not.know");
//
//        final ContactType contactInfo = new ContactType();
//        contactInfo.setContactInstructions("Some contakt instruction.");
//        contactInfo.setHoursOfService("24 x 7");
//
//        final ResponsiblePartySubsetType responsiblePartySubsetType = new ResponsiblePartySubsetType();
//        responsiblePartySubsetType.setContactInfo(contactInfo);
//
//        final ServiceProvider serviceProvider = new ServiceProvider();
//        serviceProvider.setProviderName("Unknown");
//        serviceProvider.setProviderSite(onlineResourceType);
//        serviceProvider.setServiceContact(responsiblePartySubsetType);
//
//
//        final Capabilities capabilities = new Capabilities();
//        capabilities.setLang("en");
//        capabilities.setService("WPS");
//        capabilities.setVersion("1.0.0");
//        capabilities.setServiceProvider(serviceProvider);
//        return capabilities;
        try {
            final GetCapabilitiesOperation getCapabilitiesOperation = new GetCapabilitiesOperation(context);
            return getCapabilitiesOperation.getCapabilities();
        } catch (IOException | URISyntaxException exception) {
            logger.log(Level.SEVERE, "Unable to perform GetCapabilities operation successfully", exception);
            throw new WpsServiceException(exception);
        }
    }

    @Override
    public List<ProcessDescriptionType> describeProcess(WpsRequestContext context, String processId) throws WpsServiceException {
        throw new RuntimeException("not implemented");
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
