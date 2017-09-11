package com.juntaki.springfennec

import com.google.auto.service.AutoService
import io.swagger.annotations.Api
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.swagger.annotations.*", "org.springframework.web.bind.annotation.*")
class SpringfennecProcessor : AbstractProcessor() {
    var checked = false
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Only first round is enough.
        if (checked) return true
        checked = true

        // Check if annotation processor work
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Springfennec is running")

        // spec.json
        val printWriter = File("spec.json").printWriter()
         roundEnv.getElementsAnnotatedWith(Api::class.java).forEach {
            printWriter.println("ELEMENT: " + it.toString())
            it.enclosedElements.forEach {
                printWriter.println(" enclosed: " + it.toString())
                printWriter.println(" enclosed: " + it.toString() +
                        processingEnv.elementUtils.getAllAnnotationMirrors(it))
            }
        }
        printWriter.flush()
        printWriter.close()
        return true
    }
}