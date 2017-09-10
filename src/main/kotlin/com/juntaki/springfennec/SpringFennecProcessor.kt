package com.juntaki.springfennec

import com.google.auto.service.AutoService
import io.swagger.annotations.Api
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.swagger.annotations.*", "org.springframework.web.bind.annotation.*")
class SpringfennecProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Test")

        roundEnv.getElementsAnnotatedWith(Api::class.java).forEach {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "ELEMENT: " + it.toString())
            it.enclosedElements.forEach {
                processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, " enclosed: " + it.toString())
                processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, " enclosed: " +
                        processingEnv.elementUtils.getAllAnnotationMirrors(it))
            }
        }
        return true
    }
}