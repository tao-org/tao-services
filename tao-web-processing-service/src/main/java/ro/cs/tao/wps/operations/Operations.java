package ro.cs.tao.wps.operations;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.schema.*;
import com.bc.wps.api.utils.CapabilitiesBuilder;
import com.bc.wps.api.utils.WpsTypeConverter;
import org.springframework.web.util.UriComponentsBuilder;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.wps.controllers.WPSController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Sabine
 */
public class Operations {

    private final WpsRequestContext wpsRequestContext;
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final WebProcessingService webProcessingService;

    public Operations(WpsRequestContext context, WebProcessingService webProcessingService) throws IOException {
        this.wpsRequestContext = context;
        this.webProcessingService = webProcessingService;
    }

    public Capabilities getCapabilities() throws IOException, URISyntaxException, PersistenceException {
        logger.finest("GetCapabilities for user " + wpsRequestContext.getUserName());
        return CapabilitiesBuilder.create()
                    .withOperationsMetadata(getOperationsMetadata())
                    .withServiceIdentification(getServiceIdentification())
                    .withServiceProvider(getServiceProvider())
                    .withProcessOfferings(getProcessOfferings())
                    .withLanguages(getLanguages())
                    .build();
    }

    private OperationsMetadata getOperationsMetadata() {
        final OperationsMetadata operationsMetadata = new OperationsMetadata();
        final String requestUrl = wpsRequestContext.getServerContext().getRequestUrl();

        Operation getCapabilitiesOperation = new Operation();
        getCapabilitiesOperation.setName("GetCapabilities");
        DCP getCapabilitiesDcp = getGetDcp(requestUrl+"?");
        getCapabilitiesOperation.getDCP().add(getCapabilitiesDcp);
        operationsMetadata.getOperation().add(getCapabilitiesOperation);

        Operation describeProcessOperation = new Operation();
        describeProcessOperation.setName("DescribeProcess");
        DCP describeProcessDcp = getGetDcp(requestUrl +"?");
        describeProcessOperation.getDCP().add(describeProcessDcp);
        operationsMetadata.getOperation().add(describeProcessOperation);

        Operation executeOperation = new Operation();
        executeOperation.setName("Execute");
        DCP executeDcp = getPostDcp(requestUrl);
        executeOperation.getDCP().add(executeDcp);
        operationsMetadata.getOperation().add(executeOperation);

        Operation getStatusOperation = new Operation();
        getStatusOperation.setName("GetStatus");
        DCP getStatusDcp = getGetDcp(requestUrl+"?");
        getStatusOperation.getDCP().add(getStatusDcp);
        operationsMetadata.getOperation().add(getStatusOperation);

        return operationsMetadata;
    }

    private ServiceProvider getServiceProvider() {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setProviderName(getPropertyValue("company.name"));

        OnlineResourceType siteUrl = new OnlineResourceType();
        siteUrl.setHref(getPropertyValue("company.website"));
        serviceProvider.setProviderSite(siteUrl);

        ResponsiblePartySubsetType contact = new ResponsiblePartySubsetType();
        contact.setIndividualName(getPropertyValue("project.manager.name"));
        contact.setPositionName(getPropertyValue("project.manager.position.name"));

        ContactType contactInfo = new ContactType();

        TelephoneType phones = new TelephoneType();
        phones.getVoice().add(getPropertyValue("company.phone.number"));
        phones.getFacsimile().add(getPropertyValue("company.fax.number"));
        contactInfo.setPhone(phones);

        AddressType address = new AddressType();
        address.getDeliveryPoint().add(getPropertyValue("company.address"));
        address.setCity(getPropertyValue("company.city"));
        address.setAdministrativeArea(getPropertyValue("company.administrative.area"));
        address.setPostalCode(getPropertyValue("company.post.code"));
        address.setCountry(getPropertyValue("company.country"));
        address.getElectronicMailAddress().add(getPropertyValue("company.email.address"));
        contactInfo.setAddress(address);

        contactInfo.setOnlineResource(siteUrl);
        contactInfo.setHoursOfService(getPropertyValue("company.service.hours"));
        contactInfo.setContactInstructions(getPropertyValue("company.contact.instruction"));

        contact.setContactInfo(contactInfo);

        CodeType role = new CodeType();
        role.setValue("PointOfContact");
        contact.setRole(role);
        serviceProvider.setServiceContact(contact);

        return serviceProvider;
    }

