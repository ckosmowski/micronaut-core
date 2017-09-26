package org.particleframework.annotation.processing;

import javax.inject.Qualifier;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;

class AnnotationUtils {

    private static final List<String> IGNORED_ANNOTATIONS = Arrays.asList("Retention", "Documented", "Target");

    private final Elements elementUtils;

    AnnotationUtils(Elements elementUtils) {
        this.elementUtils = elementUtils;
    }

    boolean hasStereotype(Element classElement, Class<? extends Annotation> stereotype) {
        return hasStereotype(classElement, stereotype.getName());
    }

    boolean hasStereotype(Element classElement, String... stereotypes) {
        return hasStereotype(classElement, Arrays.asList(stereotypes));
    }

    boolean hasStereotype(Element element, List<String> stereotypes) {
        if (element == null) {
            return false;
        }
        if (stereotypes.contains(element.toString())) {
            return true;
        }
        List<? extends AnnotationMirror> annotationMirrors = elementUtils.getAllAnnotationMirrors(element);
        for (AnnotationMirror ann : annotationMirrors) {
            DeclaredType annotationType = ann.getAnnotationType();
            String annotationTypeString = annotationType.toString();
            if (stereotypes.contains(annotationTypeString)) {
                return true;
            } else if (!IGNORED_ANNOTATIONS.contains(
                    annotationType.asElement().getSimpleName().toString())) {
                if (hasStereotype(annotationType.asElement(), stereotypes)) {
                    return true;
                }
            }
        }
        if (element instanceof ExecutableElement) {
            ExecutableElement executableElement = (ExecutableElement) element;
            if (findAnnotation(element, Override.class) != null) {
                ExecutableElement overridden = findOverriddenMethod(executableElement);
                if (overridden != null) {
                    return hasStereotype(overridden, stereotypes);
                }
            }
        }
        return false;
    }

    private ExecutableElement findOverriddenMethod(ExecutableElement executableElement) {
        ExecutableElement overridden = null;
        Element enclosingElement = executableElement.getEnclosingElement();
        if (enclosingElement instanceof TypeElement) {
            TypeElement thisType = (TypeElement) enclosingElement;
            TypeMirror superMirror = thisType.getSuperclass();
            TypeElement supertype = superMirror instanceof TypeElement ? (TypeElement) superMirror : null;
            while (supertype != null && !supertype.toString().equals(Object.class.getName())) {
                Optional<ExecutableElement> result = findOverridden(executableElement, supertype);
                if (result.isPresent()) {
                    overridden = result.get();
                    break;
                }
                else {
                    overridden = findOverriddenInterfaceMethod(executableElement, supertype);

                }
                supertype = (TypeElement) supertype.getSuperclass();
            }
            if (overridden == null) {
                overridden = findOverriddenInterfaceMethod(executableElement, thisType);
            }
        }
        return overridden;
    }

    /**
     * Finds an annotation for the given class element and stereotypes. A stereotype is a meta annotation on another annotation.
     *
     * @param element     The element to search
     * @param stereotypes The stereotypes to look for
     * @return An array of matching {@link AnnotationMirror}
     */
    AnnotationMirror[] findAnnotationsWithStereotype(Element element, String... stereotypes) {
        if (element == null) {
            return new AnnotationMirror[0];
        }
        List<String> stereoTypeList = Arrays.asList(stereotypes);
        List<AnnotationMirror> annotationMirrorList = new ArrayList<>();
        List<? extends AnnotationMirror> annotationMirrors = elementUtils.getAllAnnotationMirrors(element);
        for (AnnotationMirror ann : annotationMirrors) {
            DeclaredType annotationType = ann.getAnnotationType();
            String annotationTypeString = annotationType.toString();
            if (stereoTypeList.contains(annotationTypeString)) {
                annotationMirrorList.add(ann);
            } else {
                Element annotationElement = annotationType.asElement();
                if (!IGNORED_ANNOTATIONS.contains(
                        annotationElement.getSimpleName().toString())) {
                    if (hasStereotype(annotationElement, stereotypes)) {
                        annotationMirrorList.add(ann);
                    }
                }
            }
        }
        if (element instanceof ExecutableElement && annotationMirrorList.isEmpty()) {
            ExecutableElement executableElement = (ExecutableElement) element;
            if (findAnnotation(element, Override.class) != null) {
                ExecutableElement overridden = findOverriddenMethod(executableElement);
                if (overridden != null) {
                    return findAnnotationsWithStereotype(overridden, stereotypes);
                }
            }
        }
        return annotationMirrorList.toArray(new AnnotationMirror[annotationMirrorList.size()]);
    }

    /**
     * Finds an annotation for the given class element and stereotype. A stereotype is a meta annotation on another annotation.
     *
     * @param element    The element to search
     * @param stereotype The stereotype to look for
     * @return An array of matching {@link AnnotationMirror}
     */
    Optional<AnnotationMirror> findAnnotationWithStereotype(Element element, Class<? extends Annotation> stereotype) {
        return findAnnotationWithStereotype(element, stereotype.getName());
    }

