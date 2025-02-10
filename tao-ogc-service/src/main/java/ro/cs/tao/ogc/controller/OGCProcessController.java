package ro.cs.tao.ogc.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.*;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.param.JavaType;
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
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/process")
@Tag(name = "OGC Process API Endpoint", description = "Endpoint compliant with OGC Process API for exposing TAO workflows")
public class OGCProcessController extends BaseController {
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private RepositoryProvider repositoryProvider;
    @Autowired
    private TransferService transferService;
    @Autowired
    private OrchestratorService orchestratorService;
    @Autowired
    private ExecutionJobProvider executionJobProvider;



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
                Link linkExecution = new Link();
                linkExecution.setTitle("Process execution");
                linkExecution.setType(MediaType.APPLICATION_JSON_VALUE);
                linkExecution.setRel("http://www.opengis.net/def/rel/ogc/1.0/execute");
                linkExecution.setHref(request.getRequestURI() + "/" + w.getId() + "/execution");
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
            final List<WorkflowNodeDescriptor> nodes = workflow.getNodes();
            // Describe inputs
            final List<InputDescription<?>> inputs = new ArrayList<>();
            final List<WorkflowNodeDescriptor> firstLevelNodes = WorkflowUtilities.findFirstLevel(nodes);
            for (WorkflowNodeDescriptor node : nodes) {
                final TaoComponent component = WorkflowUtilities.findComponent(node);
                // Parameters that are required and have no default value should be inputs
                // Parameters with default Values should be inputs as well
                if (component instanceof ProcessingComponent) {
                    final Set<ParameterDescriptor> descriptors = ((ProcessingComponent) component).getParameterDescriptors();
                    for (ParameterDescriptor descriptor : descriptors) {
                        if (descriptor.isNotNull()){// && StringUtilities.isNullOrEmpty(descriptor.getDefaultValue())) {
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
                                       descriptor.javaType() == JavaType.STRING) {
                                input = InputDescription.of(Type.STRING);
                                StringDescriptor schema = new StringDescriptor();
                                input.setSchema(schema);
                                schema.setType(Type.STRING);
                                if(descriptor.javaType() == JavaType.DATE){
                                    schema.setFormat("yyyy-MM-ddTHH:mm:ss");
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
                            input.setMinOccurs(1);
                            input.setTitle(descriptor.getId());
                            input.setDescription(descriptor.getName());
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

    @RequestMapping(value = "/processes/{id:.+}/execution", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> execute(@PathVariable("id") String processId, @RequestBody ExecutionRequest request) {
        Long jobId;
        try{
            final WorkflowDescriptor workflow = workflowService.findById(Long.parseLong(processId));
            final List<WorkflowNodeDescriptor> nodes = workflow.getNodes();
            final Input inputs = request.getInputs();
            //outputs
            final List<WorkflowNodeDescriptor> terminalNodes = WorkflowUtilities.findTerminals(nodes);
            final List<Output> outputs = request.getOutputs();
            for (WorkflowNodeDescriptor node : terminalNodes) {
                final List<TargetDescriptor> targets = WorkflowUtilities.findComponent(node).getTargets();
                for (TargetDescriptor target : targets) {
                    Output output = getRequestOutput(node.getName(), outputs);
                    DataDescriptor targetDataDescriptor = target.getDataDescriptor();
                    // no output processing at this time, transmission mode is by reference as per TAO implementation
                }
            }
            String user = currentUser();
            List<Repository> userRepos = repositoryProvider.getByUser(currentUser());
            if (userRepos.isEmpty()) {
                throw new IOException("Workspaces do not exist");
            }
            Repository localRepo = userRepos.stream()
                    .filter(repo -> repo.getType() == RepositoryType.LOCAL)
                    .findFirst().orElseThrow(() -> new IOException("Local Workspace does not exist"));
            checkDestRepo(user, localRepo);
            for (WorkflowNodeDescriptor node : nodes) {
                //parameters
                final TaoComponent component = WorkflowUtilities.findComponent(node);
                setParametersFromInputs(component, inputs);
                //sources
                final List<SourceDescriptor> sources = component.getSources();
                final Set<ComponentLink> incomingLinks = node.getIncomingLinks();
                for (SourceDescriptor source : sources) {
                    // if there is already a link pass the setting of the Source Input
                    if(incomingLinks.stream().anyMatch(link -> link.getOutput().equals(source))){
                        continue;
                    }
                    List<String> localPaths = new ArrayList<>();
                    InputBase requestInput = getRequestInput(source.getName(), inputs);
                    List<Link> imageLinks = getImageLinks(requestInput);
                    List<String> imagesPaths = imageLinks.stream().map(Link::getHref).toList();
                    if (imagesPaths.isEmpty())
                        throw new IllegalArgumentException("Source Input is invalid. Image link or Array of Image Links are required.");
                    for (var imagePath : imagesPaths) {
                        checkPath(imagePath);
                        // refactor using StorageService information to know if it is a folder
                        boolean isFolder = imageLinks.stream().filter(link -> link.getHref().equals(imagePath)).findFirst().get().getType().equals("folder");
                        Repository sourceRepo = userRepos.stream()
                                .filter(repo -> imagePath.startsWith(repo.getUrlPrefix()))
                                .findFirst()
                                .orElseThrow(() -> new IOException("Workspace does not exist for path " + imagePath));
                        if (!user.equals(sourceRepo.getUserId())) {
                            throw new AccessDeniedException("Operation not allowed");
                        }
                        final RepositoryBean bean = ServiceTransformUtils.toBean(sourceRepo);
                        Future<?> emptyFuture = asyncExecute(() -> {
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
                            return String.format("Transfer queued (%d files, %d bytes)", count, size);
                        }, message -> Messaging.send(user, Topic.INFORMATION.getCategory(), message), null);
                        emptyFuture.get();
                    }
                    final StorageService localService = getRepositoryService(localRepo);
                    //wait for all the downloads before proceeding, very BAD code, to refactor
                    for(String path : localPaths){
                        while(!localService.exists(localRepo.resolve(path))){
                            Thread.sleep(5000);
                        }
                    }
                    final DataSourceComponent virtualProductSet = createVirtualComponent(localPaths, node.getName()+"_"+source.getName(), currentPrincipal());
                    //sourceId might be required
                    ComponentLink newLink = new ComponentLink();
                    newLink.setOutput(source);
                    newLink.setInput(virtualProductSet.getTargets().get(0));
                    node.getIncomingLinks().add(newLink);
                }
            }
            jobId = orchestratorService.startWorkflow(getExecutionRequest(workflow, nodes));
        }
        catch (Throwable e) {
            Exception exception = new Exception();
            exception.setTitle(e.getClass().getSimpleName());
            exception.setDetail(e.getMessage());
            exception.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.internalServerError().body(exception);
        }
        return prepareResult(jobId);
    }

    private ro.cs.tao.execution.model.ExecutionRequest getExecutionRequest(WorkflowDescriptor workflow, List<WorkflowNodeDescriptor> nodes) {
        ro.cs.tao.execution.model.ExecutionRequest executionRequest = new ro.cs.tao.execution.model.ExecutionRequest();
        executionRequest.setWorkflowId(workflow.getId());
        executionRequest.setName(workflow.getName());
        executionRequest.setJobType("EXECUTION");
        executionRequest.setEnvironment("DEFAULT");
        executionRequest.setInputs(new HashMap<>());
        Map<String, Map<String, String>> parametersMap = new HashMap<>();
        executionRequest.setParameters(parametersMap);
        for(WorkflowNodeDescriptor nodeDescriptor : nodes){
            String name = nodeDescriptor.getName();
            Map<String, String> parameterMap = nodeDescriptor.getCustomValues().stream()
                    .collect(Collectors.groupingBy(ParameterValue::getParameterName, Collectors.mapping(ParameterValue::getParameterValue, Collectors.joining())));
            parametersMap.put(name, parameterMap);
        }
        return executionRequest;
    }

    private DataSourceComponent createVirtualComponent(List<String> productPaths, String label, Principal principal){
        if (productPaths == null || productPaths.isEmpty()) {
            throw new IllegalArgumentException("Product list is empty");
        }
        if (principal == null) {
            throw new IllegalArgumentException("Invalid principal (null)");
        }
        final Repository repository = this.repositoryProvider.getByUserAndName(principal.getName(), "LOCAL");
        final List<String> list = productPaths.stream()
                .map(n -> Paths.get(repository.resolve(n)).toUri().toString())
                .collect(Collectors.toList());
        DataSourceComponent userDSC = new DataSourceComponent("n/a", "Local");
        userDSC.setFetchMode(FetchMode.OVERWRITE);
        userDSC.setVersion("1.0");
        userDSC.setDescription(label + " (local product set)");
        userDSC.setAuthors("TAO Team");
        userDSC.setCopyright("(C) TAO Team");
        userDSC.setNodeAffinity(NodeAffinity.Any);
        LocalDateTime time = LocalDateTime.now();
        final String newId = String.join("-", "product", "set", principal.getName(),
                time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        userDSC.setId(newId);
        userDSC.getSources().forEach(s -> s.setParentId(newId));
        userDSC.getTargets().forEach(t -> t.setParentId(newId));
        userDSC.setLabel(label != null && !label.isEmpty() ? label : newId);
        SourceDescriptor sourceDescriptor = userDSC.getSources().stream().filter(s -> s.getName().equals(DataSourceComponent.QUERY_PARAMETER)).findFirst().get();
        sourceDescriptor.getDataDescriptor().setSensorType(SensorType.UNKNOWN);
        sourceDescriptor.getDataDescriptor().setLocation(String.join(",", list));
        sourceDescriptor.setCardinality(productPaths.size());
        TargetDescriptor targetDescriptor = userDSC.getTargets().get(0);
        targetDescriptor.setId(UUID.randomUUID().toString());
        targetDescriptor.setParentId(newId);
        targetDescriptor.setCardinality(productPaths.size());
        userDSC.setSystem(false);
        return userDSC;
    }

    private void setParametersFromInputs(TaoComponent component, Input inputs) throws PersistenceException {
        if (component instanceof ProcessingComponent) {
            final Set<ParameterDescriptor> descriptors = ((ProcessingComponent) component).getParameterDescriptors();
            for (ParameterDescriptor descriptor : descriptors) {
                if (descriptor.isNotNull()) {
                    InputBase requestInput= getRequestInput(descriptor.getName(), inputs);
                    if(requestInput instanceof QualifiedInputValue<?> requestQualifiedValue){
                        descriptor.setDefaultValue(requestQualifiedValue.getValue().toString());
                    } else if(requestInput instanceof InputValueNoObject<?> requestInputValueNoObject){
                        descriptor.setDefaultValue(requestInputValueNoObject.getValue().toString());
                    } else if (requestInput instanceof Link requestLink) {
                        // href is required, the rest are not
                        descriptor.setDefaultValue(requestLink.getHref());
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
            throw new IllegalArgumentException("No node input corresponds to this request input");
    }

    private Output getRequestOutput(String nodeOutputName, List<Output> outputs){
        return outputs.stream()
                .filter(output -> output.getName().equals(nodeOutputName))
                .findFirst()
                .orElse(null);
    }

    private static StorageService getRepositoryService(Repository workspace) {
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
            List<InputValueNoObject<Link>> imagesInputArr = (List<InputValueNoObject<Link>>) arrImages.getValue();
            for(var imageVal : imagesInputArr){
                imageLinks.add(imageVal.getValue());
            }
        } else {
            throw new IOException("Invalid format for Source input");
        }
        return imageLinks;
    }

    private void checkPath(String path) {
        IllegalArgumentException ex = null;
        if (StringUtilities.isNullOrEmpty(path)) {
            ex = new IllegalArgumentException("Empty path");
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
            if (StringUtilities.isNullOrEmpty(batchId)) {
                terminalTasksOutputs.addAll(job.getTasks().stream()
                                               .filter(t -> TaskUtilities.isTerminalTask(t) && t.getExecutionStatus() == ExecutionStatus.DONE)
                                               .map(ExecutionTask::getInstanceTargetOutput)
                                               .collect(Collectors.toList()));
            } else {
                final List<ExecutionJob> jobs = executionJobProvider.list(new ArrayList<String>() {{
                    add(batchId);
                }});
                for (ExecutionJob eJob : jobs) {
                    terminalTasksOutputs.addAll(eJob.getTasks().stream()
                                                   .filter(t -> TaskUtilities.isTerminalTask(t) && t.getExecutionStatus() == ExecutionStatus.DONE)
                                                   .map(ExecutionTask::getInstanceTargetOutput)
                                                   .collect(Collectors.toList()));
                }
            }
            return ResponseEntity.ok(terminalTasksOutputs);
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
}
