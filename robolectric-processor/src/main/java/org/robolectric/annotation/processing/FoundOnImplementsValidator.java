package org.robolectric.annotation.processing;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public abstract class FoundOnImplementsValidator extends Validator {

  protected AnnotationMirror imp;
  
  public FoundOnImplementsValidator(RoboModel model,
      ProcessingEnvironment env,
      String annotationType) {
    super(model, env, annotationType);
  }

  @Override
  public void init(Element elem, Element p) {
    super.init(elem, p);
    imp = model.getImplementsMirror(p);
    if (imp == null) {
      error('@' + annotationType.getSimpleName().toString() + " without @Implements");
    }
  }
  
  @Override
  final public Void visitVariable(VariableElement elem, Element parent) {
    return visitVariable(elem, RoboModel.typeVisitor.visit(parent));
  }
  
  public Void visitVariable(VariableElement elem, TypeElement parent) {
    return null;
  }

  @Override
  final public Void visitExecutable(ExecutableElement elem, Element parent) {
    return visitExecutable(elem, RoboModel.typeVisitor.visit(parent));
  }

  public Void visitExecutable(ExecutableElement elem, TypeElement parent) {
    return null;
  }
}
