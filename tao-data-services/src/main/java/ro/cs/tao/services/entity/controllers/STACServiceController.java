package ro.cs.tao.services.entity.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.NodeAffinity;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.WebServiceAuthentication;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.datasource.stac.STACSource;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerType;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.repository.SourceDescriptorRepository;
import ro.cs.tao.persistence.repository.TargetDescriptorRepository;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.WebServiceBean;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.interfaces.WebServiceAuthenticationService;
import ro.cs.tao.stac.core.STACClient;
import ro.cs.tao.stac.core.model.Collection;
import ro.cs.tao.stac.core.model.CollectionList;
import ro.cs.tao.utils.StringUtilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/stac")
@Tag(name = "STAC Components", description = "Operations related to STAC components management")
public class STACServiceController extends BaseController {
    @Autowired
    private ContainerService containerService;
    @Autowired
    private WebServiceAuthenticationService webServiceAuthenticationService;
    @Autowired
    private DataSourceComponentService dataSourceComponentService;
    @Autowired
    private SourceDescriptorRepository sourceDescriptorRepository;
    @Autowired
    private TargetDescriptorRepository targetDescriptorRepository;

    private final Logger logger = Logger.getLogger(STACServiceController.class.getName());

    @RequestMapping(value = {"/list"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list() {
        try {
            final List<Container> containers = containerService.listByType(ContainerType.STAC);
            final List<WebServiceBean> results = new ArrayList<>();
            for (Container container : containers) {
                results.add(ServiceTransformUtils.toBean(container,
                                                         webServiceAuthenticationService.findById(container.getId())));
            }
            return prepareResult(results);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Creates a new STAC component (client). The new component will act as a TAO data source.
     * @param bean  Structure defining connection and authentication information for the remote STAC service
     */
    @RequestMapping(value = "/", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> createSTACService(@RequestBody WebServiceBean bean) {
        try {
            if (bean == null) {
                throw new IllegalArgumentException("Empty body");
            }
            if (bean.getType() != ContainerType.STAC) {
                throw new IllegalArgumentException("Wrong container type");
            }
            if (StringUtilities.isNullOrEmpty(bean.getId())) {
                bean.setId(UUID.randomUUID().toString());
            }
            Container container = ServiceTransformUtils.getContainerPart(bean);
            if (StringUtilities.isNullOrEmpty(container.getId())) {
                container.setId(UUID.randomUUID().toString());
            }
            WebServiceAuthentication auth = ServiceTransformUtils.getAuthenticationPart(bean);
            container.setOwnerId(currentUser());
            container = containerService.save(container);
            auth.setId(container.getId());
            auth = webServiceAuthenticationService.save(auth);
            final List<Application> applications = container.getApplications();
            if (applications != null) { // Create the corresponding data sources
                DataSourceManager.getInstance().registerDataSource(new STACSource(container.getName()));
                for (Application application : applications) {
                    DataSourceComponent component = new DataSourceComponent(application.getPath(), container.getName());
                    component.setSystem(false);
                    component.setUserName(currentUser());
                    component.setFetchMode(FetchMode.OVERWRITE);
                    component.setVersion("1.0");
                    component.setDescription(bean.getDescription());
                    component.setAuthors("AVL Team");
                    component.setCopyright("(C) AVL Team");
                    component.setNodeAffinity(NodeAffinity.Any);
                    final String newId = String.join("-", "stac", "service", container.getName(), application.getPath(), currentUser());
                    component.setId(newId);
                    component.getSources().forEach(s -> s.setParentId(newId));
                    component.getTargets().forEach(t -> t.setParentId(newId));
                    component.setLabel(bean.getName() + ", collection " + application.getName());
                    SourceDescriptor sourceDescriptor = component.getSources().stream().filter(s -> s.getName().equals(DataSourceComponent.QUERY_PARAMETER)).findFirst().get();
                    sourceDescriptor.getDataDescriptor().setSensorType(SensorType.UNKNOWN);
                    sourceDescriptor.setCardinality(0);
                    sourceDescriptorRepository.save(sourceDescriptor);
                    TargetDescriptor targetDescriptor = component.getTargets().get(0);
                    targetDescriptor.setId(UUID.randomUUID().toString());
                    targetDescriptor.setParentId(newId);
                    targetDescriptor.setCardinality(0);
                    targetDescriptor.addConstraint("Same cardinality");
                    targetDescriptorRepository.save(targetDescriptor);
                    component = dataSourceComponentService.save(component);
                    dataSourceComponentService.tag(component.getId(), new ArrayList<>() {{
                        add(bean.getName());
                    }});
                    logger.info(String.format("STAC Data source component for [%s, %s] created",
                                              bean.getName(), application.getPath()));
                }
            }
            return prepareResult(ServiceTransformUtils.toBean(container, auth));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Updates a STAC component definition.
     * @param bean  Structure defining connection and authentication information for the remote STAC service
     */
    @RequestMapping(value = "/", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateSTACService(@RequestBody WebServiceBean bean) {
        try {
            if (bean == null) {
                throw new IllegalArgumentException("Empty body");
            }
            if (bean.getType() != ContainerType.STAC) {
                throw new IllegalArgumentException("Wrong container type");
            }
            if (StringUtilities.isNullOrEmpty(bean.getId())) {
                throw new IllegalArgumentException("Invalid service id");
            }
            Container container = ServiceTransformUtils.getContainerPart(bean);
            if (container.getTag() == null) {
                container.setTag("STAC");
            }
            WebServiceAuthentication auth = ServiceTransformUtils.getAuthenticationPart(bean);
            container = containerService.update(container);
            auth = webServiceAuthenticationService.update(auth);
            List<DataSourceComponent> existing = dataSourceComponentService.getBySource(container.getName());
            if (existing != null) {
                for (DataSourceComponent component : existing) {
                    dataSourceComponentService.delete(component.getId());
                }
            }
            final List<Application> applications = container.getApplications();
            if (applications != null && !applications.isEmpty()) { // Create the corresponding data sources
                DataSourceManager.getInstance().registerDataSource(new STACSource(container.getName()));
                for (Application application : applications) {
                    DataSourceComponent component = new DataSourceComponent(application.getPath(), container.getName());
                    component.setSystem(false);
                    component.setUserName(currentUser());
                    component.setFetchMode(FetchMode.OVERWRITE);
                    component.setVersion("1.0");
                    component.setDescription(bean.getDescription());
                    component.setAuthors("AVL Team");
                    component.setCopyright("(C) AVL Team");
                    component.setNodeAffinity(NodeAffinity.Any);
                    final String newId = String.join("-", "stac", "service", container.getName(), application.getPath(), currentUser());
                    component.setId(newId);
                    component.getSources().forEach(s -> s.setParentId(newId));
                    component.getTargets().forEach(t -> t.setParentId(newId));
                    component.setLabel(bean.getName() + ", collection " + application.getName());
                    SourceDescriptor sourceDescriptor = component.getSources().stream().filter(s -> s.getName().equals(DataSourceComponent.QUERY_PARAMETER)).findFirst().get();
                    sourceDescriptor.getDataDescriptor().setSensorType(SensorType.UNKNOWN);
                    sourceDescriptor.setCardinality(0);
                    sourceDescriptorRepository.save(sourceDescriptor);
                    TargetDescriptor targetDescriptor = component.getTargets().get(0);
                    targetDescriptor.setId(UUID.randomUUID().toString());
                    targetDescriptor.setParentId(newId);
                    targetDescriptor.setCardinality(0);
                    targetDescriptor.addConstraint("Same cardinality");
                    targetDescriptorRepository.save(targetDescriptor);
                    component = dataSourceComponentService.save(component);
                    dataSourceComponentService.tag(component.getId(), new ArrayList<>() {{
                        add(bean.getName());
                    }});
                    logger.info(String.format("STAC Data source component for [%s, %s] created",
                                              bean.getName(), application.getPath()));
                }
            } else {
                DataSourceManager.getInstance().unregisterDataSource(container.getName());
            }
            return prepareResult(ServiceTransformUtils.toBean(container, auth));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Removes a STAC component.
     * @param id    The component identifier
     */
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteSTACService(@PathVariable("id") String id) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            webServiceAuthenticationService.delete(id);
            Container service = containerService.findById(id);
            containerService.delete(id);
            List<DataSourceComponent> existing = dataSourceComponentService.getBySource(service.getName());
            if (existing != null) {
                for (DataSourceComponent component : existing) {
                    dataSourceComponentService.delete(component.getId());
                }
            }
            DataSourceManager.getInstance().unregisterDataSource(service.getName());
            responseEntity = prepareResult("STAC service deleted", ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Inspects the remote STAC service for the available collections
     * @param endpoint  The remote STAC endpoint (URL)
     * @param authentication    If needed, authentication information for the remote service
     */
    @RequestMapping(value = "/collections", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> inspectSTACService(@RequestParam(name = "remoteAddress") String endpoint,
                                                @RequestParam(name = "authentication", required = false) String authentication) {
        try {
            try {
                final URL url = new URL(endpoint);
            } catch (MalformedURLException mex) {
                throw new IllegalArgumentException("[remoteAddress] Malformed URL", mex);
            }
            final WebServiceAuthentication auth = new ObjectMapper().readerFor(WebServiceAuthentication.class).readValue(authentication);
            final STACClient client = new STACClient(endpoint, auth);
            final CollectionList collectionList = client.listCollections();
            final List<Collection> collections = collectionList.getCollections();
            final List<Application> applications = new ArrayList<>();
            if (collections != null) {
                for (Collection collection : collections) {
                    Application application = new Application();
                    application.setName(collection.getTitle());
                    application.setPath(collection.getId());
                    applications.add(application);
                }
            }
            final Container container = new Container();
            container.setName("STAC Service");
            container.setType(ContainerType.STAC);
            container.setApplications(applications);
            return prepareResult(container);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
