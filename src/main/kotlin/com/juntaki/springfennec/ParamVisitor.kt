package com.juntaki.springfennec

import io.swagger.annotations.ApiParam
import io.swagger.models.ArrayModel
import io.swagger.models.ComposedModel
import io.swagger.models.RefModel
import io.swagger.models.Swagger
import io.swagger.models.parameters.*
import io.swagger.models.properties.PropertyBuilder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
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
    private fun checkSerializable(e: VariableElement): Boolean {
        val serializable = elementUtils.getTypeElement("java.io.Serializable").asType()
        val tm = e.asType()
        return typeUtils.isAssignable(tm, serializable)
    }

    private fun DataTypeConvert(e: VariableElement): Pair<String, String?> {
        fun isAssignable(tm: TypeMirror, className: String): Boolean {
            return typeUtils.isAssignable(tm, elementUtils.getTypeElement(className).asType())
        }

        val tm = e.asType()

        // DateTime
        if (isAssignable(tm, "java.time.LocalDateTime") ||
                isAssignable(tm,"java.time.ZonedDateTime") ||
                isAssignable(tm,"java.time.OffsetDateTime") ||
                isAssignable(tm,"java.util.Date")
                )
            return Pair("string", "date-time")

        // Date
        if (isAssignable(tm, "java.time.LocalDate")) return Pair("string", "date")

        // Primitive types
        if (isAssignable(tm, "java.lang.Boolean"))
            return Pair("boolean", null)
        if (isAssignable(tm, "java.lang.Byte"))
            return Pair("string", "byte")
        if (isAssignable(tm, "java.lang.Integer"))
            return Pair("integer", "int32")
        if (isAssignable(tm, "java.lang.Long"))
            return Pair("integer", "int64")
        if (isAssignable(tm, "java.lang.Float"))
            return Pair("number", "float")
        if (isAssignable(tm, "java.lang.Double"))
            return Pair("number", "double")

        // String
        if (isAssignable(tm, "java.lang.String"))
            return Pair("string", null)

        // List
        // FIXME: How do I check List?
        val listRegex = Regex("java.util.List")
        if (listRegex.containsMatchIn(tm.toString()))
            return Pair("array", null)

        TODO("not implemented class: " + e.toString() + tm.toString()) // binary, password and the other classes
    }

    private fun springAnnotatedParam(e: VariableElement): Parameter {
        var param: Parameter
        var annotated = false
        //Default is query parameter.
        param = QueryParameter()

        val queryParamAnnotation = e.getAnnotation(RequestParam::class.java)
        queryParamAnnotation?.let {
            annotated = true

            val queryParam = QueryParameter()
            //queryParam.setDefaultValue(it.defaultValue)
            //if (!checkSerializable(e)) throw Exception("Query parameter must be serializable" + e.asType())
            val (type, format) = DataTypeConvert(e)
            queryParam.type = type
            queryParam.format = format
            // TODO: array
            param = queryParam
            param.name = it.name
            if (param.name.isEmpty()) param.name = e.toString()
            param.required = it.required
        }

        val pathParamAnnotation = e.getAnnotation(PathVariable::class.java)
        pathParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true

            val pathParam = PathParameter()
            //if (!checkSerializable(e)) throw Exception("Query parameter must be serializable")
            val (type, format) = DataTypeConvert(e)
            pathParam.type = type
            pathParam.format = format

            param = pathParam
            param.name = it.name
            if (param.name.isEmpty()) param.name = e.toString()
            param.required = it.required
        }

        val headerParamAnnotation = e.getAnnotation(RequestHeader::class.java)
        headerParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true

            val headerParam = HeaderParameter()
            //headerParam.setDefaultValue(it.defaultValue)
            val (type, format) = DataTypeConvert(e)
            headerParam.type = type
            headerParam.format = format

            param = headerParam
            param.name = it.name
            if (headerParam.name.isEmpty()) param.name = e.toString()
            param.required = it.required
        }

        val bodyParamAnnotation = e.getAnnotation(RequestBody::class.java)
        bodyParamAnnotation?.let {
            if (annotated) Exception("Parameter has multiple annotation")
            annotated = true

            val bodyParam = BodyParameter()
            bodyParam.schema = RefModel(e.asType().toString())
            // TODO: array, seriarizable params

            param = bodyParam
            param.required = it.required
        }

        return param
    }

    private fun addSwaggerAnnotation(e: VariableElement, param: Parameter) {
        // Check ApiParam annotation
        val apiParam = e.getAnnotation(ApiParam::class.java)
        apiParam?.let {
            param.isReadOnly = it.readOnly
            param.access = it.access
            param.allowEmptyValue = it.allowEmptyValue
            param.description = it.value
        }

    }

    override fun visitVariable(e: VariableElement?, p: Void?): Void? {
        // TODO: model definition
        val param = springAnnotatedParam(e!!)
        addSwaggerAnnotation(e, param)
        checkSerializable(e)
        parameters.add(param)
        return super.visitVariable(e, p)
    }
}