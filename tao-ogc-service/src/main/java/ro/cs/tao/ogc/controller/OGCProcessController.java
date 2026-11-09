package ro.cs.tao.ogc.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.cs.tao.component.*;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.param.JavaType;
import ro.cs.tao.datasource.persistence.DataSourceComponentProvider;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.enums.DataFormat;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.execution.model.*;
import ro.cs.tao.execution.persistence.ExecutionJobProvider;
import ro.cs.tao.execution.util.TaskUtilities;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.ogc.model.common.ConfClasses;
import ro.cs.tao.ogc.model.common.Exception;
import ro.cs.tao.ogc.model.common.LandingPage;
import ro.cs.tao.ogc.model.common.Link;
import ro.cs.tao.ogc.model.processes.core.Process;
import ro.cs.tao.ogc.model.processes.core.*;
import ro.cs.tao.ogc.model.processes.core.execution.*;
import ro.cs.tao.ogc.model.processes.dru.ExecutionRequest;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.entity.beans.RepositoryBean;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.services.utils.WorkflowUtilities;
import ro.cs.tao.services.workspace.impl.TransferService;
import ro.cs.tao.services.workspace.model.TransferableItem;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

@RestController
@RequestMapping("/process")
@Tag(name = "OGC Process API Endpoint", description = "Endpoint compliant with OGC Process API for exposing TAO workflows")
public class OGCProcessController extends BaseController {
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private RepositoryProvider repositoryProvider;
    @Autowired
    private DataSourceComponentProvider dataSourceComponentProvider;
    @Autowired
    private TransferService transferService;
    @Autowired
    private OrchestratorService orchestratorService;
    @Autowired
    private ExecutionJobProvider executionJobProvider;

    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
            Map.entry("txt", "text/plain"),
            Map.entry("xml", "application/xml"),
            Map.entry("json", "application/json"),
            Map.entry("geojson", "application/geo+json"),
            Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("zip", "application/zip"),
            Map.entry("tif", "image/tiff"),
            Map.entry("tiff", "image/tiff"),
            Map.entry("jp2", "image/jp2"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("gml", "application/gml+xml"),
            Map.entry("kml", "application/vnd.google-earth.kml+xml"),
            Map.entry("shp", "application/zip"), // usually zipped shapefile
            Map.entry("hdf", "application/x-hdf"),
            Map.entry("hdf5", "application/x-hdf"),
            Map.entry("nc", "application/x-netcdf"),
            Map.entry("netcdf", "application/x-netcdf"),

            Map.entry(".txt", "text/plain"),
            Map.entry(".xml", "application/xml"),
            Map.entry(".json", "application/json"),
            Map.entry(".geojson", "application/geo+json"),
            Map.entry(".csv", "text/csv"),
            Map.entry(".html", "text/html"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".zip", "application/zip"),
            Map.entry(".tif", "image/tiff"),
            Map.entry(".tiff", "image/tiff"),
            Map.entry(".jp2", "image/jp2"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".gml", "application/gml+xml"),
            Map.entry(".kml", "application/vnd.google-earth.kml+xml"),
            Map.entry(".shp", "application/zip"), // usually zipped shapefile
            Map.entry(".hdf", "application/x-hdf"),
            Map.entry(".hdf5", "application/x-hdf"),
            Map.entry(".nc", "application/x-netcdf"),
            Map.entry(".netcdf", "application/x-netcdf")

    );

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
            final List<WorkflowInfo> workflows = workflowService.getUserVisibleWorkflows(currentUser(), true);
            workflows.removeIf(s -> !s.isActive());
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
                self.setHref(normalizeUri(request) + "/" + w.getId());
                list.addLink(self);
                Link linkExecution = new Link();
                linkExecution.setTitle("Process execution");
                linkExecution.setType(MediaType.APPLICATION_JSON_VALUE);
                linkExecution.setRel("http://www.opengis.net/def/rel/ogc/1.0/execute");
                linkExecution.setHref(normalizeUri(request) + "/" + w.getId() + "/execution");
                list.addLink(linkExecution);
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
            process.setId(processId);
            process.setVersion("1.0"); // we do not have yet a version of the workflow
            process.setTitle(workflow.getName());
            process.setDescription(workflow.getDescription());
            final List<WorkflowNodeDescriptor> nodes = workflow.getNodes();
            // Describe inputs
            final Map<String, InputDescription<?>> inputs = new LinkedHashMap<>();
            final List<WorkflowNodeDescriptor> firstLevelNodes = WorkflowUtilities.findFirstLevel(nodes);
            boolean isFirstLevelNode;
            for (WorkflowNodeDescriptor node : nodes) {

                final TaoComponent component = WorkflowUtilities.findComponent(node);
                List<ParameterValue> customValuesList = node.getCustomValues();
                Map<String, ParameterValue> customValues = null;
                if (customValuesList != null) {
                    customValues = node.getCustomValues().stream()
                            .collect(Collectors.toMap(ParameterValue::getParameterName, p -> p));
                }
                isFirstLevelNode = firstLevelNodes.contains(node);
                int xUiOrder = 1;
                // Parameters that are required and have no default value should be inputs
                // Parameters with default Values should be inputs as well
                if (component instanceof ProcessingComponent) {
                    final Set<ParameterDescriptor> descriptors = ((ProcessingComponent) component).getParameterDescriptors();
                    for (ParameterDescriptor descriptor : descriptors) {
                        // TODO: For sure we need a condition here but this is not the right one
                        // if (descriptor.isNotNull()){// && StringUtilities.isNullOrEmpty(descriptor.getDefaultValue())) {
                        if (isFirstLevelNode) {
                            final InputDescription input;
                            if (descriptor.javaType().isArrayType()) {
                                input = InputDescription.of(Type.ARRAY);
                                ListDescriptor schema = new ListDescriptor();
                                input.setSchema(schema);
                                schema.setType(Type.ARRAY);
                            } else if (descriptor.javaType() == JavaType.BOOLEAN) {
                                input = InputDescription.of(Type.BOOLEAN);
                                StringDescriptor schema = new StringDescriptor();
                                input.setSchema(schema);
                                schema.setType(Type.BOOLEAN);
                            } else if (descriptor.javaType() == JavaType.DATE ||
                                       descriptor.javaType() == JavaType.PATH ||
                                       descriptor.javaType() == JavaType.POLYGON ||
                                       descriptor.javaType() == JavaType.WKT ||
                                       descriptor.javaType() == JavaType.STRING) {
                                input = InputDescription.of(Type.STRING);
                                StringDescriptor schema = new StringDescriptor();
                                input.setSchema(schema);
                                schema.setType(Type.STRING);
                                schema.setMinLength(1);
                                schema.setMaxLength(255);
                                if(descriptor.javaType() == JavaType.DATE){
                                    // schema.setFormat("yyyy-MM-dd");
                                    schema.setFormat(JavaType.DATE.friendlyName());
                                } else  if(descriptor.javaType() == JavaType.WKT){
                                    schema.setMaxLength(4096);
                                    schema.setUiWidget("map");
                                    schema.setFormat(JavaType.WKT.friendlyName());
                                }
                            } else {
                                input = InputDescription.of(Type.NUMBER);
                                NumericDescriptor schema = new NumericDescriptor();
                                input.setSchema(schema);
                                schema.setType(Type.NUMBER);
                            }
                            Descriptor schema = input.getSchema();
                            schema.setDefaultValue(descriptor.getDefaultValue());
                            schema.setNullable(!descriptor.isNotNull());
                            schema.setTitle("Parameter Schema");
                            schema.setDescription(descriptor.getDescription());
                            if (descriptor.getValueSet() != null && descriptor.getValueSet().length > 0) {
                                schema.setUiWidget(descriptor.getValueSet().length > 6 ? "combo" : "radio");
                                schema.setNullable(false);
                                schema.setEnumValues(descriptor.getValueSet());
                            }
                            input.setMinOccurs(1);
                            input.setTitle(descriptor.getLabel());
                            input.setDescription(descriptor.getDescription());
                            input.setUiOrder(String.valueOf(xUiOrder++));
                            if (descriptor.getDefaultValue() == null || "".equals(descriptor.getDefaultValue()) ) {
                                input.setUiAdvanced(false);
                            } else {
                                input.setUiAdvanced(true);
                            }
                            inputs.put(descriptor.getName(), input);
                        }
                    }
                }
                // Sources of the first level nodes
                final List<SourceDescriptor> sources = component.getSources();
                for (SourceDescriptor source : sources) {
                    Set<ComponentLink> inLinks = node.getIncomingLinks();
                    if (inLinks != null) {
                        boolean isInLink = inLinks.stream()
                                .anyMatch(link -> link.getOutput().getId().equals(source.getId()));
                        if (isInLink) {
                            continue; // ignore sources that are already connected to other nodes
                        }
                    }

                    final InputDescription input = InputDescription.of(Type.STRING);
                    input.setTitle(source.getName());
                    input.setDescription(source.getName());
                    input.setUiAdvanced(false);
                    Descriptor schema;
                    final DataDescriptor dataDescriptor = source.getDataDescriptor();
                    if (source.getCardinality() == 1) {
                        schema = new ObjectDescriptor();
                        schema.setType(Type.STRING);
                        ((ObjectDescriptor)schema).setRequired(List.of("href"));
                        ObjectDescriptorProperties href = new ObjectDescriptorProperties();
                        href.setType("string");
                        href.setFormat("uri-reference");
                        ObjectDescriptorProperties type = new ObjectDescriptorProperties();
                        type.setType("string");
                        type.setDefaultValue("application/xml");
                        Map<String, ObjectDescriptorProperties> props = new LinkedHashMap<>();
                        props.put("href", href);
                        props.put("type", type);
                        ((ObjectDescriptor)schema).setProperties(props);
//                        schema = InputDescription.of(dataDescriptor.getLocation());
//                        if (schema instanceof StringDescriptor) {
//                            ((StringDescriptor) schema).setMinLength(1);
//                            ((StringDescriptor) schema).setMaxLength(4096);
//                        }
//                        schema.setType(Type.STRING);
                    } else {
                        // TODO : This should be updated according to the one above but for array case.
                        schema = InputDescription.of(new ArrayList<>() {{ add(dataDescriptor.getLocation()); }});
                        schema.setType(Type.ARRAY);
                        if (schema instanceof ListDescriptor) {
                            if (source.getCardinality() > 1) {
                                ((ListDescriptor) schema).setMinItems(source.getCardinality());
                                ((ListDescriptor) schema).setMaxItems(source.getCardinality());
                            } else if (source.getCardinality() == 0) {
                                ((ListDescriptor) schema).setMinItems(1);
                                ((ListDescriptor) schema).setMaxItems(4096);
                            }
                        }
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
                    input.setMinOccurs(1);
                    inputs.put(source.getName(), input);
                }
            }
            process.setInputs(inputs);

            // Describe outputs
            final List<WorkflowNodeDescriptor> terminalNodes = WorkflowUtilities.findTerminals(nodes);
            final Map<String, OutputDescription> outputs = new LinkedHashMap<>();
            for (WorkflowNodeDescriptor node : terminalNodes) {
                final List<TargetDescriptor> targets = WorkflowUtilities.findComponent(node).getTargets();
                for (TargetDescriptor target : targets) {
                    final OutputDescription output = new OutputDescription();
                    output.setTitle(target.getName());
                    output.setDescription(target.getName());
                    Descriptor schema;
                    final DataDescriptor dataDescriptor = target.getDataDescriptor();
                    if (target.getCardinality() == 1) {
                        schema = InputDescription.of(dataDescriptor.getLocation());
                        schema.setType(Type.STRING);
                    } else {
                        schema = InputDescription.of(new ArrayList<>() {{ add(dataDescriptor.getLocation()); }});
                        schema.setType(Type.ARRAY);
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
                    outputs.put(target.getName(), output);
                }
                process.setOutputs(outputs);
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

    @RequestMapping(value = "/processes/{id:.+}/execution", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> execute(@PathVariable("id") String processId, @RequestBody ExecutionRequest request) {
        Long jobId = -1L;
        try{
            final WorkflowDescriptor workflow = workflowService.findById(Long.parseLong(processId));
            final List<WorkflowNodeDescriptor> nodes = workflow.getNodes();
            final Input inputs = request.getInputs();
            //outputs
//            final List<WorkflowNodeDescriptor> terminalNodes = WorkflowUtilities.findTerminals(nodes);
//            final List<Output> outputs = request.getOutputs();
//            for (WorkflowNodeDescriptor node : terminalNodes) {
//                final List<TargetDescriptor> targets = WorkflowUtilities.findComponent(node).getTargets();
//                for (TargetDescriptor target : targets) {
//                    Output output = getRequestOutput(node.getName(), outputs);
//                    DataDescriptor targetDataDescriptor = target.getDataDescriptor();
//                    // no output processing at this time, transmission mode is by reference as per TAO implementation
//                }
//            }
            String user = currentUser();
            List<Repository> userRepos = repositoryProvider.getByUser(currentUser());
            if (userRepos.isEmpty()) {
                throw new IOException("Workspaces do not exist");
            }
            Repository localRepo = userRepos.stream()
                    .filter(repo -> repo.getType() == RepositoryType.LOCAL)
                    .findFirst().orElseThrow(() -> new IOException("Local Workspace does not exist"));
            checkDestRepo(user, localRepo);
            Map<Long, Map<String, String>> inputsRequest = new HashMap<>();
            for (WorkflowNodeDescriptor node : nodes) {
                Map<String, String> nodeInputs = new HashMap<>();
                //parameters
                final TaoComponent component = WorkflowUtilities.findComponent(node);
                setParametersFromInputs(node, component, inputs);
                //sources
                final List<SourceDescriptor> sources = component.getSources();
                final Set<ComponentLink> incomingLinks = node.getIncomingLinks();
                for (SourceDescriptor source : sources) {
                    // if there is already a link skip the setting of the Source Input
                    if(incomingLinks.stream().anyMatch(link -> link.getOutput().equals(source))){
                        continue;
                    }
                    List<String> localPaths = new ArrayList<>();
                    InputBase requestInput = getRequestInput(source.getId(), inputs);
                    if (requestInput == null) {
                        throw new IllegalArgumentException("No input found in the request for the input " + source.getId());
                    }
                    List<Link> imageLinks = getImageLinks(requestInput);
                    List<String> imagesPaths = imageLinks.stream().map(Link::getHref).toList();
                    if (imagesPaths.isEmpty())
                        throw new IllegalArgumentException("Source Input is invalid. Image link or Array of Image Links are required.");
                    for (var imagePath : imagesPaths) {
                        checkPath(imagePath);
                        // if path is already accessible, we can use it as it is as input, no need to copy it locally
                        // TODO: maybe we can add a config key to force copy in local repository (for cases when products in /eodata or other s3 mounted directory)
                        if (isPathAccessible(imagePath)) {
                            localPaths.add(imagePath.startsWith("file://") ?
                                    imagePath.substring("file://".length()) : imagePath);
                        } else {
                            // refactor using StorageService information to know if it is a folder
                            String stacMetadata = handleStacProductUrl(imagePath);
                            if (stacMetadata != null) {
                                localPaths.add(stacMetadata);
                            } else {
                                boolean isFolder = imageLinks.stream().filter(link -> link.getHref().equals(imagePath)).findFirst().get().getType().equals("folder");
                                Repository sourceRepo = userRepos.stream()
                                        .filter(repo -> imagePath.startsWith(repo.getUrlPrefix()))
                                        .findFirst()
                                        .orElseThrow(() -> new IOException("Workspace does not exist for path " + imagePath));
                                if (!user.equals(sourceRepo.getUserId())) {
                                    throw new AccessDeniedException("Operation not allowed");
                                }
                                final RepositoryBean bean = ServiceTransformUtils.toBean(sourceRepo);
                                String fullPath = sourceRepo.relativizeToRoot(imagePath);
                                String bucketName = sourceRepo.getParameters().get(bean.getRootKey());
                                String relativeToBucket = fullPath.split(bucketName + "/")[1];
                                FileObject fileObject = new FileObject(sourceRepo.getUrlPrefix(), relativeToBucket, isFolder, 0, relativeToBucket);
                                fileObject.addAttribute("remotePath", fullPath);
                                final StorageService service = getRepositoryService(sourceRepo);
                                int count = 0;
                                long size = 0;
                                String dstFolder = localRepo.root();
                                if (!dstFolder.endsWith("/")) {
                                    dstFolder += "/";
                                }
                                String objectPath = fileObject.getRelativePath();
                                String targetFolder = dstFolder;
                                if (fileObject.isFolder()) {
                                    targetFolder += sourceRepo.fileName(objectPath);
                                    final List<FileObject> files = service.listTree(fileObject.getRelativePath());
                                    final List<TransferableItem> items = new ArrayList<>();
                                    for (FileObject fObject : files) {
                                        if (!fObject.isFolder()) {
                                            String path = sourceRepo.relativize(objectPath, fObject.getRelativePath());
                                            path = targetFolder + (path.startsWith("/") ? "" : (targetFolder.endsWith("/") ? "" : "/") + path);
                                            items.add(new TransferableItem(user, objectPath, sourceRepo, fObject, localRepo, path, false, true));
                                            size += fObject.getSize();
                                            count++;
                                            localPaths.add(path);
                                        }
                                    }
                                    queueFiles(items.toArray(new TransferableItem[0]));
                                } else {
                                    queueFile(objectPath, fileObject, sourceRepo, localRepo, targetFolder + sourceRepo.fileName(objectPath), false, true);
                                    localPaths.add(targetFolder + sourceRepo.fileName(objectPath));
                                    count = 1;
                                }
                            }
                        }
                    }
                    // testing a timeout on transferring files, should make it to wait for transfer else it will fail
                    final StorageService localService = getRepositoryService(localRepo);
                    long timeoutMs = 300000;
                    long startTime = System.currentTimeMillis();

                    for(String path : localPaths){
                        long elapsed;
                        while(!FileUtilities.isPathAccessible(Paths.get(path))){
                            elapsed = System.currentTimeMillis() - startTime;
                            if (elapsed > timeoutMs) {
                                throw new IOException("Timeout waiting for file transfer: " + path);
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IOException("Interrupted while waiting for file transfer", e);
                            }
                        }
                    }
                    final DataSourceComponent virtualProductSet = createVirtualComponent(localPaths, node.getName()+"_"+source.getName(), currentPrincipal());
                    nodeInputs.put(source.getId(), virtualProductSet.getId());
                }
                inputsRequest.put(node.getId(), nodeInputs);
            }
            jobId = orchestratorService.startWorkflow(getExecutionRequest(workflow, nodes, inputsRequest));
        }
        catch (Throwable e) {
            Exception exception = new Exception();
            exception.setTitle(e.getClass().getSimpleName());
            exception.setDetail(e.getMessage());
            exception.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.internalServerError().body(exception);
        }
        URI uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/process/jobs/" + jobId)
                .buildAndExpand(processId)
                .toUri();
        StatusInfo info = getJobStatus(uri.toString(), jobId);
        return ResponseEntity.ok(info);
    }

    private ro.cs.tao.execution.model.ExecutionRequest getExecutionRequest(WorkflowDescriptor workflow, List<WorkflowNodeDescriptor> nodes,
                                                                           Map<Long, Map<String,String>> inputs) {
        ro.cs.tao.execution.model.ExecutionRequest executionRequest = new ro.cs.tao.execution.model.ExecutionRequest();
        executionRequest.setWorkflowId(workflow.getId());
        String executionName = String.join("_", workflow.getName(), "virtual", workflow.getCreated().toString());
        executionRequest.setName(executionName);
        executionRequest.setLabel(executionName.replaceAll("[/:]", "-"));
        executionRequest.setJobType("EXECUTION");
        executionRequest.setEnvironment("DEFAULT");
        executionRequest.setInputs(new HashMap<>(inputs));
        Map<String, Map<String, String>> parametersMap = new HashMap<>();
        executionRequest.setParameters(parametersMap);
        for(WorkflowNodeDescriptor nodeDescriptor : nodes){
            String name = nodeDescriptor.getName();
            List<ParameterValue> customVals = nodeDescriptor.getCustomValues();
            if (customVals != null) {
                Set<String> removeNames = Set.of("nodeAffinity");
                customVals.removeIf(p -> removeNames.contains(p.getParameterName()));
                if (customVals.size() > 0) {
                    Map<String, String> parameterMap = customVals.stream()
                            .collect(Collectors.groupingBy(ParameterValue::getParameterName, Collectors.mapping(ParameterValue::getParameterValue, Collectors.joining())));
                    parametersMap.put(name, parameterMap);
                }
            }
        }
        return executionRequest;
    }

    private DataSourceComponent createVirtualComponent(List<String> productPaths, String label, Principal principal) throws PersistenceException {
        if (productPaths == null || productPaths.isEmpty()) {
            throw new IllegalArgumentException("Product list is empty");
        }
        if (principal == null) {
            throw new IllegalArgumentException("Invalid principal (null)");
        }
        final Repository repository = this.repositoryProvider.getByUserAndName(principal.getName(), "LOCAL");
        final List<String> list = productPaths.stream()
                .map(n -> Paths.get(isPathAccessible(n) ? n : repository.resolve(n)).toUri().toString())
                .toList();
        LocalDateTime time = LocalDateTime.now();
        String sensorName = String.join("_","virtual", time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        DataSourceComponent userDSC = new DataSourceComponent(sensorName, "Local");
        userDSC.setFetchMode(FetchMode.OVERWRITE);
        userDSC.setVersion("1.0");
        userDSC.setDescription(label + " (local product set)");
        userDSC.setVolatile(false);
        userDSC.setAuthors("TAO Team");
        userDSC.setCopyright("(C) TAO Team");
        userDSC.setNodeAffinity(NodeAffinity.Any);
        final String newId = String.join("-", "product", "set", principal.getName(),
                time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        userDSC.setId(newId);
        userDSC.getSources().forEach(s -> s.setParentId(newId));
        userDSC.getTargets().forEach(t -> t.setParentId(newId));
        userDSC.setLabel(label != null && !label.isEmpty() ? label : newId);
        SourceDescriptor sourceDescriptor = userDSC.getSources().stream().filter(s -> s.getName().equals(DataSourceComponent.QUERY_PARAMETER)).findFirst().get();
        sourceDescriptor.getDataDescriptor().setSensorType(SensorType.UNKNOWN);
        sourceDescriptor.getDataDescriptor().setLocation(list.stream()
                .map(this::toFilePath)
                .collect(Collectors.joining(",")));
        sourceDescriptor.setCardinality(productPaths.size());
        TargetDescriptor targetDescriptor = userDSC.getTargets().getFirst();
        targetDescriptor.setId(UUID.randomUUID().toString());
        targetDescriptor.getDataDescriptor().setLocation(list.stream()
                .map(this::toFilePath)
                .collect(Collectors.joining(",")));
        targetDescriptor.setParentId(newId);
        targetDescriptor.setCardinality(productPaths.size());
        userDSC.setSystem(false);
        return dataSourceComponentProvider.save(userDSC);
    }

    private void setParametersFromInputs(WorkflowNodeDescriptor nodeDescriptor, TaoComponent component, Input inputs) throws PersistenceException {
        if (component instanceof ProcessingComponent) {
            final Set<ParameterDescriptor> descriptors = ((ProcessingComponent) component).getParameterDescriptors();
            List<ParameterValue> customVals = nodeDescriptor.getCustomValues();
            for (ParameterDescriptor descriptor : descriptors) {
                InputBase requestInput= getRequestInput(descriptor.getName(), inputs);
                String val = null;
                if (requestInput != null) {
                    if(requestInput instanceof QualifiedInputValue<?> requestQualifiedValue){
                        val = requestQualifiedValue.getValue().toString();
                    } else if(requestInput instanceof InputValueNoObject<?> requestInputValueNoObject){
                        val = requestInputValueNoObject.getValue().toString();
                    } else if (requestInput instanceof Link requestLink) {
                        val = requestLink.getHref();
                    }
                } else if (descriptor.getDefaultValue() != null) {
                    // do not force receiving the parameter if it has a default value
                    val = descriptor.getDefaultValue();
                } /*else {
                    throw new IllegalArgumentException("No input found in the request for parameter (without default value). Parameter id =" +
                            descriptor.getId() + ", name = " + descriptor.getName() + ", label = " + descriptor.getLabel() +
                            ", description = " + descriptor.getDescription());
                }*/
                if (val != null) {
                    customVals.add(new ParameterValue(descriptor.getName(), val));
                    // TODO: Why this condition???
                    if (descriptor.isNotNull()) {
                        descriptor.setDefaultValue(val);
                    }
                }
            }
        }
    }

    private InputBase getRequestInput(String nodeInputName, Input inputs) {
        Optional<InputValueNoObject<?>> requestInputValue = inputs.getInputValueNoObjects()
                .stream()
                .filter(input -> input.getName().equals(nodeInputName))
                .findFirst();
        if(requestInputValue.isPresent())
            return requestInputValue.get();
        Optional<QualifiedInputValue<?>> requestQualifiedInputValue = inputs.getQualifiedInputValues()
                .stream()
                .filter(input -> input.getName().equals(nodeInputName))
                .findFirst();
        if(requestQualifiedInputValue.isPresent())
            return requestQualifiedInputValue.get();
        Optional<Link> requestLink = inputs.getLinks()
                .stream()
                .filter(input -> input.getName().equals(nodeInputName))
                .findFirst();
        if(requestLink.isPresent())
            return requestLink.get();
        else
            return null;
    }

    private Output getRequestOutput(String nodeOutputName, List<Output> outputs){
        return outputs.stream()
                .filter(output -> output.getName().equals(nodeOutputName))
                .findFirst()
                .orElse(null);
    }

    private StorageService getRepositoryService(Repository workspace) {
        StorageService instance = StorageServiceFactory.getInstance(workspace);
        instance.associate(workspace);
        return instance;
    }

    private void queueFile(String batchId, FileObject file, Repository source, Repository destination, String folder, boolean move, boolean overwrite) {
        final TransferableItem request = new TransferableItem(currentUser(), batchId, source, file, destination, folder, move, overwrite);
        transferService.request(request);
    }

    private void queueFiles(TransferableItem[] items) {
        transferService.request(items);
    }

    private void checkDestRepo(String user, Repository destRepo) throws AccessDeniedException {
        if (!user.equals(destRepo.getUserId())) {
            throw new AccessDeniedException("Operation not allowed");
        }
        if (destRepo.isReadOnly()) {
            throw new AccessDeniedException("Destination is read-only");
        }
    }

    private List<Link> getImageLinks(InputBase requestInput) throws IOException {
        List<Link> imageLinks = new ArrayList<>();
        if(requestInput instanceof Link imageLink){
            imageLinks.add(imageLink);
        } else if(requestInput instanceof InputValueNoObject<?> arrImages){
            // only accept arrays of images with links for source inputs at this time
            if (arrImages.getValue() instanceof String) {
                Link link = new Link();
                String val = (String) arrImages.getValue();
                link.setHref(val);
                link.setType("folder");
                imageLinks.add(link);
            } else {
                List<InputValueNoObject<Link>> imagesInputArr = (List<InputValueNoObject<Link>>) arrImages.getValue();
                for (var imageVal : imagesInputArr) {
                    imageLinks.add(imageVal.getValue());
                }
            }
        } else {
            throw new IOException("Invalid format for Source input");
        }
        return imageLinks;
    }

    private boolean isPathAccessible(String path) {
        Path filePath;
        if (path.startsWith("file:")) {
            filePath = Paths.get(URI.create(path));
        } else {
            filePath = Paths.get(path);
        }
        if (FileUtilities.isPathAccessible(filePath)) {
            return true;
        }
        return false;
    }
    private void checkPath(String path) {
        IllegalArgumentException ex = null;
        if (StringUtilities.isNullOrEmpty(path)) {
            ex = new IllegalArgumentException("Empty path");
        }
    }

    public String toFilePath(String input) {
        if (input.startsWith("file:/")) {
            URI uri = URI.create(input);
            return Paths.get(uri).toString();
        } else {
            return input;
        }
    }

    public String normalizeUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.endsWith("/") && uri.length() > 1
                ? uri.substring(0, uri.length() - 1)
                : uri;
    }

    @RequestMapping(value = "/jobs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobs(HttpServletRequest request) {
        JobsInfo jobsInfo = new JobsInfo();
        final List<StatusInfo> jobInfosList = new ArrayList<>();
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
                final StatusInfo info = getJobStatus(request.getRequestURI() + summary.getId(), summary);
                jobInfosList.add(info);
            }
            jobsInfo.setJobs(jobInfosList);
        }
        return ResponseEntity.ok(jobsInfo);
    }

    @RequestMapping(value = "/jobs/{id:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobInfo(@PathVariable("id") String jobId,
                                        HttpServletRequest request) {
        StatusInfo jobStatus = getJobStatus(request.getRequestURI(), Long.parseLong(jobId));
        return ResponseEntity.ok(jobStatus);
    }

    @RequestMapping(value = "/jobs/{id:.+}/results", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getJobResults(@PathVariable("id") String jobId) {
        try {
            final ExecutionJob job = executionJobProvider.get(Long.parseLong(jobId));
            if (job == null) {
                return ResponseEntity.notFound().build();
            }
            if (job.getExecutionStatus() != ExecutionStatus.DONE) {
                return ResponseEntity.noContent().build();
            }
            final String batchId = job.getBatchId();
            final List<String> terminalTasksOutputs = new ArrayList<>();
            Map<String, ResultOutput> resultOutputs = new LinkedHashMap<>();
            if (StringUtilities.isNullOrEmpty(batchId)) {
                try (Stream<ExecutionTask> terminalTasks = job.getTasks().stream()
                                                         .filter(t -> TaskUtilities.isTerminalTask(t) && t.getExecutionStatus() == ExecutionStatus.DONE)) {
                    terminalTasks.forEach(t -> {
                        if (t.getInstanceTargetOutput() != null) {
                            terminalTasksOutputs.addAll(t.getInstanceTargetOutput().values());
                        }
                    });
                }
            } else {
                final List<ExecutionJob> jobs = executionJobProvider.list(new ArrayList<String>() {{
                    add(batchId);
                }});
                for (ExecutionJob eJob : jobs) {
                    List<ExecutionTask> tasks = eJob.getTasks().stream()
                            .filter(t -> TaskUtilities.isTerminalTask(t) && t.getExecutionStatus() == ExecutionStatus.DONE).collect(Collectors.toList());
                    for(ExecutionTask task: tasks) {
                        for (String output: task.getInstanceTargetOutput().values()) {
                            ResultOutput resOut = new ResultOutput();
                            Variable var = task.getOutputParameterValues().stream()
                                    .filter(v -> task.getInstanceTargetOutput().containsValue(v.getValue()))
                                    .findFirst().orElse(null);
                            String format = getResultFormat(output);
                            resOut.setType(StringUtilities.isNullOrEmpty(format) ? "folder" : format);
                            String hrefVal = output;
                            if (hrefVal != null && Paths.get(hrefVal).getFileName().toString().endsWith("stac_items.json")) {

//                                List<String> lines = Files.readAllLines(Paths.get(hrefVal));
//                                if (lines.size() > 0) {
//                                    hrefVal = lines.get(0);
//                                }
                            }
                            resOut.setHref(hrefVal);
                            if (var != null) {
                                TaoComponent comp = TaskUtilities.getComponentFor(task);
                                TargetDescriptor tgtDescr = comp.getTargets().stream()
                                        .filter(i -> var.getKey().equals(i.getName()))
                                        .findFirst()
                                        .orElse(null);
                                if (tgtDescr != null) {
                                    resultOutputs.put(tgtDescr.getName(), resOut);
                                } else {
                                    resultOutputs.put(task.getId() + "_" + var.getKey(), resOut);
                                }
                            }
                        }
                    }
//                    try (Stream<ExecutionTask> terminalTasks = eJob.getTasks().stream()
//                                                                   .filter(t -> TaskUtilities.isTerminalTask(t) && t.getExecutionStatus() == ExecutionStatus.DONE)) {
//                        terminalTasks.forEach(t -> {
//                            if (t.getInstanceTargetOutput() != null) {
//                                terminalTasksOutputs.addAll(t.getInstanceTargetOutput().values());
//                            }
//                        });
//                    }
                }
            }
//            JobResultsResponse resultsResponse = new JobResultsResponse();
//            resultsResponse.setJobId(jobId);
//            resultsResponse.setOutputs(resultOutputs);
            return ResponseEntity.ok(resultOutputs);
        } catch (Throwable e) {
            return ResponseEntity.internalServerError().build();
        }
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

    private StatusInfo getJobStatus(String jobUri, Long jobId) {
        final ExecutionJobSummary summary = orchestratorService.getJobInfo(jobId);
        StatusInfo info;
        if (summary != null) {
            info = getJobStatus(jobUri, summary);
        } else {
            info = new StatusInfo();
            info.setJobID(jobId.toString());
            info.setStatus(StatusCode.DISMISSED);
        }

        // Set the ID of the real/original workflow
        WorkflowDescriptor workflow = workflowService.findById(summary.getWorkflowId());
        Long workflowId = workflow.getCreatedFromWorkflowId() == null ? workflow.getId() :
                workflow.getCreatedFromWorkflowId();
        info.setProcessID(String.valueOf(workflowId));

        return info;
    }
    private StatusInfo getJobStatus(String jobUri, ExecutionJobSummary summary) {
        final StatusInfo info = new StatusInfo();
        WorkflowDescriptor workflow = workflowService.findById(summary.getWorkflowId());
        Long workflowId = workflow.getCreatedFromWorkflowId() == null ? workflow.getId() :
                workflow.getCreatedFromWorkflowId();
        info.setProcessID(String.valueOf(workflowId));
        info.setJobID(String.valueOf(summary.getId()));
        StatusCode status;
        double progress = 0;
        switch (summary.getJobStatus()) {
            case ExecutionStatus.UNDETERMINED:
            case ExecutionStatus.QUEUED_ACTIVE:
                status = StatusCode.ACCEPTED;
                break;
            case ExecutionStatus.RUNNING:
            case ExecutionStatus.PENDING_FINALISATION:
                status = StatusCode.RUNNING;
                break;
            case ExecutionStatus.DONE:
                status = StatusCode.SUCCESSFUL;
                progress = 100;
                break;
            case ExecutionStatus.CANCELLED:
            case ExecutionStatus.FAILED:
                status = StatusCode.FAILED;
                progress = 100;
                break;
            default:
                status = StatusCode.DISMISSED;
                break;
        }
        info.setStatus(status);
        LocalDateTime jobStart = summary.getJobStart();
        LocalDateTime jobEnd = summary.getJobEnd();
        LocalDateTime jobUpdated = LocalDateTime.now(ZoneOffset.UTC);
        if (jobEnd != null && status != StatusCode.RUNNING) {
            jobUpdated = jobEnd.atOffset(ZoneOffset.UTC).toLocalDateTime();
        }
        info.setCreated(jobStart == null ? null : jobStart.atOffset(ZoneOffset.UTC).toLocalDateTime());
        info.setStarted(jobStart == null ? null : jobStart.atOffset(ZoneOffset.UTC).toLocalDateTime());
        info.setUpdated(jobUpdated);
        final List<ExecutionTaskSummary> tasks = summary.getTaskSummaries();
        if (status == StatusCode.RUNNING) {
            for (int i = 0; i < tasks.size(); i++) {
                double percentageTask = tasks.get(i).getPercentComplete();
                if (percentageTask < 0.0)
                    percentageTask = 0.0;
                if (percentageTask > 100.0) {
                    percentageTask = 100.0;
                }
                progress += percentageTask;
            }
            progress = progress / tasks.size();
        }
        info.setProgress((int) progress);
        info.setLinks(new ArrayList<>() {{
            Link linkStatus = new Link();
            linkStatus.setTitle("Job status");
            linkStatus.setType(MediaType.APPLICATION_JSON_VALUE);
            linkStatus.setRel("self");
            linkStatus.setHref(jobUri);
            add(linkStatus);

            Link linkResults = new Link();
            linkResults.setTitle("Job results (when status=successful)");
            linkResults.setType(MediaType.APPLICATION_JSON_VALUE);
            linkResults.setRel("self");
            linkResults.setHref(jobUri + "/results");
            add(linkResults);

        }});
        return info;
    }

    private String getResultFormat(String path) {
        String ext = FileUtilities.getExtension(path);
        if (!StringUtilities.isNullOrEmpty(ext)) {
            return EXTENSION_TO_MIME.get(ext);
        }
        return null;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String handleStacProductUrl(String itemUrl) throws java.lang.Exception {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(itemUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            // throw new RuntimeException("STAC item request failed: HTTP " + response.statusCode());
            return null;
        }

        JsonNode item = MAPPER.readTree(response.body());
        JsonNode assets = item.path("assets");
        if (assets.isMissingNode()) {
            //throw new RuntimeException("No assets found.");
            return null;
        }

        JsonNode metadataAsset = assets.path("metadata");
        if (metadataAsset.isMissingNode()) {
            //throw new RuntimeException("No metadata asset found.");
            return null;
        }

        String href = metadataAsset.path("href").asText();
        if (href.isEmpty()) {
            //throw new RuntimeException("Metadata asset has no href.");
            return null;
        }
        return href;

        //String baseUrl = getBaseUrl(item); // computed from "self" rel
        //return makeAbsolute(baseUrl, href);
    }

    private static String getBaseUrl(JsonNode item) {
        JsonNode links = item.path("links");
        if (links.isArray()) {
            for (JsonNode link : links) {
                if ("self".equals(link.path("rel").asText())) {
                    String selfUrl = link.path("href").asText();
                    return extractRoot(selfUrl);
                }
            }
        }
        return "";
    }
    private static String extractRoot(String selfUrl) {
        int idx = selfUrl.indexOf("/collections/");
        if (idx > 0) {
            return selfUrl.substring(0, idx);
        }
        return selfUrl;
    }

    private static String makeAbsolute(String baseUrl, String relativePath) {
        if (relativePath.startsWith("http")) {
            return relativePath;
        }
        if (relativePath.startsWith("/")) {
            return baseUrl + relativePath;
        }
        return baseUrl + "/" + relativePath;
    }
}
