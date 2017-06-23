package com.jd.unibase.auth.client;

import com.jd.unibase.auth.util.JsonUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by pengan on 17-6-13.
 */
public class AuthHttpClient {
    private static final Logger logger = Logger.getLogger(AuthHttpClient.class);
    private static final String CHARSET = "UTF-8";

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String SCHEMA = "schema";
    private static final String SCHEMAS = "schemas";
    private static final String TABLES = "tables";

    private static final String TABLE = "table";

    private final static String RESPONSE_MESSAGE = "message";
    private static final String CODE = "code";
    private static final int SUCCESS = 0;

    private static final String DEFAULT_AUTH_URL = "http://127.0.0.1:5741/auth/verify/";
    private static final String DEFAULT_SHOW_DATABASES_URL = "http://127.0.0.1:5741/auth/show/databases/";
    private static final String DEFAULT_SHOW_TABLES_URL = "http://127.0.0.1:5741/auth/show/tables/";
    private static final String DEFAULT_SHOW_COLUMNS_URL = "http://127.0.0.1:5741/auth/show/columns/";

    private static String authUrl;
    private static String showDatabaseUrl;
    private static String showTablesUrl;
    private static String showColumnsUrl;

    public static void init() {
        InputStream inputStream = AuthHttpClient.class.getResourceAsStream("/auth.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
            authUrl = properties.getProperty("spark.thrift.server.auth.url",
                    DEFAULT_AUTH_URL).trim();
            logger.info("auth url [" + authUrl + "]");
            showDatabaseUrl = properties.getProperty("spark.thrift.server.auth.show.databases.url",
                    DEFAULT_SHOW_DATABASES_URL).trim();
            logger.info("show database url [" + showDatabaseUrl + "]");
            showTablesUrl = properties.getProperty("spark.thrift.server.auth.show.tables.url",
                    DEFAULT_SHOW_TABLES_URL).trim();
            logger.info("show tables url [" + showTablesUrl + "]");
            showColumnsUrl = properties.getProperty("spark.thrift.server.auth.show.columns.url",
                    DEFAULT_SHOW_COLUMNS_URL).trim();
            logger.info("show columns url [" + showColumnsUrl + "]");

        } catch (IOException e) {
            logger.error("load input stream error : " + e.getLocalizedMessage());
        }
    }

    /**
     * 用户名 密码 逻辑库 以及逻辑库当中的表名
     *
     * @param user
     * @param password
     * @param schemas
     * @return
     */
    public static void verify(
            String user,
            String password,
            Map<String, List<String>> schemas) throws Exception {
        String paras = assembleToJson(user, password, schemas);
        logger.debug("paras [" + paras + "]");
        if (schemas.size() == 0) {
            logger.info("only user & password no schemas");
        }
        request(authUrl, paras);
    }

    public static void verify(
            String user,
            String password,
            String schema) throws Exception {
        showQuery(authUrl, user, password, schema);
    }

    private static String[] showQuery(String url, String user, String password, String schema) throws Exception {
        Map<String, List<String>> schemas = new LinkedHashMap<String, List<String>>();
        schemas.put(schema, new LinkedList<String>());
        String paras = assembleToJson(user, password, schemas);

        logger.debug("paras [" + paras + "]");
        return request(url, paras);
    }


    private static String[] request(
            String url,
            String paras) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);

        logger.debug("json paras: " + paras);
        StringEntity strEntity;
        try {
            strEntity = new StringEntity(paras, CHARSET);
            httppost.setEntity(strEntity);
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new IOException("error response http status " + response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String content = EntityUtils.toString(entity, CHARSET);
                    JsonNode node = JsonUtil.parseJson(content);
                    if (node.get(CODE) != null && node.get(CODE).asInt() == SUCCESS) {
                        logger.debug("request from " + url + " get success response content: " + content);
                        List<String> rst = new LinkedList<String>();
                        Iterator<JsonNode> iter = node.get(RESPONSE_MESSAGE).getElements();
                        while (iter.hasNext()) {
                            rst.add(iter.next().asText());
                        }
                        return rst.toArray(new String[rst.size()]);
                    }
                    throw new IOException("error response " + content);
                }
                throw new IOException("null response for " + url);
            } finally {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
        } finally {
            // 关闭连接,释放资源
            try {
                httpclient.close();
            } catch (IOException e) {
            }
        }
    }

    protected static String assembleToJson(String user,
                                           String password,
                                           Map<String, List<String>> schemas) {
        Map<String, Object> target = new LinkedHashMap<String, Object>();
        target.put(USER, user);
        target.put(PASSWORD, password);

        List<Map<String, Object>> schemaMap = new LinkedList<Map<String, Object>>();
        for (Map.Entry<String, List<String>> schema : schemas.entrySet()) {
            Map<String, Object> dbMap = new LinkedHashMap<String, Object>(2);
            dbMap.put(SCHEMA, schema.getKey());
            if (schema.getValue().size() != 0) {
                List<Map<String, String>> tables = new LinkedList<Map<String, String>>();
                for (String tableName : schema.getValue()) {
                    Map<String, String> map = new LinkedHashMap<String, String>();
                    map.put(TABLE, tableName);
                    tables.add(map);
                }
                dbMap.put(TABLES, tables);
            }
            schemaMap.add(dbMap);
        }
        target.put(SCHEMAS, schemaMap);
        return JsonUtil.maptoJson(target);
    }

    public static String[] showDatabases(String user, String password) throws Exception {
        Map<String, Object> target = new LinkedHashMap<String, Object>();
        target.put(USER, user);
        target.put(PASSWORD, password);
        String paras = JsonUtil.maptoJson(target);
        return request(showDatabaseUrl, paras);
    }

    public static String[] showTables(String user, String password, String schema) throws Exception {
        return showQuery(showTablesUrl, user, password, schema);
    }

    public static boolean haveColumnRight(String user, String password, String schema,
                                          String table, String... columns) {
        logger.debug("haveColumnRight");
        return false;
    }
}
