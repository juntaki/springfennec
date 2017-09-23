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
import io.swagger.models.Swagger
import io.swagger.util.Json
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.swagger.annotations.*", "org.springframework.web.bind.annotation.*")
class Processor : AbstractProcessor() {
    var checked = false
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Only first round is enough.
        if (checked) return true
        checked = true

        // Check if annotation processor work
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Springfennec is running")

        val swagger = Swagger()
        val propertyUtil = PropertyUtil( processingEnv.elementUtils, processingEnv.typeUtils)

        // Create spec.json
        roundEnv.rootElements.forEach {
            it.accept(FunctionVisitor(swagger,propertyUtil), null)
        }

        File("spec.json").printWriter().use {
            Json.mapper()!!.writeValue(it, swagger)
        }
        return true
    }
}
