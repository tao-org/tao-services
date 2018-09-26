package ro.cs.tao.wps.operations;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.schema.AddressType;
import com.bc.wps.api.schema.Capabilities;
import com.bc.wps.api.schema.CodeType;
import com.bc.wps.api.schema.ContactType;
import com.bc.wps.api.schema.DCP;
import com.bc.wps.api.schema.HTTP;
import com.bc.wps.api.schema.LanguageStringType;
import com.bc.wps.api.schema.Languages;
import com.bc.wps.api.schema.LanguagesType;
import com.bc.wps.api.schema.OnlineResourceType;
import com.bc.wps.api.schema.Operation;
import com.bc.wps.api.schema.OperationsMetadata;
import com.bc.wps.api.schema.ProcessBriefType;
import com.bc.wps.api.schema.ProcessOfferings;
import com.bc.wps.api.schema.RequestMethodType;
import com.bc.wps.api.schema.ResponsiblePartySubsetType;
import com.bc.wps.api.schema.ServiceIdentification;
import com.bc.wps.api.schema.ServiceProvider;
import com.bc.wps.api.schema.TelephoneType;
import com.bc.wps.api.utils.CapabilitiesBuilder;
import com.bc.wps.api.utils.WpsTypeConverter;
import com.bc.wps.utilities.PropertiesWrapper;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Sabine
 */
public class GetCapabilitiesOperation {

    final WpsRequestContext wpsRequestContext;
    final Logger logger = Logger.getLogger(this.getClass().getName());

    public GetCapabilitiesOperation(WpsRequestContext context) throws IOException {
        wpsRequestContext = context;
    }

    public Capabilities getCapabilities() throws IOException, URISyntaxException {
        logger.info("GetCapabilities for user " + wpsRequestContext.getUserName());
        return CapabilitiesBuilder.create()
                    .withOperationsMetadata(getOperationsMetadata())
                    .withServiceIdentification(getServiceIdentification())
                    .withServiceProvider(getServiceProvider())
                    .withProcessOfferings(getProcessOfferings())
                    .withLanguages(getLanguages())
                    .build();
    }

    OperationsMetadata getOperationsMetadata() {
        final OperationsMetadata operationsMetadata = new OperationsMetadata();

        Operation getCapabilitiesOperation = new Operation();
        getCapabilitiesOperation.setName("GetCapabilities");
        DCP getCapabilitiesDcp = getGetDcp(PropertiesWrapper.get("wps.get.request.url"));
        getCapabilitiesOperation.getDCP().add(getCapabilitiesDcp);
        operationsMetadata.getOperation().add(getCapabilitiesOperation);

        Operation describeProcessOperation = new Operation();
        describeProcessOperation.setName("DescribeProcess");
        DCP describeProcessDcp = getGetDcp(PropertiesWrapper.get("wps.get.request.url"));
        describeProcessOperation.getDCP().add(describeProcessDcp);
        operationsMetadata.getOperation().add(describeProcessOperation);

        Operation executeOperation = new Operation();
        executeOperation.setName("Execute");
        DCP executeDcp = getPostDcp(PropertiesWrapper.get("wps.post.request.url"));
        executeOperation.getDCP().add(executeDcp);
        operationsMetadata.getOperation().add(executeOperation);

        Operation getStatusOperation = new Operation();
        getStatusOperation.setName("GetStatus");
        DCP getStatusDcp = getGetDcp(PropertiesWrapper.get("wps.get.request.url"));
        getStatusOperation.getDCP().add(getStatusDcp);
        operationsMetadata.getOperation().add(getStatusOperation);


        return operationsMetadata;
    }

    ServiceProvider getServiceProvider() {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setProviderName(PropertiesWrapper.get("company.name"));

        OnlineResourceType siteUrl = new OnlineResourceType();
        siteUrl.setHref(PropertiesWrapper.get("company.website"));
        serviceProvider.setProviderSite(siteUrl);

        ResponsiblePartySubsetType contact = new ResponsiblePartySubsetType();
        contact.setIndividualName(PropertiesWrapper.get("project.manager.name"));
        contact.setPositionName(PropertiesWrapper.get("project.manager.position.name"));

        ContactType contactInfo = new ContactType();

        TelephoneType phones = new TelephoneType();
        phones.getVoice().add(PropertiesWrapper.get("company.phone.number"));
        phones.getFacsimile().add(PropertiesWrapper.get("company.fax.number"));
        contactInfo.setPhone(phones);

        AddressType address = new AddressType();
        address.getDeliveryPoint().add(PropertiesWrapper.get("company.address"));
        address.setCity(PropertiesWrapper.get("company.city"));
        address.setAdministrativeArea(PropertiesWrapper.get("company.administrative.area"));
        address.setPostalCode(PropertiesWrapper.get("company.post.code"));
        address.setCountry(PropertiesWrapper.get("company.country"));
        address.getElectronicMailAddress().add(PropertiesWrapper.get("company.email.address"));
        contactInfo.setAddress(address);

        contactInfo.setOnlineResource(siteUrl);
        contactInfo.setHoursOfService(PropertiesWrapper.get("company.service.hours"));
        contactInfo.setContactInstructions(PropertiesWrapper.get("company.contact.instruction"));

        contact.setContactInfo(contactInfo);

        CodeType role = new CodeType();
        role.setValue("PointOfContact");
        contact.setRole(role);
        serviceProvider.setServiceContact(contact);

        return serviceProvider;
    }