    /**
     * Finds an annotation for the given class element and stereotype. A stereotype is a meta annotation on another annotation.
     *
     * @param element    The element to search
     * @param stereotype The stereotype to look for
     * @return An array of matching {@link AnnotationMirror}
     */
    Optional<AnnotationMirror> findAnnotationWithStereotype(Element element, String stereotype) {
        List<? extends AnnotationMirror> annotationMirrors = elementUtils.getAllAnnotationMirrors(element);
        for (AnnotationMirror ann : annotationMirrors) {
            DeclaredType annotationType = ann.getAnnotationType();
            if (stereotype.equals(annotationType.toString())) {
                return Optional.of(ann);
            } else if (!Arrays.asList("Retention", "Documented", "Target").contains(annotationType.asElement().getSimpleName().toString())) {
                if (findAnnotationWithStereotype(annotationType.asElement(), stereotype).isPresent()) {
                    return Optional.of(ann);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds an annotation for the given element by type. A stereotype is a meta annotation on another annotation.
     *
     * @param element        The element to search
     * @param annotationType The stereotype to look for
     * @return An array of matching {@link AnnotationMirror}
     */
    Optional<AnnotationMirror> findAnnotation(Element element, Class<? extends Annotation> annotationType) {
        return findAnnotationWithStereotype(element, annotationType.getName());
    }

    /**
     * Finds an annotation for the given class element and stereotype. A stereotype is a meta annotation on another annotation.
     *
     * @param element    The element to search
     * @param stereotype The stereotype to look for
     * @return An array of matching {@link AnnotationMirror}
     */
    Optional<AnnotationMirror> findAnnotation(Element element, String stereotype) {
        List<? extends AnnotationMirror> annotationMirrors = elementUtils.getAllAnnotationMirrors(element);
        for (AnnotationMirror ann : annotationMirrors) {
            DeclaredType annotationType = ann.getAnnotationType();
            if (stereotype.equals(annotationType.toString())) {
                return Optional.of(ann);
            } else if (!Arrays.asList("Retention", "Documented", "Target").contains(annotationType.asElement().getSimpleName().toString())) {
                Optional<AnnotationMirror> found = findAnnotation(annotationType.asElement(), stereotype);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    Object resolveQualifier(Element element) {
        Optional<AnnotationMirror> qualifier = findAnnotationWithStereotype(element, Qualifier.class);
        return qualifier.map(val -> val.getAnnotationType().toString()).orElse(null);
    }

    // TODO this needs a test
    Optional<String> getAnnotationAttributeValue(AnnotationMirror annMirror, String attributeName) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> annValues = annMirror.getElementValues();
        if (annValues.isEmpty()) {
            return Optional.empty();
        }
        Optional<? extends ExecutableElement> executableElement = annValues.keySet().stream()
                .filter(execElem -> execElem.getSimpleName().toString().equals(attributeName))
                .findFirst();

        return Optional.ofNullable(
                executableElement.map(executableElement1 -> annValues.get(executableElement1).getValue().toString()).orElse(null)
        );
    }

    boolean isAttributeTrue(Element element, String annotationType, String attributeName) {
        return findAnnotation(element, annotationType)
                .map(annotationMirror -> {
                            Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
                            Optional<? extends ExecutableElement> foundElement = values.keySet().stream()
                                    .filter(execElem -> execElem.getSimpleName().toString().equals(attributeName))
                                    .findFirst();
                            return foundElement.map(exec ->
                                    {
                                        AnnotationValue annotationValue = values.get(exec);
                                        if (annotationValue != null) {
                                            Object value = annotationValue.getValue();
                                            if (value instanceof Boolean) {
                                                return (Boolean) value;
                                            }
                                        }
                                        return false;
                                    }
                            ).orElse(false);

                        }
                ).orElse(false);
    }

    private ExecutableElement findOverriddenInterfaceMethod(ExecutableElement executableElement, TypeElement thisType) {

        ExecutableElement overridden = null;
        TypeElement supertype = thisType;
        while (supertype != null && !supertype.toString().equals(Object.class.getName())) {
            List<? extends TypeMirror> interfaces = supertype.getInterfaces();

            for (TypeMirror anInterface : interfaces) {
                if (anInterface instanceof DeclaredType) {
                    DeclaredType iElement = (DeclaredType) anInterface;
                    Optional<ExecutableElement> result = findOverridden(executableElement, (TypeElement) iElement.asElement());
                    if (result.isPresent()) {
                        overridden = result.get();
                        break;
                    } else {
                        overridden = findOverriddenInterfaceMethod(executableElement, (TypeElement) iElement.asElement());
                        if (overridden != null) break;
                    }
                }
            }
            TypeMirror superMirror = supertype.getSuperclass();
            if (superMirror instanceof DeclaredType) {
                supertype = (TypeElement) ((DeclaredType) superMirror).asElement();
            } else {
                break;
            }
        }
        return overridden;
    }

    private Optional<ExecutableElement> findOverridden(ExecutableElement executableElement, TypeElement supertype) {
        Stream<? extends Element> elements = supertype.getEnclosedElements().stream();
        return elements.filter(el -> el.getKind() == ElementKind.METHOD && el.getEnclosingElement().equals(supertype))
                .map(el -> (ExecutableElement) el)
                .filter(method -> elementUtils.overrides(executableElement, method, (TypeElement) method.getEnclosingElement()))
                .findFirst();
    }
}
