package es.us.isa.restest.mutation.operators;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import es.us.isa.jsonmutator.mutator.AbstractOperator;
import es.us.isa.jsonmutator.mutator.array.operator.ArrayAddElementOperator;
import es.us.isa.jsonmutator.mutator.array.operator.ArrayRemoveElementOperator;
import es.us.isa.jsonmutator.mutator.object.operator.ObjectAddElementOperator;
import es.us.isa.jsonmutator.mutator.object.operator.ObjectRemoveElementOperator;
import es.us.isa.jsonmutator.util.JsonManager;
import es.us.isa.jsonmutator.util.PropertyManager;

import static es.us.isa.jsonmutator.util.JsonManager.insertElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JsonMutatorLocal {
    private static final Logger logger = LogManager.getLogger(es.us.isa.jsonmutator.JsonMutator.class.getName());
    private ObjectMapper objectMapper = new ObjectMapper();
    private Random rand = new Random();
    private boolean firstIteration;
    private int jsonProgress;
    private Integer elementIndex;
    private List<Integer> elementIndexes;
    private boolean mutationApplied;
    private boolean singleOrderActive;
    private JsonNode rootJson;
    private StringMutator stringMutator;
    private LongMutator longMutator;
    private DoubleMutator doubleMutator;
    private BooleanMutator booleanMutator;
    private NullMutator nullMutator;
    private ObjectMutator objectMutator;
    private ArrayMutator arrayMutator;
    public static List<String> propertytype = new ArrayList<String>();
    private int counter;
    
    public JsonMutatorLocal() {
        this.resetJsonMutator();
        this.resetMutators();
    }

    public List<JsonNode> getAllMutants(JsonNode jsonNode, double probability) {
        return this.getAllMutants(jsonNode, "", probability);
    }

    public List<String> getAllMutants(String jsonString, double probability) {
        try {
            List<JsonNode> nodeMutants = this.getAllMutants(this.objectMapper.readTree(jsonString), probability);
            return (List)nodeMutants.stream().map((n) -> {
                try {
                    return this.objectMapper.writeValueAsString(n);
                } catch (JsonProcessingException var3) {
                    logger.warn("Some mutant could not be transformed to a string.");
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException var5) {
            logger.warn("The string passed as argument is not a JSON object.");
            return Collections.singletonList(jsonString);
        }
    }

    public List<JsonNode> getAllMutants(JsonNode jsonNode) {
        return this.getAllMutants(jsonNode, "", 1.0D);
    }

    public List<String> getAllMutants(String jsonString) {
        return this.getAllMutants(jsonString, 1.0D);
    }

    private List<JsonNode> getAllMutants(JsonNode jsonNode, String parentPath, double probability) {
        List<JsonNode> mutants = new ArrayList();
        AbstractMutatorLocal mutator = this.getMutator(jsonNode);
        boolean firstIterationOccurred = false;
        Iterator jsonIterator;
        JsonNode element;
        if (this.firstIteration) {
            this.setUpSingleOrderMutation();
            this.firstIteration = false;
            firstIterationOccurred = true;
            this.rootJson = jsonNode.deepCopy();
            if (mutator != null) {
                ((AbstractObjectOrArrayMutatorLocal)mutator).resetFirstLevelOperators();
                jsonIterator = mutator.getOperators().values().iterator();

                while(jsonIterator.hasNext()) {
                    AbstractOperator operator = (AbstractOperator)jsonIterator.next();
                    element = jsonNode.deepCopy();
                    if ((double)this.rand.nextFloat() < probability) {
                        mutants.add((JsonNode)operator.mutate(element));
                    }
                }

                ((AbstractObjectOrArrayMutatorLocal)mutator).resetOperators();
            }
        }

        jsonIterator = jsonNode.elements();

        for(int i = 0; jsonIterator.hasNext(); ++i) {
            element = (JsonNode)jsonIterator.next();
            mutator = this.getMutator(element);
            String propertyName = jsonNode.isObject() ? (String)Lists.newArrayList(jsonNode.fieldNames()).get(i) : null;
            Integer index = jsonNode.isArray() ? i : null;
            if (mutator != null) {
                Iterator var13 = mutator.getOperators().values().iterator();

                while(var13.hasNext()) {
                    AbstractOperator operator = (AbstractOperator)var13.next();
                    if ((double)this.rand.nextFloat() < probability) {
                        mutants.add(this.getMutatedJson(this.rootJson, parentPath, propertyName, index, operator));
                    }
                }
            }

            if (element.isContainerNode()) {
                mutants.addAll(this.getAllMutants(element, parentPath + "/" + (index == null ? propertyName : index), probability));
            }
        }

        if (firstIterationOccurred) {
            this.rootJson = null;
            this.firstIteration = true;
            this.resetMutators();
        }

        return mutants;
    }

    private JsonNode getMutatedJson(JsonNode jsonNode, String jsonPath, String propertyName, Integer index, AbstractOperator operator) {
        JsonNode jsonNodeCopy = jsonNode.deepCopy();
        JsonNode element = jsonNodeCopy.at(jsonPath + "/" + (index == null ? propertyName : index));
        Object mutatedElement = operator.mutate(JsonManager.getNodeElement(element));
        JsonManager.insertElement(jsonNodeCopy.at(jsonPath), mutatedElement, propertyName, index);
        return jsonNodeCopy;
    }

    public JsonNode mutateJson(JsonNode jsonNode, boolean singleOrder) {
        
    	if(jsonNode.size() == 0)
    	{
    		return jsonNode;
    	}
    		
    	if (singleOrder) {
            if (!this.singleOrderActive) {
                this.setUpSingleOrderMutation();
            }

            this.singleOrderActive = true;
            return this.singleOrderMutation(jsonNode);
        } else {
            if (this.singleOrderActive) {
                this.resetMutators();
            }

            this.singleOrderActive = false;
            return this.multipleOrderMutation(jsonNode);
        }
    }

    public String mutateJson(String jsonString, boolean singleOrder) {
        try {
            return this.objectMapper.writeValueAsString(this.mutateJson(this.objectMapper.readTree(jsonString), singleOrder));
        } catch (IOException var4) {
            logger.warn("The string passed as argument is not a JSON object.");
            return jsonString;
        }
    }

    private void setUpSingleOrderMutation() {
        if (Boolean.parseBoolean(PropertyManager.readProperty("operator.value.string.enabled"))) {
            this.stringMutator.setProb(1.0F);
        }

        if (Boolean.parseBoolean(PropertyManager.readProperty("operator.value.long.enabled"))) {
            this.longMutator.setProb(1.0F);
        }

        if (Boolean.parseBoolean(PropertyManager.readProperty("operator.value.double.enabled"))) {
            this.doubleMutator.setProb(1.0F);
        }

        if (Boolean.parseBoolean(PropertyManager.readProperty("operator.value.boolean.enabled"))) {
            this.booleanMutator.setProb(1.0F);
        }

        if (Boolean.parseBoolean(PropertyManager.readProperty("operator.value.null.enabled"))) {
            this.nullMutator.setProb(1.0F);
        }

        if (Boolean.parseBoolean(PropertyManager.readProperty("operator.object.enabled"))) {
            this.objectMutator.setProb(1.0F);
            this.objectMutator.setMinMutations(1);
            this.objectMutator.setMaxMutations(1);
            ((ObjectAddElementOperator)this.objectMutator.getOperators().get("addElement")).setMinAddedProperties(1);
            ((ObjectAddElementOperator)this.objectMutator.getOperators().get("addElement")).setMaxAddedProperties(1);
            ((ObjectRemoveElementOperator)this.objectMutator.getOperators().get("removeElement")).setMinRemovedProperties(1);
            ((ObjectRemoveElementOperator)this.objectMutator.getOperators().get("removeElement")).setMaxRemovedProperties(1);
        }

        if (Boolean.parseBoolean(PropertyManager.readProperty("operator.array.enabled"))) {
            this.arrayMutator.setProb(1.0F);
            this.arrayMutator.setMinMutations(1);
            this.arrayMutator.setMaxMutations(1);
            ((ArrayAddElementOperator)this.arrayMutator.getOperators().get("addElement")).setMinAddedElements(1);
            ((ArrayAddElementOperator)this.arrayMutator.getOperators().get("addElement")).setMaxAddedElements(1);
            ((ArrayRemoveElementOperator)this.arrayMutator.getOperators().get("removeElement")).setMinRemovedElements(1);
            ((ArrayRemoveElementOperator)this.arrayMutator.getOperators().get("removeElement")).setMaxRemovedElements(1);
        }

    }

    private void resetJsonMutator() {
        this.firstIteration = true;
        this.jsonProgress = 0;
        this.singleOrderActive = this.elementIndex != null && this.elementIndex != -1;
        this.elementIndex = null;
        this.elementIndexes = new ArrayList();
        this.mutationApplied = false;
    }

    private void resetMutators() {
        this.stringMutator = Boolean.parseBoolean(PropertyManager.readProperty("operator.value.string.enabled")) ? new StringMutator() : null;
        this.longMutator = Boolean.parseBoolean(PropertyManager.readProperty("operator.value.long.enabled")) ? new LongMutator() : null;
        this.doubleMutator = Boolean.parseBoolean(PropertyManager.readProperty("operator.value.double.enabled")) ? new DoubleMutator() : null;
        this.booleanMutator = Boolean.parseBoolean(PropertyManager.readProperty("operator.value.boolean.enabled")) ? new BooleanMutator() : null;
        this.nullMutator = Boolean.parseBoolean(PropertyManager.readProperty("operator.value.null.enabled")) ? new NullMutator() : null;
        this.objectMutator = Boolean.parseBoolean(PropertyManager.readProperty("operator.object.enabled")) ? new ObjectMutator() : null;
        this.arrayMutator = Boolean.parseBoolean(PropertyManager.readProperty("operator.array.enabled")) ? new ArrayMutator() : null;
    }

    private JsonNode singleOrderMutation(JsonNode jsonNode) {
        boolean firstIterationOccurred = false;
        JsonNode jsonNodeCopy = jsonNode;
        int currentJsonProgress = 0;
        if (this.firstIteration) {
            this.firstIteration = false;
            firstIterationOccurred = true;
            jsonNodeCopy = jsonNode.deepCopy();
            if (this.isElementSubjectToChange(jsonNodeCopy)) {
                this.elementIndexes.add(-1);
            }
        }

        if (this.elementIndex != null && this.elementIndex == -1 && !this.mutationApplied) {
            if (this.objectMutator != null && jsonNodeCopy.isObject()) {
            	jsonNodeCopy = this.objectMutator.getMutatedNode(jsonNodeCopy);
            } else if (this.arrayMutator != null && jsonNodeCopy.isArray()) {
            	jsonNodeCopy = this.arrayMutator.getMutatedNode(jsonNodeCopy);
            }

            this.mutationApplied = true;
        }

        Iterator jsonIterator = jsonNodeCopy.elements();

        while(jsonIterator.hasNext()) {
            JsonNode subJsonNode = (JsonNode)jsonIterator.next();
            if (this.elementIndex == null) {
                if (this.isElementSubjectToChange(subJsonNode)) {
                    this.elementIndexes.add(this.jsonProgress);
                }
            } else if (this.elementIndex == this.jsonProgress) {
                if (jsonNodeCopy.isObject()) {
                	this.mutateElement(jsonNodeCopy, (String)Lists.newArrayList(jsonNodeCopy.fieldNames()).get(currentJsonProgress), (Integer)null);
                } else if (jsonNodeCopy.isArray()) {
                	this.mutateElement(jsonNodeCopy, (String)null, currentJsonProgress);
                }

                this.mutationApplied = true;
            }

            ++currentJsonProgress;
            ++this.jsonProgress;
            if (this.mutationApplied) {
                break;
            }

            if (subJsonNode.isContainerNode()) {
                this.singleOrderMutation(subJsonNode);
            }
        }

        if (firstIterationOccurred) {
            if (this.elementIndexes.size() > 0) {
                this.elementIndex = (Integer)this.elementIndexes.get(this.rand.nextInt(this.elementIndexes.size()));
                this.jsonProgress = 0;
                this.singleOrderMutation(jsonNodeCopy);
            }

            this.resetJsonMutator();
        }

        return jsonNodeCopy;
    }

    private JsonNode multipleOrderMutation(JsonNode jsonNode) {
        boolean firstIterationOccurred = false;
        JsonNode jsonNodeCopy = jsonNode;
        if (this.firstIteration) {
            this.firstIteration = false;
            firstIterationOccurred = true;
            jsonNodeCopy = jsonNode.deepCopy();
            if (this.objectMutator != null && jsonNodeCopy.isObject()) {
                jsonNodeCopy = this.objectMutator.getMutatedNode(jsonNodeCopy);
            } else if (this.arrayMutator != null && jsonNodeCopy.isArray()) {
                jsonNodeCopy = this.arrayMutator.getMutatedNode(jsonNodeCopy);
            }
        }

        if (jsonNodeCopy.isObject()) {
            Iterator keysIterator = jsonNodeCopy.fieldNames();

            label52:
            while(true) {
                String propertyName;
                do {
                    if (!keysIterator.hasNext()) {
                        break label52;
                    }

                    propertyName = (String)keysIterator.next();
                    this.mutateElement(jsonNodeCopy, propertyName, (Integer)null);
                } while(!jsonNodeCopy.get(propertyName).isObject() && !jsonNodeCopy.get(propertyName).isArray());

                ((ObjectNode)jsonNodeCopy).replace(propertyName, this.multipleOrderMutation(jsonNodeCopy.get(propertyName)));
            }
        } else if (jsonNodeCopy.isArray()) {
            for(int arrayIndex = 0; arrayIndex < jsonNodeCopy.size(); ++arrayIndex) {
                this.mutateElement(jsonNodeCopy, (String)null, arrayIndex);
                if (jsonNodeCopy.get(arrayIndex).isObject() || jsonNodeCopy.get(arrayIndex).isArray()) {
                    ((ArrayNode)jsonNodeCopy).set(arrayIndex, this.multipleOrderMutation(jsonNodeCopy.get(arrayIndex)));
                }
            }
        }

        if (firstIterationOccurred) {
            this.firstIteration = true;
        }

        return jsonNodeCopy;
    }

    private boolean isElementSubjectToChange(JsonNode element) {
        return this.longMutator != null && element.isIntegralNumber() || this.doubleMutator != null && element.isFloatingPointNumber() || this.stringMutator != null && element.isTextual() || this.booleanMutator != null && element.isBoolean() || this.nullMutator != null && element.isNull() || this.objectMutator != null && element.isObject() || this.arrayMutator != null && element.isArray();
    }

    public void mutateElement(JsonNode jsonNode, String propertyName, Integer index) {
     	
    	if(counter <=120)
    	{
    		System.out.print("+");
    	}
    	else
    	{
    		System.out.println();
    		counter = 0;
    	}
    	counter++;
    	
    	boolean isObj = index == null;
        JsonNode element = isObj ? jsonNode.get(propertyName) : jsonNode.get(index);
        if(propertyName==null)
            propertyName = "null";
        propertytype.add(propertyName);
        if (this.longMutator != null && element.isIntegralNumber()) {
            if (isObj) {
                this.longMutator.mutate((ObjectNode)jsonNode, propertyName);
            } else {
                this.longMutator.mutate((ArrayNode)jsonNode, index);
            }
        } else if (this.doubleMutator != null && element.isFloatingPointNumber()) {
            if (isObj) {
                this.doubleMutator.mutate((ObjectNode)jsonNode, propertyName);
            } else {
                this.doubleMutator.mutate((ArrayNode)jsonNode, index);
            }
        } else if (this.stringMutator != null && element.isTextual()) {
            
        	//Raj
        	try
        	{
            	String strVal = element.asText();
	        	JsonNode node = null;
	        	JsonNode temp = null;
	        	String modifiedStringNode;
	        	//System.out.println("--->\n"+strVal);
	        	
	        	try
	        	{
	        		node = this.objectMapper.readTree(strVal);
		        	
	        	}catch(Exception e)
	        	{
	        		
	        	}
	        	if(node == null || !node.isObject())
	        	{
	        		
	        		if (isObj) {
	                    this.stringMutator.mutate((ObjectNode)jsonNode, propertyName);
	                } else {
	                    this.stringMutator.mutate((ArrayNode)jsonNode, index);
	                }
	        	}
	        	else
	        	{
	        		JsonMutatorLocal jml = new JsonMutatorLocal();
	        		temp = jml.mutateJson(node, true);
	        		modifiedStringNode = this.objectMapper.writeValueAsString(temp);
	        		//modifiedStringNode = modifiedStringNode.replace("\"", "\\\"");
	        		//System.out.println("What I what is here\n"+modifiedStringNode);
	        		insertElement(jsonNode, modifiedStringNode, propertyName, index); // Replace original element with mutated element
	                
	        	}
        	}catch(Exception e1)
        	{
        		e1.printStackTrace();
        		System.exit(-1);
        	}	
        	
        } else if (this.booleanMutator != null && element.isBoolean()) {
            if (isObj) {
                this.booleanMutator.mutate((ObjectNode)jsonNode, propertyName);
            } else {
                this.booleanMutator.mutate((ArrayNode)jsonNode, index);
            }
        } else if (this.nullMutator != null && element.isNull()) {
            if (isObj) {
                this.nullMutator.mutate((ObjectNode)jsonNode, propertyName);
            } else {
                this.nullMutator.mutate((ArrayNode)jsonNode, index);
            }
        } else if (this.objectMutator != null && element.isObject()) {
            if (isObj) {
                this.objectMutator.mutate((ObjectNode)jsonNode, propertyName);
            } else {
                this.objectMutator.mutate((ArrayNode)jsonNode, index);
            }
        } else if (this.arrayMutator != null && element.isArray()) {
            if (isObj) {
                this.arrayMutator.mutate((ObjectNode)jsonNode, propertyName);
            } else {
                this.arrayMutator.mutate((ArrayNode)jsonNode, index);
            }
        }

    }

    private AbstractMutatorLocal getMutator(JsonNode jsonNode) {
        if (jsonNode.isIntegralNumber()) {
            return this.longMutator;
        } else if (jsonNode.isFloatingPointNumber()) {
            return this.doubleMutator;
        } else if (jsonNode.isTextual()) {
            return this.stringMutator;
        } else if (jsonNode.isBoolean()) {
            return this.booleanMutator;
        } else if (jsonNode.isNull()) {
            return this.nullMutator;
        } else if (jsonNode.isObject()) {
            return this.objectMutator;
        } else {
            return jsonNode.isArray() ? this.arrayMutator : null;
        }
    }

    public void setProperty(String propertyName, String propertyValue) {
        PropertyManager.setProperty(propertyName, propertyValue);
        this.resetMutators();
    }

    public void resetProperties() {
        PropertyManager.resetProperties();
        this.resetMutators();
    }
}
