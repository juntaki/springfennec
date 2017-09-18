package com.juntaki.springfennec

import io.swagger.annotations.ApiParam
import io.swagger.models.ComposedModel
import io.swagger.models.Model
import io.swagger.models.Swagger
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.parameters.RefParameter
import org.springframework.core.annotation.AnnotatedElementUtils
import javax.lang.model.element.Element
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementScanner8
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types


class ParamVisitor(
        private val swagger: Swagger,
        private val parameters: MutableList<Parameter>,
        private val elementUtils: Elements,
        private val typeUtils: Types
) : ElementScanner8<Void?, Void?>() {
    override fun visitVariable(e: VariableElement?, p: Void?): Void? {
        val param: Parameter
        // Check Serializable
        val serializable = elementUtils.getTypeElement("java.io.Serializable").asType()
        val tm = e!!.asType()
        if (typeUtils.isAssignable(tm, serializable)) {
            param = QueryParameter()
            param.name = e.toString()
        } else {
            val modelName = e.asType().toString()
            param = RefParameter(modelName)
            swagger.addDefinition(modelName, ComposedModel())
        }

        // Check ApiParam annotation
        val apiParam = e!!.getAnnotation(ApiParam::class.java)
        apiParam?.let {
            param.required = it.required
            param.isReadOnly = it.readOnly
            //param.name = it.name
            param.access = it.access
            param.allowEmptyValue = it.allowEmptyValue
            param.description = it.value
        }

        parameters.add(param)
        return super.visitVariable(e, p)
    }
}