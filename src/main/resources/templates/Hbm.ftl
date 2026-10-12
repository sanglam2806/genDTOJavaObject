<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-mapping PUBLIC
  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
<hibernate-mapping>
  <class name="${pkg}.dto.${cls}DtoImpl" table="${table}">
<#list cols as c><#if c.pk>
    <id name="${c.prop}" column="${c.name}" type="${c.hibType}"/>
</#if></#list>
<#list cols as c><#if !c.pk>
    <property name="${c.prop}" column="${c.name}" type="${c.hibType}"<#if !c.nullable> not-null="true"</#if>/>
</#if></#list>
  </class>
</hibernate-mapping>
