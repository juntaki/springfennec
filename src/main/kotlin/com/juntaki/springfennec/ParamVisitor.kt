/*
 *    Copyright 2017 juntaki
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.juntaki.springfennec

import com.juntaki.springfennec.util.PropertyUtil
import io.swagger.annotations.ApiParam
import io.swagger.models.Swagger
import io.swagger.models.parameters.*
import io.swagger.models.properties.*
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementScanner8


class ParamVisitor(
        private val swagger: Swagger,
        private val parameters: MutableList<Parameter>,
        private val propertyUtil: PropertyUtil
) : ElementScanner8<Void?, Void?>() {
    private fun createParamBySpringAnnotation(element: VariableElement): Parameter {
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
        val propertyMap = mutableMapOf<String, Property>()

        fun _addDefinition(e: VariableElement) {
            val fieldProperty = propertyUtil.getProperty(e.asType())!!
            propertyMap[e.toString()] = fieldProperty

            if (fieldProperty is RefProperty && fieldProperty.simpleRef != className) {
                addDefinition(fieldProperty.simpleRef)
            }
        }
        // Check if already defined
        swagger.definitions?.get(className)?.let { return }

        // Create concrete definition

        propertyUtil.doEachClassField(className, ::_addDefinition)

        val objectProperty = ObjectProperty()
        objectProperty.name = className
        val model = PropertyBuilder.toModel(objectProperty)!!
        model.properties = propertyMap

        swagger.addDefinition(className, model)
    }

    override fun visitVariable(e: VariableElement?, p: Void?): Void? {
        // Ignore AuthenticationPrincipal parameter, it will set by spring security.
        e!!.annotationMirrors.forEach {
            if (it.toString() == "@org.springframework.security.core.annotation.AuthenticationPrincipal")
                return super.visitVariable(e, p)
        }

        // Get empty parameter
        val param = createParamBySpringAnnotation(e)
        addSwaggerAnnotation(e, param)
        val property = propertyUtil.getProperty(e.asType())!!

        if (param is SerializableParameter) {
            when (property) {
                is RefProperty -> null // Ignore
                is ArrayProperty -> {
                    param.type = property.type
                    param.items = property
                }
                else -> {
                    param.type = property.type
                    param.format = property.format
                }
            }
        } else if (param is BodyParameter) {
            param.schema = PropertyBuilder.toModel(property)
            if (property is RefProperty) {
                addDefinition(property.simpleRef)
            }
        }

        parameters.add(param)
        return super.visitVariable(e, p)
    }
}