package com.juntaki.springfennec

import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
class SpringfennecProcessor : AbstractProcessor() {
  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "AAAAAAAAAAAAAAAAAAA")
    return true
  }
}
