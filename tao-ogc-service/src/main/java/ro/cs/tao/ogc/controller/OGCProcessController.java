package ro.cs.tao.ogc.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.*;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.param.JavaType;
import ro.cs.tao.eodata.enums.DataFormat;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.execution.model.ExecutionJobSummary;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTaskSummary;
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
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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
                self.setType(MediaType.APPLICATION_JSON_VALUE);
                self.setRel("self");
                self.setHref(request.getRequestURI() + "/" + w.getId());
                list.addLink(self);
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
            final List<WorkflowNodeDescriptor> nodes = workflow.getNodes();
            // Describe inputs
            final List<InputDescription<?>> inputs = new ArrayList<>();
            final List<WorkflowNodeDescriptor> firstLevelNodes = WorkflowUtilities.findFirstLevel(nodes);
            for (WorkflowNodeDescriptor node : nodes) {
                final TaoComponent component = WorkflowUtilities.findComponent(node);
                // Parameters that are required and have no default value should be inputs
                if (component instanceof ProcessingComponent) {
                    final Set<ParameterDescriptor> descriptors = ((ProcessingComponent) component).getParameterDescriptors();
                    for (ParameterDescriptor descriptor : descriptors) {
                        if (descriptor.isNotNull() && StringUtilities.isNullOrEmpty(descriptor.getDefaultValue())) {
                            final InputDescription input;
                            if (descriptor.javaType().isArrayType()) {
                                input = InputDescription.of(Type.ARRAY);
                            } else if (descriptor.javaType() == JavaType.BOOLEAN) {
                                input = InputDescription.of(Type.BOOLEAN);
                            } else if (descriptor.javaType() == JavaType.DATE ||
                                       descriptor.javaType() == JavaType.PATH ||
                                       descriptor.javaType() == JavaType.POLYGON ||
                                       descriptor.javaType() == JavaType.STRING) {
                                input = InputDescription.of(Type.STRING);
                            } else {
                                input = InputDescription.of(Type.NUMBER);
                            }
                            inputs.add(input);
                        }
                    }
                }
                // Sources of the first level nodes
                final List<SourceDescriptor> sources = component.getSources();
                for (SourceDescriptor source : sources) {
                    final InputDescription input = InputDescription.of(Type.STRING);
                    input.setTitle(source.getId());
                    input.setDescription(source.getName());
                    Descriptor schema;
                    final DataDescriptor dataDescriptor = source.getDataDescriptor();
                    if (source.getCardinality() == 1) {
                        schema = InputDescription.of(new ArrayList<>() {{ add(dataDescriptor.getLocation()); }});
                        schema.setType(Type.ARRAY);
                    } else {
                        schema = InputDescription.of(dataDescriptor.getLocation());
                        schema.setType(Type.STRING);
                    }
                    schema.setFormat(dataDescriptor.getFormatName());
                    input.setSchema(schema);
                    final List<Metadata> metadata = new ArrayList<>();
                    Metadata m = new Metadata();
                    m.setTitle("cardinality");
                    m.setValue(source.getCardinality());
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
                    input.setMetadata(metadata);
                    inputs.add(input);
                }
            }
            process.setInputDescription(inputs);

            // Describe outputs
            final List<WorkflowNodeDescriptor> terminalNodes = WorkflowUtilities.findTerminals(nodes);
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
    public ResponseEntity<?> getJobs(HttpServletRequest request) {
        final List<StatusInfo> jobInfos = new ArrayList<>();
        List<ExecutionJobSummary> jobs = orchestratorService.getRunningJobs(isCurrentUserAdmin() ? null : currentUser());
        if (jobs == null) {
            jobs = new ArrayList<>();
        }
        List<ExecutionJobSummary> list = orchestratorService.getCompletedJobs(isCurrentUserAdmin() ? null : currentUser());
        if (list != null) {
            jobs.addAll(list);
        }
        Queue<ExecutionJobSummary> queue = isCurrentUserAdmin() ? orchestratorService.getQueuedJobs() : orchestratorService.getQueuedJobs(currentUser());
        if (queue != null) {
            jobs.addAll(queue);
        }
        if (jobs != null) {
            for (ExecutionJobSummary summary : jobs) {
                final StatusInfo info = new StatusInfo();
                info.setProcessID(summary.getWorkflowName());
                info.setJobID(String.valueOf(summary.getId()));
                StatusCode status;
                switch (summary.getJobStatus()) {
                    case ExecutionStatus.QUEUED_ACTIVE:
                        status = StatusCode.ACCEPTED;
                        break;
                    case ExecutionStatus.RUNNING:
                    case ExecutionStatus.PENDING_FINALISATION:
                        status = StatusCode.RUNNING;
                        break;
                    case ExecutionStatus.DONE:
                        status = StatusCode.SUCCESSFUL;
                        break;
                    case ExecutionStatus.CANCELLED:
                    case ExecutionStatus.FAILED:
                        status = StatusCode.FAILED;
                        break;
                    default:
                        status = StatusCode.DISMISSED;
                        break;
                }
                info.setStatus(status);
                info.setCreated(summary.getJobStart());
                info.setStarted(summary.getJobStart());
                info.setUpdated(status != StatusCode.RUNNING ? summary.getJobEnd() : LocalDateTime.now());
                double progress = 0;
                final List<ExecutionTaskSummary> tasks = summary.getTaskSummaries();
                for (int i = 1; i <= tasks.size(); i++) {
                    progress = ((double) 1 / i) * (i - 1) * progress + tasks.get(i).getPercentComplete();
                }
                info.setProgress((int) (progress * 100));
                info.setLinks(new ArrayList<>() {{
                    Link self = new Link();
                    self.setTitle("Job information");
                    self.setType(MediaType.APPLICATION_JSON_VALUE);
                    self.setRel("self");
                    self.setHref(request.getRequestURI() + "/" + info.getJobID());
                    add(self);
                }});
                jobInfos.add(info);
            }
        }
        return ResponseEntity.ok(jobInfos);
    }

    @RequestMapping(value = "/jobs/{id:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobInfo(@PathVariable("id") String jobId,
                                        HttpServletRequest request) {
        final ExecutionJobSummary summary = orchestratorService.getJobInfo(Long.parseLong(jobId));
        if (summary != null) {
            final StatusInfo info = new StatusInfo();
            info.setProcessID(summary.getWorkflowName());
            info.setJobID(String.valueOf(summary.getId()));
            StatusCode status;
            switch (summary.getJobStatus()) {
                case ExecutionStatus.QUEUED_ACTIVE:
                    status = StatusCode.ACCEPTED;
                    break;
                case ExecutionStatus.RUNNING:
                case ExecutionStatus.PENDING_FINALISATION:
                    status = StatusCode.RUNNING;
                    break;
                case ExecutionStatus.DONE:
                    status = StatusCode.SUCCESSFUL;
                    break;
                case ExecutionStatus.CANCELLED:
                case ExecutionStatus.FAILED:
                    status = StatusCode.FAILED;
                    break;
                default:
                    status = StatusCode.DISMISSED;
                    break;
            }
            info.setStatus(status);
            info.setCreated(summary.getJobStart());
            info.setStarted(summary.getJobStart());
            info.setUpdated(status != StatusCode.RUNNING ? summary.getJobEnd() : LocalDateTime.now());
            double progress = 0;
            final List<ExecutionTaskSummary> tasks = summary.getTaskSummaries();
            for (int i = 1; i <= tasks.size(); i++) {
                progress = ((double) 1 / i) * (i - 1) * progress + tasks.get(i).getPercentComplete();
            }
            info.setProgress((int) (progress * 100));
            info.setLinks(new ArrayList<>() {{
                Link self = new Link();
                self.setTitle("Job information");
                self.setType(MediaType.APPLICATION_JSON_VALUE);
                self.setRel("self");
                self.setHref(request.getRequestURI() + "/" + info.getJobID());
                add(self);
            }});
        }
        return ResponseEntity.ok(summary);
    }

    @RequestMapping(value = "/jobs/{id:.+}/results", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobResults(@PathVariable("id") String jobId) {
        // TODO
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/jobs", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> execute(@RequestBody ExecutionRequest request) {
        // TODO
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
