package com.juntaki.springfennec

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

        // Create spec.json
        roundEnv.rootElements.forEach {
            it.accept(FunctionVisitor(swagger, processingEnv.elementUtils, processingEnv.typeUtils), null)
        }

        File("spec.json").printWriter().use {
            Json.mapper()!!.writeValue(it, swagger)
        }
        return true
    }
}
