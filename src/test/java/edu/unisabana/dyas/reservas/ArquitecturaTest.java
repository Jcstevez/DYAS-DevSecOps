package edu.unisabana.dyas.reservas;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Reglas de la arquitectura hexagonal escritas como pruebas (funciones de aptitud).
 * Salen de docs/adr/ADR-001-arquitectura-hexagonal.md y corren en cada build: si alguien rompe
 * una frontera del estilo, el pipeline se detiene.
 */
@AnalyzeClasses(packages = "edu.unisabana.dyas.reservas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    /** Regla 1: el dominio no conoce las capas externas. */
    @ArchTest
    static final ArchRule dominioNoDependeDeCapasExternas = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..aplicacion..", "..adaptadores..", "..configuracion..")
            .because("el dominio es el centro del hexágono y no debe cambiar cuando cambian las capas externas");

    /** Regla 2: el dominio es Java puro, sin frameworks. */
    @ArchTest
    static final ArchRule dominioNoDependeDeFrameworks = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..",
                    "jakarta.validation..", "tools.jackson..", "com.fasterxml.jackson..")
            .because("las reglas de negocio deben poder probarse y reutilizarse sin Spring ni JPA");

    /** Regla 3: la aplicación solo usa el dominio, sus propios puertos y Java estándar. */
    @ArchTest
    static final ArchRule aplicacionSoloDependeDelDominioYDeSusPuertos = classes()
            .that().resideInAPackage("..aplicacion..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..dominio..", "..aplicacion..", "java..")
            .because("los casos de uso hablan con el exterior solo a través de puertos");

    /**
     * Regla 4: los adaptadores dependen hacia adentro y no se conocen entre sí.
     * El paquete configuracion es la raíz de composición (conecta los casos de uso con los adaptadores),
     * por eso se le permite conocer todas las capas.
     */
    @ArchTest
    static final ArchRule adaptadoresAisladosEntreSi = onionArchitecture()
            .domainModels("..dominio..")
            .applicationServices("..aplicacion..")
            .adapter("web", "..adaptadores.entrada.web..")
            .adapter("persistencia", "..adaptadores.salida.persistencia..")
            .withOptionalLayers(true)
            .ignoreDependency(resideInAPackage("..configuracion.."), alwaysTrue());

    /**
     * Regla 5 (propia): el dominio no escribe logs.
     * Decidir como y donde se registran los eventos (por ejemplo la auditoria de cancelaciones, H-06 en
     * docs/seguridad.md) es de las capas externas; el dominio solo lanza excepciones o devuelve resultados.
     */
    @ArchTest
    static final ArchRule dominioNoUsaLibreriasDeLogs = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.slf4j..", "java.util.logging..", "org.apache.logging..")
            .because("el dominio no decide como ni donde se registran los eventos; eso es de las capas externas");}
