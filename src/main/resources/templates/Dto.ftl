package ${pkg}.dto;

public interface ${cls}Dto {
<#list cols as c>
	/** ${c.comment}を設定する */
    ${c.javaType} get${c.prop?cap_first}();

	/** ${c.comment}を取得する */
    void set${c.prop?cap_first}(${c.javaType} ${c.prop});

</#list>
}
