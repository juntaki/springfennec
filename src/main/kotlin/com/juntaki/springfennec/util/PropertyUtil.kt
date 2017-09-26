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

package com.juntaki.springfennec.util

import io.swagger.models.properties.*
import javax.lang.model.element.ElementKind
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class PropertyUtil(
        private val elementUtils: Elements,
        private val typeUtils: Types
) {
    fun doEachClassField(className: String, fn: (VariableElement) -> Unit) {
        val typeElement = elementUtils.getTypeElement(className) ?: return
        ElementFilter.fieldsIn(typeElement.enclosedElements).forEach {
            it ?: return@forEach
            // enum class cannot converted to swagger spec automatically.
            // ApiParam.allowableValues should be used, but not implemented.
            if (elementUtils.getTypeElement(it.asType().toString())?.kind == ElementKind.ENUM) return@forEach
            fn(it)
        }
    }

    // return null only if tm is Void type
    fun getProperty(tm: TypeMirror): Property? {
        fun isAssignable(tm: TypeMirror, className: String): Boolean {
            return typeUtils.isAssignable(tm, elementUtils.getTypeElement(className).asType())
        }

        fun isSameClassName(tm: TypeMirror, className: String): Boolean {
            return tm.toString() == className
        }

        if (isAssignable(tm, "java.time.LocalDateTime") ||
                isAssignable(tm, "java.time.ZonedDateTime") ||
                isAssignable(tm, "java.time.OffsetDateTime") ||
                isAssignable(tm, "java.util.Date") ||
                isSameClassName(tm, "org.joda.time.DateTime")
                ) {
            return DateTimeProperty()
        }
        if (isAssignable(tm, "java.time.LocalDate")) {
            return DateProperty()
        }
        if (isAssignable(tm, "java.lang.Boolean")) {
            return BooleanProperty()
        }
        if (isAssignable(tm, "java.lang.Byte")) {
            return ByteArrayProperty()
        }
        if (isAssignable(tm, "java.lang.Integer")) {
            return IntegerProperty()
        }
        if (isAssignable(tm, "java.lang.Long")) {
            return LongProperty()
        }
        if (isAssignable(tm, "java.lang.Float")) {
            return FloatProperty()
        }
        if (isAssignable(tm, "java.lang.Double")) {
            return DoubleProperty()
        }
        if (isAssignable(tm, "java.lang.String")) {
            return StringProperty()
        }
        if (isSameClassName(tm, "org.springframework.web.multipart.MultipartFile")) {
            return FileProperty()
        }

        // Array
        val listRegex = Regex("""^java.util.List|^java.util.ArrayList""")
        if (tm is DeclaredType && listRegex.containsMatchIn(tm.toString())) {
            val arrayProperty = ArrayProperty()
            arrayProperty.items = getProperty(tm.typeArguments[0])
            return arrayProperty
        }
        if (tm is ArrayType) {
            val arrayProperty = ArrayProperty()
            arrayProperty.items = getProperty(tm.componentType)
            return arrayProperty
        }

        // Map
        val mapRegex = Regex("""^java.util.Map""")
        if (tm is DeclaredType && mapRegex.containsMatchIn(tm.toString())) {
            if (!isAssignable(tm.typeArguments[0], "java.lang.String")) {
                TODO("Not implemented.")
                // TODO: Read JSON Schema and https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#responses-definitions-object
            }
            val mapProperty = MapProperty()
            mapProperty.additionalProperties = getProperty(tm.typeArguments[1])
            return mapProperty

        }

        // Void
        if (tm.toString() == "java.lang.Void") {
            return null
        }

        // Class
        val refProperty = RefProperty()
        refProperty.`$ref` = tm.toString()
        return refProperty
    }
}