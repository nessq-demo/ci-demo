package es.us.isa.restest.generators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.us.isa.restest.configuration.pojos.Generator;
import es.us.isa.restest.configuration.pojos.Operation;
import es.us.isa.restest.configuration.pojos.TestConfigurationObject;
import es.us.isa.restest.configuration.pojos.TestParameter;
import es.us.isa.restest.inputs.ITestDataGenerator;
import es.us.isa.restest.inputs.perturbation.ObjectPerturbator;
import es.us.isa.restest.inputs.random.RandomInputValueIterator;
import es.us.isa.restest.specification.OpenAPISpecification;
import es.us.isa.restest.specification.ParameterFeatures;
import es.us.isa.restest.testcases.TestCase;
import es.us.isa.restest.util.FileManager;
import es.us.isa.restest.util.SpecificationVisitor;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * This class implements a generator of fuzzing test cases. It uses a customizable dictionary to obtain
 * fuzzing parameters. Those parameters are classified by type (string, integer, number, boolean).
 *
 * @author José Ramón Fernández
 */

public class FuzzingTestCaseGenerator extends AbstractTestCaseGenerator {

    private Map<String, List<String>> fuzzingMap;

    private static Logger logger = LogManager.getLogger(FuzzingTestCaseGenerator.class.getName());

    public FuzzingTestCaseGenerator(OpenAPISpecification spec, TestConfigurationObject conf, int nTests, int nGetTests) {
        super(spec, conf, nTests, nGetTests);
        TypeReference<HashMap<String, List<String>>> typeRef = new TypeReference<HashMap<String, List<String>>>() {
        };
        try {
            this.fuzzingMap = new ObjectMapper().readValue(FileManager.readFile("src/main/resources/fuzzing-dictionary.json"), typeRef);
        } catch (JsonProcessingException e) {
            logger.error("Error processing JSON fuzzing dictionary", e);
        }
    }

    @Override
    protected Collection<TestCase> generateOperationTestCases(Operation testOperation) {

        List<TestCase> testCases = new ArrayList<>();

        resetOperation();

        // Set up generators for each parameter
        for (TestParameter testParam: testOperation.getTestParameters()) {
            if (!testParam.getIn().equals("body")) {
                ParameterFeatures param = SpecificationVisitor.findParameter(testOperation.getOpenApiOperation(), testParam.getName(), testParam.getIn());
                List<String> fuzzingList = new ArrayList<>(fuzzingMap.get("common"));
                fuzzingList.addAll(fuzzingMap.getOrDefault(param.getType(), fuzzingMap.get("string"))); // If unknown type, or array, or object, add "string"-type values
                if (param.getEnumValues() != null) {
                    fuzzingList.addAll(param.getEnumValues());
                }
                ITestDataGenerator generator = new RandomInputValueIterator<>(fuzzingList);
                nominalGenerators.replace(Pair.with(testParam.getName(), testParam.getIn()), Collections.singletonList(generator));
            }
        }

        while (hasNext(testOperation.getMethod())) {
            TestCase test = generateNextTestCase(testOperation);
            test.setFulfillsDependencies(false);
            test.setFaulty(false);

            authenticateTestCase(test);
            testCases.add(test);
            updateIndexes(test);
        }

        return testCases;
    }

    @Override
    public TestCase generateNextTestCase(Operation testOperation) {
        TestCase tc = createTestCaseTemplate(testOperation);

        if (testOperation.getTestParameters() != null) {
            for (TestParameter testParam : testOperation.getTestParameters()) {
                if (testParam.getWeight() == null || rand.nextFloat() <= testParam.getWeight()) {
                    if (!testParam.getIn().equals("body")) {
                        tc.addParameter(testParam, nominalGenerators.get(Pair.with(testParam.getName(), testParam.getIn())).get(0).nextValueAsString());
                    } else {
                        generateFuzzingBody(tc, testParam, testOperation);
                    }
                }
            }
        }
        return tc;
    }

    private void generateFuzzingBody(TestCase tc, TestParameter testParam, Operation testOperation) {
        MediaType requestBody = testOperation.getOpenApiOperation().getRequestBody().getContent().get("application/json");

        if (requestBody != null) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.createObjectNode();
            generateFuzzingBody(requestBody.getSchema(), mapper, node);
            tc.addParameter(testParam, node.toPrettyString());
        } else {
            ITestDataGenerator generator = getRandomGenerator(nominalGenerators.get(Pair.with(testParam.getName(), testParam.getIn())));
            if (generator instanceof ObjectPerturbator) {
                tc.addParameter(testParam, ((ObjectPerturbator) generator).getRandomOriginalStringObject());
            } else {
                tc.addParameter(testParam, generator.nextValueAsString());
            }
        }
    }

    private void generateFuzzingBody(Schema schema, ObjectMapper mapper, ObjectNode rootNode) {
        if (schema.get$ref() != null) {
            schema = spec.getSpecification().getComponents().getSchemas().get(schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1));
        }

        for (Object o : schema.getProperties().entrySet()) {
            Map.Entry<String, Schema> entry = (Map.Entry<String, Schema>) o;
            JsonNode childNode;

            if (entry.getValue().getType().equals("object")) {
                childNode = mapper.createObjectNode();
                generateFuzzingBody(entry.getValue(), mapper, (ObjectNode) childNode);
            } else {
                childNode = createValueNode(entry.getValue(), mapper);
            }

            rootNode.set(entry.getKey(), childNode);
        }
    }

    private JsonNode createValueNode(Schema schema, ObjectMapper mapper) {
        JsonNode node = null;
        List<String> fuzzingList = fuzzingMap.get("common");
        fuzzingList.addAll(fuzzingMap.get(schema.getType()));
        if (schema.getEnum() != null) {
            fuzzingList.addAll(schema.getEnum());
        }
        String value = fuzzingList.get(rand.nextInt(fuzzingList.size()));
        if (NumberUtils.isCreatable(value)) {
            Number n = NumberUtils.createNumber(value);
            if (n instanceof Integer || n instanceof Long) {
                node = mapper.getNodeFactory().numberNode(n.longValue());
            } else if (n instanceof BigInteger) {
                node = mapper.getNodeFactory().numberNode((BigInteger) n);
            } else if (n instanceof Double || n instanceof Float) {
                node = mapper.getNodeFactory().numberNode(n.doubleValue());
            } else if (n instanceof BigDecimal) {
                node = mapper.getNodeFactory().numberNode((BigDecimal) n);
            }
        } else if("true".equals(value) || "false".equals(value)) {
            node = mapper.getNodeFactory().booleanNode(Boolean.parseBoolean(value));
        }

        if (node == null) {
            node = mapper.getNodeFactory().textNode(value);
        }

        return node;
    }

    @Override
    protected boolean hasNext(String testMethod) {
        return nTests < numberOfTests;
    }
}
