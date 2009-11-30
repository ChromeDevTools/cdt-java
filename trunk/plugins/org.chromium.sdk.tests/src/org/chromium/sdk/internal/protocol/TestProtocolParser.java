package org.chromium.sdk.internal.protocol;

import java.util.List;

import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JsonProtocolParser;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

public class TestProtocolParser {
  private JsonProtocolParser testParser;
  @Before
  public void setUpBefore() throws Exception {
    testParser = new JsonProtocolParser(SimpleData.class, Cases.class);
  }

  @Test
  public void test1() throws JsonProtocolParseException, ParseException {
    // JsonProtocolParser parser = new JsonProtocolParser(Arrays.asList(CommandResponse.class));
    String sample = "{'seq':0,'request_seq':1,'type':'response','command':'version'," +
        "'success':true,'body':{'V8Version':'1.3.19 (candidate)'},'refs':[],'running':true}"
        .replace('\'', '"');

    JSONObject json = JsonUtil.jsonObjectFromJson(sample);

    JsonProtocolParser parser = V8ProtocolUtil.getV8Parser();
    IncomingMessage response = parser.parse(json, IncomingMessage.class);
    Long l1 = response.getSeq();
    MessageType type = response.getType();
    CommandResponse commandResponse = response.asCommandResponse();
    Long l2 = commandResponse.getRequestSeq();
    boolean success = commandResponse.success();
    SuccessCommandResponse successResponse = commandResponse.asSuccess();
    Boolean running = successResponse.running();
    Object body = successResponse.getBody();
    List<?> refs = successResponse.getRefs();
    SuccessCommandResponse successResponse2 = parser.parse(json, SuccessCommandResponse.class);
    response = null;
  }

  @Test
  public void testTypeOverArray() throws JsonProtocolParseException, ParseException {
    // JsonProtocolParser parser = new JsonProtocolParser(Arrays.asList(CommandResponse.class));
    String sample = "{'a': [1, 2, 3]}".replace('\'', '"');
    JSONObject json = JsonUtil.jsonObjectFromJson(sample);

    SimpleData simpleData = testParser.parse(json, SimpleData.class);
    Cases cases = simpleData.getA();
    List<Object> array = cases.asList();
    array = null;
  }

  @Test
  public void testTypeOverNumber() throws JsonProtocolParseException, ParseException {
    // JsonProtocolParser parser = new JsonProtocolParser(Arrays.asList(CommandResponse.class));
    String sample = "{'a': 1}".replace('\'', '"');
    JSONObject json = JsonUtil.jsonObjectFromJson(sample);
    SimpleData simpleData = testParser.parse(json, SimpleData.class);
    Cases cases = simpleData.getA();
    long num = cases.asNumber();
    num = 0;
  }

  @JsonType
  interface SimpleData {
    Cases getA();
  }
  @JsonType(subtypesChosenManually=true)
  interface Cases {
    @JsonSubtypeCasting List<Object> asList() throws JsonProtocolParseException;
    @JsonSubtypeCasting long asNumber() throws JsonProtocolParseException;
  }
}
