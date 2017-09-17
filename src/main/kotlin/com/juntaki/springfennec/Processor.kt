package com.juntaki.springfennec

import com.google.auto.service.AutoService
import io.swagger.annotations.Api
import io.swagger.models.Swagger
import io.swagger.util.Json
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
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
        val springfennecVisitor = Visitor(swagger)

        // Create spec.json
        roundEnv.getElementsAnnotatedWith(Api::class.java).forEach {
            it.accept(springfennecVisitor, swagger)
        }

        File("spec.json").printWriter().use {
            Json.mapper()!!.writeValue(it, swagger)
        }
        return true
    }
}
