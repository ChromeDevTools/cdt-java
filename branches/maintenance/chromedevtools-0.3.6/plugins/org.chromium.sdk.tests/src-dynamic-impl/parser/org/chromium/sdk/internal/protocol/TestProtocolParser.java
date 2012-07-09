package org.chromium.sdk.internal.protocol;

import java.util.ArrayList;
import java.util.List;

import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.chromium.sdk.internal.v8native.protocol.input.CommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.IncomingMessage;
import org.chromium.sdk.internal.v8native.protocol.input.MessageType;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.V8NativeProtocolParser;
import org.chromium.sdk.internal.v8native.protocol.input.V8ProtocolParserAccess;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

public class TestProtocolParser {
  private TestParser testParser;
  @Before
  public void setUpBefore() throws Exception {
    List<Class<?>> types = new ArrayList<Class<?>>(2);
    types.add(SimpleData.class);
    types.add(Cases.class);
    testParser = new DynamicParserImpl<TestParser>(TestParser.class, types).getParserRoot();
  }

  @Test
  public void test1() throws JsonProtocolParseException, ParseException {
    // JsonProtocolParser parser = new JsonProtocolParser(Arrays.asList(CommandResponse.class));
    String sample = ("{'seq':0,'request_seq':1,'type':'response','command':'version'," +
        "'success':true,'body':{'V8Version':'1.3.19 (candidate)'},'refs':[],'running':true}")
        .replace('\'', '"');

    JSONObject json = JsonUtil.jsonObjectFromJson(sample);

    V8NativeProtocolParser parser = V8ProtocolParserAccess.get();

    IncomingMessage response = parser.parseIncomingMessage(json);
    Long l1 = response.seq();
    MessageType type = response.type();
    CommandResponse commandResponse = response.asCommandResponse();
    Long l2 = commandResponse.requestSeq();
    boolean success = commandResponse.success();
    SuccessCommandResponse successResponse = commandResponse.asSuccess();
    Boolean running = successResponse.running();
    Object body = successResponse.body();
    List<?> refs = successResponse.refs();
    SuccessCommandResponse successResponse2 = parser.parseSuccessCommandResponse(json);
    response = null;
  }

  @Test
  public void testTypeOverArray() throws JsonProtocolParseException, ParseException {
    // JsonProtocolParser parser = new JsonProtocolParser(Arrays.asList(CommandResponse.class));
    String sample = "{'a': [1, 2, 3]}".replace('\'', '"');
    JSONObject json = JsonUtil.jsonObjectFromJson(sample);

    SimpleData simpleData = testParser.parseSimpleData(json);
    Cases cases = simpleData.a();
    List<Object> array = cases.asList();
    array = null;
  }

  @Test
  public void testTypeOverNumber() throws JsonProtocolParseException, ParseException {
    // JsonProtocolParser parser = new JsonProtocolParser(Arrays.asList(CommandResponse.class));
    String sample = "{'a': 1}".replace('\'', '"');
    JSONObject json = JsonUtil.jsonObjectFromJson(sample);
    SimpleData simpleData = testParser.parseSimpleData(json);
    Cases cases = simpleData.a();
    long num = cases.asNumber();
    num = 0;
  }

  @JsonType
  interface SimpleData {
    Cases a();
  }
  @JsonType(subtypesChosenManually=true)
  interface Cases {
    @JsonSubtypeCasting List<Object> asList() throws JsonProtocolParseException;
    @JsonSubtypeCasting long asNumber() throws JsonProtocolParseException;
  }

  @JsonParserRoot
  interface TestParser {

    @JsonParseMethod
    SimpleData parseSimpleData(JSONObject json) throws JsonProtocolParseException;

  }
}
