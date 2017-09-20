package com.juntaki.springfennec

import io.swagger.annotations.ApiParam
import io.swagger.models.RefModel
import io.swagger.models.Swagger
import io.swagger.models.parameters.*
import io.swagger.models.properties.*
import io.swagger.util.ParameterProcessor
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
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
                isAssignable(tm,"java.util.Date") ){
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

        TODO("map")
    }

    private fun springAnnotatedParam(element :VariableElement): Parameter {
        var param: Parameter
        var annotated = false
        //Default is query parameter. TODO: Check Spring Documentation.
        param = QueryParameter()

        val queryParamAnnotation = element.getAnnotation(RequestParam::class.java)
        queryParamAnnotation?.let {
            annotated = true
            param = QueryParameter()
        }

        val pathParamAnnotation = element.getAnnotation(PathVariable::class.java)
        pathParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true
            param = PathParameter()
        }

        val headerParamAnnotation = element.getAnnotation(RequestHeader::class.java)
        headerParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true
            param = HeaderParameter()
        }

        val bodyParamAnnotation = element.getAnnotation(RequestBody::class.java)
        bodyParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true
            param = BodyParameter()
        }

        return param
    }

//    private fun addSwaggerAnnotation(param: Parameter) {
//        // Check ApiParam annotation
//        val apiParam = element.getAnnotation(ApiParam::class.java)
//        apiParam?.let {
//            param.isReadOnly = it.readOnly
//            param.access = it.access
//            param.allowEmptyValue = it.allowEmptyValue
//            param.description = it.value
//        }
//    }

    override fun visitVariable(e: VariableElement?, p: Void?): Void? {
        // Get empty parameter
        val param = springAnnotatedParam(e!!)
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
            val model = PropertyBuilder.toModel(property)
            param.schema = model
            if (property is RefProperty) swagger.addDefinition(property.`$ref`, model)
        }

        println(e.asType())
        println(property)

        parameters.add(param)
        return super.visitVariable(e, p)
    }
}