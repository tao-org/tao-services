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
package ro.cs.tao.wps.operations;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.schema.ProcessDescriptionType;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WorkflowService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DescribeProcessOperation {

    final Logger logger = Logger.getLogger(this.getClass().getName());
    final WpsRequestContext context;
    final WorkflowService workflowService;
    final OrchestratorService orchestratorService;

    public DescribeProcessOperation(WpsRequestContext context, WorkflowService workflowService, OrchestratorService orchestratorService) {
        this.context = context;
        this.workflowService = workflowService;
        this.orchestratorService = orchestratorService;
    }

    public List<ProcessDescriptionType> describeProcess(String processIentifier) {
        final String userName = context.getUserName();
        logger.info("Getting processors " + processIentifier + " for user " + userName);
//        try {
            String[] processorIdArray = processIentifier.split(",");
            List<ProcessDescriptionType> processDescriptionTypeList = new ArrayList<>();
//            List<WpsProcess> processors = new ArrayList<>();
//            if (processorIdArray.length > 1) {
//                processors.addAll(getMultipleTaoProcessors(taoFacade, processorIdArray));
//                processors.addAll(getMultipleLocalProcessors(processorIdArray));
//                processDescriptionTypeList.addAll(getMultipleProcessType(processors));
//            } else {
//                ProcessorNameConverter parser = new ProcessorNameConverter(processorId);
//                final Map<String, List<Parameter>> workflowParameters = workflowService.getWorkflowParameters(Long.parseLong(processIentifier));
//                if (processor == null) {
//                    throw new WpsProcessorNotFoundException("Unable to retrieve processor '" + parser.getProcessorIdentifier() + "'");
//                }
//                processDescriptionTypeList.add(getSingleProcess(processor));
//            }
            return processDescriptionTypeList;
//        } catch (IOException | ProductSetsNotAvailableException | InvalidProcessorIdException exception) {
//            throw new WpsProcessorNotFoundException("Unable to retrieve the selected process(es)", exception);
//        }
    }
}
