package org.word.service;

import org.word.dto.Definition;
import org.word.dto.Table;

import java.util.List;
import java.util.Map;

/**
 * Created by XiuYin.Cui on 2018/1/12.
 */
public interface WordService {

    List<Table> tableList(String swaggerUrl);
    Map<String, List<Table>> controllerMap(String swaggerUrl);

    List<Definition> getDefinitions(String swaggerUrl);
    String getServiceName(String swaggerUrl);
}
