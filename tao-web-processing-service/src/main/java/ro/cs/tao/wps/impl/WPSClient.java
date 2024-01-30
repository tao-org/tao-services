package ro.cs.tao.wps.impl;

import net.opengis.wps10.*;
import org.eclipse.emf.common.util.EList;
import org.geotools.data.wps.WebProcessingService;
import org.geotools.data.wps.request.DescribeProcessRequest;
import org.geotools.data.wps.response.DescribeProcessResponse;
import org.geotools.ows.ServiceException;
import ro.cs.tao.component.DataDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.WebServiceAuthentication;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.datasource.param.JavaType;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerType;
import ro.cs.tao.eodata.enums.DataFormat;
import ro.cs.tao.execution.wps.WPSHttpClient;
import ro.cs.tao.services.interfaces.WebProcessingService.ProcessInfo;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.enums.Status;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.*;

public class WPSClient {
    private final String url;
    private final WebServiceAuthentication authentication;
    private final Principal currentPrincipal;

    public WPSClient(String url, WebServiceAuthentication authentication, Principal principal) {
        this.url = url;
        this.authentication = authentication;
        this.currentPrincipal = principal;
    }

    public Container getCapabilities() throws ServiceException, IOException {
        final WebProcessingService service = getService();
        final WPSCapabilitiesType capabilities = service.getCapabilities();
        final ProcessOfferingsType processOfferings = capabilities.getProcessOfferings();
        final EList processes = processOfferings.getProcess();
        final Container container = new Container();
        container.setFormat(new HashSet<String>() {{ add("WPS"); }});
        container.setId(url);
        container.setApplicationPath(url);
        container.setName("WPS Service");
        container.setType(ContainerType.WPS);
        for (Object o : processes) {
            final ProcessBriefType process = (ProcessBriefType) o;
            final Application application = new Application();
            application.setName(process.getTitle().getValue());
            application.setPath(process.getIdentifier().getValue());
            container.addApplication(application);
        }
        return container;
    }

    public ProcessInfo describeProcess(String identifier) throws ServiceException, IOException {
        final WebProcessingService service = getService();
        final DescribeProcessRequest descRequest = service.createDescribeProcessRequest();
        descRequest.setIdentifier(identifier);
        final DescribeProcessResponse descResponse = service.issueRequest(descRequest);
        final ProcessDescriptionsType processDesc = descResponse.getProcessDesc();
        final ProcessDescriptionType pdt = (ProcessDescriptionType) processDesc.getProcessDescription().get(0);
        final WebProcessingServiceImpl.ProcessInfoImpl info = new WebProcessingServiceImpl.ProcessInfoImpl();
        final WorkflowDescriptor workflowDescriptor = new WorkflowDescriptor();
        workflowDescriptor.setId(Long.parseLong(pdt.getIdentifier().getValue()));
        final String title = pdt.getTitle().getValue();
        workflowDescriptor.setName(title);
        workflowDescriptor.setUserId(this.currentPrincipal.getName());
        workflowDescriptor.setActive(true);
        workflowDescriptor.setStatus(Status.PUBLISHED);
        workflowDescriptor.setPath(url);
        info.setWorkflowInfo(new WorkflowInfo(workflowDescriptor, null));
        Map<String, List<Parameter>> parameters = new HashMap<>();
        final EList paramList = pdt.getDataInputs().getInput();
        for (Object o : paramList) {
            final InputDescriptionType parameter = (InputDescriptionType) o;
            Parameter param = new Parameter();
            final String pId = parameter.getIdentifier().getValue();
            param.setType(parameter.getLiteralData().getDataType().getValue());
            String value = parameter.getLiteralData().getDefaultValue();
            if (JavaType.BOOLEAN.friendlyName().equals(param.getType())) {
                if ("0".equals(value)) {
                    value = "false";
                } else if ("1".equals(value)) {
                    value = "true";
                }
            }
            param.setValue(value);
            if (pId.contains("~")) {
                final String[] tokens = pId.split("~");
                if (!parameters.containsKey(tokens[0])) {
                    parameters.put(tokens[0], new ArrayList<>());
                }
                param.setName(tokens[1]);
                parameters.get(tokens[0]).add(param);
            } else {
                if (!parameters.containsKey(pId)) {
                    parameters.put(pId, new ArrayList<>());
                }
                param.setName(pId);
                parameters.get(pId).add(param);
            }
        }
        info.setParameters(parameters);
        final EList output = pdt.getProcessOutputs().getOutput();
        final List<TargetDescriptor> outDecriptors = new ArrayList<>();
        for (Object o : output) {
            final OutputDescriptionType outParam = (OutputDescriptionType) o;
            final TargetDescriptor descriptor = new TargetDescriptor();
            descriptor.setId(outParam.getIdentifier().getValue());
            descriptor.setCardinality(0);
            descriptor.setName(title + " output");
            final DataDescriptor dataDescriptor = new DataDescriptor();
            final String mimeType = outParam.getComplexOutput().getDefault().getFormat().getMimeType();
            DataFormat format;
            switch (mimeType.toLowerCase()) {
                case "application/json":
                    format = DataFormat.JSON;
                    break;
                case "application/octet-stream":
                    format = DataFormat.RASTER;
                    break;
                case "plain/text":
                    format = DataFormat.FOLDER;
                    break;
                default:
                    format = DataFormat.OTHER;
                    break;
            }
            dataDescriptor.setFormatType(format);
            dataDescriptor.setFormatName(mimeType);
            dataDescriptor.setLocation(outParam.getTitle().getValue());
            descriptor.setDataDescriptor(dataDescriptor);
            outDecriptors.add(descriptor);
        }
        info.setOutputs(outDecriptors);
        return info;
    }

    private WebProcessingService getService() throws ServiceException, IOException {
        return new WebProcessingService(new URL(this.url),
                                        new WPSHttpClient(this.authentication),
                                        null);
    }
}