    private ProcessOfferings getProcessOfferings() throws PersistenceException {
        ProcessOfferings processOfferings = new ProcessOfferings();
        List<ProcessBriefType> taoProcesses = getTaoProcesses();
        processOfferings.getProcess().addAll(taoProcesses);
        return processOfferings;
    }

    private ServiceIdentification getServiceIdentification() {
        ServiceIdentification serviceIdentification = new ServiceIdentification();
        LanguageStringType title = new LanguageStringType();
        title.setValue(getPropertyValue("wps.service.id"));
        serviceIdentification.setTitle(title);

        LanguageStringType abstractText = new LanguageStringType();
        abstractText.setValue(getPropertyValue("wps.service.abstract"));
        serviceIdentification.setAbstract(abstractText);

        CodeType serviceType = new CodeType();
        serviceType.setValue(getPropertyValue("wps.service.type"));
        serviceIdentification.setServiceType(serviceType);

        serviceIdentification.getServiceTypeVersion().add(0, getPropertyValue("wps.version"));
        return serviceIdentification;
    }

    private Languages getLanguages() {
        Languages languages = new Languages();

        Languages.Default defaultLanguage = new Languages.Default();
        defaultLanguage.setLanguage(getPropertyValue("wps.default.lang"));
        languages.setDefault(defaultLanguage);

        LanguagesType languageType = new LanguagesType();
        languageType.getLanguage().add(0, getPropertyValue("wps.supported.lang"));
        languages.setSupported(languageType);

        return languages;
    }

    private String getPropertyValue(String name) {
        return ConfigurationManager.getInstance().getValue(name);
    }

    private DCP getPostDcp(String serviceUrl) {
        DCP executeDcp = new DCP();
        HTTP executeHttp = new HTTP();
        RequestMethodType executeRequestMethod = new RequestMethodType();
        executeRequestMethod.setHref(serviceUrl);
        executeHttp.setPost(executeRequestMethod);
        executeDcp.setHTTP(executeHttp);
        return executeDcp;
    }

    private DCP getGetDcp(String serviceUrl) {
        final RequestMethodType describeProcessRequestMethod = new RequestMethodType();
        describeProcessRequestMethod.setHref(serviceUrl);

        final HTTP describeProcessHttp = new HTTP();
        describeProcessHttp.setGet(describeProcessRequestMethod);

        final DCP describeProcessDcp = new DCP();
        describeProcessDcp.setHTTP(describeProcessHttp);
        return describeProcessDcp;
    }

    private List<ProcessBriefType> getTaoProcesses() throws PersistenceException {
        List<WorkflowInfo> workflows = webProcessingService.getCapabilities();

        ProcessOfferings processOfferings = new ProcessOfferings();
        for (WorkflowInfo workflow : workflows) {
            ProcessBriefType singleProcess = new ProcessBriefType();
            final String workflowId = workflow.getId().toString();
            singleProcess.setIdentifier(WpsTypeConverter.str2CodeType(workflowId));
            singleProcess.setTitle(WpsTypeConverter.str2LanguageStringType(workflow.getName()));
//            singleProcess.setAbstract(WpsTypeConverter.str2LanguageStringType(process.getAbstractText()));
            singleProcess.setProcessVersion("na");
            processOfferings.getProcess().add(singleProcess);
        }
        return processOfferings.getProcess();
    }

    private UriComponentsBuilder componentsBuilder() {
        return WPSController.currentURL().queryParam("Service", "WPS");
    }
}
