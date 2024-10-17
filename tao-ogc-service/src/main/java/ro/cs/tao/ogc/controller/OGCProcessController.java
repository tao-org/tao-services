package ro.cs.tao.ogc.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.DataDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.enums.DataFormat;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.ogc.model.common.ConfClasses;
import ro.cs.tao.ogc.model.common.Exception;
import ro.cs.tao.ogc.model.common.LandingPage;
import ro.cs.tao.ogc.model.common.Link;
import ro.cs.tao.ogc.model.processes.core.Process;
import ro.cs.tao.ogc.model.processes.core.*;
import ro.cs.tao.ogc.model.processes.dru.ExecutionRequest;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.services.utils.WorkflowUtilities;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/process")
@Tag(name = "OGC Process API Endpoint", description = "Endpoint compliant with OGC Process API for exposing TAO workflows")
public class OGCProcessController extends BaseController {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private OrchestratorService orchestratorService;

    //region Core operations
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLandingPage(HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        final LandingPage page = new LandingPage();
        page.setTitle("TAO OGC Process API service");
        page.setDescription("REST API compliant with OGC Process API - Core for workflows invocation");
        Link link = new Link();
        link.setTitle("This document");
        link.setType(MediaType.APPLICATION_JSON_VALUE);
        link.setRel("self");
        link.setHref(requestURI);
        page.addLink(link);
        link = new Link();
        link.setTitle("OGC API - Processes conformance classes implemented by this service");
        link.setType(MediaType.APPLICATION_JSON_VALUE);
        link.setRel("http://www.opengis.net/def/rel/ogc/1.0/conformance");
        link.setHref(requestURI + "conformance");
        page.addLink(link);
        link = new Link();
        link.setTitle("Metadata about the processes (workflows)");
        link.setType(MediaType.APPLICATION_JSON_VALUE);
        link.setRel("http://www.opengis.net/def/rel/ogc/1.0/processes");
        link.setHref(requestURI + "processes");
        page.addLink(link);
        link = new Link();
        link.setTitle("Endpoint for job monitoring");
        link.setType(MediaType.APPLICATION_JSON_VALUE);
        link.setRel("http://www.opengis.net/def/rel/ogc/1.0/job-list");
        link.setHref(requestURI + "jobs");
        page.addLink(link);
        return ResponseEntity.ok(page);
    }

