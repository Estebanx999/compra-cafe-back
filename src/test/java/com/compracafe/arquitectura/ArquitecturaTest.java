package com.compracafe.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.persistence.Entity;
import org.springframework.stereotype.Controller;

/**
 * Reglas de la arquitectura hexagonal. Ver la sección "Arquitectura" del README.
 *
 * Todas las reglas usan allowEmptyShould(true): hoy no existe ningún módulo y
 * ArchUnit, por defecto, falla cuando una regla no encuentra clases a las que
 * aplicarse. Con esta opción pasan con cero clases y empiezan a vigilar en
 * cuanto aparece la primera clase que cae dentro de su alcance.
 */
@AnalyzeClasses(packages = "com.compracafe", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String PAQUETE_MODULOS = "com.compracafe.modulos.";

    // 1. El dominio es Java puro.
    @ArchTest
    static final ArchRule dominioSinDependenciasDeFrameworkNiDeOtrasCapas = noClasses()
            .that().resideInAPackage("com.compracafe..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "com.compracafe..application..",
                    "com.compracafe..infrastructure..")
            .as("1. El dominio no depende de Spring, Jakarta Persistence, Hibernate, application ni infrastructure")
            .allowEmptyShould(true);

    // 2. application no conoce la infraestructura.
    @ArchTest
    static final ArchRule aplicacionNoDependeDeInfraestructura = noClasses()
            .that().resideInAPackage("com.compracafe..application..")
            .should().dependOnClassesThat().resideInAPackage("com.compracafe..infrastructure..")
            .as("2. La capa application no depende de infrastructure")
            .allowEmptyShould(true);

    // 3. Entre módulos solo se usa la capa application del otro módulo.
    @ArchTest
    static final ArchRule modulosSoloUsanLaCapaApplicationDeOtrosModulos = classes()
            .that().resideInAPackage("com.compracafe.modulos..")
            .should(soloUsarLaCapaApplicationDeOtrosModulos())
            .as("3. Un módulo solo usa de otro módulo su capa application (casos de uso y DTOs)")
            .allowEmptyShould(true);

    // 4. Controladores solo en infrastructure.web.
    @ArchTest
    static final ArchRule controladoresSoloEnInfrastructureWeb = classes()
            .that().areMetaAnnotatedWith(Controller.class)
            .should().resideInAPackage("com.compracafe..infrastructure.web..")
            .as("4. Los controladores solo están en infrastructure.web")
            .allowEmptyShould(true);

    // 5. Entidades JPA solo en infrastructure.persistence.
    @ArchTest
    static final ArchRule entidadesJpaSoloEnInfrastructurePersistence = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("com.compracafe..infrastructure.persistence..")
            .as("5. Las clases @Entity solo están en infrastructure.persistence")
            .allowEmptyShould(true);

    private static ArchCondition<JavaClass> soloUsarLaCapaApplicationDeOtrosModulos() {
        return new ArchCondition<>("usar de otros módulos solo su capa application") {
            @Override
            public void check(JavaClass clase, ConditionEvents events) {
                String moduloOrigen = modulo(clase);
                for (Dependency dependencia : clase.getDirectDependenciesFromSelf()) {
                    JavaClass destino = dependencia.getTargetClass().getBaseComponentType();
                    String moduloDestino = modulo(destino);
                    if (moduloDestino == null || moduloDestino.equals(moduloOrigen)) {
                        continue;
                    }
                    if (!capaDentroDelModulo(destino).equals("application")) {
                        events.add(SimpleConditionEvent.violated(dependencia,
                                "El módulo '" + moduloOrigen + "' usa la capa '" + capaDentroDelModulo(destino)
                                        + "' del módulo '" + moduloDestino + "' (solo se permite su capa application): "
                                        + dependencia.getDescription()));
                    }
                }
            }
        };
    }

    /** Nombre del módulo (com.compracafe.modulos.<modulo>...) o null si la clase no está en un módulo. */
    private static String modulo(JavaClass clase) {
        String paquete = clase.getPackageName();
        if (!paquete.startsWith(PAQUETE_MODULOS)) {
            return null;
        }
        String resto = paquete.substring(PAQUETE_MODULOS.length());
        int punto = resto.indexOf('.');
        return punto < 0 ? resto : resto.substring(0, punto);
    }

    /** Primera parte del paquete después del módulo: domain, application, infrastructure... */
    private static String capaDentroDelModulo(JavaClass clase) {
        String resto = clase.getPackageName().substring(PAQUETE_MODULOS.length());
        String[] partes = resto.split("\\.");
        return partes.length > 1 ? partes[1] : "(raíz del módulo)";
    }
}
