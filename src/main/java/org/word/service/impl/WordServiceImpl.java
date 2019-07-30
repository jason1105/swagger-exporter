package org.word.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.word.dto.Definition;
import org.word.dto.Request;
import org.word.dto.Response;
import org.word.dto.Table;
import org.word.service.WordService;
import org.word.utils.JsonUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by XiuYin.Cui on 2018/1/12.
 */
@Slf4j
@Service
public class WordServiceImpl implements WordService {

    @Autowired
    private RestTemplate restTemplate;

    //@Value("${swagger.url}")
    private String swaggerUrl;

    private static final String SUBSTR = "://";
    private static final String LEFT_BRACKETS = "{";
    private static final String RIGHT_BRACKETS = "}";
    private String jsonStr;
    Map<String, Object> map;
    static List<String > ignoreControllers = new ArrayList<>();
    static {
        //
        ignoreControllers.add("log-controller");
        ignoreControllers.add("feign-url-controller");
        ignoreControllers.add("tenant-controller");
        ignoreControllers.add("auth-client-controller");
    }

    @Override
    public List<Table> tableList(String swaggerUrl) {

        this.swaggerUrl = swaggerUrl;

        List<Table> result = new ArrayList<>();
        try {
            init();

            //得到 host 和 basePath，拼接访问路径
            String host = StringUtils.substringBeforeLast(this.swaggerUrl, SUBSTR) + SUBSTR + map.get("host") + map.get("basePath");
            //解析paths
            Map<String, LinkedHashMap> paths = (LinkedHashMap) map.get("paths");
            if (paths != null) {
                Iterator<Map.Entry<String, LinkedHashMap>> it = paths.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, LinkedHashMap> path = it.next();
                    // 1.请求路径, 即接口地址
                    String url = path.getKey();
                    // 2.请求方式，类似为 get,post,delete,put 这样
                    String requestType = "";
                    Map<String, LinkedHashMap> swaggerPaths = path.getValue();
                    Set<String> requestTypes = swaggerPaths.keySet();
                    for (String str : requestTypes) {
                        requestType += str + ",";
                    }

                    // 一个接口地址对应多个http请求方法
                    Iterator<Map.Entry<String, LinkedHashMap>> httpMethods = swaggerPaths.entrySet().iterator();

                    while (httpMethods.hasNext()) {

                        Map.Entry<String, LinkedHashMap> httpMethod = httpMethods.next();
                        Map<String, Object> content = httpMethod.getValue();
                        // 4. 大标题（类说明）
                        String title = String.valueOf(((List) content.get("tags")).get(0));

                        // if exist any of controller what existed in dependencies, then break
                        if (ignoreControllers.contains(title)) {
                            continue;
                        }

                        // 5.小标题 （方法说明）
                        String tag = String.valueOf(content.get("summary"));
                        // 6.接口描述
                        String description = String.valueOf(content.get("description"));




                        // 7.请求参数格式，类似于 multipart/form-data
                        String requestForm = "";
                        List<String> consumes = (List) content.get("consumes");
                        if (consumes != null && consumes.size() > 0) {
                            for (String consume : consumes) {
                                requestForm += consume + ",";
                            }
                        }
                        // 8.返回参数格式，类似于 application/json
                        String responseForm = "";
                        List<String> produces = (List) content.get("produces");
                        if (produces != null && produces.size() > 0) {
                            for (String produce : produces) {
                                responseForm += produce + ",";
                            }
                        }

                        // 9. 请求体 swaggerMap > "paths" > "/student" > "post" > "parameters"
                        // 即请求参数
                        List<Request> requestList = new ArrayList<>();
                        List<LinkedHashMap> parameters = (ArrayList) content.get("parameters");
                        if (parameters != null && parameters.size() > 0) {
                            for (int i = 0; i < parameters.size(); i++) {
                                Request request = new Request();
                                Map<String, Object> param = parameters.get(i);
                                request.setName(String.valueOf(param.get("name")));
                                request.setType(param.get("type") == null ? "Object" : param.get("type").toString());
                                request.setParamType(String.valueOf(param.get("in")));
                                request.setRequire((Boolean) param.get("required"));

                                // description
                                String desc = "";

                                try {
                                    desc = String.valueOf(param.get("description"));
                                    desc +="  ref: " + toString(((Map) param.get("schema")).get("$ref"));
                                } catch (Exception e) {
                                    // do nothing
                                }

                                request.setRemark(desc);


                                requestList.add(request);
                            }
                        }


                        // 10.返回体
                        // 即返回值, 根据状态分为 200 401 403 404
                        List<Response> responseList = new ArrayList<>();
                        Map<String, Object> responses = (LinkedHashMap) content.get("responses");
                        Iterator<Map.Entry<String, Object>> itResponses = responses.entrySet().iterator(); // Map<Http status, Content>

                        while (itResponses.hasNext()) {
                            Response response = new Response();
                            Map.Entry<String, Object> httpResponse = itResponses.next();

                            // 状态码 200 201 401 403 404 这样
                            response.setName(httpResponse.getKey());
                            LinkedHashMap<String, Object> statusCodeInfo = (LinkedHashMap) httpResponse.getValue();


                            // description
                            String httpResDesc = toString(statusCodeInfo.get("description"));
                            String ref = getRef(statusCodeInfo);
                            String remark = StringUtils.isEmpty(ref) ? "" :  "ref: " + ref;


                            response.setDescription(httpResDesc);
                            response.setRemark(remark);
                            responseList.add(response);
                        }

                        // 模拟一次HTTP请求,封装请求体和返回体
                        // 得到请求方式
                        String restType = httpMethod.getKey();
                        Map<String, Object> paramMap = buildParamMap(requestList);
                        String buildUrl = buildUrl(host + url, requestList);

                        //封装Table
                        Table table = new Table();
                        table.setTitle(title);
                        table.setUrl(url);
                        table.setTag(tag);
                        table.setDescription(description);
                        table.setRequestForm(StringUtils.removeEnd(requestForm, ","));
                        table.setResponseForm(StringUtils.removeEnd(responseForm, ","));
                        table.setRequestType(StringUtils.removeEnd(httpMethod.getKey()    , ","));
                        table.setRequestList(requestList);
                        table.setResponseList(responseList);
                        table.setRequestParam(paramMap.toString());

                        // table.setResponseParam(doRestRequest(restType, buildUrl, paramMap, url.contains(LEFT_BRACKETS)));
                        result.add(table);
                    }
                }
            }
        } catch (Exception e) {
            log.error("parse error", e);
        }
        return result;
    }

    @Override
    public Map<String, List<Table>> controllerMap(String swaggerUrl) {

        List<Table> tableList = tableList(swaggerUrl);

        Map<String, List<Table>> tableMap = new HashMap<>();

        for (int i = 0; i < tableList.size(); i++) {
            Table table = tableList.get(i);

            List<Table> tableListInMap = tableMap.getOrDefault(table.getTitle(), new ArrayList<>());

            tableListInMap.add(table);

            tableMap.put(table.getTitle(), tableListInMap);
        }

        return tableMap;
    }

    @Override
    public List<Definition> getDefinitions(String swaggerUrl) {

        List<Definition> list = new ArrayList<>();
        this.swaggerUrl = swaggerUrl;

        try {
            init();
            //解析Definitions
            Map<String, LinkedHashMap> paths = (LinkedHashMap) map.get("definitions");

            if (null != paths) {

                Iterator<Map.Entry<String, LinkedHashMap>> it = paths.entrySet().iterator();

                // 遍历对象定义
                while (it.hasNext()) {

                    Definition def  = new Definition();

                    Map.Entry<String, LinkedHashMap> definition = it.next();

                    List<Definition> propList = new ArrayList<>();

                    String title = definition.getKey();
                    String defDesc = ""; //
                    if (title.startsWith("ApiResponse")) {
                        defDesc = "API返回值";
                    } else if(title.startsWith("ApiPage")) {
                        defDesc = "API返回的分页数据";
                    } else {
                        defDesc = toString(definition.getValue().get("description"));
                    }
                    def.setTitle(title);
                    def.setDescription(defDesc);
                    def.setType(toString(definition.getValue().get("type")));

                    LinkedHashMap<String, LinkedHashMap> props = (LinkedHashMap)definition.getValue().get("properties");

                    if (null != props) {

                        Iterator<Map.Entry<String, LinkedHashMap>> propIt = props.entrySet().iterator();

                        // 遍历对象中的属性
                        while (propIt.hasNext()) {
                            Definition propDef  = new Definition();

                            Map.Entry<String, LinkedHashMap> prop = propIt.next();


                            //
                            String desc = toString(prop.getValue().get("description"));

                            // 例外场合 array
                            String type = toString(prop.getValue().get("type"));
                            if ("array".equals(type)) {
                                desc = desc + " ref: " + toString(((Map) prop.getValue().get("items")).get("$ref"))
                                        + " " + toString(((Map) prop.getValue().get("items")).get("type"));
                            }

                            // 例外场合 采用泛型
                            String gene = toString(prop.getValue().get("$ref"));
                            if (!"".equals(gene)) {
                                desc = desc  + " ref: " + gene;
                            }



                            propDef.setTitle(prop.getKey());
                            propDef.setType(toString(prop.getValue().get("type")));
                            propDef.setDescription(desc);

                            propList.add(propDef);
                        }


                    }

                    def.setProperties(propList);

                    list.add(def);
                }
            }
        } catch (Exception e) {

            log.error("parse error", e);
        }

        return list;
    }

    @Override
    public String getServiceName(String swaggerUrl) {

        String sRet;
        Pattern pattern = Pattern.compile("[a-zA-Z0-9]/(dms.*?)/");
        Matcher matcher = pattern.matcher(swaggerUrl);

        if (matcher.find()) {
            sRet = matcher.group(1);
        } else {
            sRet = "";
        }

        return sRet;
    }

    public void init() throws IOException {

        try {
            jsonStr = restTemplate.getForObject(swaggerUrl, String.class);
            // convert JSON string to Map
            map = JsonUtils.readValue(jsonStr, HashMap.class);

        } catch (Exception e) {
            jsonStr = "";
            map = new LinkedHashMap<>();

        }

    }

    public String toString(Object obj) {
        if (null != obj) {
            return obj.toString();
        } else {
            return "";
        }
    }

    /**
     *
     * @return
     */
    public String getRef(Map object) {

        String ret = "";

        Map schema = (Map)object.get("schema");

        if (null != schema) {

            Object type = schema.get("type");

            if (null == type) {
                ret = toString(schema.get("$ref"));
            } else {

                if ("string".equals(toString(type))) {
                    ret = "string";
                } else {
                    ret = toString(((Map)schema.get("items")).get("$ref"));
                }

            }

        } else {
            ret = "";
        }

        return ret;
    }

    /**
     * 重新构建url
     *
     * @param url
     * @param requestList
     * @return etc:http://localhost:8080/rest/delete?uuid={uuid}
     */
    private String buildUrl(String url, List<Request> requestList) {
        // 针对 pathParams 的参数做额外处理
        if (url.contains(LEFT_BRACKETS) && url.contains(RIGHT_BRACKETS)) {
            String before = StringUtils.substringBefore(url, LEFT_BRACKETS);
            String after = StringUtils.substringAfter(url, RIGHT_BRACKETS);
            return before + 0 + after;
        }
        StringBuffer buffer = new StringBuffer();
        if (requestList != null && requestList.size() > 0) {
            for (Request request : requestList) {
                String name = request.getName();
                buffer.append(name)
                        .append("={")
                        .append(name)
                        .append("}&");
            }
        }
        if (StringUtils.isNotEmpty(buffer.toString())) {
            url += "?" + StringUtils.removeEnd(buffer.toString(), "&");
        }
        return url;

    }

    /**
     * 发送一个 Restful 请求
     *
     * @param restType   "get", "head", "post", "put", "delete", "options", "patch"
     * @param url        资源地址
     * @param paramMap   参数
     * @param pathParams 是否是 pathParams 传参数方式
     * @return
     */
    private String doRestRequest(String restType, String url, Map<String, Object> paramMap, boolean pathParams) {
        Object object = new Object();
        try {
            if (pathParams) {
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                HttpGet httpGet = new HttpGet(url);
                httpGet.setHeader("accept", "application/json");
                CloseableHttpResponse execute = httpClient.execute(httpGet);
                return EntityUtils.toString(execute.getEntity());
            }
            switch (restType) {
                case "get":
                    object = restTemplate.getForObject(url, Object.class, paramMap);
                    break;
                case "post":
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                    HttpEntity request = new HttpEntity("{}", headers);
                    object = restTemplate.postForObject(url, request, Object.class, paramMap);
                    break;
                case "put":
                    restTemplate.put(url, paramMap, paramMap);
                    break;
                case "head":
                    HttpHeaders httpHeaders = restTemplate.headForHeaders(url, paramMap);
                    return JsonUtils.writeJsonStr(httpHeaders);
                case "delete":
                    restTemplate.delete(url, paramMap);
                    break;
                case "options":
                    Set<HttpMethod> httpMethodSet = restTemplate.optionsForAllow(url, paramMap);
                    return JsonUtils.writeJsonStr(httpMethodSet);
                case "patch":
                    object = restTemplate.execute(url, HttpMethod.PATCH, null, null, paramMap);
                    break;
                case "trace":
                    object = restTemplate.execute(url, HttpMethod.TRACE, null, null, paramMap);
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            // 无法使用 restTemplate 发送的请求，返回""
            // ex.printStackTrace();
            return "";
        }
        return object == null ? "" : object.toString();
    }

    /**
     * 封装post请求体
     *
     * @param list
     * @return
     */
    private Map<String, Object> buildParamMap(List<Request> list) {
        Map<String, Object> map = new HashMap<>(8);
        if (list != null && list.size() > 0) {
            for (Request request : list) {
                String name = request.getName();
                String type = request.getType();
                switch (type) {
                    case "string":
                        map.put(name, "string");
                        break;
                    case "integer":
                        map.put(name, 0);
                        break;
                    case "number":
                        map.put(name, 0.0);
                        break;
                    case "boolean":
                        map.put(name, true);
                        break;
                    case "body":
                        map.put(name, new HashMap<>(2));
                        break;
                    default:
                        map.put(name, null);
                        break;
                }
            }
        }
        return map;
    }
}
