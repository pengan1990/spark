package com.jd.unibase.auth.util;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;

public class JsonUtil {

	public static String maptoJson(Map<String, Object> maps)  {

		ObjectMapper objectMap = new ObjectMapper();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = null;
		try {
			gen = objectMap.getJsonFactory().createJsonGenerator(sw);
			gen.writeStartObject();
			for (Entry<String, Object> e : maps.entrySet()) {
				gen.writeObjectField(e.getKey(), e.getValue());
			}
			gen.writeEndObject();
			gen.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (gen != null)
					gen.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        return sw.toString();
	}
	
	public static String ObjtoJson(Object obj)  {

		ObjectMapper objectMap = new ObjectMapper();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = null;
		try {
			gen = objectMap.getJsonFactory().createJsonGenerator(sw);
			gen.writeObject(obj);
			gen.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (gen != null)
					gen.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        return sw.toString();
	}
	
	public static JsonNode parseJson(String json) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonFactory f = new MappingJsonFactory();
		JsonParser jp = null;
		JsonNode rootNode = null;
		try {
			jp = f.createJsonParser(json);
			rootNode = mapper.readTree(jp);
		} catch (Exception e) {
			throw e;
		} finally {
			if (jp != null)
				jp.close();
		}
		return rootNode;
	}
}
