package ro.cs.tao.ogc.model.processes.dru;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ro.cs.tao.ogc.model.common.Link;
import ro.cs.tao.ogc.model.processes.core.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InputDescriptionDeserializer extends StdDeserializer<Input> {

    public InputDescriptionDeserializer(){
        this(null);
    }

    public InputDescriptionDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Input deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException, JacksonException {
        Input inputs = initializeInput();
        List<InputValueNoObject> inputValueNoObjects = inputs.getInputValueNoObjects();
        List<QualifiedInputValue> qualifiedInputValues = inputs.getQualifiedInputValues();
        List<Link> links = inputs.getLinks();
        try {
            JsonNode node = jp.getCodec().readTree(jp);
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isArray()) {
                    List<InputValueNoObject<?>> arrayList = new ArrayList<>();
                    for(JsonNode element : value){
                        if(element.isObject()) {
                            if(element.get("bbox") != null){
                                InputValueNoObject<?> input = checkAndGetItemNoObject(null, element);
                                arrayList.add(input);
                                continue;
                            }
                            if(element.get("href") != null){
                                Link link = parseAndGetLink(null, element);
                                InputValueNoObject<Link> input = InputValueNoObject.of(link);
                                arrayList.add(input);
                                continue;
                            }
                            arrayList.add(getQualifiedInputValue(null, element));
                        }else {
                            arrayList.add(checkAndGetItemNoObject(null, element));
                        }
                    }
                    InputValueNoObject<?> input = InputValueNoObject.of(arrayList);
                    input.setName(key);
                    inputValueNoObjects.add(input);
                } else
                if (value.isObject()) {
                    if(value.get("bbox") != null){
                        InputValueNoObject<?> input = checkAndGetItemNoObject(key, value);
                        inputValueNoObjects.add(input);
                        continue;
                    }
                    if(value.get("href") != null){
                        Link input = parseAndGetLink(key, value);
                        links.add(input);
                        continue;
                    }
                    QualifiedInputValue<?> inputObject = getQualifiedInputValue(key, value);
                    qualifiedInputValues.add(inputObject);
                }
                else{
                    InputValueNoObject<?> input = checkAndGetItemNoObject(key, value);
                    if(input != null)
                        inputValueNoObjects.add(input);
                }
            }
        } catch (IOException e){
            throw new IOException(e.getMessage());
        }
        return inputs;
    }



    private Input initializeInput() {
        Input input = new Input();
        input.setInputValueNoObjects(new ArrayList<>());
        input.setQualifiedInputValues(new ArrayList<>());
        input.setLinks(new ArrayList<>());
        return input;
    }

    private InputValueNoObject<?> checkAndGetItemNoObject(String key, JsonNode value) {
        InputValueNoObject<?> input = null;
        if(value.isArray()){
            List<InputValueNoObject<?>> arrayList = new ArrayList<>();
            for(JsonNode element : value){
                if(element.isObject()) {
                    //see obj
                    String key2 = element.fields().next().getKey();
                    arrayList.add(getQualifiedInputValue(key2, element));
                }else{
                    arrayList.add(checkAndGetItemNoObject(null, element));
                }
            }
            input = InputValueNoObject.of(arrayList);
        }else if(value.isObject()){
            if(value.get("bbox") != null){
                BBox bbox = parseBbox(value);
                input = InputValueNoObject.of(bbox);
            } else {
                //technically the item is not supposed to pass through this decision tree unless a mistake in the JSON exists
                throw new IllegalArgumentException("No object should be found here");
            }
        } else if (value.isBoolean()) {
            input = InputValueNoObject.of(value.booleanValue());
        } else if (value.isIntegralNumber()) {
            input = InputValueNoObject.of(value.intValue());
        } else if (value.isFloatingPointNumber()){
            input = InputValueNoObject.of(value.doubleValue());
        } else if (value.isTextual()) {
            input = InputValueNoObject.of(value.textValue());
        }
        if(input != null && key != null && !key.isEmpty()){
            input.setName(key);
        }
        return input;
    }

    private QualifiedInputValue<?> getQualifiedInputValue(String key, JsonNode value) {
        Iterator<Map.Entry<String, JsonNode>> iteratorObj = value.fields();
        QualifiedInputValue<?> inputObject;
        if (value.get("value") != null) {
            if (!value.get("value").isObject())
                inputObject = new QualifiedInputValue<>(value.get("value").asText());
            else {
                List<?> listInner = getListElements(value.get("value"));
                inputObject = new QualifiedInputValue<>(listInner);
            }
        } else
            inputObject = new QualifiedInputValue<>();

        inputObject.setName(key);
        List<InputValueNoObject<?>> additionalProperties = new ArrayList<>();
        inputObject.setAdditionalProperties(additionalProperties);
        while (iteratorObj.hasNext()) {
            Map.Entry<String, JsonNode> entryObj = iteratorObj.next();
            String keyObj = entryObj.getKey();
            JsonNode valueObj = entryObj.getValue();
            if (keyObj.equals("value"))
                continue;
            if (keyObj.equals("format")) {
                Format format = deserializeFormat(valueObj);
                inputObject.setFormat(format);
            } else {
                //TODO check for obj inside obj
                InputValueNoObject<?> inputValueNoObject = checkAndGetItemNoObject(keyObj, valueObj);
                additionalProperties.add(inputValueNoObject);
            }
        }
        return inputObject;
    }


    private List<InputValueNoObject<?>> getListElements(JsonNode node){
        List<InputValueNoObject<?>> inputs = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            InputValueNoObject input = checkAndGetItemNoObject(entry.getKey(), entry.getValue());
            if(input != null)
                inputs.add(input);
        }
        return inputs;
    }

    private Format deserializeFormat(JsonNode jsonNode){
        Format format = new Format();
        if(jsonNode.get("mediaType") != null)
            format.setMediaType(jsonNode.get("mediaType").asText());
        if(jsonNode.get("encoding") != null)
            format.setEncoding(jsonNode.get("encoding").asText());
        if(jsonNode.get("schema") != null)
            format.setSchema(jsonNode.get("schema").asText());
        return format;
    }

    private BBox parseBbox(JsonNode jsonNode) {
        BBox bbox = new BBox();
        List<Double> bboxList = new ArrayList<>();
        for(JsonNode element : jsonNode.get("bbox")){
            bboxList.add(element.doubleValue());
        }
        bbox.setBbox(bboxList);
        bbox.setCrs(jsonNode.get("crs").asText() != null ? jsonNode.get("crs").asText() : "http://www.opengis.net/def/crs/OGC/1.3/CRS84");
        return bbox;
    }

    private Link parseAndGetLink(String name, JsonNode jsonNode) {
        Link link = new Link();
        link.setHref(jsonNode.get("href").asText());
        if(jsonNode.get("rel") != null)
            link.setRel(jsonNode.get("rel").asText());
        if(jsonNode.get("type") != null)
            link.setType(jsonNode.get("type").asText());
        if(jsonNode.get("hreflang") != null)
            link.setHrefLang(jsonNode.get("hreflang").asText());
        if(jsonNode.get("title") != null)
            link.setTitle(jsonNode.get("title").asText());
        link.setName(name);
        return link;
    }


}