    @RequestMapping(value = "/conformance", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getConformances() {
        final ConfClasses confClasses = new ConfClasses();
        confClasses.addConformance("http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/core");
        confClasses.addConformance("http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/json");
        return ResponseEntity.ok(confClasses);
    }

    @RequestMapping(value = "/processes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProcessSummaries(HttpServletRequest request) {
        try {
            final List<WorkflowInfo> workflows = workflowService.getPublicWorkflows();
            final ProcessList list = new ProcessList();
            Link link = new Link();
            link.setType(MediaType.APPLICATION_JSON_VALUE);
            link.setRel("self");
            link.setHref(request.getRequestURI());
            list.addLink(link);
            list.setProcesses(workflows.stream().map(w -> {
                ProcessSummary p = new ProcessSummary();
                p.setId(w.getId().toString());
                p.setTitle(w.getName());
                p.setDescription(w.getDescription());
                p.setKeywords(w.getTags());
                p.setVersion("1.0");
                Link self = new Link();
                self.setTitle("Process description");
                link.setType(MediaType.APPLICATION_JSON_VALUE);
                link.setRel("self");
                link.setHref(request.getRequestURI() + "/" + w.getId());
                list.addLink(link);
                p.addJobControlOption(JobControlOptions.ASYNC_EXECUTE);
                p.addOutputTransmission(TransmissionMode.VALUE);
                return p;
            }).toList());
            return ResponseEntity.ok(list);
        } catch (Throwable e) {
            Exception exception = new Exception();
            exception.setTitle(e.getClass().getSimpleName());
            exception.setDetail(e.getMessage());
            exception.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.internalServerError().body(exception);
        }
    }

    @RequestMapping(value = "/processes/{id:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProcessDescription(@PathVariable("id") String processId,
                                                   HttpServletRequest request) {
        try {
            final WorkflowDescriptor workflow = workflowService.findById(Long.parseLong(processId));
            final Process process = new Process();
            // TODO: Describe inputs


            // Describe outputs
            final List<WorkflowNodeDescriptor> terminalNodes = WorkflowUtilities.findTerminals(workflow.getNodes());
            final List<OutputDescription> outputs = new ArrayList<>();
            for (WorkflowNodeDescriptor node : terminalNodes) {
                final List<TargetDescriptor> targets = WorkflowUtilities.findComponent(node).getTargets();
                for (TargetDescriptor target : targets) {
                    final OutputDescription output = new OutputDescription();
                    output.setTitle(target.getId());
                    output.setDescription(target.getName());
                    Descriptor schema;
                    final DataDescriptor dataDescriptor = target.getDataDescriptor();
                    if (target.getCardinality() == 1) {
                        schema = InputDescription.of(new ArrayList<>() {{ add(dataDescriptor.getLocation()); }});
                        schema.setType(Type.ARRAY);
                    } else {
                        schema = InputDescription.of(dataDescriptor.getLocation());
                        schema.setType(Type.STRING);
                    }
                    schema.setFormat(dataDescriptor.getFormatName());
                    output.setSchema(schema);
                    final List<Metadata> metadata = new ArrayList<>();
                    Metadata m = new Metadata();
                    m.setTitle("cardinality");
                    m.setValue(target.getCardinality());
                    metadata.add(m);
                    Object value = null;
                    if ((value = dataDescriptor.getCrs()) != null) {
                        m = new Metadata();
                        m.setTitle("crs");
                        m.setValue(value);
                        metadata.add(m);
                    }
                    if ((value = dataDescriptor.getGeometry()) != null) {
                        m = new Metadata();
                        m.setTitle("geometry");
                        m.setValue(value);
                        metadata.add(m);
                    }
                    if ((value = dataDescriptor.getFormatType()) != null) {
                        m = new Metadata();
                        m.setTitle("format");
                        m.setValue(((DataFormat) value).friendlyName());
                        metadata.add(m);
                    }
                    if ((value = dataDescriptor.getSensorType()) != null) {
                        m = new Metadata();
                        m.setTitle("sensor");
                        m.setValue(((SensorType) value).friendlyName());
                        metadata.add(m);
                    }
                    output.setMetadata(metadata);
                    outputs.add(output);
                }
                process.setOutputDescription(outputs);
            }

            return ResponseEntity.ok(process);
        } catch (Throwable e) {
            Exception exception = new Exception();
            exception.setTitle(e.getClass().getSimpleName());
            exception.setDetail(e.getMessage());
            exception.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.internalServerError().body(exception);
        }
    }

    @RequestMapping(value = "/jobs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobs() {
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/jobs/{id:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobInfo(@PathVariable("id") String jobId) {
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/jobs/{id:.+}/results", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobResults(@PathVariable("id") String jobId) {
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/jobs", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> execute(@RequestBody ExecutionRequest request) {
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/jobs/{id:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteJob(@PathVariable("id") String jobId) {
        final StatusInfo jobInfo = new StatusInfo();
        jobInfo.setJobID(jobId);
        jobInfo.setStatus(StatusCode.DISMISSED);
        jobInfo.setMessage("Job dismissed");
        jobInfo.setProgress(56);
        Link link = new Link();
        link.setHref(ConfigurationManager.getInstance().getValue("tao.services.base") + "/jobs");
        link.setRel("up");
        link.setType(MediaType.APPLICATION_JSON_VALUE);
        link.setTitle("The job list of this service");
        jobInfo.addLink(link);
        return prepareResult(jobInfo);
    }

    //endregion
}
