package org.word.dto;

import lombok.Data;

import java.util.List;

/**
 * Created by XiuYin.Cui on 2018/1/11.
 */
@Data
public class Definition {

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 类型
     */
    private String type;


    /**
     * 关联的类型
     */
    private String ref;

    /**
     * 属性
     */
    private List<Definition> properties;
}
