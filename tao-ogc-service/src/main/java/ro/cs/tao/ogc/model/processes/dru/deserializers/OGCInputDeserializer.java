package ro.cs.tao.ogc.model.processes.dru.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ro.cs.tao.ogc.model.common.Link;
import ro.cs.tao.ogc.model.processes.core.execution.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OGCInputDeserializer extends StdDeserializer<Input> {

    public OGCInputDeserializer(){
        this(null);
    }

    public OGCInputDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Input deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException, JacksonException {
        Input inputs = initializeInput();
        try {
            JsonNode node = jp.getCodec().readTree(jp);
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                addEntryInputToInputs(entry, inputs);
            }
        } catch (IOException e){
            throw new IOException(e.getMessage());
        }
        return inputs;
    }

    private void addEntryInputToInputs(Map.Entry<String, JsonNode> entry, Input inputs) {
        String name = entry.getKey();
        JsonNode jsonInput = entry.getValue();
        List<InputValueNoObject<?>> inputValueNoObjects = inputs.getInputValueNoObjects();
        List<QualifiedInputValue<?>> qualifiedInputValues = inputs.getQualifiedInputValues();
        List<Link> links = inputs.getLinks();
        if (jsonInput.isArray()) {
            List<InputValueNoObject<?>> listInput = new ArrayList<>();
            for(JsonNode element : jsonInput){
                if(element.isObject()) {
                    if(element.get("bbox") != null){
                        InputValueNoObject<?> input = getInputValueNoObject(null, element);
                        listInput.add(input);
                        continue;
                    }
                    if(element.get("href") != null){
                        Link link = getLink(null, element);
                        InputValueNoObject<Link> input = InputValueNoObject.of(link);
                        listInput.add(input);
                        continue;
                    }
                    listInput.add(getQualifiedInputValue(null, element));
                }else {
                    listInput.add(getInputValueNoObject(null, element));
                }
            }
            InputValueNoObject<?> input = InputValueNoObject.of(listInput);
            input.setName(name);
            inputValueNoObjects.add(input);
        } else if (jsonInput.isObject()) {
            if(jsonInput.get("bbox") != null){
                InputValueNoObject<?> input = getInputValueNoObject(name, jsonInput);
                inputValueNoObjects.add(input);
                return;
            }
            if(jsonInput.get("href") != null){
                Link input = getLink(name, jsonInput);
                links.add(input);
                return;
            }
            QualifiedInputValue<?> inputObject = getQualifiedInputValue(name, jsonInput);
            qualifiedInputValues.add(inputObject);
        }
        else{
            InputValueNoObject<?> input = getInputValueNoObject(name, jsonInput);
            if(input != null)
                inputValueNoObjects.add(input);
        }
    }

    private Input initializeInput() {
        Input input = new Input();
        input.setInputValueNoObjects(new ArrayList<>());
        input.setQualifiedInputValues(new ArrayList<>());
        input.setLinks(new ArrayList<>());
        return input;
    }

    private InputValueNoObject<?> getInputValueNoObject(String name, JsonNode value) {
        InputValueNoObject<?> input = null;
        if(value.isArray()){
            List<InputValueNoObject<?>> arrayList = new ArrayList<>();
            for(JsonNode element : value){
                if(element.isObject()) {
                    if(element.get("bbox") != null){
                        InputValueNoObject<?> bboxVal = getInputValueNoObject(null, element);
                        arrayList.add(bboxVal);
                        continue;
                    }
                    if(element.get("href") != null){
                        Link link = getLink(null, element);
                        InputValueNoObject<Link> linkArr = InputValueNoObject.of(link);
                        arrayList.add(linkArr);
                        continue;
                    }
                    String key2 = element.fields().next().getKey();
                    arrayList.add(getQualifiedInputValue(key2, element));
                }else{
                    arrayList.add(getInputValueNoObject(null, element));
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
        // if name is null it means it's an array element
        if(input != null && name != null && !name.isEmpty()){
            input.setName(name);
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
                if(valueObj.isObject()) {
                    if (valueObj.get("bbox") != null) {
                        InputValueNoObject<?> inputValueNoObject = getInputValueNoObject(keyObj, valueObj);
                        additionalProperties.add(inputValueNoObject);
                        continue;
                    }
                    if (valueObj.get("href") != null) {
                        Link link = getLink(keyObj, valueObj);
                        InputValueNoObject<Link> input = InputValueNoObject.of(link);
                        additionalProperties.add(input);
                        continue;
                    }
                    InputValueNoObject<?> qualifiedInputValue = getQualifiedInputValue(keyObj, valueObj);
                    additionalProperties.add(qualifiedInputValue);
                } else {
                    InputValueNoObject<?> inputValueNoObject = getInputValueNoObject(keyObj, valueObj);
                    additionalProperties.add(inputValueNoObject);
                }
            }
        }
        return inputObject;
    }


    private List<InputValueNoObject<?>> getListElements(JsonNode node){
        List<InputValueNoObject<?>> inputs = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            InputValueNoObject input = getInputValueNoObject(entry.getKey(), entry.getValue());
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

    private Link getLink(String name, JsonNode jsonNode) {
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