    ProcessOfferings getProcessOfferings() throws IOException, URISyntaxException {
        ProcessOfferings processOfferings = new ProcessOfferings();
        List<ProcessBriefType> calvalusProcesses = getCalvalusProcesses();
        processOfferings.getProcess().addAll(calvalusProcesses);
        List<ProcessBriefType> localProcesses = getLocalProcesses();
        if (!localProcesses.isEmpty()) {
            processOfferings.getProcess().addAll(localProcesses);
        }
        return processOfferings;
    }

    ServiceIdentification getServiceIdentification() {
        ServiceIdentification serviceIdentification = new ServiceIdentification();
        LanguageStringType title = new LanguageStringType();
        title.setValue(PropertiesWrapper.get("wps.service.id"));
        serviceIdentification.setTitle(title);

        LanguageStringType abstractText = new LanguageStringType();
        abstractText.setValue(PropertiesWrapper.get("wps.service.abstract"));
        serviceIdentification.setAbstract(abstractText);

        CodeType serviceType = new CodeType();
        serviceType.setValue(PropertiesWrapper.get("wps.service.type"));
        serviceIdentification.setServiceType(serviceType);

        serviceIdentification.getServiceTypeVersion().add(0, PropertiesWrapper.get("wps.version"));
        return serviceIdentification;
    }

    Languages getLanguages() {
        Languages languages = new Languages();

        Languages.Default defaultLanguage = new Languages.Default();
        defaultLanguage.setLanguage(PropertiesWrapper.get("wps.default.lang"));
        languages.setDefault(defaultLanguage);

        LanguagesType languageType = new LanguagesType();
        languageType.getLanguage().add(0, PropertiesWrapper.get("wps.supported.lang"));
        languages.setSupported(languageType);

        return languages;
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

    private List<ProcessBriefType> getLocalProcesses() throws URISyntaxException, IOException {
        List<ProcessBriefType> localProcessList = new ArrayList<>();
        URL descriptorDirUrl = this.getClass().getResource("/local-process-descriptor");
        if (descriptorDirUrl == null) {
            return localProcessList;
        }
        URI descriptorDirUri = descriptorDirUrl.toURI();
        File descriptorDirectory = Paths.get(descriptorDirUri).toFile();
        File[] descriptorFiles = descriptorDirectory.listFiles();
        if (descriptorFiles != null) {
            for (File descriptorFile : descriptorFiles) {
                FileInputStream fileInputStream = new FileInputStream(descriptorFile);
                String bundleDescriptorXml = IOUtils.toString(fileInputStream);
// @todo uncomment and implement
//                ParameterBlockConverter parameterBlockConverter = new ParameterBlockConverter();
//                BundleDescriptor bundleDescriptor = new BundleDescriptor();
//                parameterBlockConverter.convertXmlToObject(bundleDescriptorXml, bundleDescriptor);
//                ProcessorDescriptor[] processorDescriptors = bundleDescriptor.getProcessorDescriptors();
//
//                for (ProcessorDescriptor processorDescriptor : processorDescriptors) {
//                    WpsProcess process = new CalvalusProcessor(bundleDescriptor, processorDescriptor);
//                    ProcessBriefType localSubsetProcessor = new ProcessBriefType();
//                    localSubsetProcessor.setIdentifier(WpsTypeConverter.str2CodeType(process.getIdentifier()));
//                    localSubsetProcessor.setTitle(WpsTypeConverter.str2LanguageStringType(process.getTitle()));
//                    localSubsetProcessor.setAbstract(WpsTypeConverter.str2LanguageStringType(
//                                process.getAbstractText() == null ? process.getTitle() : process.getAbstractText()));
//                    localSubsetProcessor.setProcessVersion(process.getVersion());
//                    localProcessList.add(localSubsetProcessor);
//                }
            }
        }
        return localProcessList;
    }

    private List<ProcessBriefType> getCalvalusProcesses() {
// @todo uncomment and implement
//        List<WpsProcess> processList = calvalusFacade.getProcessors();
        ProcessOfferings processOfferings = new ProcessOfferings();
// @todo uncomment and implement
//        for (WpsProcess process : processList) {
//            ProcessBriefType singleProcessor = new ProcessBriefType();
//            singleProcessor.setIdentifier(WpsTypeConverter.str2CodeType(process.getIdentifier()));
//            singleProcessor.setTitle(WpsTypeConverter.str2LanguageStringType(process.getTitle()));
//            singleProcessor.setAbstract(WpsTypeConverter.str2LanguageStringType(process.getAbstractText()));
//            singleProcessor.setProcessVersion(process.getVersion());
//            processOfferings.getProcess().add(singleProcessor);
//        }
        return processOfferings.getProcess();
    }
}
