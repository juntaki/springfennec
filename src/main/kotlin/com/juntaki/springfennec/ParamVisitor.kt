package com.juntaki.springfennec

import io.swagger.annotations.ApiParam
import io.swagger.models.Swagger
import io.swagger.models.parameters.*
import io.swagger.models.properties.*
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.ElementScanner8
import javax.lang.model.util.Elements
import javax.lang.model.util.Types


class ParamVisitor(
        private val swagger: Swagger,
        private val parameters: MutableList<Parameter>,
        private val elementUtils: Elements,
        private val typeUtils: Types
        ) : ElementScanner8<Void?, Void?>() {

    private fun getProperty(tm: TypeMirror):Property {
        fun isAssignable(tm: TypeMirror, className: String): Boolean {
            return typeUtils.isAssignable(tm, elementUtils.getTypeElement(className).asType())
        }

        if (isAssignable(tm, "java.time.LocalDateTime") ||
                isAssignable(tm,"java.time.ZonedDateTime") ||
                isAssignable(tm,"java.time.OffsetDateTime") ||
                isAssignable(tm,"java.util.Date") ||
                isAssignable(tm, "org.joda.time.DateTime")){
            return DateTimeProperty()
        }
        if (isAssignable(tm, "java.time.LocalDate")){
            return DateProperty()
        }
        if (isAssignable(tm, "java.lang.Boolean")){
            return BooleanProperty()
        }
        if (isAssignable(tm, "java.lang.Byte")){
            return ByteArrayProperty()
        }
        if (isAssignable(tm, "java.lang.Integer")){
            return IntegerProperty()
        }
        if (isAssignable(tm, "java.lang.Long")){
            return LongProperty()
        }
        if (isAssignable(tm, "java.lang.Float")){
            return FloatProperty()
        }
        if (isAssignable(tm, "java.lang.Double")){
            return DoubleProperty()
        }
        if (isAssignable(tm, "java.lang.String")){
            return StringProperty()
        }
        if (isAssignable(tm, "org.springframework.web.multipart.MultipartFile")) {
            return FileProperty()
        }

        // Array
        val listRegex = Regex("""^java.util.List|^java.util.ArrayList""")
        if (tm is DeclaredType && listRegex.containsMatchIn(tm.toString())){
            val arrayProperty = ArrayProperty()
            arrayProperty.items = getProperty(tm.typeArguments[0])
            return arrayProperty
        }
        if (tm is ArrayType) {
            val arrayProperty = ArrayProperty()
            arrayProperty.items = getProperty(tm.componentType)
            return arrayProperty
        }

        // Class
        val refProperty = RefProperty()
        refProperty.`$ref` = tm.toString()
        return refProperty

        TODO("map? not implemented")
    }

    private fun createParamBySpringAnnotation(element :VariableElement): Parameter {
        var param: Parameter
        var annotated = false
        //Default is query parameter. TODO: Check Spring Documentation.
        param = QueryParameter()

        val queryParamAnnotation = element.getAnnotation(RequestParam::class.java)
        queryParamAnnotation?.let {
            annotated = true
            param = QueryParameter()
            param.name = it.name
            if (param.name.isEmpty()) param.name = element.toString()
            param.required = it.required
        }

        val pathParamAnnotation = element.getAnnotation(PathVariable::class.java)
        pathParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true
            param = PathParameter()
            param.name = it.name
            if (param.name.isEmpty()) param.name = element.toString()
            param.required = it.required
        }

        val headerParamAnnotation = element.getAnnotation(RequestHeader::class.java)
        headerParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true
            param = HeaderParameter()
            param.name = it.name
            if (param.name.isEmpty()) param.name = element.toString()
            param.required = it.required
        }

        val bodyParamAnnotation = element.getAnnotation(RequestBody::class.java)
        bodyParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true
            param = BodyParameter()
            param.required = it.required
        }

        return param
    }

    private fun addSwaggerAnnotation(element: VariableElement, param: Parameter) {
        // Check ApiParam annotation
        val apiParam = element.getAnnotation(ApiParam::class.java)
        apiParam?.let {
            param.isReadOnly = it.readOnly
            param.access = it.access
            param.allowEmptyValue = it.allowEmptyValue
            param.description = it.value
        }
    }

    private fun addDefinition(className: String) {
        // Check if already defined
        swagger.definitions?.get(className)?.let { return }

        // Create concrete definition

        val propertyMap = mutableMapOf<String, Property>()

        val typeElement = elementUtils.getTypeElement(className)
        ElementFilter.fieldsIn(typeElement.enclosedElements).forEach{
            // enum class cannot converted to swagger spec automatically.
            // See ApiParam.allowableValues
            if (elementUtils.getTypeElement(it.asType().toString())?.kind == ElementKind.ENUM) return@forEach

            val fieldProperty = getProperty(it.asType())
            propertyMap[it.toString()] = fieldProperty

            if(fieldProperty is RefProperty && fieldProperty.simpleRef != className) {
                addDefinition(fieldProperty.simpleRef)
            }
        }

        val objectProperty = ObjectProperty()
        objectProperty.name = className
        val model = PropertyBuilder.toModel(objectProperty)!!
        model.properties = propertyMap

        swagger.addDefinition(className, model)
    }

    override fun visitVariable(e: VariableElement?, p: Void?): Void? {
        // Get empty parameter
        val param = createParamBySpringAnnotation(e!!)
        addSwaggerAnnotation(e, param)
        val property = getProperty(e.asType())

        if (param is SerializableParameter) {
            when (property){
                is RefProperty -> throw Exception("Not serializable parameter")
                is ArrayProperty -> {
                    param.type = property.type
                    param.items = property
                }
                else -> {
                    param.type = property.type
                    param.format = property.format
                }
            }
        } else if (param is BodyParameter){
            param.schema = PropertyBuilder.toModel(property)
            if (property is RefProperty) {
                addDefinition(property.simpleRef)
            }
        }

        parameters.add(param)
        return super.visitVariable(e, p)
    }
}