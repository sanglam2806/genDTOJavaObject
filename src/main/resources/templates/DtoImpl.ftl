package ${pkg}.dto;

public class ${cls}DtoImpl implements ${cls}Dto {
<#list cols as c>
    private ${c.javaType} ${c.prop};
</#list>
<#list cols as c>

   	/** ${c.comment}を取得する */
    @Override 
	public ${c.javaType} get${c.prop?cap_first}() {
		return ${c.prop};
	}
   
   	/** ${c.comment}を設定する */
   @Override 
	public void set${c.prop?cap_first}(${c.javaType} ${c.prop}) { 
		this.${c.prop} = ${c.prop};
	}
</#list>
}
