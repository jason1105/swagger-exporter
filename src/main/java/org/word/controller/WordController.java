package org.word.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.word.dto.Table;
import org.word.service.WordService;

import java.util.List;

/**
 * Created by XiuYin.Cui on 2018/1/11.
 */
@Controller
@Log4j2
public class WordController {

    @Autowired
    private WordService tableService;

    /**
     * @param model
     * @return
     * @see #toWord(Model)
     */
    @Deprecated
    @RequestMapping("/getWord")
    public String getWord(Model model, @ModelAttribute(value="swaggerUrl") String swaggerUrl) {

        log.info(">>>>>>swaggerUrl: " + swaggerUrl);
        List<Table> tables = tableService.tableList(swaggerUrl);
        model.addAttribute("tables", tables);
        model.addAttribute("definitions", tableService.getDefinitions(swaggerUrl));
        model.addAttribute("serviceName", tableService.getServiceName(swaggerUrl));
        return "word";
    }


    @RequestMapping("/toWord")
    public String toWord(Model model, @ModelAttribute(value="swaggerUrl") String swaggerUrl) {
        List<Table> tables = tableService.tableList(swaggerUrl);
        model.addAttribute("tables", tables);
        model.addAttribute("definitions", tableService.getDefinitions(swaggerUrl));
        model.addAttribute("serviceName", tableService.getServiceName(swaggerUrl));
        return "word";
    }
}
